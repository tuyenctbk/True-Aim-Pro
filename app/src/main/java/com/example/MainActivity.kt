package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.ai.configureAppCheck
import com.example.data.engagement.SmartPopupProposal
import com.example.ui.AiLabViewModel
import com.example.ui.CrosshairViewModel
import com.example.ui.components.DeletePresetConfirmDialog
import com.example.ui.components.ImportCrosshairDialog
import com.example.ui.components.PermissionExplanationDialog
import com.example.ui.components.RenamePresetDialog
import com.example.ui.components.SavePresetDialog
import com.example.ui.components.ShareCrosshairDialog
import com.example.ui.components.SmartRateAppDialog
import com.example.ui.components.SmartShareAppDialog
import com.example.ui.components.SmartUpdateAppDialog
import com.example.ui.screens.AiLabScreen
import com.example.ui.screens.AimTrainerScreen
import com.example.ui.screens.GuidesAndSettingsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PredefinedDesignsScreen
import com.example.ui.screens.ReticleGalleryScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.tvFocusable

class MainActivity : ComponentActivity() {

    private val viewModel: CrosshairViewModel by viewModels()
    private val aiLabViewModel: AiLabViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            MyApplicationTheme(darkTheme = isDarkMode) {
                CrosshairMainApp(
                    viewModel = viewModel,
                    aiLabViewModel = aiLabViewModel
                )
            }
        }
    }
}

enum class NavSection(val routeKey: String, val titleRes: Int, val icon: ImageVector) {
    STUDIO("studio", R.string.nav_studio, Icons.Default.Tune),
    GALLERY("gallery", R.string.nav_presets, Icons.Default.GridView),
    AI_LAB("ai_lab", R.string.nav_ai_lab, Icons.Default.AutoAwesome),
    TRAINER("trainer", R.string.nav_trainer, Icons.Default.SportsEsports),
    GUIDES("settings", R.string.nav_guides_settings, Icons.Default.Info)
}

