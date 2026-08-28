package com.example.model

enum class CrosshairStyle(val displayName: String, val category: String) {
    CLASSIC_CROSS("Classic Cross", "Classic"),
    PRECISION_DOT("Precision Dot", "Dot"),
    TACTICAL_CIRCLE("Tactical Circle", "Tactical"),
    MIL_DOT_SNIPER("Mil-Dot Sniper", "Sniper"),
    CYBER_HEX("Cyber Hex", "Sci-Fi"),
    CHEVRON_ARROW("Chevron Arrow", "Tactical"),
    SHOTGUN_BOX("Shotgun Box", "Tactical"),
    T_SHAPE("T-Crosshair", "Classic"),
    DYNAMIC_RING("Dynamic Ring", "Tactical"),
    TRI_BLADE("Tri-Blade", "Sci-Fi"),
    HOLLOW_DIAMOND("Hollow Diamond", "Dot"),
    CROSS_CIRCLE("Cross Circle", "Classic"),
    CORNER_BRACKETS("Corner Brackets", "Tactical"),
    SONAR_RADAR("Sonar Radar", "Sci-Fi"),
    MICRO_PLUS("Micro Plus", "Dot"),
    HEARTBEAT("Pulse Cross", "Sci-Fi")
}

data class CrosshairConfig(
    val id: String = "default",
    val name: String = "Toxic Tactical",
    val style: CrosshairStyle = CrosshairStyle.CLASSIC_CROSS,
    val sizeDp: Float = 36f,
    val thicknessDp: Float = 3f,
    val gapDp: Float = 8f,
    val centerDot: Boolean = true,
    val centerDotSizeDp: Float = 4f,
    val rotationDeg: Float = 0f,
    val opacity: Float = 1.0f,
    val primaryColorHex: Long = 0xFF00E676, // Toxic Green
    val outlineEnabled: Boolean = true,
    val outlineColorHex: Long = 0xFF000000, // Black outline for contrast
    val outlineThicknessDp: Float = 1.5f,
    val tStyle: Boolean = false,
    val offsetX: Int = 0,
    val offsetY: Int = 0,
    val isCustom: Boolean = false,
    val category: String = "Classic"
) {
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
        fun fromJson(jsonStr: String): CrosshairConfig? {
            return try {
                val json = org.json.JSONObject(jsonStr.trim())
                val styleStr = json.optString("style", CrosshairStyle.CLASSIC_CROSS.name)
                val style = try { CrosshairStyle.valueOf(styleStr) } catch (e: Exception) { CrosshairStyle.CLASSIC_CROSS }
                CrosshairConfig(
                    id = json.optString("id", System.currentTimeMillis().toString()),
                    name = json.optString("name", "Community Reticle"),
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
                    category = json.optString("category", "Custom")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
