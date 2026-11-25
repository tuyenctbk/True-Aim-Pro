package com.example.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.model.CrosshairConfig
import com.example.ui.theme.ArenaDarkCanvas
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.ReticleColors
import com.example.ui.theme.tvFocusable
import java.util.Locale
import kotlin.math.roundToInt

fun colorLongToHsv(colorHex: Long): FloatArray {
    val hsv = FloatArray(3)
    val argb = (colorHex or 0xFF000000L).toInt()
    AndroidColor.colorToHSV(argb, hsv)
    return hsv
}

fun hsvToColorLong(hue: Float, saturation: Float, value: Float): Long {
    val argb = AndroidColor.HSVToColor(
        255,
        floatArrayOf(
            hue.coerceIn(0f, 360f),
            saturation.coerceIn(0f, 1f),
            value.coerceIn(0f, 1f)
        )
    )
    return (argb.toLong() and 0x00FFFFFFL) or 0xFF000000L
}

fun formatHexRgb(colorHex: Long): String {
    val r = ((colorHex shr 16) and 0xFF).toInt()
    val g = ((colorHex shr 8) and 0xFF).toInt()
    val b = (colorHex and 0xFF).toInt()
    return String.format(Locale.US, "#%02X%02X%02X", r, g, b)
}

fun contrastContentColor(colorHex: Long): Color {
    val r = ((colorHex shr 16) and 0xFF).toFloat() / 255f
    val g = ((colorHex shr 8) and 0xFF).toFloat() / 255f
    val b = (colorHex and 0xFF).toFloat() / 255f
    val luminance = 0.299f * r + 0.587f * g + 0.114f * b
    return if (luminance > 0.62f) Color.Black else Color.White
}

fun parseHexColorInput(hexInput: String): Long? {
    val cleaned = hexInput.trim().removePrefix("#")
    if (cleaned.length != 6 && cleaned.length != 8) return null
    return try {
        val parsed = cleaned.toLong(16)
        if (cleaned.length == 6) (0xFF000000L or parsed) else (parsed or 0xFF000000L)
    } catch (_: NumberFormatException) {
        null
    }
}

@Composable
fun HsvSaturationValueBoard(
    hue: Float,
    saturation: Float,
    value: Float,
    onSatValChanged: (Float, Float) -> Unit,
    onInteractionFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val latestOnChange by rememberUpdatedState(onSatValChanged)
    val latestOnFinish by rememberUpdatedState(onInteractionFinished)
    val pureHueColor = remember(hue) { Color.hsv(hue.coerceIn(0f, 359.99f), 1f, 1f) }
    val pickedColor = remember(hue, saturation, value) {
        Color.hsv(hue.coerceIn(0f, 359.99f), saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
    }
    val boardDesc = stringResource(R.string.color_spectrum_title)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(172.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .semantics { contentDescription = boardDesc }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val s = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    val v = (1f - (offset.y / size.height.toFloat())).coerceIn(0f, 1f)
                    latestOnChange(s, v)
                    latestOnFinish()
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val s = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val v = (1f - (offset.y / size.height.toFloat())).coerceIn(0f, 1f)
                        latestOnChange(s, v)
                    },
                    onDragEnd = { latestOnFinish() },
                    onDragCancel = { latestOnFinish() },
                    onDrag = { change, _ ->
                        change.consume()
                        val s = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val v = (1f - (change.position.y / size.height.toFloat())).coerceIn(0f, 1f)
                        latestOnChange(s, v)
                    }
                )
            }
            .testTag("color_spectrum_board")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = pureHueColor)
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.White, Color.Transparent)
                )
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black)
                )
            )

            // Precision thumb indicator
            val cx = (saturation.coerceIn(0f, 1f) * size.width)
            val cy = ((1f - value.coerceIn(0f, 1f)) * size.height)
            val thumbCenter = Offset(cx, cy)

            drawCircle(
                color = Color.Black.copy(alpha = 0.45f),
                radius = 14.dp.toPx(),
                center = thumbCenter
            )
            drawCircle(
                color = Color.White,
                radius = 12.dp.toPx(),
                center = thumbCenter,
                style = Stroke(width = 2.5.dp.toPx())
            )
            drawCircle(
                color = pickedColor,
                radius = 9.5.dp.toPx(),
                center = thumbCenter
            )
        }
    }
}

