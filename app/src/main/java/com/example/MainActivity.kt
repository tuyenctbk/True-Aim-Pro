package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ui.theme.tvFocusable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CrosshairViewModel
import com.example.ui.components.ImportCrosshairDialog
import com.example.ui.components.PermissionExplanationDialog
import com.example.ui.components.SavePresetDialog
import com.example.ui.components.ShareCrosshairDialog
import com.example.ui.components.SmartRateAppDialog
import com.example.ui.components.SmartShareAppDialog
import com.example.ui.screens.AimTrainerScreen
import com.example.ui.screens.GuidesAndSettingsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PresetsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.LaserCrimson
import com.example.ui.theme.MinimalBorder
import com.example.ui.theme.MinimalCanvas
import com.example.ui.theme.MinimalGreen
import com.example.ui.theme.MinimalPurple
import com.example.ui.theme.MinimalPurpleContainer
import com.example.ui.theme.MinimalPurpleDark
import com.example.ui.theme.MinimalPurpleLight
import com.example.ui.theme.MinimalSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

class MainActivity : ComponentActivity() {

    private val viewModel: CrosshairViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            MyApplicationTheme(darkTheme = isDarkMode) {
                CrosshairMainApp(viewModel = viewModel)
            }
        }
    }
}

enum class NavSection(val titleRes: Int, val icon: ImageVector) {
    STUDIO(R.string.nav_studio, Icons.Default.Tune),
    GALLERY(R.string.nav_presets, Icons.Default.GridView),
    TRAINER(R.string.onboarding_slide3_title, Icons.Default.SportsEsports),
    GUIDES(R.string.nav_guides_settings, Icons.Default.Info)
}

@Composable
fun CrosshairMainApp(viewModel: CrosshairViewModel) {
    var selectedSection by remember { mutableIntStateOf(0) }

    val isOverlayActive by viewModel.isOverlayActive.collectAsState()
    val showSaveDialog by viewModel.showSaveDialog.collectAsState()
    val showPermissionDialog by viewModel.showPermissionDialog.collectAsState()
    val showShareDialog by viewModel.showShareDialog.collectAsState()
    val showImportDialog by viewModel.showImportDialog.collectAsState()
    val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsState()
    val isPermissionGranted by viewModel.isOverlayPermissionGranted.collectAsState()
    val showSmartRateDialog by viewModel.showSmartRateDialog.collectAsState()
    val showSmartShareDialog by viewModel.showSmartShareDialog.collectAsState()

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isWideScreen = screenWidthDp >= 600

    if (!hasCompletedOnboarding) {
        OnboardingScreen(
            isPermissionGranted = isPermissionGranted,
            onCompleteOnboarding = { viewModel.completeOnboarding() },
            onRefreshPermission = { viewModel.refreshPermissionState() }
        )
    } else {
        if (isWideScreen) {
            // Adaptive TV & Tablet Side-by-Side Dual Pane Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                GamerNavRail(
                    selectedIndex = selectedSection,
                    onSelectIndex = { selectedSection = it },
                    isOverlayActive = isOverlayActive,
                    onToggleOverlay = { viewModel.toggleOverlay() }
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                ) {
                    TopGamerAppBar(
                        isOverlayActive = isOverlayActive,
                        onToggleOverlay = { viewModel.toggleOverlay() }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        when (selectedSection) {
                            0 -> StudioScreen(
                                viewModel = viewModel,
                                onNavigateToTrainer = { selectedSection = 2 }
                            )
                            1 -> com.example.ui.screens.ReticleGalleryScreen(
                                viewModel = viewModel,
                                onNavigateToStudio = { selectedSection = 0 }
                            )
                            2 -> AimTrainerScreen(
                                viewModel = viewModel
                            )
                            3 -> GuidesAndSettingsScreen(
                                viewModel = viewModel,
                                onReopenWalkthrough = {
                                    viewModel.reopenOnboarding()
                                    selectedSection = 0
                                },
                                onResetToDefaults = {
                                    viewModel.resetToFactoryDefaults()
                                }
                            )
                        }
                    }
                }
            }
        } else {
            // Standard Mobile Layout
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    TopGamerAppBar(
                        isOverlayActive = isOverlayActive,
                        onToggleOverlay = { viewModel.toggleOverlay() }
                    )
                },
                bottomBar = {
                    BottomGamerNavBar(
                        selectedIndex = selectedSection,
                        onSelectIndex = { selectedSection = it }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (selectedSection) {
                        0 -> StudioScreen(
                            viewModel = viewModel,
                            onNavigateToTrainer = { selectedSection = 2 }
                        )
                        1 -> com.example.ui.screens.ReticleGalleryScreen(
                            viewModel = viewModel,
                            onNavigateToStudio = { selectedSection = 0 }
                        )
                        2 -> AimTrainerScreen(
                            viewModel = viewModel
                        )
                        3 -> GuidesAndSettingsScreen(
                            viewModel = viewModel,
                            onReopenWalkthrough = {
                                viewModel.reopenOnboarding()
                                selectedSection = 0
                            },
                            onResetToDefaults = {
                                viewModel.resetToFactoryDefaults()
                            }
                        )
                    }
                }
            }
        }

        // Dialogs
        if (showSaveDialog) {
            SavePresetDialog(
                onDismiss = { viewModel.closeSaveDialog() },
                onSave = { name, gameTag -> viewModel.saveCurrentAsPreset(name, gameTag) }
            )
        }

        if (showPermissionDialog) {
            PermissionExplanationDialog(
                onDismiss = { viewModel.dismissPermissionDialog() }
            )
        }

        showShareDialog?.let { configToShare ->
            ShareCrosshairDialog(
                config = configToShare,
                code = viewModel.encodeCrosshairCode(configToShare),
                onDismiss = { viewModel.closeShareDialog() }
            )
        }

        if (showImportDialog) {
            ImportCrosshairDialog(
                onDismiss = { viewModel.closeImportDialog() },
                onImport = { code -> viewModel.importCrosshairCode(code) }
            )
        }

        if (showSmartRateDialog) {
            SmartRateAppDialog(
                onRateSubmitted = { viewModel.markAppRated() },
                onDismissLater = { viewModel.dismissRatePrompt() }
            )
        }

        if (showSmartShareDialog) {
            SmartShareAppDialog(
                onShareConfirmed = { viewModel.markAppShared() },
                onDismissLater = { viewModel.dismissSharePrompt() }
            )
        }
    }
}

