package com.example.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.CrosshairConfig
import com.example.model.CrosshairStyle

@Entity(tableName = "crosshair_presets")
data class PresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val styleName: String,
    val sizeDp: Float,
    val thicknessDp: Float,
    val gapDp: Float,
    val centerDot: Boolean,
    val centerDotSizeDp: Float,
    val rotationDeg: Float,
    val opacity: Float,
    val primaryColorHex: Long,
    val outlineEnabled: Boolean,
    val outlineColorHex: Long,
    val outlineThicknessDp: Float,
    val tStyle: Boolean,
    val offsetX: Int,
    val offsetY: Int,
    val isCustom: Boolean,
    val category: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toCrosshairConfig(): CrosshairConfig {
        val style = try {
            CrosshairStyle.valueOf(styleName)
        } catch (e: Exception) {
            CrosshairStyle.CLASSIC_CROSS
        }
        return CrosshairConfig(
            id = id,
            name = name,
            style = style,
            sizeDp = sizeDp,
            thicknessDp = thicknessDp,
            gapDp = gapDp,
            centerDot = centerDot,
            centerDotSizeDp = centerDotSizeDp,
            rotationDeg = rotationDeg,
            opacity = opacity,
            primaryColorHex = primaryColorHex,
            outlineEnabled = outlineEnabled,
            outlineColorHex = outlineColorHex,
            outlineThicknessDp = outlineThicknessDp,
            tStyle = tStyle,
            offsetX = offsetX,
            offsetY = offsetY,
            isCustom = isCustom,
            category = category
        )
    }

    companion object {
        fun fromCrosshairConfig(config: CrosshairConfig): PresetEntity {
            return PresetEntity(
                id = config.id,
                name = config.name,
                styleName = config.style.name,
                sizeDp = config.sizeDp,
                thicknessDp = config.thicknessDp,
                gapDp = config.gapDp,
                centerDot = config.centerDot,
                centerDotSizeDp = config.centerDotSizeDp,
                rotationDeg = config.rotationDeg,
                opacity = config.opacity,
                primaryColorHex = config.primaryColorHex,
                outlineEnabled = config.outlineEnabled,
                outlineColorHex = config.outlineColorHex,
                outlineThicknessDp = config.outlineThicknessDp,
                tStyle = config.tStyle,
                offsetX = config.offsetX,
                offsetY = config.offsetY,
                isCustom = config.isCustom,
                category = config.category
            )
        }
    }
}
