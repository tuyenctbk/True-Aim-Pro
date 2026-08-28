package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.data.room.AppDatabase
import com.example.data.room.PresetEntity
import com.example.model.CrosshairConfig
import com.example.model.CrosshairStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class UsageStats(
    val totalActiveTimeSeconds: Long = 0L,
    val totalSessionsCount: Int = 0,
    val currentSessionDurationSeconds: Long = 0L,
    val averageSessionDurationSeconds: Long = 0L,
    val shakeAutoToggleEnabled: Boolean = true,
    val autoLightOpacityEnabled: Boolean = true,
    val proximityAutoHideEnabled: Boolean = true
)

class CrosshairRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("crosshair_pro_prefs", Context.MODE_PRIVATE)

    private val database = AppDatabase.getDatabase(context)
    private val presetDao = database.presetDao()
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private var sessionTimerJob: Job? = null
    private var currentSessionStartTimeMs: Long = 0L

    private val _activeConfig = MutableStateFlow(loadActiveConfig())
    val activeConfig: StateFlow<CrosshairConfig> = _activeConfig.asStateFlow()

    private val _isOverlayActive = MutableStateFlow(prefs.getBoolean("is_overlay_active", false))
    val isOverlayActive: StateFlow<Boolean> = _isOverlayActive.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _hasCompletedOnboarding = MutableStateFlow(prefs.getBoolean("has_completed_onboarding", false))
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    private val _savedPresets = MutableStateFlow<List<CrosshairConfig>>(emptyList())
    val savedPresets: StateFlow<List<CrosshairConfig>> = _savedPresets.asStateFlow()

    private val _usageStats = MutableStateFlow(loadUsageStats())
    val usageStats: StateFlow<UsageStats> = _usageStats.asStateFlow()

    init {
        repositoryScope.launch {
            presetDao.getAllPresets().collectLatest { entities ->
                _savedPresets.value = entities.map { it.toCrosshairConfig() }
            }
        }

        if (_isOverlayActive.value) {
            startSessionTimer()
        }
    }

    private fun loadUsageStats(): UsageStats {
        val totalSec = prefs.getLong("total_active_time_sec", 0L)
        val sessions = prefs.getInt("total_sessions_count", 0)
        val shake = prefs.getBoolean("shake_auto_toggle_enabled", true)
        val light = prefs.getBoolean("auto_light_opacity_enabled", true)
        val prox = prefs.getBoolean("proximity_auto_hide_enabled", true)
        val avgSec = if (sessions > 0) totalSec / sessions else 0L

        return UsageStats(
            totalActiveTimeSeconds = totalSec,
            totalSessionsCount = sessions,
            currentSessionDurationSeconds = 0L,
            averageSessionDurationSeconds = avgSec,
            shakeAutoToggleEnabled = shake,
            autoLightOpacityEnabled = light,
            proximityAutoHideEnabled = prox
        )
    }

    private fun startSessionTimer() {
        currentSessionStartTimeMs = System.currentTimeMillis()
        val currentTotalSessions = prefs.getInt("total_sessions_count", 0)
        val newSessions = currentTotalSessions + 1
        prefs.edit().putInt("total_sessions_count", newSessions).apply()

        sessionTimerJob?.cancel()
        sessionTimerJob = repositoryScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                val elapsedSec = (System.currentTimeMillis() - currentSessionStartTimeMs) / 1000
                val totalSec = prefs.getLong("total_active_time_sec", 0L) + 1
                prefs.edit().putLong("total_active_time_sec", totalSec).apply()

                val avgSec = if (newSessions > 0) totalSec / newSessions else 0L
                _usageStats.value = _usageStats.value.copy(
                    totalActiveTimeSeconds = totalSec,
                    totalSessionsCount = newSessions,
                    currentSessionDurationSeconds = elapsedSec,
                    averageSessionDurationSeconds = avgSec
                )
            }
        }
    }

    fun setOverlayActive(active: Boolean) {
        _isOverlayActive.value = active
        prefs.edit().putBoolean("is_overlay_active", active).apply()

        if (active) {
            startSessionTimer()
        } else {
            sessionTimerJob?.cancel()
            sessionTimerJob = null
            _usageStats.value = _usageStats.value.copy(
                currentSessionDurationSeconds = 0L
            )
        }
    }

    fun setShakeAutoToggleEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("shake_auto_toggle_enabled", enabled).apply()
        _usageStats.value = _usageStats.value.copy(shakeAutoToggleEnabled = enabled)
    }

    fun setAutoLightOpacityEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_light_opacity_enabled", enabled).apply()
        _usageStats.value = _usageStats.value.copy(autoLightOpacityEnabled = enabled)
    }

    fun setProximityAutoHideEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("proximity_auto_hide_enabled", enabled).apply()
        _usageStats.value = _usageStats.value.copy(proximityAutoHideEnabled = enabled)
    }

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("is_dark_mode", enabled).apply()
        _isDarkMode.value = enabled
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean("has_completed_onboarding", completed).apply()
        _hasCompletedOnboarding.value = completed
    }

    fun reopenOnboarding() {
        prefs.edit().putBoolean("has_completed_onboarding", false).apply()
        _hasCompletedOnboarding.value = false
    }

    fun incrementAimGamesCompleted() {
        val count = prefs.getInt("aim_games_completed_count", 0) + 1
        prefs.edit().putInt("aim_games_completed_count", count).apply()
    }

    fun shouldShowRatePopup(): Boolean {
        val hasRated = prefs.getBoolean("has_rated_app", false)
        if (hasRated) return false

        val lastDismissed = prefs.getLong("rate_prompt_dismissed_time", 0L)
        val currentTime = System.currentTimeMillis()
        val oneDayMs = 24 * 3600 * 1000L
        if (currentTime - lastDismissed < oneDayMs) return false

        val sessions = prefs.getInt("total_sessions_count", 0)
        val aimGames = prefs.getInt("aim_games_completed_count", 0)
        val presetsCount = _savedPresets.value.size

        return sessions >= 2 || aimGames >= 1 || presetsCount >= 1
    }

    fun markAppRated() {
        prefs.edit().putBoolean("has_rated_app", true).apply()
    }

    fun dismissRatePrompt() {
        prefs.edit().putLong("rate_prompt_dismissed_time", System.currentTimeMillis()).apply()
    }

    fun shouldShowSharePopup(): Boolean {
        val hasShared = prefs.getBoolean("has_shared_app", false)
        if (hasShared) return false

        val lastDismissed = prefs.getLong("share_prompt_dismissed_time", 0L)
        val currentTime = System.currentTimeMillis()
        val oneDayMs = 24 * 3600 * 1000L
        if (currentTime - lastDismissed < oneDayMs) return false

        val aimGames = prefs.getInt("aim_games_completed_count", 0)
        val presetsCount = _savedPresets.value.size

        return aimGames >= 2 || presetsCount >= 2
    }

    fun markAppShared() {
        prefs.edit().putBoolean("has_shared_app", true).apply()
    }

    fun dismissSharePrompt() {
        prefs.edit().putLong("share_prompt_dismissed_time", System.currentTimeMillis()).apply()
    }

    fun resetUsageStats() {
        prefs.edit()
            .putLong("total_active_time_sec", 0L)
            .putInt("total_sessions_count", 0)
            .apply()
        _usageStats.value = _usageStats.value.copy(
            totalActiveTimeSeconds = 0L,
            totalSessionsCount = 0,
            currentSessionDurationSeconds = 0L,
            averageSessionDurationSeconds = 0L
        )
    }

    companion object {
        val DEFAULT_PRESETS = listOf(
            CrosshairConfig(
                id = "preset_toxic_pro",
                name = "Toxic CS Pro",
                style = CrosshairStyle.CLASSIC_CROSS,
                sizeDp = 34f,
                thicknessDp = 2.5f,
                gapDp = 6f,
                centerDot = true,
                centerDotSizeDp = 3f,
                primaryColorHex = 0xFF00E676,
                outlineEnabled = true,
                category = "Classic"
            ),
            CrosshairConfig(
                id = "preset_valorant_dot",
                name = "Valorant One-Tap",
                style = CrosshairStyle.PRECISION_DOT,
                sizeDp = 24f,
                thicknessDp = 2f,
                gapDp = 4f,
                centerDot = true,
                centerDotSizeDp = 4.5f,
                primaryColorHex = 0xFF00E5FF,
                outlineEnabled = true,
                category = "Dot"
            ),
            CrosshairConfig(
                id = "preset_pubg_t",
                name = "PUBG Headshot T",
                style = CrosshairStyle.T_SHAPE,
                sizeDp = 38f,
                thicknessDp = 3f,
                gapDp = 8f,
                centerDot = false,
                tStyle = true,
                primaryColorHex = 0xFF76FF03,
                outlineEnabled = true,
                category = "Classic"
            ),
            CrosshairConfig(
                id = "preset_warzone_circle",
                name = "Warzone Tactical",
                style = CrosshairStyle.TACTICAL_CIRCLE,
                sizeDp = 44f,
                thicknessDp = 2.5f,
                gapDp = 10f,
                centerDot = true,
                centerDotSizeDp = 3.5f,
                primaryColorHex = 0xFFFFEA00,
                outlineEnabled = true,
                category = "Tactical"
            ),
            CrosshairConfig(
                id = "preset_cod_sniper",
                name = "COD Mil-Dot Sniper",
                style = CrosshairStyle.MIL_DOT_SNIPER,
                sizeDp = 48f,
                thicknessDp = 2.5f,
                gapDp = 12f,
                centerDot = true,
                centerDotSizeDp = 4f,
                primaryColorHex = 0xFFFF1744,
                outlineEnabled = true,
                category = "Sniper"
            ),
            CrosshairConfig(
                id = "preset_apex_chevron",
                name = "Apex Wingman V",
                style = CrosshairStyle.CHEVRON_ARROW,
                sizeDp = 38f,
                thicknessDp = 3.5f,
                gapDp = 6f,
                centerDot = false,
                primaryColorHex = 0xFFFF9100,
                outlineEnabled = true,
                category = "Tactical"
            ),
            CrosshairConfig(
                id = "preset_shotgun_cqb",
                name = "Shotgun CQB Box",
                style = CrosshairStyle.SHOTGUN_BOX,
                sizeDp = 46f,
                thicknessDp = 3f,
                gapDp = 10f,
                centerDot = true,
                centerDotSizeDp = 4f,
                primaryColorHex = 0xFF00E5FF,
                outlineEnabled = true,
                category = "Tactical"
            ),
            CrosshairConfig(
                id = "preset_cyber_hex",
                name = "Cyberpunk Hex HUD",
                style = CrosshairStyle.CYBER_HEX,
                sizeDp = 46f,
                thicknessDp = 2.5f,
                gapDp = 6f,
                centerDot = true,
                centerDotSizeDp = 3f,
                primaryColorHex = 0xFFD500F9,
                outlineEnabled = true,
                category = "Sci-Fi"
            ),
            CrosshairConfig(
                id = "preset_micro_plus",
                name = "Micro Precision +",
                style = CrosshairStyle.MICRO_PLUS,
                sizeDp = 22f,
                thicknessDp = 2f,
                gapDp = 0f,
                centerDot = false,
                primaryColorHex = 0xFFFFFFFF,
                outlineEnabled = true,
                category = "Dot"
            ),
            CrosshairConfig(
                id = "preset_tri_blade",
                name = "Sci-Fi Tri-Blade",
                style = CrosshairStyle.TRI_BLADE,
                sizeDp = 42f,
                thicknessDp = 3f,
                gapDp = 6f,
                centerDot = true,
                centerDotSizeDp = 4f,
                primaryColorHex = 0xFF76FF03,
                outlineEnabled = true,
                category = "Sci-Fi"
            ),
            CrosshairConfig(
                id = "preset_sonar_radar",
                name = "Sonar Radar 360",
                style = CrosshairStyle.SONAR_RADAR,
                sizeDp = 50f,
                thicknessDp = 2f,
                gapDp = 8f,
                centerDot = true,
                centerDotSizeDp = 3.5f,
                primaryColorHex = 0xFF40C4FF,
                outlineEnabled = true,
                category = "Sci-Fi"
            ),
            CrosshairConfig(
                id = "preset_dynamic_ring",
                name = "Segmented Ring",
                style = CrosshairStyle.DYNAMIC_RING,
                sizeDp = 40f,
                thicknessDp = 3.5f,
                gapDp = 6f,
                centerDot = true,
                centerDotSizeDp = 3.5f,
                primaryColorHex = 0xFFFF5252,
                outlineEnabled = true,
                category = "Tactical"
            ),
            CrosshairConfig(
                id = "preset_diamond_dot",
                name = "Hollow Diamond",
                style = CrosshairStyle.HOLLOW_DIAMOND,
                sizeDp = 32f,
                thicknessDp = 2.5f,
                gapDp = 4f,
                centerDot = true,
                centerDotSizeDp = 3f,
                primaryColorHex = 0xFFFFEA00,
                outlineEnabled = true,
                category = "Dot"
            )
        )
    }

    fun updateActiveConfig(config: CrosshairConfig) {
        _activeConfig.value = config
        saveConfigToPrefs(config)
    }

    fun saveCustomPreset(preset: CrosshairConfig) {
        val updatedPreset = preset.copy(
            isCustom = true,
            category = if (preset.category.isNotBlank() && preset.category != "Custom") preset.category else "Custom"
        )
        repositoryScope.launch {
            presetDao.insertPreset(PresetEntity.fromCrosshairConfig(updatedPreset))
        }
    }

    fun deleteCustomPreset(id: String) {
        repositoryScope.launch {
            presetDao.deletePresetById(id)
        }
    }

    private fun saveConfigToPrefs(config: CrosshairConfig) {
        val json = configToJson(config)
        prefs.edit().putString("active_crosshair_config", json.toString()).apply()
    }

    private fun loadActiveConfig(): CrosshairConfig {
        val jsonStr = prefs.getString("active_crosshair_config", null) ?: return DEFAULT_PRESETS[0]
        return try {
            jsonToConfig(JSONObject(jsonStr))
        } catch (e: Exception) {
            DEFAULT_PRESETS[0]
        }
    }

    private fun loadSavedPresets(): List<CrosshairConfig> {
        val jsonStr = prefs.getString("saved_custom_presets", null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<CrosshairConfig>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(jsonToConfig(obj))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun persistSavedPresets(presets: List<CrosshairConfig>) {
        val array = JSONArray()
        presets.forEach { array.put(configToJson(it)) }
        prefs.edit().putString("saved_custom_presets", array.toString()).apply()
    }

    private fun configToJson(c: CrosshairConfig): JSONObject {
        return JSONObject().apply {
            put("id", c.id)
            put("name", c.name)
            put("style", c.style.name)
            put("sizeDp", c.sizeDp.toDouble())
            put("thicknessDp", c.thicknessDp.toDouble())
            put("gapDp", c.gapDp.toDouble())
            put("centerDot", c.centerDot)
            put("centerDotSizeDp", c.centerDotSizeDp.toDouble())
            put("rotationDeg", c.rotationDeg.toDouble())
            put("opacity", c.opacity.toDouble())
            put("primaryColorHex", c.primaryColorHex)
            put("outlineEnabled", c.outlineEnabled)
            put("outlineColorHex", c.outlineColorHex)
            put("outlineThicknessDp", c.outlineThicknessDp.toDouble())
            put("tStyle", c.tStyle)
            put("offsetX", c.offsetX)
            put("offsetY", c.offsetY)
            put("isCustom", c.isCustom)
            put("category", c.category)
        }
    }

    private fun jsonToConfig(obj: JSONObject): CrosshairConfig {
        val styleName = obj.optString("style", CrosshairStyle.CLASSIC_CROSS.name)
        val style = try {
            CrosshairStyle.valueOf(styleName)
        } catch (e: Exception) {
            CrosshairStyle.CLASSIC_CROSS
        }

        return CrosshairConfig(
            id = obj.optString("id", System.currentTimeMillis().toString()),
            name = obj.optString("name", "Custom Crosshair"),
            style = style,
            sizeDp = obj.optDouble("sizeDp", 36.0).toFloat(),
            thicknessDp = obj.optDouble("thicknessDp", 3.0).toFloat(),
            gapDp = obj.optDouble("gapDp", 8.0).toFloat(),
            centerDot = obj.optBoolean("centerDot", true),
            centerDotSizeDp = obj.optDouble("centerDotSizeDp", 4.0).toFloat(),
            rotationDeg = obj.optDouble("rotationDeg", 0.0).toFloat(),
            opacity = obj.optDouble("opacity", 1.0).toFloat(),
            primaryColorHex = obj.optLong("primaryColorHex", 0xFF00E676),
            outlineEnabled = obj.optBoolean("outlineEnabled", true),
            outlineColorHex = obj.optLong("outlineColorHex", 0xFF000000),
            outlineThicknessDp = obj.optDouble("outlineThicknessDp", 1.5).toFloat(),
            tStyle = obj.optBoolean("tStyle", false),
            offsetX = obj.optInt("offsetX", 0),
            offsetY = obj.optInt("offsetY", 0),
            isCustom = obj.optBoolean("isCustom", false),
            category = obj.optString("category", "Custom")
        )
    }
}
