package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CrosshairRepository
import com.example.model.CrosshairConfig
import com.example.model.CrosshairStyle
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
import kotlin.random.Random

enum class PreviewBackground(val displayName: String, val darkTone: Boolean) {
    CQB_DEPOT("CQB Depot", true),
    DESERT_OUTPOST("Desert Camp", false),
    CYBER_NIGHT("Cyber City", true),
    TARGET_RANGE("Paper Range", false),
    SNIPER_MOUNTAIN("Mountain Valley", false),
    TACTICAL_DARK("Carbon Dark", true)
}

enum class AimDifficulty(val title: String, val radiusMultiplier: Float, val scoreBonus: Int) {
    EASY("Casual", 1.35f, 0),
    MEDIUM("Operator", 1.0f, 50),
    HARD("Apex Predator", 0.65f, 120)
}

enum class AimGameState {
    IDLE,
    PLAYING,
    FINISHED
}

data class TargetPosition(
    val id: Long = System.currentTimeMillis(),
    val xFraction: Float = 0.5f,
    val yFraction: Float = 0.5f,
    val spawnTime: Long = System.currentTimeMillis(),
    val isHeadshotRing: Boolean = false,
    val radiusDp: Float = 28f
)

data class AimGameStats(
    val state: AimGameState = AimGameState.IDLE,
    val score: Int = 0,
    val targetsHit: Int = 0,
    val totalTargets: Int = 0,
    val headshots: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val avgReactionTimeMs: Long = 0,
    val timeLeftSec: Int = 20,
    val currentTarget: TargetPosition? = null,
    val difficulty: AimDifficulty = AimDifficulty.MEDIUM
)

class CrosshairViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CrosshairRepository(application)
    private val vibrator = application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    val activeConfig: StateFlow<CrosshairConfig> = repository.activeConfig
    val isOverlayActive: StateFlow<Boolean> = repository.isOverlayActive
    val savedPresets: StateFlow<List<CrosshairConfig>> = repository.savedPresets
    val usageStats: StateFlow<com.example.data.UsageStats> = repository.usageStats
    val isDarkMode: StateFlow<Boolean> = repository.isDarkMode
    val hasCompletedOnboarding: StateFlow<Boolean> = repository.hasCompletedOnboarding

    private val _showSmartRateDialog = MutableStateFlow(false)
    val showSmartRateDialog: StateFlow<Boolean> = _showSmartRateDialog.asStateFlow()

    private val _showSmartShareDialog = MutableStateFlow(false)
    val showSmartShareDialog: StateFlow<Boolean> = _showSmartShareDialog.asStateFlow()

    fun toggleDarkMode(enabled: Boolean) {
        repository.setDarkMode(enabled)
        hapticFeedback(20)
    }

    fun toggleShakeAutoToggle(enabled: Boolean) {
        repository.setShakeAutoToggleEnabled(enabled)
        hapticFeedback(20)
    }

    fun toggleAutoLightOpacity(enabled: Boolean) {
        repository.setAutoLightOpacityEnabled(enabled)
        hapticFeedback(20)
    }

    fun toggleProximityAutoHide(enabled: Boolean) {
        repository.setProximityAutoHideEnabled(enabled)
        hapticFeedback(20)
    }

    fun resetUsageStats() {
        repository.resetUsageStats()
        hapticFeedback(40)
    }

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedBackground = MutableStateFlow(PreviewBackground.CQB_DEPOT)
    val selectedBackground: StateFlow<PreviewBackground> = _selectedBackground.asStateFlow()

    private val _showSaveDialog = MutableStateFlow(false)
    val showSaveDialog: StateFlow<Boolean> = _showSaveDialog.asStateFlow()

    private val _showPermissionDialog = MutableStateFlow(false)
    val showPermissionDialog: StateFlow<Boolean> = _showPermissionDialog.asStateFlow()

    private val _showShareDialog = MutableStateFlow<CrosshairConfig?>(null)
    val showShareDialog: StateFlow<CrosshairConfig?> = _showShareDialog.asStateFlow()

    private val _showImportDialog = MutableStateFlow(false)
    val showImportDialog: StateFlow<Boolean> = _showImportDialog.asStateFlow()

    private val _showGettingStartedSequence = MutableStateFlow(
        application.getSharedPreferences("crosshair_pro_prefs", Context.MODE_PRIVATE)
            .getBoolean("show_getting_started", true)
    )
    val showGettingStartedSequence: StateFlow<Boolean> = _showGettingStartedSequence.asStateFlow()

    private val _isOverlayPermissionGranted = MutableStateFlow(
        Settings.canDrawOverlays(application)
    )
    val isOverlayPermissionGranted: StateFlow<Boolean> = _isOverlayPermissionGranted.asStateFlow()

    private val _selectedDifficulty = MutableStateFlow(AimDifficulty.MEDIUM)
    val selectedDifficulty: StateFlow<AimDifficulty> = _selectedDifficulty.asStateFlow()

    // Aim Trainer Game State
    private val _gameState = MutableStateFlow(AimGameStats())
    val gameState: StateFlow<AimGameStats> = _gameState.asStateFlow()
    private var gameJob: Job? = null
    private val reactionTimes = mutableListOf<Long>()

    val allPresets: StateFlow<List<CrosshairConfig>> =
        combine(repository.savedPresets, _selectedCategory, _searchQuery) { saved, category, query ->
            val combined = saved + CrosshairRepository.DEFAULT_PRESETS
            var filtered = if (category == "All") combined
            else if (category == "Custom") combined.filter { it.isCustom || it.category == "Custom" }
            else combined.filter { it.category.equals(category, ignoreCase = true) || it.style.category.equals(category, ignoreCase = true) }

            if (query.isNotBlank()) {
                val q = query.trim().lowercase()
                filtered = filtered.filter {
                    it.name.lowercase().contains(q) ||
                            it.category.lowercase().contains(q) ||
                            it.style.displayName.lowercase().contains(q)
                }
            }
            filtered
        }.stateIn(viewModelScope, SharingStarted.Lazily, CrosshairRepository.DEFAULT_PRESETS)

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setDifficulty(diff: AimDifficulty) {
        _selectedDifficulty.value = diff
    }

    fun setBackground(bg: PreviewBackground) {
        _selectedBackground.value = bg
    }

    fun updateConfig(update: (CrosshairConfig) -> CrosshairConfig) {
        val newConfig = update(activeConfig.value)
        repository.updateActiveConfig(newConfig)
        hapticFeedback(12)
    }

    fun applyPreset(preset: CrosshairConfig) {
        repository.updateActiveConfig(preset)
        hapticFeedback(30)
    }

    fun resetToFactoryDefaults() {
        val defaultConfig = CrosshairConfig()
        repository.updateActiveConfig(defaultConfig)
        hapticFeedback(50)
    }

    fun toggleOverlay() {
        val context = getApplication<Application>()
        if (!Settings.canDrawOverlays(context)) {
            _showPermissionDialog.value = true
            return
        }

        val currentState = isOverlayActive.value
        if (currentState) {
            CrosshairOverlayService.stop(context)
        } else {
            CrosshairOverlayService.start(context)
        }
        hapticFeedback(50)
    }

    fun toggleCalibrateHud() {
        val context = getApplication<Application>()
        if (Settings.canDrawOverlays(context)) {
            CrosshairOverlayService.toggleCalibrate(context)
            hapticFeedback(30)
        } else {
            _showPermissionDialog.value = true
        }
    }

    fun dismissPermissionDialog() {
        _showPermissionDialog.value = false
    }

    fun openSaveDialog() {
        _showSaveDialog.value = true
    }

    fun closeSaveDialog() {
        _showSaveDialog.value = false
    }

    fun openShareDialog(config: CrosshairConfig) {
        _showShareDialog.value = config
    }

    fun closeShareDialog() {
        _showShareDialog.value = null
    }

    fun openImportDialog() {
        _showImportDialog.value = true
    }

    fun closeImportDialog() {
        _showImportDialog.value = false
    }

    fun encodeCrosshairCode(config: CrosshairConfig): String {
        val hex = java.lang.Long.toHexString(config.primaryColorHex).uppercase()
        return "TA;${config.style.name};$hex;${config.sizeDp};${config.thicknessDp};${config.gapDp};${config.centerDot};${config.tStyle};${config.outlineEnabled};${config.rotationDeg};${config.opacity};${config.name.replace(';', '_')}"
    }

    fun importCrosshairCode(code: String): Boolean {
        val trimmed = code.trim()
        if (trimmed.isBlank()) return false

        // 1. Try JSON import
        val jsonConfig = CrosshairConfig.fromJson(trimmed)
        if (jsonConfig != null) {
            val finalConfig = jsonConfig.copy(
                id = "import_${System.currentTimeMillis()}",
                isCustom = true,
                category = if (jsonConfig.category.isNotBlank() && jsonConfig.category != "Custom") jsonConfig.category else "Custom"
            )
            repository.saveCustomPreset(finalConfig)
            repository.updateActiveConfig(finalConfig)
            _showImportDialog.value = false
            hapticFeedback(50)
            return true
        }

        // 2. Try compact TA code format
        try {
            val parts = trimmed.split(";")
            if (parts.size < 10 || parts[0] != "TA") return false

            val style = try { CrosshairStyle.valueOf(parts[1]) } catch (e: Exception) { CrosshairStyle.CLASSIC_CROSS }
            val colorHex = try { java.lang.Long.parseLong(parts[2], 16) } catch (e: Exception) { 0xFF00E676 }
            val size = parts[3].toFloatOrNull() ?: 36f
            val thickness = parts[4].toFloatOrNull() ?: 3f
            val gap = parts[5].toFloatOrNull() ?: 8f
            val centerDot = parts[6].toBooleanStrictOrNull() ?: true
            val tStyle = parts[7].toBooleanStrictOrNull() ?: false
            val outline = parts[8].toBooleanStrictOrNull() ?: true
            val rot = parts[9].toFloatOrNull() ?: 0f
            val opacity = parts.getOrNull(10)?.toFloatOrNull() ?: 1.0f
            val name = parts.getOrNull(11)?.replace('_', ' ') ?: "Imported Reticle"

            val imported = CrosshairConfig(
                id = "import_${System.currentTimeMillis()}",
                name = name,
                style = style,
                sizeDp = size.coerceIn(12f, 100f),
                thicknessDp = thickness.coerceIn(1f, 16f),
                gapDp = gap.coerceIn(0f, 40f),
                centerDot = centerDot,
                tStyle = tStyle,
                outlineEnabled = outline,
                rotationDeg = rot,
                opacity = opacity.coerceIn(0.2f, 1.0f),
                primaryColorHex = colorHex,
                isCustom = true,
                category = "Custom"
            )

            repository.saveCustomPreset(imported)
            repository.updateActiveConfig(imported)
            _showImportDialog.value = false
            hapticFeedback(50)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun refreshPermissionState() {
        val context = getApplication<Application>()
        _isOverlayPermissionGranted.value = Settings.canDrawOverlays(context)
    }

    fun dismissGettingStarted() {
        _showGettingStartedSequence.value = false
        getApplication<Application>().getSharedPreferences("crosshair_pro_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("show_getting_started", false).apply()
        hapticFeedback(20)
    }

    fun reopenGettingStarted() {
        _showGettingStartedSequence.value = true
        getApplication<Application>().getSharedPreferences("crosshair_pro_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("show_getting_started", true).apply()
        hapticFeedback(30)
    }

    fun setHexColor(hexString: String): Boolean {
        try {
            val cleanHex = hexString.trim().removePrefix("#").removePrefix("0x")
            val parsedHex = when (cleanHex.length) {
                6 -> 0xFF000000L or java.lang.Long.parseLong(cleanHex, 16)
                8 -> java.lang.Long.parseLong(cleanHex, 16)
                else -> return false
            }
            updateConfig { it.copy(primaryColorHex = parsedHex) }
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun setRgbColor(r: Int, g: Int, b: Int) {
        val red = r.coerceIn(0, 255)
        val green = g.coerceIn(0, 255)
        val blue = b.coerceIn(0, 255)
        val colorHex = 0xFF000000L or (red.toLong() shl 16) or (green.toLong() shl 8) or blue.toLong()
        updateConfig { it.copy(primaryColorHex = colorHex) }
    }

    fun setAlphaOpacity(alpha: Float) {
        val clamped = alpha.coerceIn(0.1f, 1.0f)
        updateConfig { it.copy(opacity = clamped) }
    }

    fun completeOnboarding() {
        repository.setOnboardingCompleted(true)
        hapticFeedback(30)
    }

    fun reopenOnboarding() {
        repository.reopenOnboarding()
        hapticFeedback(30)
    }

    fun evaluateSmartPopups() {
        if (repository.shouldShowRatePopup()) {
            _showSmartRateDialog.value = true
        } else if (repository.shouldShowSharePopup()) {
            _showSmartShareDialog.value = true
        }
    }

    fun markAppRated() {
        repository.markAppRated()
        _showSmartRateDialog.value = false
        hapticFeedback(30)
    }

    fun dismissRatePrompt() {
        repository.dismissRatePrompt()
        _showSmartRateDialog.value = false
    }

    fun markAppShared() {
        repository.markAppShared()
        _showSmartShareDialog.value = false
        hapticFeedback(30)
    }

    fun dismissSharePrompt() {
        repository.dismissSharePrompt()
        _showSmartShareDialog.value = false
    }

    fun saveCurrentAsPreset(name: String, gameTag: String = "Custom") {
        val current = activeConfig.value
        val finalName = name.ifBlank { "Custom Reticle" }
        val preset = current.copy(
            id = "custom_${System.currentTimeMillis()}",
            name = finalName,
            isCustom = true,
            category = if (gameTag.isNotBlank() && gameTag != "Custom") gameTag else "Custom"
        )
        repository.saveCustomPreset(preset)
        _showSaveDialog.value = false
        hapticFeedback(40)
        evaluateSmartPopups()
    }

    fun deletePreset(id: String) {
        repository.deleteCustomPreset(id)
        hapticFeedback(30)
    }

    fun nudgeOffset(dx: Int, dy: Int) {
        val current = activeConfig.value
        updateConfig {
            it.copy(
                offsetX = (it.offsetX + dx).coerceIn(-500, 500),
                offsetY = (it.offsetY + dy).coerceIn(-500, 500)
            )
        }
        hapticFeedback(15)
    }

    fun resetOffset() {
        updateConfig { it.copy(offsetX = 0, offsetY = 0) }
        hapticFeedback(30)
    }

    // Aim Game Actions
    fun startAimTrainer() {
        gameJob?.cancel()
        reactionTimes.clear()
        val diff = _selectedDifficulty.value
        _gameState.value = AimGameStats(
            state = AimGameState.PLAYING,
            score = 0,
            targetsHit = 0,
            totalTargets = 0,
            headshots = 0,
            currentStreak = 0,
            bestStreak = 0,
            avgReactionTimeMs = 0,
            timeLeftSec = 20,
            difficulty = diff,
            currentTarget = generateNewTarget()
        )
        hapticFeedback(60)

        gameJob = viewModelScope.launch {
            while (_gameState.value.timeLeftSec > 0 && _gameState.value.state == AimGameState.PLAYING) {
                delay(1000)
                val newTime = _gameState.value.timeLeftSec - 1
                if (newTime <= 0) {
                    endAimTrainer()
                } else {
                    _gameState.value = _gameState.value.copy(timeLeftSec = newTime)
                }
            }
        }
    }

    fun onTargetHit(isHeadshot: Boolean) {
        val current = _gameState.value
        if (current.state != AimGameState.PLAYING) return

        val reactionTime = System.currentTimeMillis() - (current.currentTarget?.spawnTime ?: System.currentTimeMillis())
        reactionTimes.add(reactionTime.coerceAtLeast(100))

        val avg = if (reactionTimes.isNotEmpty()) reactionTimes.average().toLong() else 0
        val newStreak = current.currentStreak + 1
        val best = maxOf(current.bestStreak, newStreak)
        val scoreDelta = (if (isHeadshot) 150 else 100) + (newStreak * 10) + current.difficulty.scoreBonus

        hapticFeedback(if (isHeadshot) 60 else 30)

        _gameState.value = current.copy(
            score = current.score + scoreDelta,
            targetsHit = current.targetsHit + 1,
            totalTargets = current.totalTargets + 1,
            headshots = if (isHeadshot) current.headshots + 1 else current.headshots,
            currentStreak = newStreak,
            bestStreak = best,
            avgReactionTimeMs = avg,
            currentTarget = generateNewTarget()
        )
    }

    fun onTargetMiss() {
        val current = _gameState.value
        if (current.state != AimGameState.PLAYING) return

        _gameState.value = current.copy(
            totalTargets = current.totalTargets + 1,
            currentStreak = 0
        )
        hapticFeedback(20)
    }

    fun endAimTrainer() {
        gameJob?.cancel()
        _gameState.value = _gameState.value.copy(
            state = AimGameState.FINISHED,
            currentTarget = null
        )
        repository.incrementAimGamesCompleted()
        hapticFeedback(80)
        evaluateSmartPopups()
    }

    private fun generateNewTarget(): TargetPosition {
        val diff = _selectedDifficulty.value
        val baseRadius = 26f * diff.radiusMultiplier
        return TargetPosition(
            id = System.currentTimeMillis(),
            xFraction = Random.nextFloat() * 0.7f + 0.15f,
            yFraction = Random.nextFloat() * 0.65f + 0.15f,
            spawnTime = System.currentTimeMillis(),
            isHeadshotRing = Random.nextBoolean(),
            radiusDp = baseRadius
        )
    }

    private fun hapticFeedback(millis: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(millis)
            }
        } catch (e: Exception) {
            // ignore
        }
    }
}
