package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.R
import com.example.data.CrosshairRepository
import com.example.data.UsageStats
import com.example.data.analytics.AnalyticsSummary
import com.example.data.analytics.AppEventTracker
import com.example.data.analytics.AppEventType
import com.example.data.engagement.EngagementContext
import com.example.data.engagement.EngagementMoment
import com.example.data.engagement.SmartPopupEngine
import com.example.data.engagement.SmartPopupProposal
import com.example.data.room.AppEventEntity
import com.example.model.CrosshairConfig
import com.example.model.CrosshairStyle
import com.example.model.PredefinedCrosshairCatalog
import com.example.service.CrosshairOverlayService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

enum class PreviewBackground(@param:StringRes val nameRes: Int) {
    CQB_DEPOT(R.string.bg_cqb_depot),
    DESERT_OUTPOST(R.string.bg_desert_outpost),
    CYBER_NIGHT(R.string.bg_cyber_night),
    TARGET_RANGE(R.string.bg_target_range),
    SNIPER_MOUNTAIN(R.string.bg_sniper_mountain),
    TACTICAL_DARK(R.string.bg_tactical_dark)
}

enum class AimGameState {
    IDLE, PLAYING, FINISHED
}

enum class AimDifficulty(@param:StringRes val titleRes: Int, val targetRadiusRange: ClosedFloatingPointRange<Float>) {
    CASUAL(R.string.difficulty_casual, 30f..40f),
    OPERATOR(R.string.difficulty_operator, 22f..30f),
    APEX_PREDATOR(R.string.difficulty_apex_predator, 16f..22f)
}

data class HitFeedback(
    val id: Long = System.currentTimeMillis(),
    val xFraction: Float,
    val yFraction: Float,
    val isHeadshot: Boolean,
    val scoreDelta: Int
)

data class AimTarget(
    val id: Long,
    val xFraction: Float,
    val yFraction: Float,
    val radiusDp: Float,
    val spawnTimeMs: Long,
    val isHeadshotRing: Boolean = false
)

data class AimGameStats(
    val state: AimGameState = AimGameState.IDLE,
    val score: Int = 0,
    val targetsHit: Int = 0,
    val totalTargets: Int = 0,
    val timeLeftSec: Int = 20,
    val avgReactionTimeMs: Long = 0,
    val headshots: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val currentTarget: AimTarget? = null
)

class CrosshairViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CrosshairRepository.getInstance(application)
    private val eventTracker = AppEventTracker.getInstance(application)
    private val popupEngine = SmartPopupEngine.getInstance(application)

    val activeConfig: StateFlow<CrosshairConfig> = repository.activeConfig
    val isOverlayActive: StateFlow<Boolean> = repository.isOverlayActive
    val savedPresets: StateFlow<List<CrosshairConfig>> = repository.savedPresets
    val usageStats: StateFlow<UsageStats> = repository.usageStats
    val isDarkMode: StateFlow<Boolean> = repository.isDarkMode
    val hasCompletedOnboarding: StateFlow<Boolean> = repository.hasCompletedOnboarding
    val recentCustomColors: StateFlow<List<Long>> = repository.recentCustomColors
    val favoriteIds: StateFlow<Set<String>> = repository.favoriteIds

    val recentEvents: StateFlow<List<AppEventEntity>> = eventTracker.recentEvents
    val analyticsSummary: StateFlow<AnalyticsSummary> = eventTracker.summary

    private val _isSplashVisible = MutableStateFlow(true)
    val isSplashVisible: StateFlow<Boolean> = _isSplashVisible.asStateFlow()

    private val _currentScreen = MutableStateFlow("studio")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    private val _isOverlayPermissionGranted = MutableStateFlow(
        Settings.canDrawOverlays(application)
    )
    val isOverlayPermissionGranted: StateFlow<Boolean> = _isOverlayPermissionGranted.asStateFlow()

    private val _activeSmartPopup = MutableStateFlow<SmartPopupProposal?>(null)
    val activeSmartPopup: StateFlow<SmartPopupProposal?> = _activeSmartPopup.asStateFlow()

    private val _updateCheckMessageRes = MutableStateFlow<Int?>(null)
    val updateCheckMessageRes: StateFlow<Int?> = _updateCheckMessageRes.asStateFlow()

    private val _selectedBackground = MutableStateFlow(PreviewBackground.CQB_DEPOT)
    val selectedBackground: StateFlow<PreviewBackground> = _selectedBackground.asStateFlow()

    private val _selectedDifficulty = MutableStateFlow(AimDifficulty.OPERATOR)
    val selectedDifficulty: StateFlow<AimDifficulty> = _selectedDifficulty.asStateFlow()

    private val _lastHitFeedback = MutableStateFlow<HitFeedback?>(null)
    val lastHitFeedback: StateFlow<HitFeedback?> = _lastHitFeedback.asStateFlow()

    private val _lastMissTimestampMs = MutableStateFlow(0L)
    val lastMissTimestampMs: StateFlow<Long> = _lastMissTimestampMs.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val allPresets: StateFlow<List<CrosshairConfig>> = combine(
        savedPresets,
        _searchQuery
    ) { custom, query ->
        val appCtx = getApplication<Application>()
        val builtIns = PredefinedCrosshairCatalog.localizedPresets(appCtx)
        val all = custom + builtIns
        if (query.isBlank()) {
            all
        } else {
            val q = query.trim().lowercase()
            all.filter { preset ->
                val localizedName = PredefinedCrosshairCatalog.resolveDisplayName(appCtx, preset)
                val localizedCategory = CrosshairConfig.localizedCategoryName(appCtx, preset.category)
                val localizedStyle = appCtx.getString(preset.style.nameRes)
                localizedName.lowercase().contains(q) ||
                    localizedCategory.lowercase().contains(q) ||
                    preset.category.lowercase().contains(q) ||
                    localizedStyle.lowercase().contains(q)
            }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PredefinedCrosshairCatalog.localizedPresets(application)
    )

    private val _gameState = MutableStateFlow(AimGameStats())
    val gameState: StateFlow<AimGameStats> = _gameState.asStateFlow()

    private val _showSaveDialog = MutableStateFlow(false)
    val showSaveDialog: StateFlow<Boolean> = _showSaveDialog.asStateFlow()

    private val _savePresetDraftConfig = MutableStateFlow<CrosshairConfig?>(null)
    val savePresetDraftConfig: StateFlow<CrosshairConfig?> = _savePresetDraftConfig.asStateFlow()

    private val _renamingPreset = MutableStateFlow<CrosshairConfig?>(null)
    val renamingPreset: StateFlow<CrosshairConfig?> = _renamingPreset.asStateFlow()

    private val _deletingPreset = MutableStateFlow<CrosshairConfig?>(null)
    val deletingPreset: StateFlow<CrosshairConfig?> = _deletingPreset.asStateFlow()

    private val _presetFeedbackMessage = MutableStateFlow<String?>(null)
    val presetFeedbackMessage: StateFlow<String?> = _presetFeedbackMessage.asStateFlow()

    private val _showPermissionDialog = MutableStateFlow(false)
    val showPermissionDialog: StateFlow<Boolean> = _showPermissionDialog.asStateFlow()

    private val _showShareDialog = MutableStateFlow<CrosshairConfig?>(null)
    val showShareDialog: StateFlow<CrosshairConfig?> = _showShareDialog.asStateFlow()

    private val _showImportDialog = MutableStateFlow(false)
    val showImportDialog: StateFlow<Boolean> = _showImportDialog.asStateFlow()

    private val reactionTimes = mutableListOf<Long>()
    private var gameJob: Job? = null
    private var delayedPopupJob: Job? = null
    private var pendingHighScorePopupCheck = false

    init {
        eventTracker.recordAppOpen(isColdStart = true)
    }

    fun onSplashFinished() {
        if (!_isSplashVisible.value) return
        _isSplashVisible.value = false
        eventTracker.logEvent(AppEventType.SPLASH_COMPLETE, "splash")
    }

    fun onScreenSelected(screenName: String) {
        val previous = _currentScreen.value
        if (previous == screenName) return
        _currentScreen.value = screenName
        eventTracker.logScreenView(screenName)

        // Stop active drill cleanly if user navigates away from Trainer
        if (previous == "trainer") {
            if (_gameState.value.state == AimGameState.PLAYING) {
                cancelAimTrainer()
            } else if (pendingHighScorePopupCheck) {
                pendingHighScorePopupCheck = false
                scheduleSmartPopupEvaluation(EngagementMoment.AIM_DRILL_HIGH_SCORE, delayMs = 500L)
            }
        } else if (screenName == "studio" && popupEngine.hasNewerUpdateAvailable()) {
            // Check for genuine update only after user settles (40s uptime gate still enforced inside engine)
            scheduleSmartPopupEvaluation(EngagementMoment.RETURNING_USER_SETTLED, delayMs = 800L)
        }
    }

    fun updateConfig(field: String = "geometry", transform: (CrosshairConfig) -> CrosshairConfig) {
        val updated = transform(activeConfig.value)
        repository.updateActiveConfig(updated)
        eventTracker.logReticleCustomizedDebounced(field)
    }

    fun updateConfig(transform: (CrosshairConfig) -> CrosshairConfig) {
        updateConfig("geometry", transform)
    }

    fun setPrimaryColorHex(hexColor: Long) {
        updateConfig("color") { it.copy(primaryColorHex = hexColor) }
    }

    fun recordRecentColor(hexColor: Long) {
        repository.recordRecentCustomColor(hexColor)
    }

    fun parseAndApplyHexString(hexInput: String): Boolean {
        val cleaned = hexInput.trim().removePrefix("#")
        if (cleaned.length == 6 || cleaned.length == 8) {
            return try {
                val parsed = cleaned.toLong(16)
                val colorWithAlpha = 0xFF000000L or (parsed and 0x00FFFFFFL)
                setPrimaryColorHex(colorWithAlpha)
                true
            } catch (_: NumberFormatException) {
                false
            }
        }
        return false
    }

    fun setPrimaryColorRgb(r: Int, g: Int, b: Int) {
        val colorHex = (0xFFL shl 24) or
            ((r.coerceIn(0, 255).toLong()) shl 16) or
            ((g.coerceIn(0, 255).toLong()) shl 8) or
            (b.coerceIn(0, 255).toLong())
        setPrimaryColorHex(colorHex)
    }

    fun setReticleSizeDp(sizeDp: Float) {
        val clamped = sizeDp.coerceIn(14f, 90f)
        updateConfig("size") { it.copy(sizeDp = clamped) }
    }

    fun setReticleOpacity(opacity: Float) {
        val clamped = opacity.coerceIn(0.10f, 1.00f)
        updateConfig("opacity") { it.copy(opacity = clamped) }
    }

    fun setReticleRotationDeg(rotationDeg: Float) {
        val normalized = ((rotationDeg % 360f) + 360f) % 360f
        updateConfig("rotation") { it.copy(rotationDeg = normalized) }
    }

    fun setBackground(bg: PreviewBackground) {
        _selectedBackground.value = bg
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavorite(id: String): Boolean {
        return repository.toggleFavorite(id)
    }

    fun toggleDarkMode(enabled: Boolean) {
        repository.setDarkMode(enabled)
    }

    fun setShakeAutoToggle(enabled: Boolean) {
        repository.setShakeAutoToggleEnabled(enabled)
    }

    fun setAutoLightOpacity(enabled: Boolean) {
        repository.setAutoLightOpacityEnabled(enabled)
    }

    fun setProximityAutoHide(enabled: Boolean) {
        repository.setProximityAutoHideEnabled(enabled)
    }

    fun resetUsageStats() {
        repository.resetUsageStats()
    }

    fun clearEventHistory() {
        eventTracker.clearEventHistory()
    }

    fun completeOnboarding() {
        repository.setOnboardingCompleted(true)
    }

    fun reopenOnboarding() {
        repository.reopenOnboarding()
    }

    fun refreshPermissionState() {
        val previouslyGranted = _isOverlayPermissionGranted.value
        val nowGranted = Settings.canDrawOverlays(getApplication())
        _isOverlayPermissionGranted.value = nowGranted
        if (!previouslyGranted && nowGranted) {
            eventTracker.logEvent(AppEventType.PERMISSION_OVERLAY_GRANTED, _currentScreen.value)
        }
    }

    fun logNotificationsPermissionGranted() {
        eventTracker.logEvent(AppEventType.PERMISSION_NOTIFICATIONS_GRANTED, _currentScreen.value)
    }

    fun applyPreset(preset: CrosshairConfig) {
        repository.updateActiveConfig(preset)
        eventTracker.logEvent(
            AppEventType.PRESET_EQUIPPED,
            _currentScreen.value,
            mapOf("preset_name" to preset.name, "style" to preset.style.name)
        )
        triggerHapticFeedback(20)
    }

    fun applyDesignToOverlay(
        design: CrosshairConfig,
        onEnsureOverlayStarted: () -> Unit
    ) {
        repository.updateActiveConfig(design)
        eventTracker.logEvent(
            AppEventType.PRESET_EQUIPPED,
            _currentScreen.value,
            mapOf(
                "preset_name" to design.name,
                "style" to design.style.name,
                "source" to "predefined_designs",
                "overlay_was_active" to isOverlayActive.value
            )
        )
        triggerHapticFeedback(28)
        if (!isOverlayActive.value) {
            onEnsureOverlayStarted()
        }
    }

    fun triggerDesignPreviewFeedback() {
        triggerHapticFeedback(10)
    }

    fun resetToFactoryDefaults() {
        val defaultCrosshair = PredefinedCrosshairCatalog.defaultConfig(getApplication())
        repository.updateActiveConfig(defaultCrosshair)
        eventTracker.logEvent(AppEventType.SETTINGS_RESET, "settings", mapOf("target" to "crosshair_defaults"))
        triggerHapticFeedback(40)
    }

    fun resetSelectedCrosshairSettings() {
        val current = activeConfig.value
        val baseDefault = PredefinedCrosshairCatalog.resolveDefaultConfig(current)
        val restored = current.copy(
            primaryColorHex = baseDefault.primaryColorHex,
            sizeDp = baseDefault.sizeDp,
            opacity = baseDefault.opacity,
            rotationDeg = baseDefault.rotationDeg
        )
        repository.updateActiveConfig(restored)
        eventTracker.logEvent(
            AppEventType.SETTINGS_RESET,
            _currentScreen.value,
            mapOf("target" to "selected_crosshair_settings", "id" to current.id)
        )
        triggerHapticFeedback(30)
    }

    fun nudgeOffset(dx: Int, dy: Int) {
        updateConfig("offset") {
            it.copy(
                offsetX = it.offsetX + dx,
                offsetY = it.offsetY + dy
            )
        }
        triggerHapticFeedback(12)
    }

    fun resetOffset() {
        updateConfig("offset") {
            it.copy(offsetX = 0, offsetY = 0)
        }
        triggerHapticFeedback(25)
    }

    fun saveConfigAsPreset(
        name: String,
        category: String = CrosshairConfig.CATEGORY_CUSTOM,
        sourceConfig: CrosshairConfig = _savePresetDraftConfig.value ?: activeConfig.value,
        overwritePresetId: String? = null
    ): CrosshairConfig {
        val appCtx = getApplication<Application>()
        val cleanName = name.trim().ifEmpty {
            appCtx.getString(R.string.default_my_reticle_name)
        }
        val cleanCategory = category.ifBlank { CrosshairConfig.CATEGORY_CUSTOM }
        val isOverwrite = !overwritePresetId.isNullOrBlank()
        val presetId = if (isOverwrite) {
            overwritePresetId!!
        } else {
            "custom_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
        }
        val newPreset = sourceConfig.normalizedForPreset().copy(
            id = presetId,
            name = cleanName,
            isCustom = true,
            category = cleanCategory
        )
        val savedPreset = repository.saveCustomPreset(newPreset)
        repository.updateActiveConfig(savedPreset)
        eventTracker.logPresetSaved(cleanName, savedPreset.category)
        _savePresetDraftConfig.value = null
        _showSaveDialog.value = false
        _presetFeedbackMessage.value = appCtx.getString(
            if (isOverwrite) R.string.preset_updated_toast else R.string.preset_saved_toast,
            cleanName
        )
        triggerHapticFeedback(30)

        scheduleSmartPopupEvaluation(EngagementMoment.PRESET_SAVED, delayMs = 700L)
        return savedPreset
    }

    fun saveCurrentAsPreset(name: String, category: String = CrosshairConfig.CATEGORY_CUSTOM) {
        saveConfigAsPreset(
            name = name,
            category = category,
            sourceConfig = _savePresetDraftConfig.value ?: activeConfig.value,
            overwritePresetId = null
        )
    }

    fun updatePresetWithActiveConfig(preset: CrosshairConfig): CrosshairConfig {
        return saveConfigAsPreset(
            name = preset.name,
            category = preset.category,
            sourceConfig = activeConfig.value,
            overwritePresetId = preset.id
        )
    }

    fun quickSwitchPreset(preset: CrosshairConfig) {
        applyPreset(preset)
        val appCtx = getApplication<Application>()
        val displayName = PredefinedCrosshairCatalog.resolveDisplayName(appCtx, preset)
        _presetFeedbackMessage.value = appCtx.getString(R.string.preset_switched_toast, displayName)
    }

    fun switchToNextCustomPreset(): CrosshairConfig? {
        val switched = repository.switchToNextCustomPreset() ?: return null
        val appCtx = getApplication<Application>()
        val displayName = PredefinedCrosshairCatalog.resolveDisplayName(appCtx, switched)
        eventTracker.logEvent(
            AppEventType.PRESET_EQUIPPED,
            _currentScreen.value,
            mapOf("preset_name" to switched.name, "style" to switched.style.name, "source" to "quick_cycle")
        )
        _presetFeedbackMessage.value = appCtx.getString(R.string.preset_switched_toast, displayName)
        triggerHapticFeedback(20)
        return switched
    }

    fun renameCustomPreset(preset: CrosshairConfig, newName: String): Boolean {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return false
        val updated = repository.renameCustomPreset(preset.id, trimmed)
        if (updated) {
            _renamingPreset.value = null
            _presetFeedbackMessage.value = getApplication<Application>()
                .getString(R.string.preset_renamed_toast, trimmed)
            triggerHapticFeedback(20)
        }
        return updated
    }

    fun confirmDeletePreset(preset: CrosshairConfig) {
        deletePreset(preset.id)
        _deletingPreset.value = null
        val displayName = PredefinedCrosshairCatalog.resolveDisplayName(getApplication(), preset)
        _presetFeedbackMessage.value = getApplication<Application>()
            .getString(R.string.preset_deleted_toast, displayName)
    }

    fun applyAndSaveAiConfig(config: CrosshairConfig) {
        val aiPreset = config.copy(
            id = if (config.id.isBlank()) "ai_${System.currentTimeMillis()}" else config.id,
            isCustom = true
        )
        repository.saveCustomPreset(aiPreset)
        repository.updateActiveConfig(aiPreset)
        eventTracker.logPresetSaved(aiPreset.name, aiPreset.category)
        triggerHapticFeedback(30)
    }

    fun deletePreset(id: String) {
        repository.deleteCustomPreset(id)
        triggerHapticFeedback(20)
    }

    fun openSaveDialog(config: CrosshairConfig? = null) {
        _savePresetDraftConfig.value = (config ?: activeConfig.value).normalizedForPreset()
        _showSaveDialog.value = true
    }

    fun closeSaveDialog() {
        _savePresetDraftConfig.value = null
        _showSaveDialog.value = false
    }

    fun openRenameDialog(preset: CrosshairConfig) {
        _renamingPreset.value = preset
    }

    fun closeRenameDialog() {
        _renamingPreset.value = null
    }

    fun openDeleteConfirmDialog(preset: CrosshairConfig) {
        _deletingPreset.value = preset
    }

    fun closeDeleteConfirmDialog() {
        _deletingPreset.value = null
    }

    fun clearPresetFeedbackMessage() {
        _presetFeedbackMessage.value = null
    }

    fun openShareDialog(config: CrosshairConfig) {
        _showShareDialog.value = config
        eventTracker.logPresetShared(config.name)
    }

    fun closeShareDialog() {
        val wasOpen = _showShareDialog.value != null
        _showShareDialog.value = null
        if (wasOpen) {
            scheduleSmartPopupEvaluation(EngagementMoment.PRESET_SHARED_OR_IMPORTED, delayMs = 700L)
        }
    }

    fun openImportDialog() {
        _showImportDialog.value = true
    }

    fun closeImportDialog() {
        _showImportDialog.value = false
    }

    fun encodeCrosshairCode(c: CrosshairConfig): String {
        val colorHexStr = String.format(java.util.Locale.US, "%08X", c.primaryColorHex)
        return "TA;${c.style.name};${c.sizeDp.toInt()};${c.thicknessDp};${c.gapDp.toInt()};" +
            "${if (c.centerDot) 1 else 0};${c.centerDotSizeDp};${c.rotationDeg.toInt()};" +
            "${c.opacity};$colorHexStr;${if (c.outlineEnabled) 1 else 0};${if (c.tStyle) 1 else 0};${c.name}"
    }

    fun importCrosshairCode(code: String): Boolean {
        return try {
            val trimmed = code.trim()
            if (!trimmed.startsWith("TA;")) return false
            val parts = trimmed.split(";")
            if (parts.size < 12) return false

            val style = CrosshairStyle.valueOf(parts[1])
            val sizeDp = parts[2].toFloat().coerceIn(10f, 100f)
            val thicknessDp = parts[3].toFloat().coerceIn(1f, 12f)
            val gapDp = parts[4].toFloat().coerceIn(0f, 40f)
            val centerDot = parts[5] == "1"
            val centerDotSizeDp = parts[6].toFloat().coerceIn(1f, 15f)
            val rotationDeg = parts[7].toFloat().coerceIn(0f, 360f)
            val opacity = parts[8].toFloat().coerceIn(0.1f, 1.0f)
            val colorHex = parts[9].toLong(16)
            val outlineEnabled = parts[10] == "1"
            val tStyle = parts[11] == "1"
            val name = if (parts.size > 12) {
                parts.drop(12).joinToString(";").trim().ifBlank {
                    getApplication<Application>().getString(R.string.default_imported_reticle_name)
                }
            } else {
                getApplication<Application>().getString(R.string.default_imported_reticle_name)
            }

            val importedConfig = CrosshairConfig(
                id = "imported_${System.currentTimeMillis()}",
                name = name,
                style = style,
                sizeDp = sizeDp,
                thicknessDp = thicknessDp,
                gapDp = gapDp,
                centerDot = centerDot,
                centerDotSizeDp = centerDotSizeDp,
                rotationDeg = rotationDeg,
                opacity = opacity,
                primaryColorHex = colorHex,
                outlineEnabled = outlineEnabled,
                tStyle = tStyle,
                isCustom = true,
                category = CrosshairConfig.CATEGORY_CUSTOM
            ).normalizedForPreset()

            repository.saveCustomPreset(importedConfig)
            repository.updateActiveConfig(importedConfig)
            eventTracker.logEvent(
                AppEventType.PRESET_IMPORTED,
                _currentScreen.value,
                mapOf("preset_name" to name, "style" to style.name)
            )
            _showImportDialog.value = false
            triggerHapticFeedback(40)
            scheduleSmartPopupEvaluation(EngagementMoment.PRESET_SHARED_OR_IMPORTED, delayMs = 700L)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun toggleOverlay() {
        val context = getApplication<Application>()
        if (!Settings.canDrawOverlays(context)) {
            _isOverlayPermissionGranted.value = false
            _showPermissionDialog.value = true
            return
        }
        _isOverlayPermissionGranted.value = true

        if (isOverlayActive.value) {
            val completedSec = repository.setOverlayActive(false)
            CrosshairOverlayService.stop(context)
            triggerHapticFeedback(25)
            val effectiveSec = maxOf(completedSec, repository.consumeLastCompletedSessionDuration())
            if (effectiveSec >= AppEventTracker.MEANINGFUL_OVERLAY_SECONDS) {
                scheduleSmartPopupEvaluation(EngagementMoment.OVERLAY_SESSION_COMPLETED, delayMs = 800L)
            }
        } else {
            repository.setOverlayActive(true)
            CrosshairOverlayService.start(context)
            triggerHapticFeedback(35)
        }
    }

    fun toggleCalibrateHud() {
        val context = getApplication<Application>()
        if (!Settings.canDrawOverlays(context)) {
            _showPermissionDialog.value = true
            return
        }
        if (!isOverlayActive.value) {
            repository.setOverlayActive(true)
            CrosshairOverlayService.start(context)
        }
        CrosshairOverlayService.toggleCalibrate(context)
        triggerHapticFeedback(25)
    }

    fun openPermissionTutorialDialog() {
        _showPermissionDialog.value = true
    }

    fun dismissPermissionDialog() {
        _showPermissionDialog.value = false
    }

    // --- Smart Popup Evaluation & User Actions ---

    private fun hasAnyOtherDialogOpen(): Boolean {
        return _showSaveDialog.value ||
            _savePresetDraftConfig.value != null ||
            _renamingPreset.value != null ||
            _deletingPreset.value != null ||
            _showPermissionDialog.value ||
            _showShareDialog.value != null ||
            _showImportDialog.value ||
            _activeSmartPopup.value != null
    }

    private fun scheduleSmartPopupEvaluation(moment: EngagementMoment, delayMs: Long = 600L) {
        delayedPopupJob?.cancel()
        delayedPopupJob = viewModelScope.launch {
            delay(delayMs)
            val proposal = popupEngine.evaluateBestPopup(
                EngagementContext(
                    moment = moment,
                    hasCompletedOnboarding = hasCompletedOnboarding.value,
                    isSplashVisible = _isSplashVisible.value,
                    isAimDrillRunning = _gameState.value.state == AimGameState.PLAYING,
                    hasOtherDialogOpen = hasAnyOtherDialogOpen(),
                    currentScreen = _currentScreen.value,
                    savedPresetsCount = savedPresets.value.size
                )
            )
            if (proposal != null) {
                _activeSmartPopup.value = proposal
            }
        }
    }

    // Manual triggers from Settings (always allowed on explicit user tap)
    fun openManualRateDialog() {
        _activeSmartPopup.value = SmartPopupProposal.RateApp(EngagementMoment.RETURNING_USER_SETTLED)
    }

    fun openManualShareDialog() {
        _activeSmartPopup.value = SmartPopupProposal.ShareApp(EngagementMoment.RETURNING_USER_SETTLED)
    }

    fun checkForAppUpdateManually() {
        eventTracker.logEvent(
            AppEventType.UPDATE_CHECKED,
            _currentScreen.value,
            mapOf("current_version_code" to BuildConfig.VERSION_CODE, "manual" to true)
        )
        val available = popupEngine.getAvailableUpdate()
        if (available != null) {
            _activeSmartPopup.value = SmartPopupProposal.UpdateApp(
                currentVersionName = BuildConfig.VERSION_NAME,
                latestVersionName = available.latestVersionName,
                latestVersionCode = available.latestVersionCode,
                releaseNotes = available.releaseNotes
            )
        } else {
            _updateCheckMessageRes.value = R.string.update_up_to_date_msg
        }
    }

    fun clearUpdateCheckMessage() {
        _updateCheckMessageRes.value = null
    }

    fun onSmartUpdateAccepted(versionCode: Int) {
        popupEngine.onUpdateAccepted(versionCode, _currentScreen.value)
        _activeSmartPopup.value = null
    }

    fun onSmartUpdateLater() {
        popupEngine.onUpdateDismissedLater(_currentScreen.value)
        _activeSmartPopup.value = null
    }

    fun onSmartUpdateSkipped(versionCode: Int) {
        popupEngine.onUpdateSkippedVersion(versionCode, _currentScreen.value)
        _activeSmartPopup.value = null
    }

    fun onSmartRateSubmitted(stars: Int) {
        popupEngine.onRateSubmitted(stars, _currentScreen.value)
        _activeSmartPopup.value = null
    }

    fun onSmartRateDismissedLater() {
        popupEngine.onRateDismissedLater(_currentScreen.value)
        _activeSmartPopup.value = null
    }

    fun onSmartRateNeverAsk() {
        popupEngine.onRateNeverAskAgain(_currentScreen.value)
        _activeSmartPopup.value = null
    }

    fun onSmartShareConfirmed() {
        popupEngine.onShareAccepted(_currentScreen.value)
        _activeSmartPopup.value = null
    }

    fun onSmartShareDismissedLater() {
        popupEngine.onShareDismissedLater(_currentScreen.value)
        _activeSmartPopup.value = null
    }

    fun onSmartShareNeverAsk() {
        popupEngine.onShareNeverAskAgain(_currentScreen.value)
        _activeSmartPopup.value = null
    }

    // --- Aim Trainer Logic ---

    fun setDifficulty(difficulty: AimDifficulty) {
        _selectedDifficulty.value = difficulty
    }

    fun startAimTrainer() {
        gameJob?.cancel()
        pendingHighScorePopupCheck = false
        reactionTimes.clear()
        _lastHitFeedback.value = null

        eventTracker.logEvent(
            AppEventType.AIM_DRILL_STARTED,
            "trainer",
            mapOf("difficulty" to _selectedDifficulty.value.name)
        )

        val firstTarget = generateRandomTarget()
        _gameState.value = AimGameStats(
            state = AimGameState.PLAYING,
            score = 0,
            targetsHit = 0,
            totalTargets = 0,
            timeLeftSec = 20,
            avgReactionTimeMs = 0,
            headshots = 0,
            currentStreak = 0,
            bestStreak = 0,
            currentTarget = firstTarget
        )

        gameJob = viewModelScope.launch {
            for (sec in 19 downTo 0) {
                delay(1000L)
                _gameState.value = _gameState.value.copy(timeLeftSec = sec)
            }
            endAimTrainer()
        }
    }

    fun dismissAimSummaryToIdle() {
        _gameState.value = AimGameStats(state = AimGameState.IDLE)
        if (pendingHighScorePopupCheck) {
            pendingHighScorePopupCheck = false
            scheduleSmartPopupEvaluation(EngagementMoment.AIM_DRILL_HIGH_SCORE, delayMs = 450L)
        }
    }

    private fun cancelAimTrainer() {
        gameJob?.cancel()
        gameJob = null
        _gameState.value = AimGameStats(state = AimGameState.IDLE)
    }

    fun onTargetHit(isHeadshot: Boolean = false) {
        val current = _gameState.value
        if (current.state != AimGameState.PLAYING) return

        val target = current.currentTarget ?: return
        val reaction = (System.currentTimeMillis() - target.spawnTimeMs).coerceAtLeast(1L)
        reactionTimes.add(reaction)
        val avgReaction = reactionTimes.average().toLong()

        val speedBonus = (1000L - reaction).coerceAtLeast(0L) / 10L
        val nextStreak = current.currentStreak + 1
        val nextBestStreak = maxOf(current.bestStreak, nextStreak)
        val streakBonus = nextStreak * 15
        val headshotBonus = if (isHeadshot) 150 else 0
        val points = 100 + speedBonus.toInt() + streakBonus + headshotBonus

        _lastHitFeedback.value = HitFeedback(
            id = System.currentTimeMillis(),
            xFraction = target.xFraction,
            yFraction = target.yFraction,
            isHeadshot = isHeadshot,
            scoreDelta = points
        )

        triggerHapticFeedback(if (isHeadshot) 30 else 15)

        val nextTarget = generateRandomTarget()
        _gameState.value = current.copy(
            score = current.score + points,
            targetsHit = current.targetsHit + 1,
            totalTargets = current.totalTargets + 1,
            avgReactionTimeMs = avgReaction,
            headshots = if (isHeadshot) current.headshots + 1 else current.headshots,
            currentStreak = nextStreak,
            bestStreak = nextBestStreak,
            currentTarget = nextTarget
        )
    }

    fun onTargetMiss() {
        val current = _gameState.value
        if (current.state != AimGameState.PLAYING) return

        _lastMissTimestampMs.value = System.currentTimeMillis()
        triggerHapticFeedback(40)
        _gameState.value = current.copy(
            score = (current.score - 25).coerceAtLeast(0),
            totalTargets = current.totalTargets + 1,
            currentStreak = 0
        )
    }

    private fun endAimTrainer() {
        gameJob?.cancel()
        val finalStats = _gameState.value.copy(
            state = AimGameState.FINISHED,
            currentTarget = null
        )
        _gameState.value = finalStats

        val accuracy = if (finalStats.totalTargets > 0) {
            ((finalStats.targetsHit.toFloat() / finalStats.totalTargets.toFloat()) * 100f).toInt()
        } else 0

        eventTracker.logAimDrillCompleted(
            score = finalStats.score,
            accuracyPercent = accuracy,
            difficulty = _selectedDifficulty.value.name,
            bestStreak = finalStats.bestStreak
        )
        triggerHapticFeedback(50)

        // Never cover the user's SummaryScoreCard immediately! Wait for user to finish reviewing score.
        if (finalStats.score >= 800 && accuracy >= 55) {
            pendingHighScorePopupCheck = true
        }
    }

    private fun generateRandomTarget(): AimTarget {
        val x = Random.nextFloat() * 0.72f + 0.14f
        val y = Random.nextFloat() * 0.66f + 0.17f
        val diffRange = _selectedDifficulty.value.targetRadiusRange
        val radius = diffRange.start + Random.nextFloat() * (diffRange.endInclusive - diffRange.start)
        val isHeadshotRing = Random.nextFloat() < 0.30f

        return AimTarget(
            id = System.currentTimeMillis() + Random.nextInt(1000),
            xFraction = x,
            yFraction = y,
            radiusDp = radius,
            spawnTimeMs = System.currentTimeMillis(),
            isHeadshotRing = isHeadshotRing
        )
    }

    private fun triggerHapticFeedback(durationMs: Long) {
        val context = getApplication<Application>()
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(durationMs)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        gameJob?.cancel()
        delayedPopupJob?.cancel()
    }
}