@Composable
fun CrosshairMainApp(
    viewModel: CrosshairViewModel,
    aiLabViewModel: AiLabViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedSection by rememberSaveable { mutableIntStateOf(0) }
    var showPredefinedDesigns by rememberSaveable { mutableStateOf(false) }

    val isSplashVisible by viewModel.isSplashVisible.collectAsState()
    val isOverlayActive by viewModel.isOverlayActive.collectAsState()
    val showSaveDialog by viewModel.showSaveDialog.collectAsState()
    val savePresetDraftConfig by viewModel.savePresetDraftConfig.collectAsState()
    val renamingPreset by viewModel.renamingPreset.collectAsState()
    val deletingPreset by viewModel.deletingPreset.collectAsState()
    val savedPresets by viewModel.savedPresets.collectAsState()
    val presetFeedbackMessage by viewModel.presetFeedbackMessage.collectAsState()
    val showPermissionDialog by viewModel.showPermissionDialog.collectAsState()
    val showShareDialog by viewModel.showShareDialog.collectAsState()
    val showImportDialog by viewModel.showImportDialog.collectAsState()
    val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsState()
    val isPermissionGranted by viewModel.isOverlayPermissionGranted.collectAsState()
    val activeSmartPopup by viewModel.activeSmartPopup.collectAsState()
    val updateCheckMessageRes by viewModel.updateCheckMessageRes.collectAsState()

    // Automatically refresh overlay permission state whenever user returns from System Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Android 13+ POST_NOTIFICATIONS runtime permission launcher for Foreground Service notification
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.logNotificationsPermissionGranted()
        }
        viewModel.toggleOverlay()
    }

    val handleToggleOverlay: () -> Unit = {
        if (!isOverlayActive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotifPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotifPermission && isPermissionGranted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.toggleOverlay()
            }
        } else {
            viewModel.toggleOverlay()
        }
    }

    // Notify ViewModel on tab switch (analytics + clean Aim Trainer stop)
    LaunchedEffect(selectedSection, showPredefinedDesigns, isSplashVisible, hasCompletedOnboarding) {
        if (!isSplashVisible && hasCompletedOnboarding) {
            if (showPredefinedDesigns) {
                viewModel.onScreenSelected("predefined_designs")
            } else {
                val section = NavSection.entries.getOrNull(selectedSection) ?: NavSection.STUDIO
                viewModel.onScreenSelected(section.routeKey)
            }
        }
    }

    // Attempt silent Google auto sign-in for previously authorized accounts after onboarding
    LaunchedEffect(isSplashVisible, hasCompletedOnboarding) {
        if (!isSplashVisible && hasCompletedOnboarding) {
            aiLabViewModel.attemptSilentSignIn(context)
        }
    }

    // Show Toast for manual "Check for Updates" result
    LaunchedEffect(updateCheckMessageRes) {
        updateCheckMessageRes?.let { resId ->
            Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show()
            viewModel.clearUpdateCheckMessage()
        }
    }

    // Show Toast feedback when saving, updating, renaming, switching, or deleting custom presets
    LaunchedEffect(presetFeedbackMessage) {
        presetFeedbackMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearPresetFeedbackMessage()
        }
    }

    AnimatedContent(
        targetState = Triple(isSplashVisible, hasCompletedOnboarding, Unit),
        transitionSpec = {
            fadeIn(animationSpec = tween(320)) togetherWith fadeOut(animationSpec = tween(260))
        },
        label = "app_root_transition"
    ) { (showSplash, onboarded, _) ->
        when {
            showSplash -> {
                SplashScreen(
                    onFinished = { viewModel.onSplashFinished() }
                )
            }

            !onboarded -> {
                OnboardingScreen(
                    isPermissionGranted = isPermissionGranted,
                    onCompleteOnboarding = { viewModel.completeOnboarding() },
                    onRefreshPermission = { viewModel.refreshPermissionState() }
                )
            }

            else -> {
                AnimatedContent(
                    targetState = showPredefinedDesigns,
                    transitionSpec = {
                        if (targetState) {
                            (slideInVertically(tween(260, easing = FastOutSlowInEasing)) { it / 14 } + fadeIn(tween(240))) togetherWith
                                fadeOut(tween(180))
                        } else {
                            fadeIn(tween(220)) togetherWith fadeOut(tween(180))
                        }
                    },
                    label = "predefined_designs_transition"
                ) { showingDesigns ->
                    if (showingDesigns) {
                        PredefinedDesignsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { showPredefinedDesigns = false },
                            onCustomizeInStudio = {
                                showPredefinedDesigns = false
                                selectedSection = 0
                            },
                            onToggleOverlay = handleToggleOverlay,
                            onEnsureOverlayStarted = {
                                if (!isOverlayActive) {
                                    handleToggleOverlay()
                                }
                            }
                        )
                    } else {
                        BackHandler(enabled = selectedSection != 0) {
                            selectedSection = 0
                        }

                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val isWideScreen = maxWidth >= 600.dp

                            if (isWideScreen) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.background)
                                ) {
                                    GamerNavRail(
                                        selectedIndex = selectedSection,
                                        onSelectIndex = { selectedSection = it },
                                        isOverlayActive = isOverlayActive,
                                        onToggleOverlay = handleToggleOverlay
                                    )

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxSize()
                                    ) {
                                        TopGamerAppBar(
                                            isOverlayActive = isOverlayActive,
                                            onToggleOverlay = handleToggleOverlay
                                        )

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 16.dp)
                                        ) {
                                            MainSectionHost(
                                                selectedSection = selectedSection,
                                                viewModel = viewModel,
                                                aiLabViewModel = aiLabViewModel,
                                                onToggleOverlay = handleToggleOverlay,
                                                onNavigateToSection = { selectedSection = it },
                                                onNavigateToDesigns = { showPredefinedDesigns = true }
                                            )
                                        }
                                    }
                                }
                            } else {
                                Scaffold(
                                    modifier = Modifier.fillMaxSize(),
                                    containerColor = MaterialTheme.colorScheme.background,
                                    topBar = {
                                        TopGamerAppBar(
                                            isOverlayActive = isOverlayActive,
                                            onToggleOverlay = handleToggleOverlay
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
                                        MainSectionHost(
                                            selectedSection = selectedSection,
                                            viewModel = viewModel,
                                            aiLabViewModel = aiLabViewModel,
                                            onToggleOverlay = handleToggleOverlay,
                                            onNavigateToSection = { selectedSection = it },
                                            onNavigateToDesigns = { showPredefinedDesigns = true }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialogs (only active after splash & onboarding)
    if (!isSplashVisible && hasCompletedOnboarding) {
        if (showSaveDialog || savePresetDraftConfig != null) {
            val activeConfig by viewModel.activeConfig.collectAsState()
            val draftConfig = savePresetDraftConfig ?: activeConfig
            SavePresetDialog(
                config = draftConfig,
                existingPresets = savedPresets,
                onDismiss = { viewModel.closeSaveDialog() },
                onSave = { name, gameTag, overwriteId ->
                    viewModel.saveConfigAsPreset(
                        name = name,
                        category = gameTag,
                        sourceConfig = draftConfig,
                        overwritePresetId = overwriteId
                    )
                }
            )
        }

        renamingPreset?.let { presetToRename ->
            RenamePresetDialog(
                preset = presetToRename,
                onDismiss = { viewModel.closeRenameDialog() },
                onRename = { newName -> viewModel.renameCustomPreset(presetToRename, newName) }
            )
        }

        deletingPreset?.let { presetToDelete ->
            DeletePresetConfirmDialog(
                preset = presetToDelete,
                onDismiss = { viewModel.closeDeleteConfirmDialog() },
                onConfirmDelete = { viewModel.confirmDeletePreset(presetToDelete) }
            )
        }

        if (showPermissionDialog) {
            PermissionExplanationDialog(
                onDismiss = { viewModel.dismissPermissionDialog() },
                isPermissionGranted = isPermissionGranted
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

        // Single-slot Smart Auto-Popup Host (prevents overlapping popups)
        when (val proposal = activeSmartPopup) {
            is SmartPopupProposal.UpdateApp -> {
                SmartUpdateAppDialog(
                    proposal = proposal,
                    onUpdateNow = { viewModel.onSmartUpdateAccepted(proposal.latestVersionCode) },
                    onDismissLater = { viewModel.onSmartUpdateLater() },
                    onSkipVersion = { viewModel.onSmartUpdateSkipped(proposal.latestVersionCode) }
                )
            }

            is SmartPopupProposal.RateApp -> {
                SmartRateAppDialog(
                    onRateSubmitted = { stars -> viewModel.onSmartRateSubmitted(stars) },
                    onDismissLater = { viewModel.onSmartRateDismissedLater() },
                    onNeverAsk = { viewModel.onSmartRateNeverAsk() }
                )
            }

            is SmartPopupProposal.ShareApp -> {
                SmartShareAppDialog(
                    onShareConfirmed = { viewModel.onSmartShareConfirmed() },
                    onDismissLater = { viewModel.onSmartShareDismissedLater() },
                    onNeverAsk = { viewModel.onSmartShareNeverAsk() }
                )
            }

            null -> Unit
        }
    }
}

@Composable
private fun MainSectionHost(
    selectedSection: Int,
    viewModel: CrosshairViewModel,
    aiLabViewModel: AiLabViewModel,
    onToggleOverlay: () -> Unit,
    onNavigateToSection: (Int) -> Unit,
    onNavigateToDesigns: () -> Unit
) {
    AnimatedContent(
        targetState = selectedSection,
        transitionSpec = {
            val forward = targetState > initialState
            (slideInHorizontally(tween(240, easing = FastOutSlowInEasing)) { width ->
                if (forward) width / 10 else -width / 10
            } + fadeIn(tween(220))) togetherWith
                (slideOutHorizontally(tween(180, easing = FastOutSlowInEasing)) { width ->
                    if (forward) -width / 12 else width / 12
                } + fadeOut(tween(160)))
        },
        label = "section_content_transition"
    ) { targetSection ->
        when (targetSection) {
            0 -> StudioScreen(
                viewModel = viewModel,
                onNavigateToTrainer = { onNavigateToSection(3) },
                onNavigateToGallery = { onNavigateToSection(1) },
                onNavigateToDesigns = onNavigateToDesigns,
                onToggleOverlay = onToggleOverlay
            )

            1 -> ReticleGalleryScreen(
                viewModel = viewModel,
                onNavigateToStudio = { onNavigateToSection(0) },
                onNavigateToDesigns = onNavigateToDesigns
            )

            2 -> AiLabScreen(
                crosshairViewModel = viewModel,
                aiViewModel = aiLabViewModel,
                onNavigateToStudio = { onNavigateToSection(0) }
            )

            3 -> AimTrainerScreen(
                viewModel = viewModel,
                onAnalyzeWithAiCoach = {
                    aiLabViewModel.selectTool(2)
                    aiLabViewModel.runAimCoach(
                        lastDrill = viewModel.gameState.value,
                        difficulty = viewModel.selectedDifficulty.value,
                        usageStats = viewModel.usageStats.value,
                        summary = viewModel.analyticsSummary.value,
                        activeConfig = viewModel.activeConfig.value
                    )
                    onNavigateToSection(2)
                }
            )

            4 -> GuidesAndSettingsScreen(
                viewModel = viewModel,
                onReopenWalkthrough = {
                    viewModel.reopenOnboarding()
                    onNavigateToSection(0)
                },
                onResetToDefaults = {
                    viewModel.resetToFactoryDefaults()
                }
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
            .width(104.dp)
            .fillMaxHeight(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 20.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.img_trueaim_app_icon_1795534801161),
                    contentDescription = stringResource(R.string.app_name),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.height(22.dp))

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (isOverlayActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primaryContainer
                        )
                        .clickable { onToggleOverlay() }
                        .tvFocusable(CircleShape, MaterialTheme.colorScheme.primary)
                        .testTag("appbar_quick_toggle_rail"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = stringResource(R.string.cd_toggle_overlay),
                        tint = if (isOverlayActive) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NavSection.entries.forEachIndexed { index, section ->
                    val isSelected = selectedIndex == index
                    val titleStr = stringResource(section.titleRes)

                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectIndex(index) }
                            .tvFocusable(RoundedCornerShape(16.dp), MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("nav_tab_rail_${section.routeKey}"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = section.icon,
                                contentDescription = titleStr,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = titleStr,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.app_version_label, BuildConfig.VERSION_NAME),
                fontFamily = MonoFontFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
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
    val livePulse = if (isOverlayActive) {
        val pulseTransition = rememberInfiniteTransition(label = "live_status_pulse")
        pulseTransition.animateFloat(
            initialValue = 0.72f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(850, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "livePulse"
        ).value
    } else {
        1f
    }

    val powerBg by animateColorAsState(
        targetValue = if (isOverlayActive) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.primaryContainer,
        animationSpec = tween(250),
        label = "powerBg"
    )
    val powerScale by animateFloatAsState(
        targetValue = if (isOverlayActive) 1.04f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "powerScale"
    )

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.img_trueaim_app_icon_1795534801161),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(
                            width = if (isOverlayActive) 1.5.dp else 1.dp,
                            color = if (isOverlayActive) NeonGreen.copy(alpha = 0.85f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontFamily = DisplayFontFamily,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.4).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .graphicsLayer {
                                    scaleX = livePulse
                                    scaleY = livePulse
                                }
                                .clip(CircleShape)
                                .background(
                                    if (isOverlayActive) NeonGreen
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isOverlayActive) stringResource(R.string.active_live)
                            else stringResource(R.string.inactive),
                            fontFamily = MonoFontFamily,
                            color = if (isOverlayActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer {
                        scaleX = powerScale
                        scaleY = powerScale
                    }
                    .clip(CircleShape)
                    .background(powerBg)
                    .border(
                        width = if (isOverlayActive) 1.5.dp else 1.dp,
                        color = if (isOverlayActive) NeonGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        shape = CircleShape
                    )
                    .clickable { onToggleOverlay() }
                    .tvFocusable(CircleShape, MaterialTheme.colorScheme.primary)
                    .testTag("appbar_quick_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = stringResource(R.string.cd_toggle_overlay),
                    tint = if (isOverlayActive) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onPrimaryContainer,
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
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavSection.entries.forEachIndexed { index, section ->
                val isSelected = selectedIndex == index
                val titleStr = stringResource(section.titleRes)
                val pillWidth by animateDpAsState(
                    targetValue = if (isSelected) 54.dp else 36.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "nav_pill_width_$index"
                )
                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.12f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "nav_icon_scale_$index"
                )
                val pillBg by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    animationSpec = tween(200),
                    label = "nav_pill_bg_$index"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectIndex(index) }
                        .tvFocusable(RoundedCornerShape(16.dp), MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .testTag("nav_tab_${section.routeKey}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(pillWidth)
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(pillBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = section.icon,
                            contentDescription = titleStr,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = titleStr,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
