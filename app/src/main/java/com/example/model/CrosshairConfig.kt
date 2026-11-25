package com.example.model

import android.content.Context
import androidx.annotation.StringRes
import com.example.R

enum class CrosshairStyle(val category: String, @param:StringRes val nameRes: Int) {
    CLASSIC_CROSS(CrosshairConfig.CATEGORY_CLASSIC, R.string.style_classic_cross),
    PRECISION_DOT(CrosshairConfig.CATEGORY_DOT, R.string.style_precision_dot),
    TACTICAL_CIRCLE(CrosshairConfig.CATEGORY_TACTICAL, R.string.style_tactical_circle),
    MIL_DOT_SNIPER(CrosshairConfig.CATEGORY_SNIPER, R.string.style_mil_dot_sniper),
    CYBER_HEX(CrosshairConfig.CATEGORY_SCIFI, R.string.style_cyber_hex),
    CHEVRON_ARROW(CrosshairConfig.CATEGORY_TACTICAL, R.string.style_chevron_arrow),
    SHOTGUN_BOX(CrosshairConfig.CATEGORY_TACTICAL, R.string.style_shotgun_box),
    T_SHAPE(CrosshairConfig.CATEGORY_CLASSIC, R.string.style_t_crosshair),
    DYNAMIC_RING(CrosshairConfig.CATEGORY_TACTICAL, R.string.style_dynamic_ring),
    TRI_BLADE(CrosshairConfig.CATEGORY_SCIFI, R.string.style_tri_blade),
    HOLLOW_DIAMOND(CrosshairConfig.CATEGORY_DOT, R.string.style_hollow_diamond),
    CROSS_CIRCLE(CrosshairConfig.CATEGORY_CLASSIC, R.string.style_cross_circle),
    CORNER_BRACKETS(CrosshairConfig.CATEGORY_TACTICAL, R.string.style_corner_brackets),
    SONAR_RADAR(CrosshairConfig.CATEGORY_SCIFI, R.string.style_sonar_radar),
    MICRO_PLUS(CrosshairConfig.CATEGORY_DOT, R.string.style_micro_plus),
    HEARTBEAT(CrosshairConfig.CATEGORY_SCIFI, R.string.style_pulse_cross)
}

