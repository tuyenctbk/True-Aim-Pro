package com.example.data.analytics

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Log
import com.example.data.room.AppDatabase
import com.example.data.room.AppEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

enum class EventCategory(val key: String) {
    LIFECYCLE("lifecycle"),
    NAVIGATION("navigation"),
    OVERLAY("overlay"),
    CUSTOMIZATION("customization"),
    TRAINER("trainer"),
    ENGAGEMENT("engagement"),
    SETTINGS("settings"),
    AI_LAB("ai_lab")
}

enum class AppEventType(val eventName: String, val category: EventCategory) {
    APP_OPEN("app_open", EventCategory.LIFECYCLE),
    SPLASH_COMPLETE("splash_complete", EventCategory.LIFECYCLE),
    ONBOARDING_COMPLETE("onboarding_complete", EventCategory.LIFECYCLE),
    SCREEN_VIEW("screen_view", EventCategory.NAVIGATION),
    OVERLAY_STARTED("overlay_started", EventCategory.OVERLAY),
    OVERLAY_STOPPED("overlay_stopped", EventCategory.OVERLAY),
    OVERLAY_SESSION_COMPLETED("overlay_session_completed", EventCategory.OVERLAY),
    OVERLAY_CALIBRATION_TOGGLED("overlay_calibration_toggled", EventCategory.OVERLAY),
    RETICLE_CUSTOMIZED("reticle_customized", EventCategory.CUSTOMIZATION),
    PRESET_EQUIPPED("preset_equipped", EventCategory.CUSTOMIZATION),
    PRESET_SAVED("preset_saved", EventCategory.CUSTOMIZATION),
    PRESET_DELETED("preset_deleted", EventCategory.CUSTOMIZATION),
    PRESET_IMPORTED("preset_imported", EventCategory.CUSTOMIZATION),
    PRESET_SHARED("preset_shared", EventCategory.CUSTOMIZATION),
    PRESET_FAVORITED("preset_favorited", EventCategory.CUSTOMIZATION),
    AIM_DRILL_STARTED("aim_drill_started", EventCategory.TRAINER),
    AIM_DRILL_COMPLETED("aim_drill_completed", EventCategory.TRAINER),
    PERMISSION_OVERLAY_GRANTED("permission_overlay_granted", EventCategory.SETTINGS),
    PERMISSION_NOTIFICATIONS_GRANTED("permission_notifications_granted", EventCategory.SETTINGS),
    THEME_CHANGED("theme_changed", EventCategory.SETTINGS),
    SETTINGS_RESET("settings_reset", EventCategory.SETTINGS),
    UPDATE_CHECKED("update_checked", EventCategory.ENGAGEMENT),
    POPUP_IMPRESSION("popup_impression", EventCategory.ENGAGEMENT),
    POPUP_ACCEPTED("popup_accepted", EventCategory.ENGAGEMENT),
    POPUP_DISMISSED("popup_dismissed", EventCategory.ENGAGEMENT),
    POPUP_NEVER_ASK("popup_never_ask", EventCategory.ENGAGEMENT),
    POPUP_SUPPRESSED("popup_suppressed", EventCategory.ENGAGEMENT),
    AI_ARCHITECT_RUN("ai_architect_run", EventCategory.AI_LAB),
    AI_VISION_RUN("ai_vision_run", EventCategory.AI_LAB),
    AI_COACH_RUN("ai_coach_run", EventCategory.AI_LAB),
    AI_TIER_FALLBACK("ai_tier_fallback", EventCategory.AI_LAB),
    AI_GRACE_CLAIMED("ai_grace_claimed", EventCategory.AI_LAB),
    AI_ECO_MODE_TOGGLED("ai_eco_mode_toggled", EventCategory.AI_LAB),
    AI_GOOGLE_SIGN_IN("ai_google_sign_in", EventCategory.AI_LAB)
}

data class AnalyticsSummary(
    val firstInstallTimeMs: Long = 0L,
    val appOpenCount: Int = 0,
    val meaningfulOverlaySessions: Int = 0,
    val aimDrillsCompleted: Int = 0,
    val highScoreDrillsCount: Int = 0,
    val bestAimScore: Int = 0,
    val customPresetsSaved: Int = 0,
    val presetsSharedCount: Int = 0,
    val reticleEditsCount: Int = 0,
    val totalLoggedEvents: Int = 0
)

