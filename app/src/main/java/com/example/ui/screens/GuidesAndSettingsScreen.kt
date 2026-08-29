package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CrosshairViewModel
import com.example.ui.theme.LaserCrimson
import com.example.ui.theme.tvFocusable
import com.example.ui.theme.MinimalBorder
import com.example.ui.theme.MinimalCanvas
import com.example.ui.theme.MinimalGreen
import com.example.ui.theme.MinimalPurple
import com.example.ui.theme.MinimalPurpleContainer
import com.example.ui.theme.MinimalPurpleDark
import com.example.ui.theme.MinimalPurpleLight
import com.example.ui.theme.MinimalSurface
import com.example.ui.theme.MinimalSurfaceContainer
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun GuidesAndSettingsScreen(
    viewModel: CrosshairViewModel? = null,
    onReopenWalkthrough: () -> Unit = {},
    onResetToDefaults: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasOverlayPermission = Settings.canDrawOverlays(context)
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(bottom = 90.dp)
    ) {
        // Gaming Habits & Usage Statistics Dashboard
        if (viewModel != null) {
            UsageStatsDashboardCard(viewModel = viewModel)
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Smart Hardware Sensor Automations Controls
        if (viewModel != null) {
            SmartSensorControlsCard(viewModel = viewModel)
            Spacer(modifier = Modifier.height(14.dp))
            GlobalThemeToggleCard(viewModel = viewModel)
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Quick Interactive Walkthrough Launcher
        InteractiveWalkthroughBanner(onLaunchWalkthrough = onReopenWalkthrough)

        Spacer(modifier = Modifier.height(14.dp))

        // Reset to Default Crosshair Settings Action Card
        ResetToDefaultsCard(onResetToDefaults = onResetToDefaults)

        Spacer(modifier = Modifier.height(14.dp))

        // No-Ads & 100% Free Manifesto Card
        NoAdsManifestoCard()

        Spacer(modifier = Modifier.height(14.dp))

        // Overlay Permission Status Card
        PermissionStatusCard(
            hasPermission = hasOverlayPermission,
            onOpenSettings = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Game Calibration Tips
        GameGuidesCard()

        Spacer(modifier = Modifier.height(14.dp))

        // Android TV & Big Screen Optimization
        AndroidTvGuideCard()

        Spacer(modifier = Modifier.height(14.dp))

        // Fair Play & Transparency Notice
        FairPlayCard()
    }
}

private fun formatTime(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return when {
        hrs > 0 -> "${hrs}h ${mins}m ${secs}s"
        mins > 0 -> "${mins}m ${secs}s"
        else -> "${secs}s"
    }
}

@Composable
fun UsageStatsDashboardCard(
    viewModel: CrosshairViewModel
) {
    val usageStats by viewModel.usageStats.collectAsState()
    val isOverlayActive by viewModel.isOverlayActive.collectAsState()
    var showResetDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    if (showResetDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = androidx.compose.ui.res.stringResource(com.example.R.string.reset_session_timer_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = androidx.compose.ui.res.stringResource(com.example.R.string.reset_session_timer_message),
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetUsageStats()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LaserCrimson),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_reset_session_timer_btn")
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.example.R.string.reset),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(
                    onClick = { showResetDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("cancel_reset_session_timer_btn")
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.example.R.string.cancel),
                        color = TextPrimary
                    )
                }
            },
            modifier = Modifier.testTag("reset_session_timer_dialog")
        )
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MinimalSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MinimalBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("usage_stats_dashboard_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MinimalPurpleContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = MinimalPurpleDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.example.R.string.usage_stats_title),
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.example.R.string.usage_stats_subtitle),
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                if (isOverlayActive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MinimalGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(MinimalGreen)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.example.R.string.active_live),
                                color = MinimalGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stats 2x2 Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBadgeBox(
                    title = androidx.compose.ui.res.stringResource(com.example.R.string.total_playtime),
                    value = formatTime(usageStats.totalActiveTimeSeconds),
                    subText = androidx.compose.ui.res.stringResource(com.example.R.string.accumulated_hud_time),
                    modifier = Modifier.weight(1f)
                )

                StatBadgeBox(
                    title = androidx.compose.ui.res.stringResource(com.example.R.string.current_session),
                    value = if (isOverlayActive) formatTime(usageStats.currentSessionDurationSeconds) else androidx.compose.ui.res.stringResource(com.example.R.string.inactive),
                    subText = if (isOverlayActive) androidx.compose.ui.res.stringResource(com.example.R.string.live_timer_ticking) else androidx.compose.ui.res.stringResource(com.example.R.string.overlay_offline),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBadgeBox(
                    title = androidx.compose.ui.res.stringResource(com.example.R.string.total_sessions),
                    value = "${usageStats.totalSessionsCount}",
                    subText = androidx.compose.ui.res.stringResource(com.example.R.string.overlay_launches),
                    modifier = Modifier.weight(1f)
                )

                StatBadgeBox(
                    title = androidx.compose.ui.res.stringResource(com.example.R.string.avg_session),
                    value = formatTime(usageStats.averageSessionDurationSeconds),
                    subText = androidx.compose.ui.res.stringResource(com.example.R.string.duration_per_run),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Manual Reset Session Timer Button
            androidx.compose.material3.OutlinedButton(
                onClick = { showResetDialog = true },
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MinimalBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reset_session_timer_btn")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.example.R.string.reset_session_timer_btn_text),
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun StatBadgeBox(
    title: String,
    value: String,
    subText: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MinimalSurfaceContainer),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                color = TextTertiary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = MinimalPurpleDark,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subText,
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun SmartSensorControlsCard(
    viewModel: CrosshairViewModel
) {
    val usageStats by viewModel.usageStats.collectAsState()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MinimalSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MinimalBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("smart_sensor_controls_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MinimalPurpleContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = null,
                        tint = MinimalPurpleDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.smart_sensors_title),
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.smart_sensors_subtitle),
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            SensorToggleRow(
                icon = Icons.Default.VisibilityOff,
                title = stringResource(R.string.proximity_auto_hide),
                description = stringResource(R.string.proximity_desc),
                checked = usageStats.proximityAutoHideEnabled,
                onCheckedChange = { viewModel.toggleProximityAutoHide(it) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            SensorToggleRow(
                icon = Icons.Default.LightMode,
                title = stringResource(R.string.ambient_light_opacity),
                description = stringResource(R.string.ambient_light_desc),
                checked = usageStats.autoLightOpacityEnabled,
                onCheckedChange = { viewModel.toggleAutoLightOpacity(it) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            SensorToggleRow(
                icon = Icons.Default.Vibration,
                title = stringResource(R.string.shake_auto_toggle),
                description = stringResource(R.string.shake_desc),
                checked = usageStats.shakeAutoToggleEnabled,
                onCheckedChange = { viewModel.toggleShakeAutoToggle(it) }
            )
        }
    }
}

@Composable
fun GlobalThemeToggleCard(
    viewModel: CrosshairViewModel
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MinimalSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MinimalBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("global_theme_toggle_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MinimalPurpleContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = MinimalPurpleDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.padding(end = 8.dp)) {
                        Text(
                            text = stringResource(R.string.global_theme),
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isDarkMode) stringResource(R.string.dark_theme) else stringResource(R.string.light_theme),
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { viewModel.toggleDarkMode(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MinimalPurple
                    ),
                    modifier = Modifier.testTag("theme_mode_switch")
                )
            }
        }
    }
}

@Composable
fun SensorToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MinimalSurfaceContainer)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MinimalPurple,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MinimalPurple
            )
        )
    }
}

