package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import com.example.R
import com.example.data.UsageStats
import com.example.data.analytics.AnalyticsSummary
import com.example.model.CrosshairConfig
import com.example.model.CrosshairStyle
import com.example.ui.AimDifficulty
import com.example.ui.AimGameStats
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Tier 3 — Zero-Cost On-Device Deterministic Engine (0 cloud tokens, works 100% offline).
 * Guarantees users never hit a hard wall or lose progress when quota is exhausted or offline.
 */
object LocalAiFallbackEngine {

    /**
     * Local keyword/playstyle Crosshair Architect (0 tokens).
     */
    fun architectCrosshairLocally(context: Context, prompt: String): AiArchitectResult {
        val p = prompt.trim().lowercase(Locale.ROOT)

        val colorHex = when {
            p.contains("cyan") || p.contains("ice") || p.contains("blue") -> 0xFF00E5FF
            p.contains("magenta") || p.contains("pink") || p.contains("purple") -> 0xFFFF2AD4
            p.contains("yellow") || p.contains("gold") || p.contains("amber") -> 0xFFFFEA00
            p.contains("red") || p.contains("crimson") || p.contains("blood") -> 0xFFFF1744
            p.contains("white") || p.contains("minimal") -> 0xFFFFFFFF
            p.contains("orange") || p.contains("desert") || p.contains("miramar") -> 0xFF00E5FF
            p.contains("snow") || p.contains("vikendi") || p.contains("icebox") -> 0xFFFF1744
            else -> 0xFF00E676
        }

        val config = when {
            p.contains("sniper") || p.contains("awp") || p.contains("operator") || p.contains("long") -> {
                CrosshairConfig(
                    id = "ai_local_${System.currentTimeMillis()}",
                    name = context.getString(R.string.ai_local_sniper_name),
                    style = CrosshairStyle.MIL_DOT_SNIPER,
                    sizeDp = 44f,
                    thicknessDp = 2f,
                    gapDp = 6f,
                    centerDot = true,
                    centerDotSizeDp = 3f,
                    opacity = 0.95f,
                    primaryColorHex = colorHex,
                    outlineEnabled = true,
                    outlineThicknessDp = 1.5f,
                    isCustom = true,
                    category = CrosshairConfig.CATEGORY_SNIPER
                )
            }
            p.contains("shotgun") || p.contains("cqb") || p.contains("smg") || p.contains("close") -> {
                CrosshairConfig(
                    id = "ai_local_${System.currentTimeMillis()}",
                    name = context.getString(R.string.ai_local_cqb_name),
                    style = CrosshairStyle.TACTICAL_CIRCLE,
                    sizeDp = 42f,
                    thicknessDp = 3f,
                    gapDp = 10f,
                    centerDot = true,
                    centerDotSizeDp = 4f,
                    opacity = 0.90f,
                    primaryColorHex = colorHex,
                    outlineEnabled = true,
                    outlineThicknessDp = 1.5f,
                    isCustom = true,
                    category = CrosshairConfig.CATEGORY_TACTICAL
                )
            }
            p.contains("one-tap") || p.contains("headshot") || p.contains("dot") || p.contains("valorant") || p.contains("vandal") -> {
                CrosshairConfig(
                    id = "ai_local_${System.currentTimeMillis()}",
                    name = context.getString(R.string.ai_local_onetap_name),
                    style = CrosshairStyle.PRECISION_DOT,
                    sizeDp = 22f,
                    thicknessDp = 2.5f,
                    gapDp = 2f,
                    centerDot = true,
                    centerDotSizeDp = 5f,
                    opacity = 1.0f,
                    primaryColorHex = colorHex,
                    outlineEnabled = true,
                    outlineThicknessDp = 1.8f,
                    isCustom = true,
                    category = CrosshairConfig.CATEGORY_DOT
                )
            }
            p.contains("t-style") || p.contains("t style") || p.contains("head visibility") -> {
                CrosshairConfig(
                    id = "ai_local_${System.currentTimeMillis()}",
                    name = context.getString(R.string.ai_local_tstyle_name),
                    style = CrosshairStyle.T_SHAPE,
                    sizeDp = 30f,
                    thicknessDp = 2.5f,
                    gapDp = 6f,
                    centerDot = false,
                    centerDotSizeDp = 3f,
                    opacity = 0.95f,
                    primaryColorHex = colorHex,
                    outlineEnabled = true,
                    outlineThicknessDp = 1.5f,
                    tStyle = true,
                    isCustom = true,
                    category = CrosshairConfig.CATEGORY_CLASSIC
                )
            }
            p.contains("apex") || p.contains("tracking") || p.contains("fast") -> {
                CrosshairConfig(
                    id = "ai_local_${System.currentTimeMillis()}",
                    name = context.getString(R.string.ai_local_kinetic_name),
                    style = CrosshairStyle.DYNAMIC_RING,
                    sizeDp = 36f,
                    thicknessDp = 2.5f,
                    gapDp = 8f,
                    centerDot = true,
                    centerDotSizeDp = 3.5f,
                    opacity = 0.92f,
                    primaryColorHex = colorHex,
                    outlineEnabled = true,
                    outlineThicknessDp = 1.5f,
                    isCustom = true,
                    category = CrosshairConfig.CATEGORY_TACTICAL
                )
            }
            else -> {
                CrosshairConfig(
                    id = "ai_local_${System.currentTimeMillis()}",
                    name = context.getString(R.string.ai_local_classic_name),
                    style = CrosshairStyle.CLASSIC_CROSS,
                    sizeDp = 30f,
                    thicknessDp = 2.5f,
                    gapDp = 5f,
                    centerDot = true,
                    centerDotSizeDp = 3f,
                    opacity = 0.95f,
                    primaryColorHex = colorHex,
                    outlineEnabled = true,
                    outlineThicknessDp = 1.5f,
                    isCustom = true,
                    category = CrosshairConfig.CATEGORY_CLASSIC
                )
            }
        }

        return AiArchitectResult(
            config = config,
            rationale = context.getString(R.string.ai_local_architect_rationale),
            proTip = context.getString(R.string.ai_local_architect_tip),
            tierUsed = AiExecutionTier.LOCAL_ENGINE
        )
    }

