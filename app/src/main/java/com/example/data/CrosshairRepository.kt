package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.R
import com.example.data.analytics.AppEventTracker
import com.example.data.analytics.AppEventType
import com.example.data.room.AppDatabase
import com.example.data.room.PresetEntity
import com.example.model.CrosshairConfig
import com.example.model.CrosshairStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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

class CrosshairRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("crosshair_pro_prefs", Context.MODE_PRIVATE)

    private val database = AppDatabase.getDatabase(appContext)
    private val presetDao = database.presetDao()
    private val eventTracker = AppEventTracker.getInstance(appContext)
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var sessionTimerJob: Job? = null
    private var currentSessionStartTimeMs: Long = 0L
    private var lastCompletedSessionDurationSec: Long = 0L

    private val _activeConfig = MutableStateFlow(loadActiveConfig())
    val activeConfig: StateFlow<CrosshairConfig> = _activeConfig.asStateFlow()

    // Always reflects actual live service state rather than stale preference across process death
    private val _isOverlayActive = MutableStateFlow(false)
    val isOverlayActive: StateFlow<Boolean> = _isOverlayActive.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _hasCompletedOnboarding = MutableStateFlow(prefs.getBoolean("has_completed_onboarding", false))
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    private val _savedPresets = MutableStateFlow<List<CrosshairConfig>>(emptyList())
    val savedPresets: StateFlow<List<CrosshairConfig>> = _savedPresets.asStateFlow()

    private val _usageStats = MutableStateFlow(loadUsageStats())
    val usageStats: StateFlow<UsageStats> = _usageStats.asStateFlow()

    private val _recentCustomColors = MutableStateFlow(loadRecentCustomColors())
    val recentCustomColors: StateFlow<List<Long>> = _recentCustomColors.asStateFlow()

    private val _favoriteIds = MutableStateFlow(loadFavoriteIds())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    init {
        repositoryScope.launch {
            presetDao.getAllPresets().collectLatest { entities ->
                _savedPresets.value = entities.map { it.toCrosshairConfig() }
            }
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
        val baseTotalSec = prefs.getLong("total_active_time_sec", 0L)
        val newSessions = prefs.getInt("total_sessions_count", 0) + 1
        prefs.edit().putInt("total_sessions_count", newSessions).apply()

        _usageStats.value = _usageStats.value.copy(
            totalSessionsCount = newSessions,
            currentSessionDurationSeconds = 0L,
            averageSessionDurationSeconds = if (newSessions > 0) baseTotalSec / newSessions else 0L
        )

        sessionTimerJob?.cancel()
        sessionTimerJob = repositoryScope.launch {
            var tickCount = 0
            while (true) {
                delay(1000L)
                tickCount++
                val elapsedSec = ((System.currentTimeMillis() - currentSessionStartTimeMs) / 1000L).coerceAtLeast(0L)
                val liveTotalSec = baseTotalSec + elapsedSec

                // Persist to disk every 10s to minimize flash I/O
                if (tickCount % 10 == 0) {
                    prefs.edit().putLong("total_active_time_sec", liveTotalSec).apply()
                }

                val avgSec = if (newSessions > 0) liveTotalSec / newSessions else 0L
                _usageStats.value = _usageStats.value.copy(
                    totalActiveTimeSeconds = liveTotalSec,
                    totalSessionsCount = newSessions,
                    currentSessionDurationSeconds = elapsedSec,
                    averageSessionDurationSeconds = avgSec
                )
            }
        }
    }

    /**
     * Synchronizes live overlay state. Returns the completed session duration (in seconds)
     * when transitioning from active -> inactive, or 0L otherwise.
     */
    fun setOverlayActive(active: Boolean): Long {
        if (_isOverlayActive.value == active) return 0L
        _isOverlayActive.value = active

        if (active) {
            eventTracker.logEvent(AppEventType.OVERLAY_STARTED, "overlay")
            startSessionTimer()
            return 0L
        } else {
            sessionTimerJob?.cancel()
            sessionTimerJob = null

            val elapsedSec = if (currentSessionStartTimeMs > 0L) {
                ((System.currentTimeMillis() - currentSessionStartTimeMs) / 1000L).coerceAtLeast(0L)
            } else {
                _usageStats.value.currentSessionDurationSeconds
            }
            currentSessionStartTimeMs = 0L
            lastCompletedSessionDurationSec = elapsedSec

            val finalTotalSec = maxOf(
                prefs.getLong("total_active_time_sec", 0L),
                _usageStats.value.totalActiveTimeSeconds
            )
            prefs.edit().putLong("total_active_time_sec", finalTotalSec).apply()

            val sessions = _usageStats.value.totalSessionsCount
            _usageStats.value = _usageStats.value.copy(
                totalActiveTimeSeconds = finalTotalSec,
                currentSessionDurationSeconds = 0L,
                averageSessionDurationSeconds = if (sessions > 0) finalTotalSec / sessions else 0L
            )

            eventTracker.logEvent(
                AppEventType.OVERLAY_STOPPED,
                "overlay",
                mapOf("duration_sec" to elapsedSec)
            )
            if (elapsedSec > 0L) {
                eventTracker.logOverlaySessionCompleted(elapsedSec)
            }
            return elapsedSec
        }
    }

    fun consumeLastCompletedSessionDuration(): Long {
        val value = lastCompletedSessionDurationSec
        lastCompletedSessionDurationSec = 0L
        return value
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
        eventTracker.logEvent(
            AppEventType.THEME_CHANGED,
            "settings",
            mapOf("dark_mode" to enabled)
        )
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean("has_completed_onboarding", completed).apply()
        _hasCompletedOnboarding.value = completed
        if (completed) {
            eventTracker.logEvent(AppEventType.ONBOARDING_COMPLETE, "onboarding")
        }
    }

    fun reopenOnboarding() {
        prefs.edit().putBoolean("has_completed_onboarding", false).apply()
        _hasCompletedOnboarding.value = false
    }

    fun resetUsageStats() {
        val wasActive = _isOverlayActive.value
        if (wasActive) {
            currentSessionStartTimeMs = System.currentTimeMillis()
        }
        prefs.edit()
            .putLong("total_active_time_sec", 0L)
            .putInt("total_sessions_count", if (wasActive) 1 else 0)
            .apply()
        _usageStats.value = _usageStats.value.copy(
            totalActiveTimeSeconds = 0L,
            totalSessionsCount = if (wasActive) 1 else 0,
            currentSessionDurationSeconds = 0L,
            averageSessionDurationSeconds = 0L
        )
        eventTracker.logEvent(AppEventType.SETTINGS_RESET, "settings", mapOf("target" to "usage_stats"))
    }

    fun updateActiveConfig(config: CrosshairConfig) {
        _activeConfig.value = config
        saveConfigToPrefs(config)
    }

    fun recordRecentCustomColor(hexColor: Long) {
        val opaqueHex = hexColor or 0xFF000000L
        val updated = buildList {
            add(opaqueHex)
            _recentCustomColors.value.forEach { existing ->
                if (existing != opaqueHex && size < 8) {
                    add(existing)
                }
            }
        }
        _recentCustomColors.value = updated
        prefs.edit().putString("recent_custom_colors", updated.joinToString(",") { it.toString(16) }).apply()
    }

    private fun loadRecentCustomColors(): List<Long> {
        val raw = prefs.getString("recent_custom_colors", null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split(",")
            .mapNotNull { token ->
                token.trim().toLongOrNull(16)?.let { it or 0xFF000000L }
            }
            .distinct()
            .take(8)
    }

    fun saveCustomPreset(preset: CrosshairConfig) {
        val updatedPreset = preset.copy(
            isCustom = true,
            category = if (preset.category.isNotBlank() && preset.category != CrosshairConfig.CATEGORY_CUSTOM) {
                preset.category
            } else {
                CrosshairConfig.CATEGORY_CUSTOM
            }
        )
        repositoryScope.launch {
            presetDao.insertPreset(PresetEntity.fromCrosshairConfig(updatedPreset))
        }
    }

    fun toggleFavorite(id: String): Boolean {
        val current = _favoriteIds.value
        val added = id !in current
        val next = if (added) current + id else current - id
        _favoriteIds.value = next
        prefs.edit().putStringSet("favorite_crosshair_ids", next.toSet()).apply()
        eventTracker.logEvent(
            AppEventType.PRESET_FAVORITED,
            "favorites",
            mapOf("preset_id" to id, "favorited" to added)
        )
        return added
    }

    fun isFavorite(id: String): Boolean = id in _favoriteIds.value

    private fun loadFavoriteIds(): Set<String> {
        return prefs.getStringSet("favorite_crosshair_ids", null)?.toSet() ?: emptySet()
    }

    fun deleteCustomPreset(id: String) {
        repositoryScope.launch {
            presetDao.deletePresetById(id)
        }
        if (id in _favoriteIds.value) {
            val next = _favoriteIds.value - id
            _favoriteIds.value = next
            prefs.edit().putStringSet("favorite_crosshair_ids", next.toSet()).apply()
        }
        eventTracker.logEvent(AppEventType.PRESET_DELETED, "gallery", mapOf("preset_id" to id))
    }

    private fun saveConfigToPrefs(config: CrosshairConfig) {
        val json = configToJson(config)
        prefs.edit().putString("active_crosshair_config", json.toString()).apply()
    }

    private fun loadActiveConfig(): CrosshairConfig {
        val jsonStr = prefs.getString("active_crosshair_config", null)
            ?: return com.example.model.PredefinedCrosshairCatalog.defaultConfig(appContext)
        return try {
            jsonToConfig(JSONObject(jsonStr))
        } catch (_: Exception) {
            com.example.model.PredefinedCrosshairCatalog.defaultConfig(appContext)
        }
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
        } catch (_: Exception) {
            CrosshairStyle.CLASSIC_CROSS
        }
        val id = obj.optString("id", System.currentTimeMillis().toString())
        val fallbackName = com.example.model.PredefinedCrosshairCatalog.resolveTitleResById(id)
            ?.let { appContext.getString(it) }
            ?: appContext.getString(R.string.default_custom_crosshair_name)

        return CrosshairConfig(
            id = id,
            name = obj.optString("name", fallbackName).ifBlank { fallbackName },
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
            category = obj.optString("category", CrosshairConfig.CATEGORY_CUSTOM)
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: CrosshairRepository? = null

        fun getInstance(context: Context): CrosshairRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CrosshairRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