@Composable
fun GamerNavRail(
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    isOverlayActive: Boolean,
    onToggleOverlay: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .width(105.dp)
            .fillMaxHeight(),
        border = androidx.compose.foundation.BorderStroke(1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 24.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "AIM",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "PRO",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(28.dp))

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isOverlayActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
                        .clickable { onToggleOverlay() }
                        .tvFocusable(CircleShape, MaterialTheme.colorScheme.primary)
                        .testTag("appbar_quick_toggle_rail"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Toggle Crosshair Overlay",
                        tint = if (isOverlayActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NavSection.values().forEachIndexed { index, section ->
                    val isSelected = selectedIndex == index
                    val titleStr = stringResource(section.titleRes)

                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectIndex(index) }
                            .tvFocusable(RoundedCornerShape(16.dp), MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("nav_tab_rail_${section.name.lowercase()}"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = section.icon,
                                contentDescription = titleStr,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = titleStr,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Text(
                text = "v1.0.0",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun TopGamerAppBar(
    isOverlayActive: Boolean,
    onToggleOverlay: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Brand with Clean Minimalism style
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.app_name),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PRO",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isOverlayActive) NeonGreen else MinimalGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isOverlayActive) stringResource(R.string.active_live) else stringResource(R.string.inactive),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            // Clean Minimalist Power/Settings Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (isOverlayActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
                    .clickable { onToggleOverlay() }
                    .tvFocusable(CircleShape, MaterialTheme.colorScheme.primary)
                    .testTag("appbar_quick_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "Toggle Crosshair Overlay",
                    tint = if (isOverlayActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun BottomGamerNavBar(
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavSection.values().forEachIndexed { index, section ->
                val isSelected = selectedIndex == index
                val titleStr = stringResource(section.titleRes)

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectIndex(index) }
                        .tvFocusable(RoundedCornerShape(16.dp), MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .testTag("nav_tab_${section.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = section.icon,
                            contentDescription = titleStr,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = titleStr,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