data class CrosshairConfig(
    val id: String = "default",
    val name: String = "",
    val style: CrosshairStyle = CrosshairStyle.CLASSIC_CROSS,
    val sizeDp: Float = 36f,
    val thicknessDp: Float = 3f,
    val gapDp: Float = 8f,
    val centerDot: Boolean = true,
    val centerDotSizeDp: Float = 4f,
    val rotationDeg: Float = 0f,
    val opacity: Float = 1.0f,
    val primaryColorHex: Long = 0xFF00E676,
    val outlineEnabled: Boolean = true,
    val outlineColorHex: Long = 0xFF000000,
    val outlineThicknessDp: Float = 1.5f,
    val tStyle: Boolean = false,
    val offsetX: Int = 0,
    val offsetY: Int = 0,
    val isCustom: Boolean = false,
    val category: String = CATEGORY_CLASSIC
) {
    fun normalizedForPreset(): CrosshairConfig = copy(
        sizeDp = sizeDp.coerceIn(14f, 90f),
        opacity = opacity.coerceIn(0.10f, 1.00f),
        rotationDeg = ((rotationDeg % 360f) + 360f) % 360f,
        primaryColorHex = primaryColorHex or 0xFF000000L,
        outlineColorHex = outlineColorHex or 0xFF000000L
    )

    fun matchesVisualSettings(other: CrosshairConfig): Boolean {
        val thisRot = ((rotationDeg % 360f) + 360f) % 360f
        val otherRot = ((other.rotationDeg % 360f) + 360f) % 360f
        val rotDiff = kotlin.math.abs(thisRot - otherRot)
        val wrappedRotDiff = minOf(rotDiff, 360f - rotDiff)
        return style == other.style &&
            kotlin.math.abs(sizeDp - other.sizeDp) < 0.05f &&
            kotlin.math.abs(thicknessDp - other.thicknessDp) < 0.05f &&
            kotlin.math.abs(gapDp - other.gapDp) < 0.05f &&
            centerDot == other.centerDot &&
            kotlin.math.abs(centerDotSizeDp - other.centerDotSizeDp) < 0.05f &&
            wrappedRotDiff < 0.5f &&
            kotlin.math.abs(opacity - other.opacity) < 0.005f &&
            (primaryColorHex or 0xFF000000L) == (other.primaryColorHex or 0xFF000000L) &&
            outlineEnabled == other.outlineEnabled &&
            (outlineColorHex or 0xFF000000L) == (other.outlineColorHex or 0xFF000000L) &&
            kotlin.math.abs(outlineThicknessDp - other.outlineThicknessDp) < 0.05f &&
            tStyle == other.tStyle &&
            offsetX == other.offsetX &&
            offsetY == other.offsetY
    }

    fun toJson(): String {
        val json = org.json.JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("style", style.name)
        json.put("sizeDp", sizeDp.toDouble())
        json.put("thicknessDp", thicknessDp.toDouble())
        json.put("gapDp", gapDp.toDouble())
        json.put("centerDot", centerDot)
        json.put("centerDotSizeDp", centerDotSizeDp.toDouble())
        json.put("rotationDeg", rotationDeg.toDouble())
        json.put("opacity", opacity.toDouble())
        json.put("primaryColorHex", primaryColorHex)
        json.put("outlineEnabled", outlineEnabled)
        json.put("outlineColorHex", outlineColorHex)
        json.put("outlineThicknessDp", outlineThicknessDp.toDouble())
        json.put("tStyle", tStyle)
        json.put("offsetX", offsetX)
        json.put("offsetY", offsetY)
        json.put("isCustom", isCustom)
        json.put("category", category)
        return json.toString(2)
    }

    companion object {
        const val CATEGORY_CLASSIC = "Classic"
        const val CATEGORY_DOT = "Dot"
        const val CATEGORY_CIRCLE = "Circle"
        const val CATEGORY_TACTICAL = "Tactical"
        const val CATEGORY_SNIPER = "Sniper"
        const val CATEGORY_SCIFI = "Sci-Fi"
        const val CATEGORY_CUSTOM = "Custom"
        const val CATEGORY_AI_LAB = "AI Lab"
        const val CATEGORY_AI_CONTRAST = "AI Contrast"
        const val CATEGORY_AI_COACH = "AI Coach"

        @StringRes
        fun resolveCategoryLabelRes(rawCategory: String): Int? = when (rawCategory.trim().lowercase()) {
            "classic" -> R.string.category_classic
            "dot" -> R.string.category_dot
            "circle" -> R.string.category_circle
            "tactical" -> R.string.category_tactical
            "sniper" -> R.string.category_sniper
            "sci-fi", "scifi" -> R.string.category_scifi
            "custom" -> R.string.category_custom
            "ai lab" -> R.string.nav_ai_lab
            "ai contrast" -> R.string.category_ai_contrast
            "ai coach" -> R.string.category_ai_coach
            else -> null
        }

        fun localizedCategoryName(context: Context, rawCategory: String): String {
            val resId = resolveCategoryLabelRes(rawCategory)
            return if (resId != null) context.getString(resId)
            else rawCategory.ifBlank { context.getString(R.string.category_custom) }
        }

        fun fromJson(jsonStr: String, context: Context? = null): CrosshairConfig? {
            return try {
                val json = org.json.JSONObject(jsonStr.trim())
                val styleStr = json.optString("style", CrosshairStyle.CLASSIC_CROSS.name)
                val style = try {
                    CrosshairStyle.valueOf(styleStr)
                } catch (_: Exception) {
                    CrosshairStyle.CLASSIC_CROSS
                }
                val fallbackName = context?.getString(R.string.default_imported_reticle_name).orEmpty()
                CrosshairConfig(
                    id = json.optString("id", System.currentTimeMillis().toString()),
                    name = json.optString("name", fallbackName).ifBlank { fallbackName },
                    style = style,
                    sizeDp = json.optDouble("sizeDp", 36.0).toFloat(),
                    thicknessDp = json.optDouble("thicknessDp", 3.0).toFloat(),
                    gapDp = json.optDouble("gapDp", 8.0).toFloat(),
                    centerDot = json.optBoolean("centerDot", true),
                    centerDotSizeDp = json.optDouble("centerDotSizeDp", 4.0).toFloat(),
                    rotationDeg = json.optDouble("rotationDeg", 0.0).toFloat(),
                    opacity = json.optDouble("opacity", 1.0).toFloat(),
                    primaryColorHex = json.optLong("primaryColorHex", 0xFF00E676),
                    outlineEnabled = json.optBoolean("outlineEnabled", true),
                    outlineColorHex = json.optLong("outlineColorHex", 0xFF000000),
                    outlineThicknessDp = json.optDouble("outlineThicknessDp", 1.5).toFloat(),
                    tStyle = json.optBoolean("tStyle", false),
                    offsetX = json.optInt("offsetX", 0),
                    offsetY = json.optInt("offsetY", 0),
                    isCustom = true,
                    category = json.optString("category", CATEGORY_CUSTOM)
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
