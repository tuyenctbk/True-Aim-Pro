package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Base64
import android.util.Log
import com.example.R
import com.example.data.UsageStats
import com.example.data.analytics.AnalyticsSummary
import com.example.model.CrosshairConfig
import com.example.model.CrosshairStyle
import com.example.ui.AimDifficulty
import com.example.ui.AimGameStats
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Orchestrates TrueAim Pro's 3-Tier Free-Tier AI Architecture:
 * - Token Hygiene: SHA-256 fingerprint dedup cache (40s TTL) + bitmap downscaling (<= 896px, JPEG 82%)
 * - Tier 1: Firebase AI Logic Primary Cloud (gemini-3.8-flash) with structured JSON schema
 * - Tier 2: Auto-Cascade Economy Lite (gemini-3.5-flash-lite) on >= 80% quota, Eco Mode, or 429
 * - Optional BYOK: User-provided Gemini key direct REST pass-through (zero developer cost)
 * - Tier 3: Zero-Cost On-Device Local Fallback Engine (instant, offline-ready, 0 credits)
 */
class TrueAimAiRepository(
    context: Context,
    private val quotaManager: UsageQuotaManager = UsageQuotaManager.getInstance(context)
) {
    private val appContext = context.applicationContext

    private data class CacheEntry<T>(val value: T, val timestampMs: Long)

    private val architectCache = ConcurrentHashMap<String, CacheEntry<AiArchitectResult>>()
    private val visionCache = ConcurrentHashMap<String, CacheEntry<AiVisionResult>>()
    private val coachCache = ConcurrentHashMap<String, CacheEntry<AiCoachResult>>()

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(55, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    // --- 1. AI CROSSHAIR ARCHITECT ---

    suspend fun generateCrosshair(
        prompt: String,
        forceEcoLite: Boolean = false
    ): AiTaskState<AiArchitectResult> {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isBlank()) {
            return AiTaskState.Error(appContext.getString(R.string.ai_architect_empty_prompt))
        }

        // Fingerprint deduplication (40s TTL) — 0 extra tokens on accidental double-taps
        val fingerprint = sha256("architect|${cleanPrompt.lowercase()}")
        architectCache[fingerprint]?.let { cached ->
            if (System.currentTimeMillis() - cached.timestampMs <= AiModelConfig.DEDUP_CACHE_TTL_MS) {
                return AiTaskState.Success(cached.value.copy(fromCache = true))
            }
        }

        if (!isNetworkAvailable()) {
            return AiTaskState.RecoveryNeeded(
                feature = AiFeatureType.ARCHITECT,
                reason = RecoveryReason.OFFLINE_OR_CLOUD_UNAVAILABLE,
                message = appContext.getString(R.string.ai_offline_architect_msg),
                canRetryEcoLite = false
            )
        }

        val snapshot = quotaManager.quota.value
        val byokKey = quotaManager.getByokApiKey()
        if (byokKey.isBlank() && snapshot.isExhausted) {
            return AiTaskState.RecoveryNeeded(
                feature = AiFeatureType.ARCHITECT,
                reason = RecoveryReason.QUOTA_EXHAUSTED,
                message = appContext.getString(
                    R.string.ai_quota_full_detail_msg,
                    snapshot.totalUsed,
                    snapshot.effectiveDailyLimit
                ),
                canRetryEcoLite = false
            )
        }

        val startWithEco = forceEcoLite || snapshot.shouldPreferEcoLite
        val consumed = quotaManager.tryConsumeCredit(
            feature = AiFeatureType.ARCHITECT,
            recordCascade = startWithEco && !snapshot.ecoLiteMode
        )
        if (!consumed) {
            return AiTaskState.RecoveryNeeded(
                feature = AiFeatureType.ARCHITECT,
                reason = RecoveryReason.QUOTA_EXHAUSTED,
                message = appContext.getString(R.string.ai_quota_full_msg),
                canRetryEcoLite = false
            )
        }

        val fullPrompt = buildArchitectPrompt(cleanPrompt)

        return try {
            val (jsonText, tierUsed, autoCascaded) = executeCloudJsonWithCascade(
                prompt = fullPrompt,
                imageBitmap = null,
                imageJpegBytes = null,
                schema = architectResponseSchema(),
                startWithEco = startWithEco,
                byokKey = byokKey
            )
            val parsed = parseArchitectJson(jsonText, tierUsed, autoCascaded)
            architectCache[fingerprint] = CacheEntry(parsed, System.currentTimeMillis())
            AiTaskState.Success(parsed)
        } catch (e: CancellationException) {
            quotaManager.refundCredit(AiFeatureType.ARCHITECT)
            throw e
        } catch (e: Exception) {
            quotaManager.refundCredit(AiFeatureType.ARCHITECT)
            Log.e(TAG, "AI Crosshair Architect failed: ${e.message}", e)
            val isRateLimit = isRateLimitException(e)
            AiTaskState.RecoveryNeeded(
                feature = AiFeatureType.ARCHITECT,
                reason = if (isRateLimit) RecoveryReason.RATE_LIMITED_429 else RecoveryReason.OFFLINE_OR_CLOUD_UNAVAILABLE,
                message = if (isRateLimit) {
                    appContext.getString(R.string.ai_rate_limit_architect_msg)
                } else {
                    appContext.getString(
                        R.string.ai_cloud_unavailable_msg,
                        e.localizedMessage ?: appContext.getString(R.string.ai_connection_error_fallback)
                    )
                },
                canRetryEcoLite = !startWithEco && byokKey.isBlank()
            )
        }
    }

    fun generateCrosshairLocally(prompt: String): AiArchitectResult =
        LocalAiFallbackEngine.architectCrosshairLocally(appContext, prompt)

    // --- 2. SMART MAP SCREENSHOT CONTRAST ANALYZER (VISION AI) ---

    suspend fun analyzeMapScreenshot(
        sourceBitmap: Bitmap,
        forceEcoLite: Boolean = false
    ): AiTaskState<AiVisionResult> {
        // Downscale before upload (<= 896px, JPEG 82%) to protect bandwidth & tokens
        val (downscaled, jpegBytes) = withContext(Dispatchers.Default) {
            downscaleBitmapForVision(sourceBitmap)
        }

        val fingerprint = sha256("vision|${downscaled.width}x${downscaled.height}|${jpegBytes.size}|${sha256Bytes(jpegBytes)}")
        visionCache[fingerprint]?.let { cached ->
            if (System.currentTimeMillis() - cached.timestampMs <= AiModelConfig.DEDUP_CACHE_TTL_MS) {
                return AiTaskState.Success(cached.value.copy(fromCache = true))
            }
        }

        if (!isNetworkAvailable()) {
            return AiTaskState.RecoveryNeeded(
                feature = AiFeatureType.VISION,
                reason = RecoveryReason.OFFLINE_OR_CLOUD_UNAVAILABLE,
                message = appContext.getString(R.string.ai_offline_vision_msg),
                canRetryEcoLite = false
            )
        }

        val snapshot = quotaManager.quota.value
        val byokKey = quotaManager.getByokApiKey()
        if (byokKey.isBlank() && snapshot.isExhausted) {
            return AiTaskState.RecoveryNeeded(
                feature = AiFeatureType.VISION,
                reason = RecoveryReason.QUOTA_EXHAUSTED,
                message = appContext.getString(
                    R.string.ai_quota_full_detail_msg,
                    snapshot.totalUsed,
                    snapshot.effectiveDailyLimit
                ),
                canRetryEcoLite = false
            )
        }

        val startWithEco = forceEcoLite || snapshot.shouldPreferEcoLite
        val consumed = quotaManager.tryConsumeCredit(
            feature = AiFeatureType.VISION,
            recordCascade = startWithEco && !snapshot.ecoLiteMode
        )
        if (!consumed) {
            return AiTaskState.RecoveryNeeded(
                feature = AiFeatureType.VISION,
                reason = RecoveryReason.QUOTA_EXHAUSTED,
                message = appContext.getString(R.string.ai_quota_full_msg),
                canRetryEcoLite = false
            )
        }

        // Compute local palette chips alongside Vision AI so palette swatches are always grounded in real pixels
        val localBaseline = withContext(Dispatchers.Default) {
            LocalAiFallbackEngine.analyzeScreenshotLocally(appContext, downscaled)
        }
        val visionPrompt = buildVisionPrompt()

        return try {
            val (jsonText, tierUsed, autoCascaded) = executeCloudJsonWithCascade(
                prompt = visionPrompt,
                imageBitmap = downscaled,
                imageJpegBytes = jpegBytes,
                schema = visionResponseSchema(),
                startWithEco = startWithEco,
                byokKey = byokKey
            )
            val parsed = parseVisionJson(jsonText, localBaseline.dominantPaletteHex, tierUsed, autoCascaded)
            visionCache[fingerprint] = CacheEntry(parsed, System.currentTimeMillis())
            AiTaskState.Success(parsed)
        } catch (e: CancellationException) {
            quotaManager.refundCredit(AiFeatureType.VISION)
            throw e
        } catch (e: Exception) {
            quotaManager.refundCredit(AiFeatureType.VISION)
            Log.e(TAG, "AI Map Screenshot Analyzer failed: ${e.message}", e)
            val isRateLimit = isRateLimitException(e)
            AiTaskState.RecoveryNeeded(
                feature = AiFeatureType.VISION,
                reason = if (isRateLimit) RecoveryReason.RATE_LIMITED_429 else RecoveryReason.OFFLINE_OR_CLOUD_UNAVAILABLE,
                message = if (isRateLimit) {
                    appContext.getString(R.string.ai_rate_limit_vision_msg)
                } else {
                    appContext.getString(
                        R.string.ai_vision_unavailable_msg,
                        e.localizedMessage ?: appContext.getString(R.string.ai_connection_error_fallback)
                    )
                },
                canRetryEcoLite = !startWithEco && byokKey.isBlank()
            )
        }
    }

    suspend fun analyzeMapScreenshotLocally(sourceBitmap: Bitmap): AiVisionResult =
        withContext(Dispatchers.Default) {
            val (downscaled, _) = downscaleBitmapForVision(sourceBitmap)
            LocalAiFallbackEngine.analyzeScreenshotLocally(appContext, downscaled)
        }

    // --- 3. AI AIM COACH (TELEMETRY & RETICLE DIAGNOSTICS) ---

    suspend fun generateAimCoaching(
        lastDrill: AimGameStats,
        difficulty: AimDifficulty,
        usageStats: UsageStats,
        summary: AnalyticsSummary,
        activeConfig: CrosshairConfig,
        forceEcoLite: Boolean = false
    ): AiTaskState<AiCoachResult> {
        val fingerprint = sha256(
            "coach|${lastDrill.score}|${lastDrill.targetsHit}|${lastDrill.totalTargets}|${lastDrill.avgReactionTimeMs}|${difficulty.name}|${activeConfig.style.name}|${activeConfig.sizeDp}|${activeConfig.gapDp}"
        )
        coachCache[fingerprint]?.let { cached ->
            if (System.currentTimeMillis() - cached.timestampMs <= AiModelConfig.DEDUP_CACHE_TTL_MS) {
                return AiTaskState.Success(cached.value.copy(fromCache = true))
            }
        }

        if (!isNetworkAvailable()) {
            return AiTaskState.RecoveryNeeded(
                feature = AiFeatureType.COACH,
                reason = RecoveryReason.OFFLINE_OR_CLOUD_UNAVAILABLE,
                message = appContext.getString(R.string.ai_offline_coach_msg),
                canRetryEcoLite = false
            )
        }

        val snapshot = quotaManager.quota.value
        val byokKey = quotaManager.getByokApiKey()
        if (byokKey.isBlank() && snapshot.isExhausted) {
            return AiTaskState.RecoveryNeeded(
                feature = AiFeatureType.COACH,
                reason = RecoveryReason.QUOTA_EXHAUSTED,
                message = appContext.getString(
                    R.string.ai_quota_full_detail_msg,
                    snapshot.totalUsed,
                    snapshot.effectiveDailyLimit
                ),
                canRetryEcoLite = false
            )
        }

        val startWithEco = forceEcoLite || snapshot.shouldPreferEcoLite
        val consumed = quotaManager.tryConsumeCredit(
            feature = AiFeatureType.COACH,
            recordCascade = startWithEco && !snapshot.ecoLiteMode
        )
        if (!consumed) {
            return AiTaskState.RecoveryNeeded(
                feature = AiFeatureType.COACH,
                reason = RecoveryReason.QUOTA_EXHAUSTED,
                message = appContext.getString(R.string.ai_quota_full_msg),
                canRetryEcoLite = false
            )
        }

        val coachPrompt = buildCoachPrompt(lastDrill, difficulty, usageStats, summary, activeConfig)

        return try {
            val (jsonText, tierUsed, autoCascaded) = executeCloudJsonWithCascade(
                prompt = coachPrompt,
                imageBitmap = null,
                imageJpegBytes = null,
                schema = coachResponseSchema(),
                startWithEco = startWithEco,
                byokKey = byokKey
            )
            val parsed = parseCoachJson(jsonText, activeConfig, tierUsed, autoCascaded)
            coachCache[fingerprint] = CacheEntry(parsed, System.currentTimeMillis())
            AiTaskState.Success(parsed)
        } catch (e: CancellationException) {
            quotaManager.refundCredit(AiFeatureType.COACH)
            throw e
        } catch (e: Exception) {
            quotaManager.refundCredit(AiFeatureType.COACH)
            Log.e(TAG, "AI Aim Coach failed: ${e.message}", e)
            val isRateLimit = isRateLimitException(e)
            AiTaskState.RecoveryNeeded(
                feature = AiFeatureType.COACH,
                reason = if (isRateLimit) RecoveryReason.RATE_LIMITED_429 else RecoveryReason.OFFLINE_OR_CLOUD_UNAVAILABLE,
                message = if (isRateLimit) {
                    appContext.getString(R.string.ai_rate_limit_coach_msg)
                } else {
                    appContext.getString(
                        R.string.ai_coach_unavailable_msg,
                        e.localizedMessage ?: appContext.getString(R.string.ai_connection_error_fallback)
                    )
                },
                canRetryEcoLite = !startWithEco && byokKey.isBlank()
            )
        }
    }

    fun generateAimCoachingLocally(
        lastDrill: AimGameStats,
        difficulty: AimDifficulty,
        usageStats: UsageStats,
        summary: AnalyticsSummary,
        activeConfig: CrosshairConfig
    ): AiCoachResult = LocalAiFallbackEngine.coachAimLocally(
        context = appContext,
        lastDrill = lastDrill,
        difficulty = difficulty,
        usageStats = usageStats,
        summary = summary,
        activeConfig = activeConfig
    )

    // --- CLOUD EXECUTION WITH AUTO-CASCADE (TIER 1 -> TIER 2) & OPTIONAL BYOK ---

    private suspend fun executeCloudJsonWithCascade(
        prompt: String,
        imageBitmap: Bitmap?,
        imageJpegBytes: ByteArray?,
        schema: Schema,
        startWithEco: Boolean,
        byokKey: String
    ): Triple<String, AiExecutionTier, Boolean> {
        // Optional user-entered BYOK direct pass-through (zero developer quota cost)
        if (byokKey.isNotBlank()) {
            val modelName = if (startWithEco) AiModelConfig.ECONOMY_LITE_MODEL else AiModelConfig.PRIMARY_REASONING_MODEL
            val text = withTimeout(AiModelConfig.AI_TIMEOUT_MS) {
                callGeminiByokRest(
                    apiKey = byokKey,
                    modelName = modelName,
                    prompt = prompt,
                    imageJpegBytes = imageJpegBytes
                )
            }
            return Triple(text, AiExecutionTier.BYOK_DIRECT, false)
        }

        // Tier 2 directly if Eco Lite Mode is ON or >= 80% quota
        if (startWithEco) {
            val text = invokeFirebaseAi(
                modelName = AiModelConfig.ECONOMY_LITE_MODEL,
                prompt = prompt,
                imageBitmap = imageBitmap,
                schema = schema
            )
            return Triple(text, AiExecutionTier.ECONOMY_LITE, false)
        }

        // Tier 1 Primary -> Automatic Cascade to Tier 2 Economy Lite on 429 / RESOURCE_EXHAUSTED
        return try {
            val text = invokeFirebaseAi(
                modelName = AiModelConfig.PRIMARY_REASONING_MODEL,
                prompt = prompt,
                imageBitmap = imageBitmap,
                schema = schema
            )
            Triple(text, AiExecutionTier.PRIMARY_CLOUD, false)
        } catch (e: CancellationException) {
            throw e
        } catch (primaryError: Exception) {
            if (isRateLimitException(primaryError)) {
                Log.w(TAG, "Primary model rate-limited; auto-cascading to ${AiModelConfig.ECONOMY_LITE_MODEL}")
                quotaManager.recordFallbackCascade()
                val text = invokeFirebaseAi(
                    modelName = AiModelConfig.ECONOMY_LITE_MODEL,
                    prompt = prompt,
                    imageBitmap = imageBitmap,
                    schema = schema
                )
                Triple(text, AiExecutionTier.ECONOMY_LITE, true)
            } else {
                throw primaryError
            }
        }
    }

    private suspend fun invokeFirebaseAi(
        modelName: String,
        prompt: String,
        imageBitmap: Bitmap?,
        schema: Schema
    ): String = withTimeout(AiModelConfig.AI_TIMEOUT_MS) {
        val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = modelName,
            generationConfig = generationConfig {
                temperature = 0.4f
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
        val response = if (imageBitmap != null) {
            model.generateContent(
                content {
                    image(imageBitmap)
                    text(prompt)
                }
            )
        } else {
            model.generateContent(prompt)
        }
        response.text?.trim()?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException(appContext.getString(R.string.ai_empty_cloud_response_error))
    }

    private suspend fun callGeminiByokRest(
        apiKey: String,
        modelName: String,
        prompt: String,
        imageJpegBytes: ByteArray?
    ): String = withContext(Dispatchers.IO) {
        val parts = JSONArray()
        if (imageJpegBytes != null) {
            parts.put(
                JSONObject().put(
                    "inline_data",
                    JSONObject()
                        .put("mime_type", "image/jpeg")
                        .put("data", Base64.encodeToString(imageJpegBytes, Base64.NO_WRAP))
                )
            )
        }
        parts.put(JSONObject().put("text", prompt))

        val bodyJson = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", parts)))
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.4)
                    .put("responseMimeType", "application/json")
            )

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"
        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", apiKey)
            .post(bodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException(
                    appContext.getString(R.string.ai_byok_http_error, response.code, raw.take(180))
                )
            }
            val root = JSONObject(raw)
            val text = root.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text", "")
                ?.trim()
                .orEmpty()
            text.ifBlank {
                throw IllegalStateException(appContext.getString(R.string.ai_byok_empty_response_error))
            }
        }
    }

    // --- SCHEMAS, PROMPTS & SAFE PARSERS ---

    private fun validStyleNames(): List<String> = CrosshairStyle.entries.map { it.name }

    private fun architectResponseSchema(): Schema = Schema.obj(
        mapOf(
            "name" to Schema.string("Short tactical crosshair name (2-4 words)"),
            "style" to Schema.enumeration(validStyleNames(), "Exact CrosshairStyle enum name"),
            "sizeDp" to Schema.double("Reticle diameter between 16 and 64"),
            "thicknessDp" to Schema.double("Stroke thickness between 1.5 and 6.0"),
            "gapDp" to Schema.double("Center gap between 0 and 20"),
            "centerDot" to Schema.boolean("Whether to enable center dot"),
            "centerDotSizeDp" to Schema.double("Center dot diameter between 2.0 and 8.0"),
            "opacity" to Schema.double("Opacity between 0.5 and 1.0"),
            "colorHex" to Schema.string("6-char RGB hex color string like #00E676"),
            "outlineEnabled" to Schema.boolean("Whether dark contrast outline is enabled"),
            "tStyle" to Schema.boolean("Open-top T-style crosshair for head visibility"),
            "category" to Schema.string("Short category like Classic, Dot, Sniper, Tactical, or Sci-Fi"),
            "rationale" to Schema.string("1-2 concise sentences explaining why this reticle fits the user's playstyle"),
            "proTip" to Schema.string("1 short actionable in-game aiming tip")
        )
    )

    private fun visionResponseSchema(): Schema = Schema.obj(
        mapOf(
            "name" to Schema.string("Short map-matched reticle name (2-4 words)"),
            "mapLightingSummary" to Schema.string("Short description of the screenshot's map colors and lighting"),
            "colorHex" to Schema.string("Highest-contrast 6-char RGB hex reticle color like #00E5FF"),
            "style" to Schema.enumeration(validStyleNames(), "Best CrosshairStyle enum name"),
            "sizeDp" to Schema.double("Recommended reticle size 22 to 48"),
            "thicknessDp" to Schema.double("Stroke thickness 2.0 to 5.0"),
            "gapDp" to Schema.double("Center gap 2 to 12"),
            "opacity" to Schema.double("Opacity 0.75 to 1.0"),
            "outlineEnabled" to Schema.boolean("Whether black contrast outline is needed"),
            "outlineThicknessDp" to Schema.double("Outline thickness 1.0 to 3.0"),
            "contrastReasoning" to Schema.string("1-2 concise sentences explaining why this color pops cleanly against this map")
        )
    )

    private fun coachResponseSchema(): Schema = Schema.obj(
        mapOf(
            "headline" to Schema.string("Short 4-7 word coaching headline"),
            "diagnosis" to Schema.string("2 concise sentences diagnosing accuracy, reaction time, and streak metrics"),
            "drillFocus" to Schema.string("1-2 specific actionable Aim Trainer drill instructions"),
            "tweakSummary" to Schema.string("1 sentence summarizing the recommended crosshair geometry tweak"),
            "style" to Schema.enumeration(validStyleNames(), "Recommended CrosshairStyle enum name"),
            "sizeDp" to Schema.double("Reticle size 18 to 52"),
            "thicknessDp" to Schema.double("Thickness 1.5 to 5.0"),
            "gapDp" to Schema.double("Gap 0 to 16"),
            "centerDot" to Schema.boolean("Enable center dot"),
            "colorHex" to Schema.string("6-char RGB hex color like #00E676"),
            "outlineEnabled" to Schema.boolean("Enable dark outline")
        )
    )

    private fun buildArchitectPrompt(userPrompt: String): String = """
        You are an esports crosshair designer for TrueAim Pro (mobile & TV FPS HUD overlay).
        Design the optimal tactical reticle configuration for this player request:
        "$userPrompt"
        Allowed styles: ${validStyleNames().joinToString(", ")}.
        Return strictly valid JSON matching the schema. Pick bright esports colors (#00E676, #00E5FF, #FFEA00, #FF2AD4, #FF1744, #FFFFFF) with strong contrast.
    """.trimIndent()

    private fun buildVisionPrompt(): String = """
        You are an esports visual-contrast specialist for TrueAim Pro.
        Analyze the central combat aiming zone of this game map screenshot (lighting, glare, shadows, foliage, desert sand, snow, neon).
        Recommend the highest-contrast crosshair color hex, outline thickness, opacity, and reticle style so the crosshair never blends into the background.
        Allowed styles: ${validStyleNames().joinToString(", ")}.
        Return strictly valid JSON matching the schema.
    """.trimIndent()

    private fun buildCoachPrompt(
        lastDrill: AimGameStats,
        difficulty: AimDifficulty,
        usageStats: UsageStats,
        summary: AnalyticsSummary,
        activeConfig: CrosshairConfig
    ): String {
        val accuracy = if (lastDrill.totalTargets > 0) {
            ((lastDrill.targetsHit.toFloat() / lastDrill.totalTargets.toFloat()) * 100f).roundToInt()
        } else 0
        return """
            You are an elite esports aim coach for TrueAim Pro.
            Analyze this player's real telemetry and current reticle, then prescribe a targeted drill focus and 1-tap crosshair geometry optimization.
            Player Telemetry:
            - Last Drill Score: ${lastDrill.score} pts (${difficulty.name} difficulty)
            - Targets Hit: ${lastDrill.targetsHit}/${lastDrill.totalTargets} ($accuracy% accuracy)
            - Avg Reaction Time: ${lastDrill.avgReactionTimeMs} ms
            - Headshot Crits: ${lastDrill.headshots}, Best Streak: ${lastDrill.bestStreak}x
            - Lifetime Drills: ${summary.aimDrillsCompleted}, Best Lifetime Score: ${summary.bestAimScore}
            - Overlay Sessions: ${usageStats.totalSessionsCount}, Total HUD Time: ${usageStats.totalActiveTimeSeconds}s
            Current Reticle:
            - Style: ${activeConfig.style.name}, Size: ${activeConfig.sizeDp}dp, Thickness: ${activeConfig.thicknessDp}dp, Gap: ${activeConfig.gapDp}dp, CenterDot: ${activeConfig.centerDot}, Outline: ${activeConfig.outlineEnabled}
            Allowed styles: ${validStyleNames().joinToString(", ")}.
            Return strictly valid JSON matching the schema.
        """.trimIndent()
    }

    private fun parseArchitectJson(
        rawJson: String,
        tierUsed: AiExecutionTier,
        autoCascaded: Boolean
    ): AiArchitectResult {
        val obj = JSONObject(extractJsonObject(rawJson))
        val style = parseStyleSafe(obj.optString("style"))
        val colorHex = parseColorHexSafe(obj.optString("colorHex"), 0xFF00E676)
        val defaultArchName = appContext.getString(R.string.ai_default_architect_name)
        val config = CrosshairConfig(
            id = "ai_arch_${System.currentTimeMillis()}",
            name = obj.optString("name", defaultArchName).trim().take(28).ifBlank { defaultArchName },
            style = style,
            sizeDp = obj.optDouble("sizeDp", 32.0).toFloat().coerceIn(16f, 64f),
            thicknessDp = obj.optDouble("thicknessDp", 2.5).toFloat().coerceIn(1.5f, 6f),
            gapDp = obj.optDouble("gapDp", 6.0).toFloat().coerceIn(0f, 20f),
            centerDot = obj.optBoolean("centerDot", true),
            centerDotSizeDp = obj.optDouble("centerDotSizeDp", 3.5).toFloat().coerceIn(2f, 8f),
            opacity = obj.optDouble("opacity", 0.95).toFloat().coerceIn(0.5f, 1.0f),
            primaryColorHex = colorHex,
            outlineEnabled = obj.optBoolean("outlineEnabled", true),
            outlineThicknessDp = 1.6f,
            tStyle = obj.optBoolean("tStyle", false),
            isCustom = true,
            category = obj.optString("category", CrosshairConfig.CATEGORY_AI_LAB)
                .trim().take(16).ifBlank { CrosshairConfig.CATEGORY_AI_LAB }
        )
        return AiArchitectResult(
            config = config,
            rationale = obj.optString("rationale", appContext.getString(R.string.ai_default_architect_rationale)).trim(),
            proTip = obj.optString("proTip", appContext.getString(R.string.ai_default_architect_tip)).trim(),
            tierUsed = tierUsed,
            autoCascaded = autoCascaded
        )
    }

    private fun parseVisionJson(
        rawJson: String,
        dominantPaletteHex: List<Long>,
        tierUsed: AiExecutionTier,
        autoCascaded: Boolean
    ): AiVisionResult {
        val obj = JSONObject(extractJsonObject(rawJson))
        val style = parseStyleSafe(obj.optString("style"))
        val colorHex = parseColorHexSafe(obj.optString("colorHex"), 0xFF00E5FF)
        val defaultVisionName = appContext.getString(R.string.ai_default_vision_name)
        val config = CrosshairConfig(
            id = "ai_vision_${System.currentTimeMillis()}",
            name = obj.optString("name", defaultVisionName).trim().take(28).ifBlank { defaultVisionName },
            style = style,
            sizeDp = obj.optDouble("sizeDp", 32.0).toFloat().coerceIn(20f, 52f),
            thicknessDp = obj.optDouble("thicknessDp", 3.0).toFloat().coerceIn(1.5f, 6f),
            gapDp = obj.optDouble("gapDp", 6.0).toFloat().coerceIn(0f, 16f),
            centerDot = true,
            centerDotSizeDp = 3.5f,
            opacity = obj.optDouble("opacity", 1.0).toFloat().coerceIn(0.65f, 1.0f),
            primaryColorHex = colorHex,
            outlineEnabled = obj.optBoolean("outlineEnabled", true),
            outlineColorHex = 0xFF000000,
            outlineThicknessDp = obj.optDouble("outlineThicknessDp", 1.8).toFloat().coerceIn(1.0f, 3.0f),
            isCustom = true,
            category = CrosshairConfig.CATEGORY_AI_CONTRAST
        )
        return AiVisionResult(
            config = config,
            mapLightingSummary = obj.optString("mapLightingSummary", appContext.getString(R.string.ai_default_vision_summary)).trim(),
            dominantPaletteHex = dominantPaletteHex,
            contrastReasoning = obj.optString("contrastReasoning", appContext.getString(R.string.ai_default_vision_reasoning)).trim(),
            recommendedColorHex = colorHex,
            tierUsed = tierUsed,
            autoCascaded = autoCascaded
        )
    }

    private fun parseCoachJson(
        rawJson: String,
        activeConfig: CrosshairConfig,
        tierUsed: AiExecutionTier,
        autoCascaded: Boolean
    ): AiCoachResult {
        val obj = JSONObject(extractJsonObject(rawJson))
        val style = parseStyleSafe(obj.optString("style", activeConfig.style.name))
        val colorHex = parseColorHexSafe(obj.optString("colorHex"), activeConfig.primaryColorHex)
        val suggested = activeConfig.copy(
            id = "ai_coach_${System.currentTimeMillis()}",
            name = appContext.getString(R.string.ai_default_coach_name),
            style = style,
            sizeDp = obj.optDouble("sizeDp", activeConfig.sizeDp.toDouble()).toFloat().coerceIn(18f, 52f),
            thicknessDp = obj.optDouble("thicknessDp", activeConfig.thicknessDp.toDouble()).toFloat().coerceIn(1.5f, 5f),
            gapDp = obj.optDouble("gapDp", activeConfig.gapDp.toDouble()).toFloat().coerceIn(0f, 16f),
            centerDot = obj.optBoolean("centerDot", activeConfig.centerDot),
            primaryColorHex = colorHex,
            outlineEnabled = obj.optBoolean("outlineEnabled", true),
            isCustom = true,
            category = CrosshairConfig.CATEGORY_AI_COACH
        )
        return AiCoachResult(
            headline = obj.optString("headline", appContext.getString(R.string.ai_default_coach_headline)).trim(),
            diagnosis = obj.optString("diagnosis", appContext.getString(R.string.ai_default_coach_diagnosis)).trim(),
            drillFocus = obj.optString("drillFocus", appContext.getString(R.string.ai_default_coach_drill)).trim(),
            suggestedConfig = suggested,
            tweakSummary = obj.optString("tweakSummary", appContext.getString(R.string.ai_default_coach_tweak)).trim(),
            tierUsed = tierUsed,
            autoCascaded = autoCascaded
        )
    }

    // --- HYGIENE HELPERS ---

    private fun downscaleBitmapForVision(source: Bitmap): Pair<Bitmap, ByteArray> {
        val maxDim = AiModelConfig.MEDIA_MAX_DIMENSION_PX
        val w = source.width.coerceAtLeast(1)
        val h = source.height.coerceAtLeast(1)
        val scaled = if (w > maxDim || h > maxDim) {
            val scale = maxDim.toFloat() / maxOf(w, h).toFloat()
            val newW = (w * scale).roundToInt().coerceAtLeast(1)
            val newH = (h * scale).roundToInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(source, newW, newH, true)
        } else {
            source
        }
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, AiModelConfig.MEDIA_JPEG_QUALITY, out)
        return scaled to out.toByteArray()
    }

    private fun extractJsonObject(raw: String): String {
        val trimmed = raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        return if (start >= 0 && end > start) trimmed.substring(start, end + 1) else trimmed
    }

    private fun parseStyleSafe(raw: String?): CrosshairStyle =
        CrosshairStyle.entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) }
            ?: CrosshairStyle.CLASSIC_CROSS

    private fun parseColorHexSafe(raw: String?, fallback: Long): Long {
        val cleaned = raw?.trim()?.removePrefix("#")?.removePrefix("0x") ?: return fallback
        return try {
            when (cleaned.length) {
                6 -> 0xFF000000L or cleaned.toLong(16)
                8 -> cleaned.toLong(16)
                else -> fallback
            }
        } catch (_: Exception) {
            fallback
        }
    }

    private fun isRateLimitException(e: Throwable): Boolean {
        val chainMsg = generateSequence(e) { it.cause }
            .mapNotNull { it.message }
            .joinToString(" ")
            .lowercase()
        return chainMsg.contains("429") ||
            chainMsg.contains("resource_exhausted") ||
            chainMsg.contains("quota") ||
            chainMsg.contains("rate limit") ||
            chainMsg.contains("too many requests")
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun sha256(input: String): String = sha256Bytes(input.toByteArray(Charsets.UTF_8))

    private fun sha256Bytes(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val TAG = "TrueAimAiRepository"
    }
}
