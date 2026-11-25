package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.UsageStats
import com.example.data.ai.AiArchitectResult
import com.example.data.ai.AiCoachResult
import com.example.data.ai.AiExecutionTier
import com.example.data.ai.AiFeatureType
import com.example.data.ai.AiTaskState
import com.example.data.ai.AiVisionResult
import com.example.data.ai.GoogleAuthManager
import com.example.data.ai.QuotaSnapshot
import com.example.data.ai.TrueAimAiRepository
import com.example.data.ai.UsageQuotaManager
import com.example.data.analytics.AnalyticsSummary
import com.example.data.analytics.AppEventTracker
import com.example.data.analytics.AppEventType
import com.example.model.CrosshairConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AiLabViewModel(application: Application) : AndroidViewModel(application) {

    private val quotaManager = UsageQuotaManager.getInstance(application)
    private val aiRepository = TrueAimAiRepository(application, quotaManager)
    private val eventTracker = AppEventTracker.getInstance(application)

    val quota: StateFlow<QuotaSnapshot> = quotaManager.quota

    private val _selectedToolIndex = MutableStateFlow(0)
    val selectedToolIndex: StateFlow<Int> = _selectedToolIndex.asStateFlow()

    private val _architectPrompt = MutableStateFlow("")
    val architectPrompt: StateFlow<String> = _architectPrompt.asStateFlow()

    private val _architectState = MutableStateFlow<AiTaskState<AiArchitectResult>>(AiTaskState.Idle)
    val architectState: StateFlow<AiTaskState<AiArchitectResult>> = _architectState.asStateFlow()

    private val _pickedScreenshot = MutableStateFlow<Bitmap?>(null)
    val pickedScreenshot: StateFlow<Bitmap?> = _pickedScreenshot.asStateFlow()

    private val _visionState = MutableStateFlow<AiTaskState<AiVisionResult>>(AiTaskState.Idle)
    val visionState: StateFlow<AiTaskState<AiVisionResult>> = _visionState.asStateFlow()

    private val _coachState = MutableStateFlow<AiTaskState<AiCoachResult>>(AiTaskState.Idle)
    val coachState: StateFlow<AiTaskState<AiCoachResult>> = _coachState.asStateFlow()

    private val _showValueReceiptSheet = MutableStateFlow(false)
    val showValueReceiptSheet: StateFlow<Boolean> = _showValueReceiptSheet.asStateFlow()

    private val _isAuthBusy = MutableStateFlow(false)
    val isAuthBusy: StateFlow<Boolean> = _isAuthBusy.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private var lastCoachInputs: CoachInputs? = null

    private data class CoachInputs(
        val lastDrill: AimGameStats,
        val difficulty: AimDifficulty,
        val usageStats: UsageStats,
        val summary: AnalyticsSummary,
        val activeConfig: CrosshairConfig
    )

    fun selectTool(index: Int) {
        _selectedToolIndex.value = index.coerceIn(0, 2)
    }

    fun setArchitectPrompt(prompt: String) {
        _architectPrompt.value = prompt
    }

    fun openValueReceiptSheet() {
        _showValueReceiptSheet.value = true
    }

    fun closeValueReceiptSheet() {
        _showValueReceiptSheet.value = false
    }

    fun clearStatusBanner() {
        _statusBannerMessage.value = null
    }

    fun getByokApiKey(): String = quotaManager.getByokApiKey()

    fun saveByokApiKey(key: String) {
        quotaManager.setByokApiKey(key)
        _statusBannerMessage.value = if (key.isBlank()) {
            getApplication<Application>().getString(R.string.ai_byok_cleared_msg)
        } else {
            getApplication<Application>().getString(R.string.ai_byok_saved_msg)
        }
    }

    fun toggleEcoLiteMode(enabled: Boolean) {
        viewModelScope.launch {
            quotaManager.setEcoLiteMode(enabled)
            eventTracker.logEvent(
                AppEventType.AI_ECO_MODE_TOGGLED,
                "ai_lab",
                mapOf("enabled" to enabled)
            )
        }
    }

    fun claimDailyGraceCredits() {
        viewModelScope.launch {
            val claimed = quotaManager.claimDailyGraceCredits()
            if (claimed) {
                eventTracker.logEvent(AppEventType.AI_GRACE_CLAIMED, "ai_lab", mapOf("credits" to 3))
                _statusBannerMessage.value = getApplication<Application>().getString(R.string.ai_grace_claimed_toast)
                // Clear exhaustion recovery cards so the user can immediately continue
                if (_architectState.value is AiTaskState.RecoveryNeeded) _architectState.value = AiTaskState.Idle
                if (_visionState.value is AiTaskState.RecoveryNeeded) _visionState.value = AiTaskState.Idle
                if (_coachState.value is AiTaskState.RecoveryNeeded) _coachState.value = AiTaskState.Idle
            }
        }
    }

    // --- GOOGLE SIGN-IN (GUEST -> STARTER FREE TIER UNLOCK) ---

    fun attemptSilentSignIn(context: Context) {
        viewModelScope.launch {
            GoogleAuthManager.attemptAutoSignIn(context)
        }
    }

    fun signInWithGoogle(context: Context) {
        if (_isAuthBusy.value) return
        _isAuthBusy.value = true
        viewModelScope.launch {
            when (val outcome = GoogleAuthManager.signInInteractive(context)) {
                GoogleAuthManager.SignInOutcome.Success -> {
                    eventTracker.logEvent(AppEventType.AI_GOOGLE_SIGN_IN, "ai_lab", mapOf("status" to "success"))
                    _statusBannerMessage.value = context.getString(R.string.ai_auth_success_msg)
                }
                GoogleAuthManager.SignInOutcome.Cancelled -> {
                    // User dismissed bottom sheet; reset busy cleanly without alarm
                }
                is GoogleAuthManager.SignInOutcome.Failure -> {
                    _statusBannerMessage.value = outcome.message
                }
            }
            _isAuthBusy.value = false
        }
    }

    fun signOutGoogle(context: Context) {
        if (_isAuthBusy.value) return
        _isAuthBusy.value = true
        viewModelScope.launch {
            GoogleAuthManager.signOut(
                context = context,
                beforeFirebaseSignOut = { quotaManager.detachCloudListener() }
            )
            _statusBannerMessage.value = context.getString(R.string.ai_auth_signed_out_msg)
            _isAuthBusy.value = false
        }
    }

    // --- 1. AI CROSSHAIR ARCHITECT ---

    fun runArchitect(prompt: String = _architectPrompt.value, forceEcoLite: Boolean = false) {
        val clean = prompt.trim()
        _architectPrompt.value = clean
        if (clean.isBlank()) {
            _architectState.value = AiTaskState.Error(
                getApplication<Application>().getString(R.string.ai_architect_empty_prompt)
            )
            return
        }

        val plannedTier = resolvePlannedTier(forceEcoLite)
        _architectState.value = AiTaskState.Loading(plannedTier)

        viewModelScope.launch {
            val result = aiRepository.generateCrosshair(clean, forceEcoLite)
            _architectState.value = result
            if (result is AiTaskState.Success) {
                eventTracker.logEvent(
                    AppEventType.AI_ARCHITECT_RUN,
                    "ai_lab",
                    mapOf("tier" to result.data.tierUsed.name, "cached" to result.data.fromCache)
                )
                if (result.data.autoCascaded) {
                    eventTracker.logEvent(AppEventType.AI_TIER_FALLBACK, "ai_lab", mapOf("feature" to AiFeatureType.ARCHITECT.key))
                }
            }
        }
    }

    fun runArchitectLocally(prompt: String = _architectPrompt.value) {
        val effectivePrompt = prompt.trim().ifBlank {
            getApplication<Application>().getString(R.string.ai_default_local_prompt)
        }
        val local = aiRepository.generateCrosshairLocally(effectivePrompt)
        _architectState.value = AiTaskState.Success(local)
        eventTracker.logEvent(
            AppEventType.AI_ARCHITECT_RUN,
            "ai_lab",
            mapOf("tier" to AiExecutionTier.LOCAL_ENGINE.name, "cached" to false)
        )
    }

    // --- 2. MAP SCREENSHOT CONTRAST ANALYZER ---

    fun onScreenshotPicked(bitmap: Bitmap) {
        _pickedScreenshot.value = bitmap
        runVisionAnalysis(forceEcoLite = false)
    }

    fun runVisionAnalysis(forceEcoLite: Boolean = false) {
        val bitmap = _pickedScreenshot.value ?: return
        val plannedTier = resolvePlannedTier(forceEcoLite)
        _visionState.value = AiTaskState.Loading(plannedTier)

        viewModelScope.launch {
            val result = aiRepository.analyzeMapScreenshot(bitmap, forceEcoLite)
            _visionState.value = result
            if (result is AiTaskState.Success) {
                eventTracker.logEvent(
                    AppEventType.AI_VISION_RUN,
                    "ai_lab",
                    mapOf("tier" to result.data.tierUsed.name, "cached" to result.data.fromCache)
                )
                if (result.data.autoCascaded) {
                    eventTracker.logEvent(AppEventType.AI_TIER_FALLBACK, "ai_lab", mapOf("feature" to AiFeatureType.VISION.key))
                }
            }
        }
    }

    fun runVisionAnalysisLocally() {
        val bitmap = _pickedScreenshot.value ?: return
        _visionState.value = AiTaskState.Loading(AiExecutionTier.LOCAL_ENGINE)
        viewModelScope.launch {
            val local = aiRepository.analyzeMapScreenshotLocally(bitmap)
            _visionState.value = AiTaskState.Success(local)
            eventTracker.logEvent(
                AppEventType.AI_VISION_RUN,
                "ai_lab",
                mapOf("tier" to AiExecutionTier.LOCAL_ENGINE.name, "cached" to false)
            )
        }
    }

    // --- 3. AI AIM COACH ---

    fun runAimCoach(
        lastDrill: AimGameStats,
        difficulty: AimDifficulty,
        usageStats: UsageStats,
        summary: AnalyticsSummary,
        activeConfig: CrosshairConfig,
        forceEcoLite: Boolean = false
    ) {
        lastCoachInputs = CoachInputs(lastDrill, difficulty, usageStats, summary, activeConfig)
        val plannedTier = resolvePlannedTier(forceEcoLite)
        _coachState.value = AiTaskState.Loading(plannedTier)

        viewModelScope.launch {
            val result = aiRepository.generateAimCoaching(
                lastDrill = lastDrill,
                difficulty = difficulty,
                usageStats = usageStats,
                summary = summary,
                activeConfig = activeConfig,
                forceEcoLite = forceEcoLite
            )
            _coachState.value = result
            if (result is AiTaskState.Success) {
                eventTracker.logEvent(
                    AppEventType.AI_COACH_RUN,
                    "ai_lab",
                    mapOf("tier" to result.data.tierUsed.name, "cached" to result.data.fromCache)
                )
                if (result.data.autoCascaded) {
                    eventTracker.logEvent(AppEventType.AI_TIER_FALLBACK, "ai_lab", mapOf("feature" to AiFeatureType.COACH.key))
                }
            }
        }
    }

    fun retryAimCoachEcoLite() {
        val inputs = lastCoachInputs ?: return
        runAimCoach(
            lastDrill = inputs.lastDrill,
            difficulty = inputs.difficulty,
            usageStats = inputs.usageStats,
            summary = inputs.summary,
            activeConfig = inputs.activeConfig,
            forceEcoLite = true
        )
    }

    fun runAimCoachLocally(
        lastDrill: AimGameStats,
        difficulty: AimDifficulty,
        usageStats: UsageStats,
        summary: AnalyticsSummary,
        activeConfig: CrosshairConfig
    ) {
        lastCoachInputs = CoachInputs(lastDrill, difficulty, usageStats, summary, activeConfig)
        val local = aiRepository.generateAimCoachingLocally(
            lastDrill = lastDrill,
            difficulty = difficulty,
            usageStats = usageStats,
            summary = summary,
            activeConfig = activeConfig
        )
        _coachState.value = AiTaskState.Success(local)
        eventTracker.logEvent(
            AppEventType.AI_COACH_RUN,
            "ai_lab",
            mapOf("tier" to AiExecutionTier.LOCAL_ENGINE.name, "cached" to false)
        )
    }

    private fun resolvePlannedTier(forceEcoLite: Boolean): AiExecutionTier {
        val snapshot = quota.value
        return when {
            snapshot.hasByokKey -> AiExecutionTier.BYOK_DIRECT
            forceEcoLite || snapshot.shouldPreferEcoLite -> AiExecutionTier.ECONOMY_LITE
            else -> AiExecutionTier.PRIMARY_CLOUD
        }
    }
}
