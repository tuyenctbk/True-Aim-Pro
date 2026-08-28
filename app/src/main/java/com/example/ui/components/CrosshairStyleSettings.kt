package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CrosshairConfig
import com.example.ui.theme.MinimalBorder
import com.example.ui.theme.MinimalPurple
import com.example.ui.theme.MinimalPurpleContainer
import com.example.ui.theme.MinimalPurpleDark
import com.example.ui.theme.MinimalSurface
import com.example.ui.theme.MinimalSurfaceContainer
import com.example.ui.theme.ReticleColors
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun CrosshairStyleSettings(
    config: CrosshairConfig,
    onSizeChange: (Float) -> Unit,
    onThicknessChange: (Float) -> Unit,
    onGapChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onColorHexSelected: (Long) -> Unit,
    onCenterDotToggled: (Boolean) -> Unit,
    onDotRadiusChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentColor = Color(config.primaryColorHex)
    val redVal = (config.primaryColorHex shr 16 and 0xFF).toInt()
    val greenVal = (config.primaryColorHex shr 8 and 0xFF).toInt()
    val blueVal = (config.primaryColorHex and 0xFF).toInt()
    val hexDisplay = String.format("#%02X%02X%02X", redVal, greenVal, blueVal)
    var hexInput by remember(config.primaryColorHex) { mutableStateOf(hexDisplay) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MinimalSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MinimalBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("crosshair_style_settings_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MinimalPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.style_settings_header),
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                // Swatch preview badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MinimalSurfaceContainer)
                        .border(1.dp, MinimalBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(currentColor)
                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = hexDisplay,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Color Swatches Palette
            Text(
                text = stringResource(R.string.reticle_color).uppercase(),
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .testTag("color_picker_palette"),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReticleColors.forEach { color ->
                    val colorHex = color.toArgb().toLong() and 0xFFFFFFFFL
                    val isSelected = config.primaryColorHex == colorHex

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                if (isSelected) 3.dp else 1.dp,
                                if (isSelected) MinimalPurple else MinimalBorder,
                                CircleShape
                            )
                            .clickable { onColorHexSelected(colorHex) }
                            .testTag("color_swatch_${colorHex.toString(16)}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Slider: Size
            SliderRow(
                title = stringResource(R.string.size),
                valueDisplay = "${config.sizeDp.toInt()} dp",
                value = config.sizeDp,
                valueRange = 10f..100f,
                onValueChange = onSizeChange,
                testTag = "size_slider"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Slider: Thickness
            SliderRow(
                title = stringResource(R.string.thickness),
                valueDisplay = String.format("%.1f dp", config.thicknessDp),
                value = config.thicknessDp,
                valueRange = 1f..15f,
                onValueChange = onThicknessChange,
                testTag = "thickness_slider"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Slider: Gap
            SliderRow(
                title = stringResource(R.string.gap),
                valueDisplay = "${config.gapDp.toInt()} dp",
                value = config.gapDp,
                valueRange = 0f..40f,
                onValueChange = onGapChange,
                testTag = "gap_slider"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Slider: Opacity
            SliderRow(
                title = stringResource(R.string.opacity),
                valueDisplay = "${(config.opacity * 100).toInt()}%",
                value = config.opacity,
                valueRange = 0.1f..1f,
                onValueChange = onOpacityChange,
                testTag = "opacity_slider"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Center Dot Toggle & Radius Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.center_dot),
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = config.centerDot,
                    onCheckedChange = onCenterDotToggled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MinimalPurple,
                        checkedTrackColor = MinimalPurpleContainer
                    ),
                    modifier = Modifier.testTag("center_dot_switch")
                )
            }

            if (config.centerDot) {
                Spacer(modifier = Modifier.height(6.dp))
                SliderRow(
                    title = stringResource(R.string.dot_radius),
                    valueDisplay = String.format("%.1f dp", config.centerDotSizeDp),
                    value = config.centerDotSizeDp,
                    valueRange = 1f..12f,
                    onValueChange = onDotRadiusChange,
                    testTag = "dot_radius_slider"
                )
            }
        }
    }
}

@Composable
private fun SliderRow(
    title: String,
    valueDisplay: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = valueDisplay,
                color = MinimalPurpleDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MinimalPurple,
                activeTrackColor = MinimalPurple,
                inactiveTrackColor = MinimalSurfaceContainer
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}
