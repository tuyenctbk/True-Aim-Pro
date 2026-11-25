package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CrosshairConfig
import com.example.model.CrosshairStyle
import com.example.ui.CrosshairViewModel
import com.example.ui.PreviewBackground
import com.example.ui.components.BatteryOptimizationDialog
import com.example.ui.components.CrosshairColorPickerDialog
import com.example.ui.components.CrosshairView
import com.example.ui.components.CustomPresetsQuickSwitchSection
import com.example.ui.components.GameBackgroundCanvas
import com.example.ui.components.GettingStartedTooltipSequence
import com.example.ui.components.InteractiveTargetTutorialOverlay
import com.example.ui.components.RealTimeColorPickerPanel
import com.example.ui.components.formatHexRgb
import com.example.ui.theme.ArenaDarkCanvas
import com.example.ui.theme.LaserCrimson
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.PolarWhite
import com.example.ui.theme.ReticleColors
import com.example.ui.theme.tvFocusable
import java.util.Locale
import kotlin.math.abs

@Composable
fun StudioScreen(
    viewModel: CrosshairViewModel,
    onNavigateToTrainer: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onNavigateToDesigns: () -> Unit,
    onToggleOverlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config by viewModel.activeConfig.collectAsState()
    val isOverlayActive by viewModel.isOverlayActive.collectAsState()
    val background by viewModel.selectedBackground.collectAsState()
    val isOverlayPermissionGranted by viewModel.isOverlayPermissionGranted.collectAsState()
    val recentColors by viewModel.recentCustomColors.collectAsState()
    val savedPresets by viewModel.savedPresets.collectAsState()

    val context = LocalContext.current
    var showBatteryGuide by remember { mutableStateOf(false) }
    var showGettingStartedGuide by remember { mutableStateOf(!isOverlayPermissionGranted) }
    var showInteractiveTutorial by remember { mutableStateOf(false) }
    var showColorPickerDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(bottom = 96.dp)
                .testTag("studio_screen")
        ) {
            if (showGettingStartedGuide) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    GettingStartedTooltipSequence(
                        isOverlayPermissionGranted = isOverlayPermissionGranted,
                        isOverlayActive = isOverlayActive,
                        onOpenPermissionSettings = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        },
                        onToggleOverlay = onToggleOverlay,
                        onNavigateToGallery = onNavigateToGallery,
                        onOpenPermissionTutorial = { viewModel.openPermissionTutorialDialog() },
                        onDismissPermanently = { showGettingStartedGuide = false }
                    )
                }
            }

            MasterOverlayCard(
                isOverlayActive = isOverlayActive,
                onToggle = onToggleOverlay,
                onCalibrate = { viewModel.toggleCalibrateHud() }
            )

            Spacer(modifier = Modifier.height(14.dp))

            GamePreviewStage(
                config = config,
                currentBg = background,
                onSelectBg = { viewModel.setBackground(it) },
                onSizeChange = { newSize -> viewModel.setReticleSizeDp(newSize) },
                onOpacityChange = { newOpacity -> viewModel.setReticleOpacity(newOpacity) },
                onRotationChange = { newRotation -> viewModel.setReticleRotationDeg(newRotation) },
                onResetCrosshairSettings = {
                    viewModel.resetSelectedCrosshairSettings()
                    Toast.makeText(
                        context,
                        context.getString(R.string.crosshair_settings_reset_toast),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onSavePreset = { viewModel.openSaveDialog() },
                onShowTutorial = { showInteractiveTutorial = true },
                onTestRange = onNavigateToTrainer,
                onOpenColorPicker = { showColorPickerDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            CustomPresetsQuickSwitchSection(
                presets = savedPresets,
                activeConfig = config,
                onSelectPreset = { preset -> viewModel.quickSwitchPreset(preset) },
                onSaveCurrent = { viewModel.openSaveDialog() },
                onUpdatePreset = { preset -> viewModel.updatePresetWithActiveConfig(preset) },
                onRenamePreset = { preset -> viewModel.openRenameDialog(preset) },
                onDeletePreset = { preset -> viewModel.openDeleteConfirmDialog(preset) },
                onViewAll = onNavigateToGallery
            )

            Spacer(modifier = Modifier.height(18.dp))

            ReticleStyleSelector(
                currentStyle = config.style,
                onStyleSelected = { style ->
                    viewModel.updateConfig("style") { it.copy(style = style) }
                },
                onBrowseDesigns = onNavigateToDesigns
            )

            Spacer(modifier = Modifier.height(18.dp))

            RealTimeColorStudioSection(
                config = config,
                recentColors = recentColors,
                onColorHexSelected = { hex -> viewModel.setPrimaryColorHex(hex) },
                onColorCommitted = { hex -> viewModel.recordRecentColor(hex) },
                onHexTextEntered = { hexText -> viewModel.parseAndApplyHexString(hexText) },
                onRgbChanged = { r, g, b -> viewModel.setPrimaryColorRgb(r, g, b) },
                onOutlineToggled = { enabled -> viewModel.updateConfig("outline") { it.copy(outlineEnabled = enabled) } },
                onCenterDotToggled = { enabled -> viewModel.updateConfig("dot") { it.copy(centerDot = enabled) } },
                onTShapeToggled = { enabled -> viewModel.updateConfig("t_style") { it.copy(tStyle = enabled) } }
            )

            Spacer(modifier = Modifier.height(18.dp))

            AlphaTransparencySection(
                opacity = config.opacity,
                onOpacityChange = { v -> viewModel.setReticleOpacity(v) }
            )

            Spacer(modifier = Modifier.height(18.dp))

            GeometrySlidersSection(
                config = config,
                onUpdate = { transform -> viewModel.updateConfig("geometry", transform) }
            )

            Spacer(modifier = Modifier.height(18.dp))

            CenterAlignmentSection(
                config = config,
                onNudge = { dx, dy -> viewModel.nudgeOffset(dx, dy) },
                onReset = { viewModel.resetOffset() }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Save Custom Preset Quick Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("quick_save_preset_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.save_as_game_preset_title),
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.save_as_game_preset_desc),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { viewModel.openSaveDialog() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("studio_save_preset_btn")
                    ) {
                        Text(text = stringResource(R.string.save_btn), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Quick Floating Action Buttons (Battery Info + Overlay Power)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isOverlayActive) {
                FloatingActionButton(
                    onClick = { showBatteryGuide = true },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .testTag("fab_battery_guide")
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryChargingFull,
                        contentDescription = stringResource(R.string.cd_battery_info),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            val fabBg by animateColorAsState(
                targetValue = if (isOverlayActive) LaserCrimson else MaterialTheme.colorScheme.primary,
                animationSpec = tween(250),
                label = "fab_bg"
            )

            FloatingActionButton(
                onClick = onToggleOverlay,
                containerColor = fabBg,
                contentColor = if (isOverlayActive) Color.White else MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .tvFocusable(CircleShape, MaterialTheme.colorScheme.primary)
                    .testTag("fab_toggle_overlay")
            ) {
                Icon(
                    imageVector = if (isOverlayActive) Icons.Default.PowerSettingsNew else Icons.Default.Layers,
                    contentDescription = stringResource(R.string.cd_toggle_overlay),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    if (showBatteryGuide) {
        BatteryOptimizationDialog(onDismiss = { showBatteryGuide = false })
    }

    if (showInteractiveTutorial) {
        InteractiveTargetTutorialOverlay(onDismiss = { showInteractiveTutorial = false })
    }

    if (showColorPickerDialog) {
        CrosshairColorPickerDialog(
            config = config,
            recentColors = recentColors,
            onColorChanged = { hex -> viewModel.setPrimaryColorHex(hex) },
            onColorCommitted = { hex -> viewModel.recordRecentColor(hex) },
            onDismiss = { showColorPickerDialog = false }
        )
    }
}

@Composable
fun MasterOverlayCard(
    isOverlayActive: Boolean,
    onToggle: () -> Unit,
    onCalibrate: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isOverlayActive) 1.5.dp else 1.dp,
            color = if (isOverlayActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("master_overlay_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isOverlayActive) NeonGreen
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isOverlayActive) stringResource(R.string.overlay_active_label)
                            else stringResource(R.string.overlay_off_label),
                            color = if (isOverlayActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isOverlayActive) stringResource(R.string.overlay_active_desc)
                        else stringResource(R.string.overlay_off_desc),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Switch(
                    checked = isOverlayActive,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainer
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
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
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
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.floating_calibration_hud),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = onCalibrate,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("toggle_calibration_hud_btn")
                        ) {
                            Text(
                                text = stringResource(R.string.hud_tool_btn),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
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
    onSizeChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onRotationChange: (Float) -> Unit,
    onResetCrosshairSettings: () -> Unit,
    onSavePreset: () -> Unit,
    onShowTutorial: () -> Unit,
    onTestRange: () -> Unit,
    onOpenColorPicker: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("live_preview_card")
    ) {
        Column {
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
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.live_reticle_preview_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.studio_preview_spec_badge,
                                (config.opacity * 100).toInt(),
                                config.sizeDp.toInt()
                            ),
                            fontFamily = MonoFontFamily,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onResetCrosshairSettings,
                        modifier = Modifier
                            .size(36.dp)
                            .tvFocusable(CircleShape, MaterialTheme.colorScheme.primary)
                            .testTag("studio_reset_crosshair_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = stringResource(R.string.cd_reset_crosshair_settings),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = onShowTutorial,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("studio_help_tutorial_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = stringResource(R.string.getting_started_guide),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = onSavePreset,
                        modifier = Modifier
                            .size(36.dp)
                            .tvFocusable(CircleShape, MaterialTheme.colorScheme.primary)
                            .testTag("preview_save_preset_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = stringResource(R.string.cd_save_preset),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .aspectRatio(1.25f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(ArenaDarkCanvas)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                GameBackgroundCanvas(background = currentBg)

                val animatedSizeDp by animateFloatAsState(
                    targetValue = config.sizeDp,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "preview_size"
                )
                val animatedGapDp by animateFloatAsState(
                    targetValue = config.gapDp,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "preview_gap"
                )
                val animatedOpacity by animateFloatAsState(
                    targetValue = config.opacity,
                    animationSpec = tween(160),
                    label = "preview_opacity"
                )
                val animatedRotation by animateFloatAsState(
                    targetValue = config.rotationDeg,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "preview_rotation"
                )
                val animatedColor by animateColorAsState(
                    targetValue = Color(config.primaryColorHex or 0xFF000000L),
                    animationSpec = tween(180),
                    label = "preview_color"
                )

                CrosshairView(
                    config = config.copy(
                        sizeDp = animatedSizeDp,
                        gapDp = animatedGapDp,
                        opacity = animatedOpacity,
                        rotationDeg = animatedRotation,
                        primaryColorHex = animatedColor.toArgb().toLong() and 0xFFFFFFFFL
                    ),
                    modifier = Modifier.align(Alignment.Center)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(currentBg.nameRes).uppercase(),
                        fontFamily = MonoFontFamily,
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.8.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .clickable { onOpenColorPicker() }
                        .tvFocusable(RoundedCornerShape(12.dp), MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                        .testTag("preview_quick_color_picker_btn")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(13.dp)
                                .clip(CircleShape)
                                .background(Color(config.primaryColorHex))
                                .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatHexRgb(config.primaryColorHex),
                            fontFamily = MonoFontFamily,
                            color = Color.White,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = stringResource(R.string.cd_pick_color),
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .border(1.dp, NeonGreen.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                        .clickable { onTestRange() }
                        .tvFocusable(RoundedCornerShape(12.dp), NeonGreen)
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .testTag("preview_open_trainer_btn")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.cd_aim_trainer),
                            tint = NeonGreen,
                            modifier = Modifier.size(15.dp)
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

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PreviewBackground.entries.forEach { bg ->
                    val isSelected = bg == currentBg
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainer
                            )
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { onSelectBg(bg) }
                            .tvFocusable(RoundedCornerShape(20.dp), MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = stringResource(bg.nameRes),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            LiveOverlayScaleBar(
                sizeDp = config.sizeDp,
                onSizeChange = onSizeChange,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            )

            LiveOverlayOpacityBar(
                opacity = config.opacity,
                onOpacityChange = onOpacityChange,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            )

            LiveOverlayRotationBar(
                rotationDeg = config.rotationDeg,
                onRotationChange = onRotationChange,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun LiveOverlayScaleBar(
    sizeDp: Float,
    onSizeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val scaleMultiplier = (sizeDp / 36f).coerceIn(0.3f, 2.6f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("overlay_scale_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ZoomOutMap,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.overlay_scale_label),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .testTag("overlay_scale_badge")
            ) {
                Text(
                    text = stringResource(
                        R.string.overlay_scale_badge_format,
                        scaleMultiplier,
                        sizeDp.toInt()
                    ),
                    fontFamily = MonoFontFamily,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { onSizeChange((sizeDp - 2f).coerceIn(14f, 90f)) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("overlay_scale_decrease_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = stringResource(R.string.cd_decrease_scale),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Slider(
                value = sizeDp.coerceIn(14f, 90f),
                onValueChange = { onSizeChange(it) },
                valueRange = 14f..90f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("overlay_scale_slider")
            )

            IconButton(
                onClick = { onSizeChange((sizeDp + 2f).coerceIn(14f, 90f)) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("overlay_scale_increase_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_increase_scale),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(0.5f to 18f, 1.0f to 36f, 1.5f to 54f, 2.0f to 72f).forEach { (factor, targetDp) ->
                val isSelected = abs(sizeDp - targetDp) <= 2.5f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface
                        )
                        .border(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onSizeChange(targetDp) }
                        .padding(vertical = 6.dp)
                        .testTag("scale_preset_${targetDp.toInt()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.scale_preset_format, factor),
                        fontFamily = MonoFontFamily,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveOverlayOpacityBar(
    opacity: Float,
    onOpacityChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val clampedOpacity = opacity.coerceIn(0.10f, 1.00f)
    val opacityPercent = (clampedOpacity * 100).toInt()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("overlay_opacity_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Opacity,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.overlay_opacity_label),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .testTag("overlay_opacity_badge")
            ) {
                Text(
                    text = stringResource(R.string.percent_format, opacityPercent),
                    fontFamily = MonoFontFamily,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { onOpacityChange((clampedOpacity - 0.05f).coerceIn(0.10f, 1.00f)) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("overlay_opacity_decrease_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = stringResource(R.string.cd_decrease_opacity),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Slider(
                value = clampedOpacity,
                onValueChange = { onOpacityChange(it.coerceIn(0.10f, 1.00f)) },
                valueRange = 0.10f..1.00f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("overlay_opacity_slider")
            )

            IconButton(
                onClick = { onOpacityChange((clampedOpacity + 0.05f).coerceIn(0.10f, 1.00f)) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("overlay_opacity_increase_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_increase_opacity),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(0.25f to 25, 0.50f to 50, 0.75f to 75, 1.00f to 100).forEach { (targetOpacity, pct) ->
                val isSelected = abs(clampedOpacity - targetOpacity) < 0.05f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface
                        )
                        .border(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onOpacityChange(targetOpacity) }
                        .padding(vertical = 6.dp)
                        .testTag("opacity_preset_$pct"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.percent_format, pct),
                        fontFamily = MonoFontFamily,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveOverlayRotationBar(
    rotationDeg: Float,
    onRotationChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val clampedRotation = ((rotationDeg % 360f) + 360f) % 360f
    val rotationInt = clampedRotation.toInt()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("overlay_rotation_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.RotateRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.overlay_rotation_label),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .testTag("overlay_rotation_badge")
            ) {
                Text(
                    text = stringResource(R.string.unit_deg_int, rotationInt),
                    fontFamily = MonoFontFamily,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { onRotationChange(((clampedRotation - 5f) + 360f) % 360f) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("overlay_rotation_decrease_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = stringResource(R.string.cd_decrease_rotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Slider(
                value = clampedRotation,
                onValueChange = { onRotationChange(it.coerceIn(0f, 360f)) },
                valueRange = 0f..360f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("overlay_rotation_slider")
            )

            IconButton(
                onClick = { onRotationChange((clampedRotation + 5f) % 360f) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("overlay_rotation_increase_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_increase_rotation),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(0f, 45f, 90f, 135f, 180f, 270f).forEach { deg ->
                val isSelected = abs(clampedRotation - deg) < 2f
                val degInt = deg.toInt()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface
                        )
                        .border(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onRotationChange(deg) }
                        .padding(vertical = 6.dp)
                        .testTag("rotation_preset_$degInt"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.unit_deg_int, degInt),
                        fontFamily = MonoFontFamily,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ReticleStyleSelector(
    currentStyle: CrosshairStyle,
    onStyleSelected: (CrosshairStyle) -> Unit,
    onBrowseDesigns: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.reticle_shape_title),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 0.8.sp,
                modifier = Modifier.weight(1f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable { onBrowseDesigns() }
                    .tvFocusable(RoundedCornerShape(12.dp), MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("studio_open_designs_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = stringResource(R.string.studio_browse_designs_btn),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CrosshairStyle.entries.forEach { style ->
                val isSelected = style == currentStyle
                val previewConfig = CrosshairConfig(
                    style = style,
                    sizeDp = 28f,
                    thicknessDp = 2.5f,
                    gapDp = 5f,
                    centerDot = true,
                    centerDotSizeDp = 3f,
                    primaryColorHex = if (isSelected) 0xFF00E676 else 0xFF00E5FF,
                    outlineEnabled = true
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        if (isSelected) 1.5.dp else 1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .width(96.dp)
                        .clickable { onStyleSelected(style) }
                        .tvFocusable(RoundedCornerShape(16.dp), MaterialTheme.colorScheme.primary)
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
                            text = stringResource(style.nameRes),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
    recentColors: List<Long>,
    onColorHexSelected: (Long) -> Unit,
    onColorCommitted: (Long) -> Unit,
    onHexTextEntered: (String) -> Boolean,
    onRgbChanged: (Int, Int, Int) -> Unit,
    onOutlineToggled: (Boolean) -> Unit,
    onCenterDotToggled: (Boolean) -> Unit,
    onTShapeToggled: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val currentColor = Color(config.primaryColorHex)
    val redVal = (config.primaryColorHex shr 16 and 0xFF).toInt()
    val greenVal = (config.primaryColorHex shr 8 and 0xFF).toInt()
    val blueVal = (config.primaryColorHex and 0xFF).toInt()

    val hexDisplay = String.format(Locale.US, "#%02X%02X%02X", redVal, greenVal, blueVal)
    var hexInput by remember(config.primaryColorHex) { mutableStateOf(hexDisplay) }
    var isHexValid by remember { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.color_picker_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .clickable {
                            clipboardManager.setText(AnnotatedString(hexDisplay))
                            Toast.makeText(context, context.getString(R.string.code_copied_toast), Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(currentColor)
                            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = hexDisplay,
                        fontFamily = MonoFontFamily,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.color_spectrum_title),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            RealTimeColorPickerPanel(
                config = config,
                recentColors = recentColors,
                onColorChanged = onColorHexSelected,
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
                ReticleColors.forEach { color ->
                    val colorHex = color.toArgb().toLong() and 0xFFFFFFFFL
                    val isSelected = config.primaryColorHex == colorHex

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                if (isSelected) 3.dp else 1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                CircleShape
                            )
                            .clickable {
                                onColorHexSelected(colorHex)
                                onColorCommitted(colorHex)
                            }
                            .testTag("color_swatch_$colorHex"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = if (color == PolarWhite || color == Color(0xFFFFEA00)) Color.Black else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.real_time_hex_title),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
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
                    placeholder = {
                        Text(
                            stringResource(R.string.hex_placeholder),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("hex_color_input_field")
                )

                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(hexDisplay))
                        Toast.makeText(context, context.getString(R.string.code_copied_toast), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.cd_copy_hex),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.rgb_mixer_title),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            RgbSliderRow(
                channelLabel = stringResource(R.string.rgb_channel_r),
                colorValue = redVal,
                trackColor = Color(0xFFFF5252),
                onValueChange = { onRgbChanged(it, greenVal, blueVal) }
            )
            RgbSliderRow(
                channelLabel = stringResource(R.string.rgb_channel_g),
                colorValue = greenVal,
                trackColor = Color(0xFF00E676),
                onValueChange = { onRgbChanged(redVal, it, blueVal) }
            )
            RgbSliderRow(
                channelLabel = stringResource(R.string.rgb_channel_b),
                colorValue = blueVal,
                trackColor = Color(0xFF40C4FF),
                onValueChange = { onRgbChanged(redVal, greenVal, it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            StudioToggleRow(
                title = stringResource(R.string.high_contrast_outline),
                description = stringResource(R.string.outline_desc),
                checked = config.outlineEnabled,
                onCheckedChange = onOutlineToggled
            )
            Spacer(modifier = Modifier.height(10.dp))
            StudioToggleRow(
                title = stringResource(R.string.center_precision_dot),
                description = stringResource(R.string.center_dot_desc),
                checked = config.centerDot,
                onCheckedChange = onCenterDotToggled
            )
            Spacer(modifier = Modifier.height(10.dp))
            StudioToggleRow(
                title = stringResource(R.string.t_style_reticle),
                description = stringResource(R.string.t_style_desc),
                checked = config.tStyle,
                onCheckedChange = onTShapeToggled
            )
        }
    }
}

@Composable
private fun StudioToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun RgbSliderRow(
    channelLabel: String,
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
                .size(26.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(trackColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = channelLabel,
                fontFamily = MonoFontFamily,
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
                inactiveTrackColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = colorValue.toString(),
            fontFamily = MonoFontFamily,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(34.dp),
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.alpha_transparency_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.percent_format, opacityPercent),
                        fontFamily = MonoFontFamily,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.alpha_transparency_desc),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Slider(
                value = opacity,
                onValueChange = onOpacityChange,
                valueRange = 0.10f..1.00f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("alpha_transparency_slider")
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0.25f to 25, 0.50f to 50, 0.75f to 75, 1.00f to 100).forEach { (v, pct) ->
                    val isSelected = abs(opacity - v) < 0.05f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainer
                            )
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onOpacityChange(v) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.percent_format, pct),
                            fontFamily = MonoFontFamily,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = stringResource(R.string.geometry_sizing_title),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            SliderRow(
                label = stringResource(R.string.reticle_size_label),
                valueDisplay = stringResource(R.string.unit_dp_int, config.sizeDp.toInt()),
                value = config.sizeDp.coerceIn(14f, 90f),
                range = 14f..90f,
                onValueChange = { v -> onUpdate { it.copy(sizeDp = v) } },
                sliderTag = "reticle_size_slider"
            )

            SliderRow(
                label = stringResource(R.string.line_thickness_label),
                valueDisplay = stringResource(R.string.unit_dp_float, config.thicknessDp),
                value = config.thicknessDp,
                range = 1f..10f,
                onValueChange = { v -> onUpdate { it.copy(thicknessDp = v) } }
            )

            SliderRow(
                label = stringResource(R.string.center_gap_label),
                valueDisplay = stringResource(R.string.unit_dp_int, config.gapDp.toInt()),
                value = config.gapDp,
                range = 0f..35f,
                onValueChange = { v -> onUpdate { it.copy(gapDp = v) } }
            )

            if (config.centerDot) {
                SliderRow(
                    label = stringResource(R.string.dot_size_label),
                    valueDisplay = stringResource(R.string.unit_dp_float, config.centerDotSizeDp),
                    value = config.centerDotSizeDp,
                    range = 1f..12f,
                    onValueChange = { v -> onUpdate { it.copy(centerDotSizeDp = v) } }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        R.string.studio_rotation_with_value,
                        stringResource(R.string.rotation_label),
                        stringResource(R.string.unit_deg_int, config.rotationDeg.toInt())
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0f, 45f, 90f, 135f, 180f, 270f).forEach { deg ->
                        val isSel = abs(config.rotationDeg - deg) < 2f
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSel) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainer
                                )
                                .border(
                                    1.dp,
                                    if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onUpdate { it.copy(rotationDeg = deg) } }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.unit_deg_int, deg.toInt()),
                                fontFamily = MonoFontFamily,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
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
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.testTag("reticle_rotation_slider")
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = stringResource(
                            R.string.studio_offset_with_coords,
                            stringResource(R.string.offset_label),
                            stringResource(R.string.studio_offset_coords, config.offsetX, config.offsetY)
                        ),
                        fontFamily = MonoFontFamily,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onReset,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = stringResource(R.string.cd_reset_offset),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = stringResource(R.string.center_btn), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NudgeButton(icon = Icons.Default.KeyboardArrowUp, onClick = { onNudge(0, -1) }, tag = "nudge_up")

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    NudgeButton(icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft, onClick = { onNudge(-1, 0) }, tag = "nudge_left")

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.unit_1px),
                            fontFamily = MonoFontFamily,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    NudgeButton(icon = Icons.AutoMirrored.Filled.KeyboardArrowRight, onClick = { onNudge(1, 0) }, tag = "nudge_right")
                }

                Spacer(modifier = Modifier.height(6.dp))

                NudgeButton(icon = Icons.Default.KeyboardArrowDown, onClick = { onNudge(0, 1) }, tag = "nudge_down")
            }
        }
    }
}

@Composable
fun NudgeButton(
    icon: ImageVector,
    onClick: () -> Unit,
    tag: String
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .testTag(tag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(R.string.cd_nudge_aim),
            tint = MaterialTheme.colorScheme.primary,
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
    onValueChange: (Float) -> Unit,
    sliderTag: String? = null
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = valueDisplay,
                fontFamily = MonoFontFamily,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outline
            ),
            modifier = if (sliderTag != null) Modifier.testTag(sliderTag) else Modifier
        )
    }
}