@Composable
fun HueSpectrumSlider(
    hue: Float,
    onHueChanged: (Float) -> Unit,
    onInteractionFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val latestOnHue by rememberUpdatedState(onHueChanged)
    val latestOnFinish by rememberUpdatedState(onInteractionFinished)
    val hueColors = remember {
        listOf(
            Color(0xFFFF0000),
            Color(0xFFFFFF00),
            Color(0xFF00FF00),
            Color(0xFF00FFFF),
            Color(0xFF0000FF),
            Color(0xFFFF00FF),
            Color(0xFFFF0000)
        )
    }
    val hueDesc = stringResource(R.string.color_hue_label)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp) // Meets 48dp minimum touch target height
            .semantics { contentDescription = hueDesc }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newHue = ((offset.x / size.width.toFloat()).coerceIn(0f, 1f)) * 359.99f
                    latestOnHue(newHue)
                    latestOnFinish()
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val newHue = ((offset.x / size.width.toFloat()).coerceIn(0f, 1f)) * 359.99f
                        latestOnHue(newHue)
                    },
                    onDragEnd = { latestOnFinish() },
                    onDragCancel = { latestOnFinish() },
                    onDrag = { change, _ ->
                        change.consume()
                        val newHue = ((change.position.x / size.width.toFloat()).coerceIn(0f, 1f)) * 359.99f
                        latestOnHue(newHue)
                    }
                )
            }
            .testTag("color_hue_slider"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
        ) {
            val barRadius = CornerRadius(size.height / 2f, size.height / 2f)
            drawRoundRect(
                brush = Brush.horizontalGradient(hueColors),
                cornerRadius = barRadius
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.25f),
                cornerRadius = barRadius,
                style = Stroke(width = 1.dp.toPx())
            )

            val fraction = (hue / 360f).coerceIn(0f, 1f)
            val thumbX = (fraction * size.width).coerceIn(10.dp.toPx(), size.width - 10.dp.toPx())
            val thumbCenter = Offset(thumbX, size.height / 2f)
            val pureHue = Color.hsv(hue.coerceIn(0f, 359.99f), 1f, 1f)

            drawCircle(
                color = Color.Black.copy(alpha = 0.45f),
                radius = 13.dp.toPx(),
                center = thumbCenter
            )
            drawCircle(
                color = Color.White,
                radius = 11.5.dp.toPx(),
                center = thumbCenter
            )
            drawCircle(
                color = pureHue,
                radius = 8.5.dp.toPx(),
                center = thumbCenter
            )
        }
    }
}