class AppEventTracker private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val eventDao = AppDatabase.getDatabase(appContext).appEventDao()
    private val trackerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val sessionId: String = UUID.randomUUID().toString().take(12)
    val sessionStartElapsedMs: Long = SystemClock.elapsedRealtime()

    private var hasRecordedSessionOpen = false
    private var lastReticleEditLogMs = 0L

    private val _recentEvents = MutableStateFlow<List<AppEventEntity>>(emptyList())
    val recentEvents: StateFlow<List<AppEventEntity>> = _recentEvents.asStateFlow()

    private val _summary = MutableStateFlow(loadSummary(0))
    val summary: StateFlow<AnalyticsSummary> = _summary.asStateFlow()

    init {
        if (prefs.getLong(KEY_FIRST_INSTALL_MS, 0L) == 0L) {
            prefs.edit().putLong(KEY_FIRST_INSTALL_MS, System.currentTimeMillis()).apply()
        }

        trackerScope.launch {
            eventDao.getRecentEvents(60).collectLatest { events ->
                _recentEvents.value = events
            }
        }
        trackerScope.launch {
            eventDao.observeTotalEventCount().collectLatest { totalCount ->
                _summary.value = loadSummary(totalCount)
            }
        }
    }

    fun getSessionUptimeMs(): Long = SystemClock.elapsedRealtime() - sessionStartElapsedMs

    fun recordAppOpen(isColdStart: Boolean = true) {
        if (hasRecordedSessionOpen && isColdStart) return
        hasRecordedSessionOpen = true

        val nextCount = prefs.getInt(KEY_APP_OPENS, 0) + 1
        prefs.edit().putInt(KEY_APP_OPENS, nextCount).apply()
        refreshSummary()

        logEvent(
            type = AppEventType.APP_OPEN,
            screen = "app",
            params = mapOf("open_count" to nextCount, "cold_start" to isColdStart)
        )
    }

    fun logScreenView(screenName: String) {
        logEvent(
            type = AppEventType.SCREEN_VIEW,
            screen = screenName
        )
    }

    fun logReticleCustomizedDebounced(field: String) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastReticleEditLogMs < 4_000L) return
        lastReticleEditLogMs = now

        val nextEdits = prefs.getInt(KEY_RETICLE_EDITS, 0) + 1
        prefs.edit().putInt(KEY_RETICLE_EDITS, nextEdits).apply()
        refreshSummary()

        logEvent(
            type = AppEventType.RETICLE_CUSTOMIZED,
            screen = "studio",
            params = mapOf("field" to field, "total_edits" to nextEdits)
        )
    }

    fun logPresetSaved(presetName: String, category: String) {
        val nextCount = prefs.getInt(KEY_PRESETS_SAVED, 0) + 1
        prefs.edit().putInt(KEY_PRESETS_SAVED, nextCount).apply()
        refreshSummary()

        logEvent(
            type = AppEventType.PRESET_SAVED,
            screen = "studio",
            params = mapOf("name" to presetName, "category" to category, "count" to nextCount)
        )
    }

    fun logPresetShared(presetName: String) {
        val nextCount = prefs.getInt(KEY_PRESETS_SHARED, 0) + 1
        prefs.edit().putInt(KEY_PRESETS_SHARED, nextCount).apply()
        refreshSummary()

        logEvent(
            type = AppEventType.PRESET_SHARED,
            screen = "gallery",
            params = mapOf("name" to presetName, "count" to nextCount)
        )
    }

    fun logAimDrillCompleted(score: Int, accuracyPercent: Int, difficulty: String, bestStreak: Int) {
        val drills = prefs.getInt(KEY_AIM_DRILLS, 0) + 1
        val prevBest = prefs.getInt(KEY_BEST_AIM_SCORE, 0)
        val isNewBest = score > prevBest
        val highScores = prefs.getInt(KEY_HIGH_SCORE_DRILLS, 0) + if (score >= 800 && accuracyPercent >= 55) 1 else 0

        prefs.edit()
            .putInt(KEY_AIM_DRILLS, drills)
            .putInt(KEY_BEST_AIM_SCORE, maxOf(score, prevBest))
            .putInt(KEY_HIGH_SCORE_DRILLS, highScores)
            .apply()
        refreshSummary()

        logEvent(
            type = AppEventType.AIM_DRILL_COMPLETED,
            screen = "trainer",
            params = mapOf(
                "score" to score,
                "accuracy" to accuracyPercent,
                "difficulty" to difficulty,
                "best_streak" to bestStreak,
                "new_best" to isNewBest
            )
        )
    }

    fun logOverlaySessionCompleted(durationSeconds: Long) {
        val isMeaningful = durationSeconds >= MEANINGFUL_OVERLAY_SECONDS
        if (isMeaningful) {
            val next = prefs.getInt(KEY_MEANINGFUL_OVERLAY_SESSIONS, 0) + 1
            prefs.edit().putInt(KEY_MEANINGFUL_OVERLAY_SESSIONS, next).apply()
            refreshSummary()
        }

        logEvent(
            type = AppEventType.OVERLAY_SESSION_COMPLETED,
            screen = "overlay",
            params = mapOf(
                "duration_sec" to durationSeconds,
                "meaningful" to isMeaningful
            )
        )
    }

    fun logEvent(
        type: AppEventType,
        screen: String,
        params: Map<String, Any?> = emptyMap()
    ) {
        val json = JSONObject().apply {
            params.forEach { (key, value) ->
                put(key, value ?: JSONObject.NULL)
            }
        }.toString()

        val entity = AppEventEntity(
            eventName = type.eventName,
            category = type.category.key,
            screen = screen,
            metadataJson = json,
            timestampMs = System.currentTimeMillis(),
            sessionId = sessionId
        )

        Log.d(TAG, "[${type.category.key}] ${type.eventName} @ $screen -> $json")

        trackerScope.launch {
            runCatching {
                eventDao.insertEvent(entity)
                eventDao.pruneOldEvents(MAX_STORED_EVENTS)
            }
        }
    }

    fun clearEventHistory() {
        trackerScope.launch {
            runCatching { eventDao.clearAllEvents() }
        }
    }

    fun getCurrentSummarySnapshot(): AnalyticsSummary = loadSummary(_summary.value.totalLoggedEvents)

    private fun refreshSummary() {
        _summary.value = loadSummary(_summary.value.totalLoggedEvents)
    }

    private fun loadSummary(totalLoggedEvents: Int): AnalyticsSummary {
        return AnalyticsSummary(
            firstInstallTimeMs = prefs.getLong(KEY_FIRST_INSTALL_MS, System.currentTimeMillis()),
            appOpenCount = prefs.getInt(KEY_APP_OPENS, 0),
            meaningfulOverlaySessions = prefs.getInt(KEY_MEANINGFUL_OVERLAY_SESSIONS, 0),
            aimDrillsCompleted = prefs.getInt(KEY_AIM_DRILLS, 0),
            highScoreDrillsCount = prefs.getInt(KEY_HIGH_SCORE_DRILLS, 0),
            bestAimScore = prefs.getInt(KEY_BEST_AIM_SCORE, 0),
            customPresetsSaved = prefs.getInt(KEY_PRESETS_SAVED, 0),
            presetsSharedCount = prefs.getInt(KEY_PRESETS_SHARED, 0),
            reticleEditsCount = prefs.getInt(KEY_RETICLE_EDITS, 0),
            totalLoggedEvents = totalLoggedEvents
        )
    }

    companion object {
        private const val TAG = "TrueAimAnalytics"
        private const val PREFS_NAME = "trueaim_analytics_prefs"
        private const val MAX_STORED_EVENTS = 500
        const val MEANINGFUL_OVERLAY_SECONDS = 45L

        private const val KEY_FIRST_INSTALL_MS = "first_install_time_ms"
        private const val KEY_APP_OPENS = "app_open_count"
        private const val KEY_MEANINGFUL_OVERLAY_SESSIONS = "meaningful_overlay_sessions"
        private const val KEY_AIM_DRILLS = "aim_drills_completed"
        private const val KEY_HIGH_SCORE_DRILLS = "high_score_drills_count"
        private const val KEY_BEST_AIM_SCORE = "best_aim_score"
        private const val KEY_PRESETS_SAVED = "custom_presets_saved"
        private const val KEY_PRESETS_SHARED = "presets_shared_count"
        private const val KEY_RETICLE_EDITS = "reticle_edits_count"

        @Volatile
        private var INSTANCE: AppEventTracker? = null

        fun getInstance(context: Context): AppEventTracker {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppEventTracker(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
