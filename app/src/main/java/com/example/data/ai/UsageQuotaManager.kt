package com.example.data.ai

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.Keep
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Keep
@IgnoreExtraProperties
data class UsageQuotaDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateIso: String = "",
    val architectUsed: Int = 0,
    val visionUsed: Int = 0,
    val coachUsed: Int = 0,
    val bonusGraceCredits: Int = 0,
    val graceClaimed: Boolean = false,
    val ecoLiteMode: Boolean = false,
    val fallbackCascadeCount: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class QuotaSnapshot(
    val dateIso: String = "",
    val architectUsed: Int = 0,
    val visionUsed: Int = 0,
    val coachUsed: Int = 0,
    val bonusGraceCredits: Int = 0,
    val graceClaimed: Boolean = false,
    val ecoLiteMode: Boolean = false,
    val fallbackCascadeCount: Int = 0,
    val isSignedIn: Boolean = false,
    val userDisplayName: String? = null,
    val userEmail: String? = null,
    val hasByokKey: Boolean = false,
    val cloudSyncError: String? = null
) {
    val baseDailyLimit: Int
        get() = if (isSignedIn) AiModelConfig.STARTER_DAILY_LIMIT else AiModelConfig.GUEST_DAILY_LIMIT

    val effectiveDailyLimit: Int
        get() = baseDailyLimit + bonusGraceCredits

    val totalUsed: Int
        get() = architectUsed + visionUsed + coachUsed

    val remainingCredits: Int
        get() = (effectiveDailyLimit - totalUsed).coerceAtLeast(0)

    val usageFraction: Float
        get() = if (effectiveDailyLimit <= 0) 1f else (totalUsed.toFloat() / effectiveDailyLimit.toFloat()).coerceIn(0f, 1f)

    val isExhausted: Boolean
        get() = !hasByokKey && remainingCredits <= 0

    val isSoftNudgeActive: Boolean
        get() = !hasByokKey && !isExhausted && usageFraction >= AiModelConfig.SOFT_NUDGE_THRESHOLD

    val shouldPreferEcoLite: Boolean
        get() = !hasByokKey && (ecoLiteMode || usageFraction >= AiModelConfig.SOFT_NUDGE_THRESHOLD)

    val canClaimGraceToday: Boolean
        get() = !graceClaimed && !hasByokKey

    fun resolveExecutionTier(forceEcoLite: Boolean = false): AiExecutionTier = when {
        hasByokKey -> AiExecutionTier.BYOK_DIRECT
        isExhausted -> AiExecutionTier.LOCAL_ENGINE
        forceEcoLite || shouldPreferEcoLite -> AiExecutionTier.ECONOMY_LITE
        else -> AiExecutionTier.PRIMARY_CLOUD
    }

    fun formattedResetCountdown(context: Context): String {
        val now = Calendar.getInstance()
        val nextMidnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMs = (nextMidnight.timeInMillis - now.timeInMillis).coerceAtLeast(60_000L)
        val hours = diffMs / (1000L * 60L * 60L)
        val minutes = (diffMs / (1000L * 60L)) % 60L
        return context.getString(R.string.ai_reset_countdown_format, hours, minutes)
    }
}

/**
 * Hybrid Quota Ledger combining:
 * 1) Firestore source of truth at `/users/{userId}/usageQuota/daily` (when signed in with Google)
 * 2) Local SharedPreferences cache mirror for instant cold-start badges & Guest allowance
 * 3) Automatic daily rollover preserving ecoLiteMode + Mutex-serialized deduct-with-refund
 */