@Composable
fun CrosshairContrastPreviewStrip(
    previewConfig: CrosshairConfig,
    hue: Float,
    saturation: Float,
    value: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Split dark/light mini arena so user can verify color contrast in real time
        Box(
            modifier = Modifier
                .width(108.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(ArenaDarkCanvas)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFFE2E8F0))
                )
            }
            CrosshairView(
                config = previewConfig,
                overrideSize = 30.dp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.color_contrast_preview_label),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(
                    R.string.color_hsv_format,
                    hue.roundToInt().coerceIn(0, 360),
                    (saturation * 100f).roundToInt().coerceIn(0, 100),
                    (value * 100f).roundToInt().coerceIn(0, 100)
                ),
                fontFamily = MonoFontFamily,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RealTimeColorPickerPanel(
    config: CrosshairConfig,
    recentColors: List<Long>,
    onColorChanged: (Long) -> Unit,
    onColorCommitted: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var hue by remember { mutableFloatStateOf(colorLongToHsv(config.primaryColorHex)[0]) }
    var sat by remember { mutableFloatStateOf(colorLongToHsv(config.primaryColorHex)[1]) }
    var value by remember { mutableFloatStateOf(colorLongToHsv(config.primaryColorHex)[2]) }

    // Synchronize from external changes (HEX field, RGB sliders, Esports swatches, presets)
    // only if the computed HSV hex differs from incoming config.primaryColorHex.
    LaunchedEffect(config.primaryColorHex) {
        val currentComputed = hsvToColorLong(hue, sat, value)
        if ((currentComputed and 0x00FFFFFFL) != (config.primaryColorHex and 0x00FFFFFFL)) {
            val hsv = colorLongToHsv(config.primaryColorHex)
            if (hsv[1] > 0.01f && hsv[2] > 0.01f) {
                hue = hsv[0]
            }
            sat = hsv[1]
            value = hsv[2]
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        CrosshairContrastPreviewStrip(
            previewConfig = config,
            hue = hue,
            saturation = sat,
            value = value
        )

        Spacer(modifier = Modifier.height(12.dp))

        HsvSaturationValueBoard(
            hue = hue,
            saturation = sat,
            value = value,
            onSatValChanged = { newSat, newVal ->
                sat = newSat
                value = newVal
                onColorChanged(hsvToColorLong(hue, sat, value))
            },
            onInteractionFinished = {
                onColorCommitted(hsvToColorLong(hue, sat, value))
            }
        )

        Spacer(modifier = Modifier.height(6.dp))

        HueSpectrumSlider(
            hue = hue,
            onHueChanged = { newHue ->
                hue = newHue
                onColorChanged(hsvToColorLong(hue, sat, value))
            },
            onInteractionFinished = {
                onColorCommitted(hsvToColorLong(hue, sat, value))
            }
        )

        if (recentColors.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.color_recent_title),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                recentColors.forEach { recentHex ->
                    val isSelected = (config.primaryColorHex and 0x00FFFFFFL) == (recentHex and 0x00FFFFFFL)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(recentHex))
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable {
                                onColorChanged(recentHex)
                                onColorCommitted(recentHex)
                            }
                            .testTag("recent_color_chip_$recentHex"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = contrastContentColor(recentHex),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CrosshairColorPickerDialog(
    config: CrosshairConfig,
    recentColors: List<Long>,
    onColorChanged: (Long) -> Unit,
    onColorCommitted: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val initialColorHex = remember { config.primaryColorHex }
    var hexInput by remember(config.primaryColorHex) {
        mutableStateOf(formatHexRgb(config.primaryColorHex))
    }
    var isHexValid by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = {
            onColorCommitted(config.primaryColorHex)
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 24.dp)
                .testTag("crosshair_color_picker_dialog")
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.color_picker_dialog_title),
                                fontFamily = DisplayFontFamily,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatHexRgb(config.primaryColorHex),
                                fontFamily = MonoFontFamily,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            onColorCommitted(config.primaryColorHex)
                            onDismiss()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.btn_close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Before vs Current comparison pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(initialColorHex))
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.color_original_label),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.color_new_label),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(config.primaryColorHex))
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                RealTimeColorPickerPanel(
                    config = config,
                    recentColors = recentColors,
                    onColorChanged = onColorChanged,
                    onColorCommitted = onColorCommitted
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.esports_palette_title),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReticleColors.forEach { presetColor ->
                        val hexLong = (presetColor.toArgb().toLong() and 0xFFFFFFFFL) or 0xFF000000L
                        val isSelected = (config.primaryColorHex and 0x00FFFFFFL) == (hexLong and 0x00FFFFFFL)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(presetColor)
                                .border(
                                    if (isSelected) 2.5.dp else 1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    CircleShape
                                )
                                .clickable {
                                    onColorChanged(hexLong)
                                    onColorCommitted(hexLong)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = contrastContentColor(hexLong),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { input ->
                        hexInput = input
                        val parsed = parseHexColorInput(input)
                        if (parsed != null) {
                            isHexValid = true
                            onColorChanged(parsed)
                        } else {
                            isHexValid = input.length < 6
                        }
                    },
                    isError = !isHexValid,
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.hex_placeholder),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_hex_color_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onColorChanged(initialColorHex)
                        },
                        enabled = (config.primaryColorHex and 0x00FFFFFFL) != (initialColorHex and 0x00FFFFFFL),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("color_picker_revert_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.color_revert_btn),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            onColorCommitted(config.primaryColorHex)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .tvFocusable(RoundedCornerShape(12.dp), MaterialTheme.colorScheme.primary)
                            .testTag("color_picker_done_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.btn_done),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