@Composable
fun ResetToDefaultsCard(
    onResetToDefaults: () -> Unit
) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MinimalSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MinimalBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reset_to_default_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MinimalPurpleContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = MinimalPurpleDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.reset_factory_default_title),
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.reset_factory_default_desc),
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val resetToast = stringResource(R.string.reset_factory_default_toast)
            Button(
                onClick = {
                    onResetToDefaults()
                    Toast.makeText(context, resetToast, Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MinimalPurple,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .tvFocusable(RoundedCornerShape(12.dp), MinimalPurple)
                    .testTag("reset_to_default_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.reset_all_default_btn),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun InteractiveWalkthroughBanner(
    onLaunchWalkthrough: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MinimalSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MinimalPurpleLight),
        modifier = Modifier.fillMaxWidth()
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
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MinimalPurpleLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = MinimalPurpleDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.walkthrough_title),
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.walkthrough_subtitle),
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Button(
                onClick = onLaunchWalkthrough,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MinimalPurple,
                    contentColor = Color.White
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(34.dp)
                    .tvFocusable(RoundedCornerShape(12.dp), MinimalPurple)
            ) {
                Text(text = stringResource(R.string.replay), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AndroidTvGuideCard() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MinimalSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MinimalBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = MinimalPurple,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.android_tv_title),
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.android_tv_desc),
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun NoAdsManifestoCard() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MinimalPurpleContainer),
        border = androidx.compose.foundation.BorderStroke(1.dp, MinimalPurpleLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MinimalPurpleLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MinimalPurpleDark,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.no_ads_title),
                        color = MinimalPurpleDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = stringResource(R.string.no_ads_subtitle),
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.no_ads_desc),
                color = TextPrimary,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun PermissionStatusCard(
    hasPermission: Boolean,
    onOpenSettings: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MinimalSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (hasPermission) MinimalGreen.copy(alpha = 0.5f) else LaserCrimson.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (hasPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (hasPermission) MinimalGreen else LaserCrimson,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.overlay_permission_title),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (hasPermission) MinimalGreen.copy(alpha = 0.12f) else LaserCrimson.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (hasPermission) stringResource(R.string.overlay_permission_granted) else stringResource(R.string.overlay_permission_required),
                        color = if (hasPermission) MinimalGreen else LaserCrimson,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.overlay_permission_desc),
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            if (!hasPermission) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onOpenSettings,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MinimalPurple,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .tvFocusable(RoundedCornerShape(12.dp), MinimalPurple)
                        .testTag("grant_permission_btn")
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.grant_overlay_access_btn),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun GameGuidesCard() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MinimalSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MinimalBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Gamepad,
                    contentDescription = null,
                    tint = MinimalPurple,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.optimal_game_settings_title),
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            GameTipItem(
                game = "PUBG Mobile / BGMI",
                tip = "Use the PUBG Headshot T-Shape reticle. Turn on the Floating HUD in training grounds to align precisely with your default hipfire dot."
            )

            Spacer(modifier = Modifier.height(8.dp))

            GameTipItem(
                game = "Call of Duty Mobile",
                tip = "For sniper quickscopes, equip 'Valorant One-Tap Dot'. Center it before ADS so your crosshair is already on target the moment you scope."
            )

            Spacer(modifier = Modifier.height(8.dp))

            GameTipItem(
                game = "Free Fire / MAX",
                tip = "Equip 'Toxic CS Pro' with Toxic Green. Use the D-pad nudger to align with head height for clean drag headshots."
            )

            Spacer(modifier = Modifier.height(8.dp))

            GameTipItem(
                game = "Apex Mobile / Warzone",
                tip = "Use 'Apex Wingman V' or 'Shotgun CQB Box' for high-mobility close-quarters tracking."
            )
        }
    }
}

@Composable
private fun GameTipItem(game: String, tip: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MinimalSurfaceContainer)
            .border(1.dp, MinimalBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = game,
                color = MinimalPurpleDark,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = tip,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun FairPlayCard() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MinimalSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MinimalBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MinimalGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.safe_compliant_title),
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.safe_compliant_desc),
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 17.sp
            )
        }
    }
}

