package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.example.R
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CrosshairConfig
import com.example.model.CrosshairStyle
import com.example.ui.CrosshairViewModel
import com.example.ui.PreviewBackground
import com.example.ui.components.CrosshairView
import com.example.ui.components.GameBackgroundCanvas
import com.example.ui.components.GettingStartedTooltipSequence
import com.example.ui.theme.ArenaDarkCanvas
import com.example.ui.theme.MinimalBorder
import com.example.ui.theme.MinimalBorderSubtle
import com.example.ui.theme.MinimalGreen
import com.example.ui.theme.MinimalPurple
import com.example.ui.theme.MinimalPurpleContainer
import com.example.ui.theme.MinimalPurpleDark
import com.example.ui.theme.MinimalPurpleLight
import com.example.ui.theme.MinimalSurface
import com.example.ui.theme.MinimalSurfaceCard
import com.example.ui.theme.tvFocusable

import com.example.ui.theme.MinimalSurfaceContainer
import com.example.ui.theme.MinimalSurfaceHigh
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.PolarWhite
import com.example.ui.theme.ReticleColors
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun StudioScreen(
    viewModel: CrosshairViewModel,
    onNavigateToTrainer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config by viewModel.activeConfig.collectAsState()
    val isOverlayActive by viewModel.isOverlayActive.collectAsState()
    val background by viewModel.selectedBackground.collectAsState()
    val showGettingStarted by viewModel.showGettingStartedSequence.collectAsState()
    val isPermissionGranted by viewModel.isOverlayPermissionGranted.collectAsState()

    val scrollState = rememberScrollState()
    var showInteractiveTutorial by remember { mutableStateOf(false) }
    var showBatteryOptimizationDialog by remember { mutableStateOf(false) }

    if (showInteractiveTutorial) {
        com.example.ui.components.InteractiveTargetTutorialOverlay(
            onDismiss = { showInteractiveTutorial = false }
        )
    }

    if (showBatteryOptimizationDialog) {
        com.example.ui.components.BatteryOptimizationDialog(
            onDismiss = { showBatteryOptimizationDialog = false }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 120.dp)
        ) {
            // Non-Intrusive Getting Started Overlay Tooltip Sequence
            if (showGettingStarted) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    GettingStartedTooltipSequence(
                        isOverlayPermissionGranted = isPermissionGranted,
                        isOverlayActive = isOverlayActive,
                        onOpenPermissionSettings = {
                            try {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                context.startActivity(intent)
                            }
                        },
                        onToggleOverlay = { viewModel.toggleOverlay() },
                        onNavigateToGallery = { /* Can switch to gallery */ },
                        onDismissPermanently = { viewModel.dismissGettingStarted() }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Hero Master Switch Banner
            MasterOverlayCard(
                isOverlayActive = isOverlayActive,
                onToggle = { viewModel.toggleOverlay() },
                onCalibrate = { viewModel.toggleCalibrateHud() },
                onOpenBatteryDialog = { showBatteryOptimizationDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Live Game Preview Stage
            GamePreviewStage(
                config = config,
                currentBg = background,
                onSelectBg = { viewModel.setBackground(it) },
                onSavePreset = { viewModel.openSaveDialog() },
                onTestRange = onNavigateToTrainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Crosshair Gallery Composable
            com.example.ui.components.CrosshairGallery(
                currentStyle = config.style,
                onStyleSelected = { newStyle ->
                    viewModel.updateConfig {
                        it.copy(
                            style = newStyle,
                            tStyle = (newStyle == CrosshairStyle.T_SHAPE)
                        )
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Crosshair Style Settings Composable
            com.example.ui.components.CrosshairStyleSettings(
                config = config,
                onSizeChange = { newSize ->
                    viewModel.updateConfig { it.copy(sizeDp = newSize) }
                },
                onThicknessChange = { newThickness ->
                    viewModel.updateConfig { it.copy(thicknessDp = newThickness) }
                },
                onGapChange = { newGap ->
                    viewModel.updateConfig { it.copy(gapDp = newGap) }
                },
                onOpacityChange = { newOpacity ->
                    viewModel.setAlphaOpacity(newOpacity)
                },
                onColorHexSelected = { colorHex ->
                    viewModel.updateConfig { it.copy(primaryColorHex = colorHex) }
                },
                onCenterDotToggled = { dot ->
                    viewModel.updateConfig { it.copy(centerDot = dot) }
                },
                onDotRadiusChange = { radius ->
                    viewModel.updateConfig { it.copy(centerDotSizeDp = radius) }
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Preset Save Action Card
            QuickPresetSaveBanner(
                currentConfig = config,
                onSavePreset = { viewModel.openSaveDialog() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Reticle Style Picker
            ReticleStyleSelector(
                currentStyle = config.style,
                onStyleSelected = { newStyle ->
                    viewModel.updateConfig {
                        it.copy(
                            style = newStyle,
                            tStyle = (newStyle == CrosshairStyle.T_SHAPE)
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Real-Time Color Studio & Hex Picker
            RealTimeColorStudioSection(
                config = config,
                onColorHexSelected = { colorHex ->
                    viewModel.updateConfig { it.copy(primaryColorHex = colorHex) }
                },
                onHexTextEntered = { hexStr ->
                    viewModel.setHexColor(hexStr)
                },
                onRgbChanged = { r, g, b ->
                    viewModel.setRgbColor(r, g, b)
                },
                onOutlineToggled = { enabled ->
                    viewModel.updateConfig { it.copy(outlineEnabled = enabled) }
                },
                onCenterDotToggled = { dot ->
                    viewModel.updateConfig { it.copy(centerDot = dot) }
                },
                onTShapeToggled = { t ->
                    viewModel.updateConfig { it.copy(tStyle = t) }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Dedicated Alpha Transparency (Opacity) Component
            AlphaTransparencySection(
                opacity = config.opacity,
                onOpacityChange = { newOpacity ->
                    viewModel.setAlphaOpacity(newOpacity)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Reticle Geometry Dimensions (Size, Thickness, Gap, Rotation)
            GeometrySlidersSection(
                config = config,
                onUpdate = { viewModel.updateConfig(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pixel-Perfect Center Offset / D-Pad Nudge
            CenterAlignmentSection(
                config = config,
                onNudge = { dx, dy -> viewModel.nudgeOffset(dx, dy) },
                onReset = { viewModel.resetOffset() }
            )
        }

        // Floating Action Button to toggle crosshair overlay on top of other applications using SYSTEM_ALERT_WINDOW
        androidx.compose.material3.FloatingActionButton(
            onClick = {
                val hasPermission = Settings.canDrawOverlays(context)
                if (!hasPermission) {
                    try {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                        context.startActivity(intent)
                    }
                } else {
                    viewModel.toggleOverlay()
                }
            },
            containerColor = if (isOverlayActive) MinimalPurple else MinimalPurpleContainer,
            contentColor = if (isOverlayActive) Color.White else MinimalPurpleDark,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp, end = 20.dp)
                .testTag("overlay_toggle_fab")
        ) {
            Icon(
                imageVector = if (isOverlayActive) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = "Toggle Crosshair Overlay",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun QuickPresetSaveBanner(
    currentConfig: CrosshairConfig,
    onSavePreset: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MinimalSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MinimalBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("quick_preset_save_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MinimalPurpleLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = MinimalPurpleDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.save_as_game_preset_title),
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.save_as_game_preset_desc),
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Button(
                onClick = onSavePreset,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MinimalPurple,
                    contentColor = Color.White
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(text = stringResource(R.string.save_btn), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MasterOverlayCard(
    isOverlayActive: Boolean,
    onToggle: () -> Unit,
    onCalibrate: () -> Unit,
    onOpenBatteryDialog: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverlayActive) MinimalPurpleContainer else MinimalSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isOverlayActive) MinimalPurple else MinimalBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("master_overlay_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isOverlayActive) MinimalGreen else TextTertiary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isOverlayActive) stringResource(R.string.overlay_active_label) else stringResource(R.string.overlay_off_label),
                            color = if (isOverlayActive) MinimalPurpleDark else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onOpenBatteryDialog,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("battery_optimization_info_icon")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Battery Optimization Info",
                                tint = MinimalPurple,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isOverlayActive)
                            stringResource(R.string.overlay_active_desc)
                        else
                            stringResource(R.string.overlay_off_desc),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Switch(
                    checked = isOverlayActive,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MinimalPurple,
                        checkedTrackColor = MinimalPurpleLight,
                        uncheckedThumbColor = TextTertiary,
                        uncheckedTrackColor = MinimalSurfaceContainer
                    ),
                    modifier = Modifier.testTag("overlay_toggle_switch")
                )
            }

            AnimatedVisibility(visible = isOverlayActive) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MinimalBorder)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CenterFocusStrong,
                                contentDescription = null,
                                tint = MinimalPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.floating_calibration_hud),
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = onCalibrate,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MinimalPurple,
                                contentColor = Color.White
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("toggle_calibration_hud_btn")
                        ) {
                            Text(text = stringResource(R.string.hud_tool_btn), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GamePreviewStage(
    config: CrosshairConfig,
    currentBg: PreviewBackground,
    onSelectBg: (PreviewBackground) -> Unit,
    onSavePreset: () -> Unit,
    onTestRange: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MinimalSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MinimalBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("live_preview_card")
    ) {
        Column {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = MinimalPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.live_reticle_preview_title),
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Live Parameters Summary Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MinimalPurpleLight)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${config.style.displayName} • ${(config.opacity * 100).toInt()}% • ${config.sizeDp.toInt()}dp",
                            color = MinimalPurpleDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onSavePreset,
                        modifier = Modifier
                            .size(32.dp)
                            .tvFocusable(CircleShape, MinimalPurple)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "Save Preset",
                            tint = MinimalPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Canvas Stage (Square Arena)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .aspectRatio(1.25f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(ArenaDarkCanvas)
                    .border(1.dp, MinimalBorder, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Background Terrain
                GameBackgroundCanvas(background = currentBg)

                // Reticle Rendering
                CrosshairView(
                    config = config,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Background Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = currentBg.displayName.uppercase(),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    )
                }

                // Aim Range Button Overlay in corner
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .clickable { onTestRange() }
                        .tvFocusable(RoundedCornerShape(12.dp), NeonGreen)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Test reflex range",
                            tint = NeonGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.aim_trainer_btn),
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Background Switcher Strip
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PreviewBackground.values().forEach { bg ->
                    val isSelected = bg == currentBg
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) MinimalPurple else MinimalSurfaceContainer)
                            .border(
                                1.dp,
                                if (isSelected) MinimalPurple else MinimalBorder,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { onSelectBg(bg) }
                            .tvFocusable(RoundedCornerShape(20.dp), MinimalPurple)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = bg.displayName,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ReticleStyleSelector(
    currentStyle: CrosshairStyle,
    onStyleSelected: (CrosshairStyle) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.reticle_shape_title),
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CrosshairStyle.values().forEach { style ->
                val isSelected = style == currentStyle
                val previewConfig = CrosshairConfig(
                    style = style,
                    sizeDp = 28f,
                    thicknessDp = 2.5f,
                    gapDp = 5f,
                    centerDot = true,
                    centerDotSizeDp = 3f,
                    primaryColorHex = if (isSelected) 0xFF6750A4 else 0xFF10B981,
                    outlineEnabled = true
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MinimalPurpleContainer else MinimalSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        if (isSelected) 1.5.dp else 1.dp,
                        if (isSelected) MinimalPurple else MinimalBorder
                    ),
                    modifier = Modifier
                        .width(96.dp)
                        .clickable { onStyleSelected(style) }
                        .tvFocusable(RoundedCornerShape(16.dp), MinimalPurple)
                        .testTag("reticle_style_${style.name}")
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ArenaDarkCanvas),
                            contentAlignment = Alignment.Center
                        ) {
                            CrosshairView(
                                config = previewConfig,
                                overrideSize = 36.dp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = style.displayName,
                            color = if (isSelected) MinimalPurpleDark else TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RealTimeColorStudioSection(
    config: CrosshairConfig,
    onColorHexSelected: (Long) -> Unit,
    onHexTextEntered: (String) -> Boolean,
    onRgbChanged: (Int, Int, Int) -> Unit,
    onOutlineToggled: (Boolean) -> Unit,
    onCenterDotToggled: (Boolean) -> Unit,
    onTShapeToggled: (Boolean) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val currentColor = Color(config.primaryColorHex)
    val redVal = (config.primaryColorHex shr 16 and 0xFF).toInt()
    val greenVal = (config.primaryColorHex shr 8 and 0xFF).toInt()
    val blueVal = (config.primaryColorHex and 0xFF).toInt()

    val hexDisplay = String.format("#%02X%02X%02X", redVal, greenVal, blueVal)
    var hexInput by remember(config.primaryColorHex) { mutableStateOf(hexDisplay) }
    var isHexValid by remember { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MinimalSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MinimalBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("color_studio_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MinimalPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.color_picker_title),
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                // Live Color Swatch & Hex Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MinimalSurfaceContainer)
                        .border(1.dp, MinimalBorder, RoundedCornerShape(10.dp))
                        .clickable {
                            clipboardManager.setText(AnnotatedString(hexDisplay))
                        }
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

            // Pro Esports Swatches
            Text(
                text = stringResource(R.string.esports_palette_title),
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
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
                            .testTag("color_swatch_${colorHex}"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = if (color == PolarWhite) Color.Black else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Real-Time Custom Hex Input Tool
            Text(
                text = stringResource(R.string.real_time_hex_title),
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { input ->
                        hexInput = input
                        val success = onHexTextEntered(input)
                        isHexValid = success || input.length < 6
                    },
                    isError = !isHexValid,
                    placeholder = { Text("#00E676", fontSize = 12.sp, color = TextTertiary) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MinimalPurple,
                        unfocusedBorderColor = MinimalBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = MinimalSurfaceContainer,
                        unfocusedContainerColor = MinimalSurfaceContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("hex_color_input_field")
                )

                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(hexDisplay))
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MinimalSurfaceContainer)
                        .border(1.dp, MinimalBorder, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Hex Code",
                        tint = MinimalPurple,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live RGB Fine-Tuning Sliders
            Text(
                text = stringResource(R.string.rgb_mixer_title),
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Red Slider
            RgbSliderRow(
                channelLabel = "R",
                channelName = "Red",
                colorValue = redVal,
                trackColor = Color(0xFFFF5252),
                onValueChange = { newRed ->
                    onRgbChanged(newRed, greenVal, blueVal)
                }
            )

            // Green Slider
            RgbSliderRow(
                channelLabel = "G",
                channelName = "Green",
                colorValue = greenVal,
                trackColor = Color(0xFF00E676),
                onValueChange = { newGreen ->
                    onRgbChanged(redVal, newGreen, blueVal)
                }
            )

            // Blue Slider
            RgbSliderRow(
                channelLabel = "B",
                channelName = "Blue",
                colorValue = blueVal,
                trackColor = Color(0xFF40C4FF),
                onValueChange = { newBlue ->
                    onRgbChanged(redVal, greenVal, newBlue)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Toggles Row (Outline, Dot, T-Style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.high_contrast_outline),
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.outline_desc),
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = config.outlineEnabled,
                    onCheckedChange = onOutlineToggled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MinimalPurple,
                        checkedTrackColor = MinimalPurpleLight
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.center_precision_dot),
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.center_dot_desc),
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = config.centerDot,
                    onCheckedChange = onCenterDotToggled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MinimalPurple,
                        checkedTrackColor = MinimalPurpleLight
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.t_style_reticle),
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.t_style_desc),
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = config.tStyle,
                    onCheckedChange = onTShapeToggled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MinimalPurple,
                        checkedTrackColor = MinimalPurpleLight
                    )
                )
            }
        }
    }
}

@Composable
fun RgbSliderRow(
    channelLabel: String,
    channelName: String,
    colorValue: Int,
    trackColor: Color,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(trackColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = channelLabel,
                color = trackColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Slider(
            value = colorValue.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(
                thumbColor = trackColor,
                activeTrackColor = trackColor,
                inactiveTrackColor = MinimalBorder
            ),
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = colorValue.toString(),
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun AlphaTransparencySection(
    opacity: Float,
    onOpacityChange: (Float) -> Unit
) {
    val opacityPercent = (opacity * 100).toInt()

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MinimalSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MinimalBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("alpha_transparency_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Opacity,
                        contentDescription = null,
                        tint = MinimalPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.alpha_transparency_title),
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MinimalPurpleContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$opacityPercent%",
                        color = MinimalPurpleDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.alpha_transparency_desc),
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Main Opacity Slider
            Slider(
                value = opacity,
                onValueChange = onOpacityChange,
                valueRange = 0.10f..1.00f,
                colors = SliderDefaults.colors(
                    thumbColor = MinimalPurple,
                    activeTrackColor = MinimalPurple,
                    inactiveTrackColor = MinimalBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("alpha_transparency_slider")
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Quick Preset Opacity Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0.25f to "25%", 0.50f to "50%", 0.75f to "75%", 1.00f to "100%").forEach { (v, label) ->
                    val isSelected = Math.abs(opacity - v) < 0.05f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) MinimalPurple else MinimalSurfaceContainer)
                            .border(1.dp, if (isSelected) MinimalPurple else MinimalBorder, RoundedCornerShape(10.dp))
                            .clickable { onOpacityChange(v) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GeometrySlidersSection(
    config: CrosshairConfig,
    onUpdate: ((CrosshairConfig) -> CrosshairConfig) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MinimalSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MinimalBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = stringResource(R.string.geometry_sizing_title),
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Size Slider
            SliderRow(
                label = stringResource(R.string.reticle_size_label),
                valueDisplay = "${config.sizeDp.toInt()} dp",
                value = config.sizeDp,
                range = 14f..90f,
                onValueChange = { v -> onUpdate { it.copy(sizeDp = v) } }
            )

            // Line Thickness
            SliderRow(
                label = stringResource(R.string.line_thickness_label),
                valueDisplay = String.format("%.1f dp", config.thicknessDp),
                value = config.thicknessDp,
                range = 1f..10f,
                onValueChange = { v -> onUpdate { it.copy(thicknessDp = v) } }
            )

            // Gap
            SliderRow(
                label = stringResource(R.string.center_gap_label),
                valueDisplay = "${config.gapDp.toInt()} dp",
                value = config.gapDp,
                range = 0f..35f,
                onValueChange = { v -> onUpdate { it.copy(gapDp = v) } }
            )

            // Center Dot Size
            if (config.centerDot) {
                SliderRow(
                    label = stringResource(R.string.dot_size_label),
                    valueDisplay = String.format("%.1f dp", config.centerDotSizeDp),
                    value = config.centerDotSizeDp,
                    range = 1f..12f,
                    onValueChange = { v -> onUpdate { it.copy(centerDotSizeDp = v) } }
                )
            }

            // Rotation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stringResource(R.string.rotation_label)} (${config.rotationDeg.toInt()}°)",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0f, 45f, 90f).forEach { deg ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (config.rotationDeg == deg) MinimalPurpleLight else MinimalSurfaceContainer)
                                .border(
                                    1.dp,
                                    if (config.rotationDeg == deg) MinimalPurple else MinimalBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onUpdate { it.copy(rotationDeg = deg) } }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${deg.toInt()}°",
                                color = if (config.rotationDeg == deg) MinimalPurpleDark else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (config.rotationDeg == deg) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
            Slider(
                value = config.rotationDeg,
                onValueChange = { v -> onUpdate { it.copy(rotationDeg = v) } },
                valueRange = 0f..360f,
                colors = SliderDefaults.colors(
                    thumbColor = MinimalPurple,
                    activeTrackColor = MinimalPurple,
                    inactiveTrackColor = MinimalBorder
                )
            )
        }
    }
}

@Composable
fun CenterAlignmentSection(
    config: CrosshairConfig,
    onNudge: (dx: Int, dy: Int) -> Unit,
    onReset: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MinimalSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MinimalBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.position_alignment_title),
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "${stringResource(R.string.offset_label)}: X=${config.offsetX}px, Y=${config.offsetY}px",
                        color = MinimalPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onReset,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MinimalPurpleLight,
                        contentColor = MinimalPurpleDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset offset",
                        tint = MinimalPurpleDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = stringResource(R.string.center_btn), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // D-Pad Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // UP
                NudgeButton(icon = Icons.Default.KeyboardArrowUp, onClick = { onNudge(0, -1) }, tag = "nudge_up")

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // LEFT
                    NudgeButton(icon = Icons.Default.KeyboardArrowLeft, onClick = { onNudge(-1, 0) }, tag = "nudge_left")

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MinimalPurpleContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "1px",
                            color = MinimalPurpleDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // RIGHT
                    NudgeButton(icon = Icons.Default.KeyboardArrowRight, onClick = { onNudge(1, 0) }, tag = "nudge_right")
                }

                Spacer(modifier = Modifier.height(6.dp))

                // DOWN
                NudgeButton(icon = Icons.Default.KeyboardArrowDown, onClick = { onNudge(0, 1) }, tag = "nudge_down")
            }
        }
    }
}

@Composable
fun NudgeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    tag: String
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MinimalSurfaceContainer)
            .border(1.dp, MinimalBorder, RoundedCornerShape(12.dp))
            .testTag(tag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MinimalPurple,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun SliderRow(
    label: String,
    valueDisplay: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = valueDisplay,
                color = MinimalPurple,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = MinimalPurple,
                activeTrackColor = MinimalPurple,
                inactiveTrackColor = MinimalBorder
            )
        )
    }
}