    /**
     * Real On-Device Pixel Contrast Analyzer (0 cloud tokens).
     * Samples the combat center region & full screenshot, calculates WCAG relative luminance
     * and dominant HSV hue, and selects the mathematically highest-contrast reticle color.
     */
    fun analyzeScreenshotLocally(context: Context, bitmap: Bitmap): AiVisionResult {
        val w = bitmap.width.coerceAtLeast(1)
        val h = bitmap.height.coerceAtLeast(1)

        // Sample a 28x28 grid focused heavily on the central 60% aiming zone
        val stepX = (w / 28).coerceAtLeast(1)
        val stepY = (h / 28).coerceAtLeast(1)

        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var count = 0L

        val hueBuckets = IntArray(12) // 30-degree HSV buckets
        val hsv = FloatArray(3)

        var y = (h * 0.15f).toInt()
        val endY = (h * 0.85f).toInt().coerceAtLeast(y + 1)
        val startX = (w * 0.15f).toInt()
        val endX = (w * 0.85f).toInt().coerceAtLeast(startX + 1)

        while (y < endY) {
            var x = startX
            while (x < endX) {
                val px = bitmap.getPixel(x, y)
                val r = AndroidColor.red(px)
                val g = AndroidColor.green(px)
                val b = AndroidColor.blue(px)
                rSum += r
                gSum += g
                bSum += b
                count++

                AndroidColor.RGBToHSV(r, g, b, hsv)
                if (hsv[1] > 0.22f && hsv[2] > 0.18f) {
                    val bucket = ((hsv[0] / 30f).toInt()).coerceIn(0, 11)
                    hueBuckets[bucket]++
                }
                x += stepX
            }
            y += stepY
        }

        val safeCount = count.coerceAtLeast(1L)
        val avgR = (rSum / safeCount).toInt().coerceIn(0, 255)
        val avgG = (gSum / safeCount).toInt().coerceIn(0, 255)
        val avgB = (bSum / safeCount).toInt().coerceIn(0, 255)

        val avgBgColor = (0xFFL shl 24) or (avgR.toLong() shl 16) or (avgG.toLong() shl 8) or avgB.toLong()
        val bgLuminance = relativeLuminance(avgR, avgG, avgB)

        val dominantBucket = hueBuckets.indices.maxByOrNull { hueBuckets[it] } ?: 0
        val dominantHueDeg = dominantBucket * 30f + 15f
        val hasStrongChroma = (hueBuckets.maxOrNull() ?: 0) > (safeCount * 0.14f)

        // Candidate high-visibility esports reticle palette
        val candidates = listOf(
            0xFF00E5FFL to context.getString(R.string.ai_color_electric_cyan),
            0xFF00E676L to context.getString(R.string.ai_color_neon_green),
            0xFFFF2AD4L to context.getString(R.string.ai_color_hyper_magenta),
            0xFFFFEA00L to context.getString(R.string.ai_color_solar_yellow),
            0xFFFF1744L to context.getString(R.string.ai_color_laser_crimson)
        )

        // Score each candidate by WCAG contrast ratio + hue separation from dominant map hue
        val best = candidates.maxByOrNull { (hex, _) ->
            val cr = ((hex shr 16) and 0xFF).toInt()
            val cg = ((hex shr 8) and 0xFF).toInt()
            val cb = (hex and 0xFF).toInt()
            val lum = relativeLuminance(cr, cg, cb)
            val wcagContrast = (maxOf(lum, bgLuminance) + 0.05) / (minOf(lum, bgLuminance) + 0.05)

            AndroidColor.RGBToHSV(cr, cg, cb, hsv)
            val hueDelta = if (hasStrongChroma) {
                val raw = abs(hsv[0] - dominantHueDeg)
                minOf(raw, 360f - raw) / 180f // 0..1 (1 = exact complement)
            } else {
                0.5f
            }
            wcagContrast + (hueDelta * 3.2)
        } ?: (0xFF00E5FFL to context.getString(R.string.ai_color_electric_cyan))

        val lightingLabel = when {
            bgLuminance > 0.55 -> context.getString(R.string.ai_vision_scene_bright)
            bgLuminance < 0.16 -> context.getString(R.string.ai_vision_scene_dark)
            hasStrongChroma && dominantHueDeg in 20f..65f -> context.getString(R.string.ai_vision_scene_desert)
            hasStrongChroma && dominantHueDeg in 70f..165f -> context.getString(R.string.ai_vision_scene_foliage)
            hasStrongChroma && dominantHueDeg in 175f..255f -> context.getString(R.string.ai_vision_scene_cool)
            else -> context.getString(R.string.ai_vision_scene_neutral)
        }

        val needsStrongOutline = bgLuminance > 0.38
        val recommendedOpacity = if (bgLuminance < 0.15) 0.88f else 1.0f

        val matchedConfig = CrosshairConfig(
            id = "ai_vision_local_${System.currentTimeMillis()}",
            name = context.getString(R.string.ai_vision_match_name_format, best.second),
            style = CrosshairStyle.CLASSIC_CROSS,
            sizeDp = 30f,
            thicknessDp = if (needsStrongOutline) 3f else 2.5f,
            gapDp = 6f,
            centerDot = true,
            centerDotSizeDp = 3.5f,
            opacity = recommendedOpacity,
            primaryColorHex = best.first,
            outlineEnabled = true,
            outlineColorHex = 0xFF000000,
            outlineThicknessDp = if (needsStrongOutline) 2.2f else 1.5f,
            isCustom = true,
            category = CrosshairConfig.CATEGORY_AI_CONTRAST
        )

        val complementHex = hsvToArgb((dominantHueDeg + 180f) % 360f, 0.75f, 0.85f)
        val darkerSample = (0xFFL shl 24) or
            ((avgR * 0.65f).roundToInt().toLong() shl 16) or
            ((avgG * 0.65f).roundToInt().toLong() shl 8) or
            (avgB * 0.65f).roundToInt().toLong()

        val contrastRatio = computeContrastRatio(best.first, avgBgColor)
        val contrastFormatted = String.format(Locale.US, "%.1f", contrastRatio)
        val luminancePct = (bgLuminance * 100).roundToInt()
        val reasoning = if (needsStrongOutline) {
            context.getString(
                R.string.ai_vision_local_reasoning_glare,
                luminancePct,
                best.second,
                contrastFormatted
            )
        } else {
            context.getString(
                R.string.ai_vision_local_reasoning_lowlight,
                luminancePct,
                best.second,
                contrastFormatted
            )
        }

        return AiVisionResult(
            config = matchedConfig,
            mapLightingSummary = lightingLabel,
            dominantPaletteHex = listOf(avgBgColor, darkerSample, complementHex),
            contrastReasoning = reasoning,
            recommendedColorHex = best.first,
            tierUsed = AiExecutionTier.LOCAL_ENGINE
        )
    }

