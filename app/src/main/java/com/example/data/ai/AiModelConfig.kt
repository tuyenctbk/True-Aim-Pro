package com.example.data.ai

import com.example.model.CrosshairConfig

/**
 * Centralized, model-agnostic capability tier configuration.
 * All AI screens and repositories resolve model IDs and free-tier guardrails from this object.
 */
object AiModelConfig {
    // Tier 1 — Primary Standard Reasoning & Multimodal Vision
    const val PRIMARY_REASONING_MODEL = "gemini-3.8-flash"

    // Tier 2 — High-Efficiency Economy Lite (auto-activated on 429, >= 80% quota, or Eco Mode)
    const val ECONOMY_LITE_MODEL = "gemini-3.5-flash-lite"

    // Media & Token Hygiene
    const val MEDIA_MAX_DIMENSION_PX = 896
    const val MEDIA_JPEG_QUALITY = 82
    const val DEDUP_CACHE_TTL_MS = 40_000L
    const val AI_TIMEOUT_MS = 60_000L

    // Free-Tier Daily Allowances
    const val GUEST_DAILY_LIMIT = 5
    const val STARTER_DAILY_LIMIT = 15
    const val DAILY_GRACE_CREDITS = 3
    const val SOFT_NUDGE_THRESHOLD = 0.80f
}

enum class AiExecutionTier {
    PRIMARY_CLOUD,
    ECONOMY_LITE,
    LOCAL_ENGINE,
    BYOK_DIRECT
}

enum class AiFeatureType(val key: String) {
    ARCHITECT("architect"),
    VISION("vision"),
    COACH("coach")
}

data class AiArchitectResult(
    val config: CrosshairConfig,
    val rationale: String,
    val proTip: String,
    val tierUsed: AiExecutionTier,
    val fromCache: Boolean = false,
    val autoCascaded: Boolean = false
)

data class AiVisionResult(
    val config: CrosshairConfig,
    val mapLightingSummary: String,
    val dominantPaletteHex: List<Long>,
    val contrastReasoning: String,
    val recommendedColorHex: Long,
    val tierUsed: AiExecutionTier,
    val fromCache: Boolean = false,
    val autoCascaded: Boolean = false
)

data class AiCoachResult(
    val headline: String,
    val diagnosis: String,
    val drillFocus: String,
    val suggestedConfig: CrosshairConfig,
    val tweakSummary: String,
    val tierUsed: AiExecutionTier,
    val fromCache: Boolean = false,
    val autoCascaded: Boolean = false
)

enum class RecoveryReason {
    QUOTA_EXHAUSTED,
    RATE_LIMITED_429,
    OFFLINE_OR_CLOUD_UNAVAILABLE
}

sealed interface AiTaskState<out T> {
    data object Idle : AiTaskState<Nothing>
    data class Loading(val plannedTier: AiExecutionTier) : AiTaskState<Nothing>
    data class Success<T>(val data: T) : AiTaskState<T>
    data class RecoveryNeeded(
        val feature: AiFeatureType,
        val reason: RecoveryReason,
        val message: String,
        val canRetryEcoLite: Boolean
    ) : AiTaskState<Nothing>
    data class Error(val message: String) : AiTaskState<Nothing>
}