class UsageQuotaManager internal constructor(
    context: Context,
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private constructor(context: Context) : this(
        context = context.applicationContext,
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutationMutex = Mutex()
    private var snapshotJob: Job? = null

    private val _quota = MutableStateFlow(loadLocalMirror())
    val quota: StateFlow<QuotaSnapshot> = _quota.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null && user.uid.isNotBlank()) {
            attachCloudListener(user.uid)
        } else {
            detachCloudListener()
            _quota.value = loadLocalMirror()
        }
    }

    init {
        ensureLocalRollover()
        auth.addAuthStateListener(authListener)
        auth.currentUser?.uid?.takeIf { it.isNotBlank() }?.let { uid ->
            attachCloudListener(uid)
        }
    }

    fun getByokApiKey(): String = prefs.getString(KEY_BYOK_API_KEY, "")?.trim().orEmpty()

    fun setByokApiKey(rawKey: String?) {
        val cleaned = rawKey?.trim().orEmpty()
        prefs.edit().putString(KEY_BYOK_API_KEY, cleaned).apply()
        _quota.value = _quota.value.copy(hasByokKey = cleaned.isNotBlank())
    }

    fun resolveExecutionTier(forceEcoLite: Boolean = false): AiExecutionTier =
        _quota.value.resolveExecutionTier(forceEcoLite)

    fun detachCloudListener() {
        snapshotJob?.cancel()
        snapshotJob = null
    }

    private fun dailyDocRef(uid: String) =
        db.collection("users").document(uid)
            .collection("usageQuota").document(DAILY_DOC_ID)

    private fun attachCloudListener(uid: String) {
        detachCloudListener()
        val docRef = dailyDocRef(uid)
        val path = docRef.path

        val user = auth.currentUser
        _quota.value = _quota.value.copy(
            isSignedIn = true,
            userDisplayName = user?.displayName,
            userEmail = user?.email,
            cloudSyncError = null
        )

        snapshotJob = scope.launch {
            docRef.snapshots()
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    _quota.value = _quota.value.copy(
                        cloudSyncError = firestoreError?.localizedMessage ?: error.localizedMessage
                    )
                }
                .collect { snapshot ->
                    val today = todayIso()
                    if (!snapshot.exists()) {
                        mutationMutex.withLock {
                            initializeRemoteDailyDoc(uid, today)
                        }
                    } else {
                        val doc = snapshot.toObject(
                            UsageQuotaDoc::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                        if (doc != null) {
                            if (doc.dateIso != today) {
                                mutationMutex.withLock {
                                    performRemoteDailyRollover(uid, today, doc.ecoLiteMode)
                                }
                            } else {
                                persistToLocalMirror(doc)
                                val currentUser = auth.currentUser
                                _quota.value = QuotaSnapshot(
                                    dateIso = doc.dateIso,
                                    architectUsed = doc.architectUsed,
                                    visionUsed = doc.visionUsed,
                                    coachUsed = doc.coachUsed,
                                    bonusGraceCredits = doc.bonusGraceCredits,
                                    graceClaimed = doc.graceClaimed,
                                    ecoLiteMode = doc.ecoLiteMode,
                                    fallbackCascadeCount = doc.fallbackCascadeCount,
                                    isSignedIn = currentUser != null,
                                    userDisplayName = currentUser?.displayName,
                                    userEmail = currentUser?.email,
                                    hasByokKey = getByokApiKey().isNotBlank(),
                                    cloudSyncError = null
                                )
                            }
                        }
                    }
                }
        }
    }

    /**
     * Ensures the remote `/users/{uid}/usageQuota/daily` document exists and matches [today]
     * before executing partial update mutations (prevents NOT_FOUND or stale-date rule denials).
     */
    private suspend fun ensureRemoteDailyDocReady(uid: String, today: String): UsageQuotaDoc? {
        val docRef = dailyDocRef(uid)
        val path = docRef.path
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                initializeRemoteDailyDoc(uid, today)
                val created = docRef.get().await()
                created.toObject(
                    UsageQuotaDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else {
                val existing = snap.toObject(
                    UsageQuotaDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (existing != null && existing.dateIso != today) {
                    performRemoteDailyRollover(uid, today, existing.ecoLiteMode)
                    existing.copy(
                        dateIso = today,
                        architectUsed = 0,
                        visionUsed = 0,
                        coachUsed = 0,
                        bonusGraceCredits = 0,
                        graceClaimed = false,
                        fallbackCascadeCount = 0
                    )
                } else {
                    existing
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }

    private suspend fun initializeRemoteDailyDoc(uid: String, today: String): Result<Unit> {
        val docRef = dailyDocRef(uid)
        val path = docRef.path
        return try {
            val local = _quota.value
            val payload = mapOf(
                "userId" to uid,
                "dateIso" to today,
                "architectUsed" to local.architectUsed.coerceIn(0, 500),
                "visionUsed" to local.visionUsed.coerceIn(0, 500),
                "coachUsed" to local.coachUsed.coerceIn(0, 500),
                "bonusGraceCredits" to 0,
                "graceClaimed" to false,
                "ecoLiteMode" to local.ecoLiteMode,
                "fallbackCascadeCount" to local.fallbackCascadeCount.coerceIn(0, 500),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    private suspend fun performRemoteDailyRollover(
        uid: String,
        today: String,
        preservedEcoLiteMode: Boolean
    ): Result<Unit> {
        val docRef = dailyDocRef(uid)
        val path = docRef.path
        return try {
            val updateMap = mapOf(
                "dateIso" to today,
                "architectUsed" to 0,
                "visionUsed" to 0,
                "coachUsed" to 0,
                "bonusGraceCredits" to 0,
                "graceClaimed" to false,
                "fallbackCascadeCount" to 0,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updateMap).await()
            resetLocalCountersForDate(today, preservedEcoLiteMode)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /**
     * Alias matching the architecture playbook naming convention.
     */
    suspend fun recordInteraction(feature: AiFeatureType, recordCascade: Boolean = false): Boolean =
        tryConsumeCredit(feature, recordCascade)

    /**
     * Atomically checks quota and reserves 1 credit before a cloud AI call.
     * Uses an atomic Firestore transaction when signed in (with local mirror fallback offline).
     * Call [refundInteraction] if the cloud call fails or times out.
     */
    suspend fun tryConsumeCredit(feature: AiFeatureType, recordCascade: Boolean = false): Boolean =
        mutationMutex.withLock {
            ensureLocalRollover()
            val current = _quota.value
            if (current.hasByokKey) return@withLock true
            if (current.remainingCredits <= 0) return@withLock false

            val uid = auth.currentUser?.uid
            if (!uid.isNullOrBlank()) {
                val txOutcome = runAtomicUsageConsumeTransaction(uid, feature, recordCascade)
                if (txOutcome != null) {
                    val (allowed, syncedSnapshot) = txOutcome
                    if (syncedSnapshot != null) {
                        _quota.value = syncedSnapshot
                        saveSnapshotToLocalPrefs(syncedSnapshot)
                    }
                    return@withLock allowed
                }
            }

            // Guest mode or offline fallback -> update local mirror directly
            val updated = applyFeatureDelta(current, feature, delta = 1, cascadeDelta = if (recordCascade) 1 else 0)
            _quota.value = updated
            saveSnapshotToLocalPrefs(updated)
            true
        }

    /**
     * Atomically refunds a previously reserved credit when a cloud call fails or times out.
     */
    suspend fun refundInteraction(feature: AiFeatureType) = mutationMutex.withLock {
        val current = _quota.value
        if (current.hasByokKey) return@withLock

        val uid = auth.currentUser?.uid
        if (!uid.isNullOrBlank()) {
            val syncedSnapshot = runAtomicUsageRefundTransaction(uid, feature)
            if (syncedSnapshot != null) {
                _quota.value = syncedSnapshot
                saveSnapshotToLocalPrefs(syncedSnapshot)
                return@withLock
            }
        }

        val updated = applyFeatureDelta(current, feature, delta = -1, cascadeDelta = 0)
        if (updated == current) return@withLock
        _quota.value = updated
        saveSnapshotToLocalPrefs(updated)
    }

    suspend fun refundCredit(feature: AiFeatureType) = refundInteraction(feature)

    suspend fun recordFallbackCascade() = mutationMutex.withLock {
        val current = _quota.value
        val uid = auth.currentUser?.uid
        if (!uid.isNullOrBlank()) {
            val syncedSnapshot = runAtomicCascadeTransaction(uid)
            if (syncedSnapshot != null) {
                _quota.value = syncedSnapshot
                saveSnapshotToLocalPrefs(syncedSnapshot)
                return@withLock
            }
        }

        val updated = current.copy(
            fallbackCascadeCount = (current.fallbackCascadeCount + 1).coerceAtMost(500)
        )
        _quota.value = updated
        saveSnapshotToLocalPrefs(updated)
    }

    /**
     * Grants one-time daily +3 Grace Credits using an atomic Firestore transaction when signed in
     * (prevents concurrent or multi-device double claims) and mirrors locally.
     */
    suspend fun claimDailyGraceCredits(): Boolean = mutationMutex.withLock {
        ensureLocalRollover()
        val current = _quota.value
        if (current.graceClaimed || current.hasByokKey) return@withLock false

        val uid = auth.currentUser?.uid
        if (!uid.isNullOrBlank()) {
            val today = todayIso()
            val ready = ensureRemoteDailyDocReady(uid, today)
            if (ready != null) {
                val docRef = dailyDocRef(uid)
                val path = docRef.path
                try {
                    val granted = db.runTransaction { transaction ->
                        val snap = transaction.get(docRef)
                        if (!snap.exists()) return@runTransaction false
                        val remoteDate = snap.getString("dateIso") ?: ""
                        val alreadyClaimed = snap.getBoolean("graceClaimed") ?: false
                        if (remoteDate != today || alreadyClaimed) {
                            return@runTransaction false
                        }
                        transaction.update(
                            docRef,
                            mapOf(
                                "graceClaimed" to true,
                                "bonusGraceCredits" to AiModelConfig.DAILY_GRACE_CREDITS,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        true
                    }.await()

                    if (granted) {
                        val updated = current.copy(
                            dateIso = today,
                            graceClaimed = true,
                            bonusGraceCredits = AiModelConfig.DAILY_GRACE_CREDITS
                        )
                        _quota.value = updated
                        saveSnapshotToLocalPrefs(updated)
                    } else {
                        val synced = current.copy(
                            dateIso = today,
                            graceClaimed = true
                        )
                        _quota.value = synced
                        saveSnapshotToLocalPrefs(synced)
                    }
                    return@withLock granted
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.UPDATE, path)
                    Log.w(TAG, "Grace credit transaction fell back to local mirror: ${e.message}")
                }
            }
        }

        val updated = current.copy(
            graceClaimed = true,
            bonusGraceCredits = AiModelConfig.DAILY_GRACE_CREDITS
        )
        _quota.value = updated
        saveSnapshotToLocalPrefs(updated)
        true
    }

    /**
     * Persists user's Eco Lite Mode preference via an atomic Firestore transaction when signed in
     * (preserved across daily rollovers) and mirrors locally.
     */
    suspend fun setEcoLiteMode(enabled: Boolean) = mutationMutex.withLock {
        ensureLocalRollover()
        val current = _quota.value
        if (current.ecoLiteMode == enabled) return@withLock

        val updated = current.copy(ecoLiteMode = enabled)
        _quota.value = updated
        saveSnapshotToLocalPrefs(updated)

        val uid = auth.currentUser?.uid
        if (!uid.isNullOrBlank()) {
            val today = todayIso()
            val ready = ensureRemoteDailyDocReady(uid, today)
            if (ready != null) {
                val docRef = dailyDocRef(uid)
                val path = docRef.path
                try {
                    db.runTransaction { transaction ->
                        val snap = transaction.get(docRef)
                        if (!snap.exists()) return@runTransaction null
                        val currentRemoteEco = snap.getBoolean("ecoLiteMode") ?: false
                        val remoteDate = snap.getString("dateIso") ?: ""
                        if (remoteDate == today && currentRemoteEco != enabled) {
                            transaction.update(
                                docRef,
                                mapOf(
                                    "ecoLiteMode" to enabled,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                        }
                        null
                    }.await()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.UPDATE, path)
                }
            }
        }
    }

    private suspend fun runAtomicUsageConsumeTransaction(
        uid: String,
        feature: AiFeatureType,
        recordCascade: Boolean
    ): Pair<Boolean, QuotaSnapshot?>? {
        val today = todayIso()
        ensureRemoteDailyDocReady(uid, today) ?: return null
        val docRef = dailyDocRef(uid)
        val path = docRef.path
        return try {
            db.runTransaction { transaction ->
                val snap = transaction.get(docRef)
                if (!snap.exists()) return@runTransaction Pair(false, null)

                val remoteDate = snap.getString("dateIso") ?: today
                if (remoteDate != today) return@runTransaction Pair(false, null)

                val architectUsed = (snap.getLong("architectUsed") ?: 0L).toInt().coerceIn(0, 500)
                val visionUsed = (snap.getLong("visionUsed") ?: 0L).toInt().coerceIn(0, 500)
                val coachUsed = (snap.getLong("coachUsed") ?: 0L).toInt().coerceIn(0, 500)
                val bonusGrace = (snap.getLong("bonusGraceCredits") ?: 0L).toInt().coerceIn(0, 3)
                val graceClaimed = snap.getBoolean("graceClaimed") ?: false
                val ecoLiteMode = snap.getBoolean("ecoLiteMode") ?: false
                val cascadeCount = (snap.getLong("fallbackCascadeCount") ?: 0L).toInt().coerceIn(0, 500)

                val totalUsed = architectUsed + visionUsed + coachUsed
                val effectiveLimit = AiModelConfig.STARTER_DAILY_LIMIT + bonusGrace
                val currentUser = auth.currentUser
                if (totalUsed >= effectiveLimit) {
                    val exhaustedSnapshot = QuotaSnapshot(
                        dateIso = remoteDate,
                        architectUsed = architectUsed,
                        visionUsed = visionUsed,
                        coachUsed = coachUsed,
                        bonusGraceCredits = bonusGrace,
                        graceClaimed = graceClaimed,
                        ecoLiteMode = ecoLiteMode,
                        fallbackCascadeCount = cascadeCount,
                        isSignedIn = currentUser != null,
                        userDisplayName = currentUser?.displayName,
                        userEmail = currentUser?.email,
                        hasByokKey = getByokApiKey().isNotBlank()
                    )
                    return@runTransaction Pair(false, exhaustedSnapshot)
                }

                val nextArchitect = (architectUsed + if (feature == AiFeatureType.ARCHITECT) 1 else 0).coerceAtMost(500)
                val nextVision = (visionUsed + if (feature == AiFeatureType.VISION) 1 else 0).coerceAtMost(500)
                val nextCoach = (coachUsed + if (feature == AiFeatureType.COACH) 1 else 0).coerceAtMost(500)
                val nextCascade = (cascadeCount + if (recordCascade) 1 else 0).coerceAtMost(500)

                transaction.update(
                    docRef,
                    mapOf(
                        "architectUsed" to nextArchitect,
                        "visionUsed" to nextVision,
                        "coachUsed" to nextCoach,
                        "fallbackCascadeCount" to nextCascade,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                val nextSnapshot = QuotaSnapshot(
                    dateIso = remoteDate,
                    architectUsed = nextArchitect,
                    visionUsed = nextVision,
                    coachUsed = nextCoach,
                    bonusGraceCredits = bonusGrace,
                    graceClaimed = graceClaimed,
                    ecoLiteMode = ecoLiteMode,
                    fallbackCascadeCount = nextCascade,
                    isSignedIn = currentUser != null,
                    userDisplayName = currentUser?.displayName,
                    userEmail = currentUser?.email,
                    hasByokKey = getByokApiKey().isNotBlank()
                )
                Pair(true, nextSnapshot)
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Log.w(TAG, "Atomic consume transaction fell back to local mirror: ${e.message}")
            null
        }
    }

    private suspend fun runAtomicUsageRefundTransaction(
        uid: String,
        feature: AiFeatureType
    ): QuotaSnapshot? {
        val today = todayIso()
        ensureRemoteDailyDocReady(uid, today) ?: return null
        val docRef = dailyDocRef(uid)
        val path = docRef.path
        return try {
            db.runTransaction { transaction ->
                val snap = transaction.get(docRef)
                if (!snap.exists()) return@runTransaction null
                val remoteDate = snap.getString("dateIso") ?: today
                if (remoteDate != today) return@runTransaction null

                val architectUsed = (snap.getLong("architectUsed") ?: 0L).toInt().coerceIn(0, 500)
                val visionUsed = (snap.getLong("visionUsed") ?: 0L).toInt().coerceIn(0, 500)
                val coachUsed = (snap.getLong("coachUsed") ?: 0L).toInt().coerceIn(0, 500)
                val bonusGrace = (snap.getLong("bonusGraceCredits") ?: 0L).toInt().coerceIn(0, 3)
                val graceClaimed = snap.getBoolean("graceClaimed") ?: false
                val ecoLiteMode = snap.getBoolean("ecoLiteMode") ?: false
                val cascadeCount = (snap.getLong("fallbackCascadeCount") ?: 0L).toInt().coerceIn(0, 500)

                val nextArchitect = if (feature == AiFeatureType.ARCHITECT) (architectUsed - 1).coerceAtLeast(0) else architectUsed
                val nextVision = if (feature == AiFeatureType.VISION) (visionUsed - 1).coerceAtLeast(0) else visionUsed
                val nextCoach = if (feature == AiFeatureType.COACH) (coachUsed - 1).coerceAtLeast(0) else coachUsed

                if (nextArchitect != architectUsed || nextVision != visionUsed || nextCoach != coachUsed) {
                    transaction.update(
                        docRef,
                        mapOf(
                            "architectUsed" to nextArchitect,
                            "visionUsed" to nextVision,
                            "coachUsed" to nextCoach,
                            "fallbackCascadeCount" to cascadeCount,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                val currentUser = auth.currentUser
                QuotaSnapshot(
                    dateIso = remoteDate,
                    architectUsed = nextArchitect,
                    visionUsed = nextVision,
                    coachUsed = nextCoach,
                    bonusGraceCredits = bonusGrace,
                    graceClaimed = graceClaimed,
                    ecoLiteMode = ecoLiteMode,
                    fallbackCascadeCount = cascadeCount,
                    isSignedIn = currentUser != null,
                    userDisplayName = currentUser?.displayName,
                    userEmail = currentUser?.email,
                    hasByokKey = getByokApiKey().isNotBlank()
                )
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Log.w(TAG, "Atomic refund transaction fell back to local mirror: ${e.message}")
            null
        }
    }

    private suspend fun runAtomicCascadeTransaction(uid: String): QuotaSnapshot? {
        val today = todayIso()
        ensureRemoteDailyDocReady(uid, today) ?: return null
        val docRef = dailyDocRef(uid)
        val path = docRef.path
        return try {
            db.runTransaction { transaction ->
                val snap = transaction.get(docRef)
                if (!snap.exists()) return@runTransaction null
                val remoteDate = snap.getString("dateIso") ?: today
                if (remoteDate != today) return@runTransaction null

                val architectUsed = (snap.getLong("architectUsed") ?: 0L).toInt().coerceIn(0, 500)
                val visionUsed = (snap.getLong("visionUsed") ?: 0L).toInt().coerceIn(0, 500)
                val coachUsed = (snap.getLong("coachUsed") ?: 0L).toInt().coerceIn(0, 500)
                val bonusGrace = (snap.getLong("bonusGraceCredits") ?: 0L).toInt().coerceIn(0, 3)
                val graceClaimed = snap.getBoolean("graceClaimed") ?: false
                val ecoLiteMode = snap.getBoolean("ecoLiteMode") ?: false
                val nextCascade = ((snap.getLong("fallbackCascadeCount") ?: 0L).toInt() + 1).coerceAtMost(500)

                transaction.update(
                    docRef,
                    mapOf(
                        "architectUsed" to architectUsed,
                        "visionUsed" to visionUsed,
                        "coachUsed" to coachUsed,
                        "fallbackCascadeCount" to nextCascade,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                val currentUser = auth.currentUser
                QuotaSnapshot(
                    dateIso = remoteDate,
                    architectUsed = architectUsed,
                    visionUsed = visionUsed,
                    coachUsed = coachUsed,
                    bonusGraceCredits = bonusGrace,
                    graceClaimed = graceClaimed,
                    ecoLiteMode = ecoLiteMode,
                    fallbackCascadeCount = nextCascade,
                    isSignedIn = currentUser != null,
                    userDisplayName = currentUser?.displayName,
                    userEmail = currentUser?.email,
                    hasByokKey = getByokApiKey().isNotBlank()
                )
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            null
        }
    }

    private fun applyFeatureDelta(
        current: QuotaSnapshot,
        feature: AiFeatureType,
        delta: Int,
        cascadeDelta: Int
    ): QuotaSnapshot = when (feature) {
        AiFeatureType.ARCHITECT -> current.copy(
            architectUsed = (current.architectUsed + delta).coerceIn(0, 500),
            fallbackCascadeCount = (current.fallbackCascadeCount + cascadeDelta).coerceIn(0, 500)
        )
        AiFeatureType.VISION -> current.copy(
            visionUsed = (current.visionUsed + delta).coerceIn(0, 500),
            fallbackCascadeCount = (current.fallbackCascadeCount + cascadeDelta).coerceIn(0, 500)
        )
        AiFeatureType.COACH -> current.copy(
            coachUsed = (current.coachUsed + delta).coerceIn(0, 500),
            fallbackCascadeCount = (current.fallbackCascadeCount + cascadeDelta).coerceIn(0, 500)
        )
    }

    private fun ensureLocalRollover() {
        val today = todayIso()
        val storedDate = prefs.getString(KEY_DATE_ISO, "")
        if (storedDate != today) {
            val ecoPref = prefs.getBoolean(KEY_ECO_LITE_MODE, false)
            resetLocalCountersForDate(today, ecoPref)
            _quota.value = loadLocalMirror()
        }
    }

    private fun resetLocalCountersForDate(today: String, preservedEcoLiteMode: Boolean) {
        prefs.edit()
            .putString(KEY_DATE_ISO, today)
            .putInt(KEY_ARCHITECT_USED, 0)
            .putInt(KEY_VISION_USED, 0)
            .putInt(KEY_COACH_USED, 0)
            .putInt(KEY_BONUS_GRACE, 0)
            .putBoolean(KEY_GRACE_CLAIMED, false)
            .putBoolean(KEY_ECO_LITE_MODE, preservedEcoLiteMode)
            .putInt(KEY_FALLBACK_CASCADE, 0)
            .apply()
    }

    private fun persistToLocalMirror(doc: UsageQuotaDoc) {
        prefs.edit()
            .putString(KEY_DATE_ISO, doc.dateIso)
            .putInt(KEY_ARCHITECT_USED, doc.architectUsed)
            .putInt(KEY_VISION_USED, doc.visionUsed)
            .putInt(KEY_COACH_USED, doc.coachUsed)
            .putInt(KEY_BONUS_GRACE, doc.bonusGraceCredits)
            .putBoolean(KEY_GRACE_CLAIMED, doc.graceClaimed)
            .putBoolean(KEY_ECO_LITE_MODE, doc.ecoLiteMode)
            .putInt(KEY_FALLBACK_CASCADE, doc.fallbackCascadeCount)
            .apply()
    }

    private fun saveSnapshotToLocalPrefs(s: QuotaSnapshot) {
        prefs.edit()
            .putString(KEY_DATE_ISO, s.dateIso.ifBlank { todayIso() })
            .putInt(KEY_ARCHITECT_USED, s.architectUsed)
            .putInt(KEY_VISION_USED, s.visionUsed)
            .putInt(KEY_COACH_USED, s.coachUsed)
            .putInt(KEY_BONUS_GRACE, s.bonusGraceCredits)
            .putBoolean(KEY_GRACE_CLAIMED, s.graceClaimed)
            .putBoolean(KEY_ECO_LITE_MODE, s.ecoLiteMode)
            .putInt(KEY_FALLBACK_CASCADE, s.fallbackCascadeCount)
            .apply()
    }

    private fun loadLocalMirror(): QuotaSnapshot {
        val today = todayIso()
        val storedDate = prefs.getString(KEY_DATE_ISO, "")
        val eco = prefs.getBoolean(KEY_ECO_LITE_MODE, false)
        if (storedDate != today) {
            resetLocalCountersForDate(today, eco)
        }
        val currentUser = auth.currentUser
        return QuotaSnapshot(
            dateIso = today,
            architectUsed = prefs.getInt(KEY_ARCHITECT_USED, 0),
            visionUsed = prefs.getInt(KEY_VISION_USED, 0),
            coachUsed = prefs.getInt(KEY_COACH_USED, 0),
            bonusGraceCredits = prefs.getInt(KEY_BONUS_GRACE, 0),
            graceClaimed = prefs.getBoolean(KEY_GRACE_CLAIMED, false),
            ecoLiteMode = eco,
            fallbackCascadeCount = prefs.getInt(KEY_FALLBACK_CASCADE, 0),
            isSignedIn = currentUser != null,
            userDisplayName = currentUser?.displayName,
            userEmail = currentUser?.email,
            hasByokKey = getByokApiKey().isNotBlank()
        )
    }

    private fun todayIso(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    companion object {
        private const val TAG = "UsageQuotaManager"
        private const val PREFS_NAME = "trueaim_ai_quota_mirror"
        private const val DAILY_DOC_ID = "daily"

        private const val KEY_DATE_ISO = "date_iso"
        private const val KEY_ARCHITECT_USED = "architect_used"
        private const val KEY_VISION_USED = "vision_used"
        private const val KEY_COACH_USED = "coach_used"
        private const val KEY_BONUS_GRACE = "bonus_grace_credits"
        private const val KEY_GRACE_CLAIMED = "grace_claimed"
        private const val KEY_ECO_LITE_MODE = "eco_lite_mode"
        private const val KEY_FALLBACK_CASCADE = "fallback_cascade_count"
        private const val KEY_BYOK_API_KEY = "byok_gemini_api_key"

        @Volatile
        private var INSTANCE: UsageQuotaManager? = null

        fun getInstance(context: Context): UsageQuotaManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UsageQuotaManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