    /**
     * Instant Local Reflex Coach (0 cloud tokens).
     */
    fun coachAimLocally(
        context: Context,
        lastDrill: AimGameStats,
        difficulty: AimDifficulty,
        usageStats: UsageStats,
        summary: AnalyticsSummary,
        activeConfig: CrosshairConfig
    ): AiCoachResult {
        val accuracy = if (lastDrill.totalTargets > 0) {
            ((lastDrill.targetsHit.toFloat() / lastDrill.totalTargets.toFloat()) * 100f).roundToInt()
        } else 0
        val reaction = lastDrill.avgReactionTimeMs
        val hasDrillData = lastDrill.totalTargets > 0
        val difficultyLabel = context.getString(difficulty.titleRes)

        val headline: String
        val diagnosis: String
        val drillFocus: String
        val suggestedConfig: CrosshairConfig
        val tweakSummary: String

        when {
            !hasDrillData -> {
                headline = context.getString(R.string.ai_coach_local_nodata_headline)
                diagnosis = context.getString(
                    R.string.ai_coach_local_nodata_diagnosis,
                    summary.aimDrillsCompleted,
                    usageStats.totalSessionsCount
                )
                drillFocus = context.getString(R.string.ai_coach_local_nodata_drill)
                suggestedConfig = activeConfig.copy(
                    id = "ai_coach_${System.currentTimeMillis()}",
                    name = context.getString(R.string.ai_coach_local_nodata_name, activeConfig.name),
                    gapDp = activeConfig.gapDp.coerceIn(4f, 7f),
                    outlineEnabled = true,
                    centerDot = true,
                    isCustom = true
                )
                tweakSummary = context.getString(R.string.ai_coach_local_nodata_tweak)
            }
            accuracy < 65 -> {
                headline = context.getString(R.string.ai_coach_local_lowacc_headline)
                diagnosis = context.getString(
                    R.string.ai_coach_local_lowacc_diagnosis,
                    accuracy,
                    reaction,
                    lastDrill.bestStreak,
                    difficultyLabel
                )
                drillFocus = context.getString(R.string.ai_coach_local_lowacc_drill)
                suggestedConfig = activeConfig.copy(
                    id = "ai_coach_${System.currentTimeMillis()}",
                    name = context.getString(R.string.ai_coach_local_lowacc_name),
                    style = CrosshairStyle.CLASSIC_CROSS,
                    sizeDp = activeConfig.sizeDp.coerceIn(24f, 32f),
                    thicknessDp = 2.5f,
                    gapDp = 4f,
                    centerDot = true,
                    centerDotSizeDp = 4f,
                    outlineEnabled = true,
                    outlineThicknessDp = 1.8f,
                    isCustom = true
                )
                tweakSummary = context.getString(R.string.ai_coach_local_lowacc_tweak)
            }
            reaction > 560L -> {
                headline = context.getString(R.string.ai_coach_local_slow_headline)
                diagnosis = context.getString(
                    R.string.ai_coach_local_slow_diagnosis,
                    accuracy,
                    reaction
                )
                drillFocus = context.getString(R.string.ai_coach_local_slow_drill)
                suggestedConfig = activeConfig.copy(
                    id = "ai_coach_${System.currentTimeMillis()}",
                    name = context.getString(R.string.ai_coach_local_slow_name),
                    sizeDp = (activeConfig.sizeDp + 4f).coerceIn(30f, 44f),
                    thicknessDp = (activeConfig.thicknessDp + 0.5f).coerceIn(2.5f, 4f),
                    gapDp = 7f,
                    opacity = 1.0f,
                    outlineEnabled = true,
                    isCustom = true
                )
                tweakSummary = context.getString(R.string.ai_coach_local_slow_tweak)
            }
            else -> {
                headline = context.getString(R.string.ai_coach_local_pro_headline)
                diagnosis = context.getString(
                    R.string.ai_coach_local_pro_diagnosis,
                    accuracy,
                    reaction,
                    lastDrill.headshots,
                    lastDrill.bestStreak
                )
                drillFocus = context.getString(R.string.ai_coach_local_pro_drill)
                suggestedConfig = activeConfig.copy(
                    id = "ai_coach_${System.currentTimeMillis()}",
                    name = context.getString(R.string.ai_coach_local_pro_name),
                    style = CrosshairStyle.MICRO_PLUS,
                    sizeDp = 24f,
                    thicknessDp = 2f,
                    gapDp = 3f,
                    centerDot = true,
                    centerDotSizeDp = 3f,
                    opacity = 0.95f,
                    outlineEnabled = true,
                    isCustom = true
                )
                tweakSummary = context.getString(R.string.ai_coach_local_pro_tweak)
            }
        }

        return AiCoachResult(
            headline = headline,
            diagnosis = diagnosis,
            drillFocus = drillFocus,
            suggestedConfig = suggestedConfig,
            tweakSummary = tweakSummary,
            tierUsed = AiExecutionTier.LOCAL_ENGINE
        )
    }

    private fun relativeLuminance(r: Int, g: Int, b: Int): Double {
        fun channel(v: Int): Double {
            val s = v / 255.0
            return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)
    }

    private fun computeContrastRatio(argb1: Long, argb2: Long): Double {
        val l1 = relativeLuminance(
            ((argb1 shr 16) and 0xFF).toInt(),
            ((argb1 shr 8) and 0xFF).toInt(),
            (argb1 and 0xFF).toInt()
        )
        val l2 = relativeLuminance(
            ((argb2 shr 16) and 0xFF).toInt(),
            ((argb2 shr 8) and 0xFF).toInt(),
            (argb2 and 0xFF).toInt()
        )
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    private fun hsvToArgb(hue: Float, sat: Float, value: Float): Long {
        val colorInt = AndroidColor.HSVToColor(floatArrayOf(hue, sat, value))
        return colorInt.toLong() and 0xFFFFFFFFL
    }
}
