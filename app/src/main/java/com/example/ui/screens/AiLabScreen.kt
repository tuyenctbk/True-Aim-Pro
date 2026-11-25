package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.UsageStats
import com.example.data.ai.AiArchitectResult
import com.example.data.ai.AiCoachResult
import com.example.data.ai.AiExecutionTier
import com.example.data.ai.AiTaskState
import com.example.data.ai.AiVisionResult
import com.example.data.ai.QuotaSnapshot
import com.example.data.analytics.AnalyticsSummary
import com.example.model.CrosshairConfig
import com.example.ui.AiLabViewModel
import com.example.ui.AimDifficulty
import com.example.ui.AimGameStats
import com.example.ui.CrosshairViewModel
import com.example.ui.components.AiTierBadge
import com.example.ui.components.CrosshairView
import com.example.ui.components.QuotaStatusHeaderCard
import com.example.ui.components.SmartQuotaRecoveryCard
import com.example.ui.components.SoftNudgeBanner
import com.example.ui.components.ValueReceiptAndUpgradeDialog
import com.example.ui.theme.ArenaDarkCanvas
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.tvFocusable

@Composable
fun AiLabScreen(
    crosshairViewModel: CrosshairViewModel,
    aiViewModel: AiLabViewModel,
    onNavigateToStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val quota by aiViewModel.quota.collectAsState()
    val selectedTool by aiViewModel.selectedToolIndex.collectAsState()
    val architectPrompt by aiViewModel.architectPrompt.collectAsState()
    val architectState by aiViewModel.architectState.collectAsState()
    val pickedScreenshot by aiViewModel.pickedScreenshot.collectAsState()
    val visionState by aiViewModel.visionState.collectAsState()
    val coachState by aiViewModel.coachState.collectAsState()
    val showValueReceipt by aiViewModel.showValueReceiptSheet.collectAsState()
    val isAuthBusy by aiViewModel.isAuthBusy.collectAsState()
    val statusBanner by aiViewModel.statusBannerMessage.collectAsState()

    val activeConfig by crosshairViewModel.activeConfig.collectAsState()
    val gameStats by crosshairViewModel.gameState.collectAsState()
    val difficulty by crosshairViewModel.selectedDifficulty.collectAsState()
    val usageStats by crosshairViewModel.usageStats.collectAsState()
    val analyticsSummary by crosshairViewModel.analyticsSummary.collectAsState()

    LaunchedEffect(statusBanner) {
        statusBanner?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            aiViewModel.clearStatusBanner()
        }
    }

    // Zero-permission Android Photo Picker (Google Play compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val decoded = decodeUriToBitmap(context, uri)
            if (decoded != null) {
                aiViewModel.onScreenshotPicked(decoded)
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.ai_vision_decode_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val applyAiConfig: (CrosshairConfig) -> Unit = { config ->
        crosshairViewModel.applyAndSaveAiConfig(config)
        Toast.makeText(
            context,
            context.getString(R.string.ai_reticle_applied_toast, config.name),
            Toast.LENGTH_SHORT
        ).show()
        onNavigateToStudio()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .padding(bottom = 28.dp)
            .testTag("ai_lab_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hybrid Quota Status Bar & Eco Lite Mode Control
        QuotaStatusHeaderCard(
            quota = quota,
            onToggleEcoMode = { aiViewModel.toggleEcoLiteMode(it) },
            onOpenValueReceipt = { aiViewModel.openValueReceiptSheet() }
        )

        // Stage 1: Soft Nudge Banner at >= 80% Usage
        if (quota.isSoftNudgeActive) {
            SoftNudgeBanner(
                quota = quota,
                onSwitchToEcoLite = { aiViewModel.toggleEcoLiteMode(true) }
            )
        }

        // 3-Tool Segmented Selector
        AiToolTabStrip(
            selectedIndex = selectedTool,
            onSelect = { aiViewModel.selectTool(it) }
        )

        when (selectedTool) {
            0 -> ArchitectToolSection(
                prompt = architectPrompt,
                onPromptChange = { aiViewModel.setArchitectPrompt(it) },
                state = architectState,
                quota = quota,
                onGenerateCloud = { aiViewModel.runArchitect() },
                onGenerateEco = { aiViewModel.runArchitect(forceEcoLite = true) },
                onGenerateLocal = { aiViewModel.runArchitectLocally() },
                onClaimGrace = { aiViewModel.claimDailyGraceCredits() },
                onOpenUnlockSheet = { aiViewModel.openValueReceiptSheet() },
                onEquipConfig = applyAiConfig
            )

            1 -> VisionContrastToolSection(
                screenshot = pickedScreenshot,
                state = visionState,
                quota = quota,
                onPickScreenshot = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onReanalyzeEco = { aiViewModel.runVisionAnalysis(forceEcoLite = true) },
                onAnalyzeLocal = { aiViewModel.runVisionAnalysisLocally() },
                onClaimGrace = { aiViewModel.claimDailyGraceCredits() },
                onOpenUnlockSheet = { aiViewModel.openValueReceiptSheet() },
                onEquipConfig = applyAiConfig
            )

            2 -> AimCoachToolSection(
                lastDrill = gameStats,
                difficulty = difficulty,
                usageStats = usageStats,
                summary = analyticsSummary,
                activeConfig = activeConfig,
                state = coachState,
                quota = quota,
                onAnalyzeCloud = {
                    aiViewModel.runAimCoach(gameStats, difficulty, usageStats, analyticsSummary, activeConfig)
                },
                onAnalyzeEco = {
                    aiViewModel.runAimCoach(
                        gameStats, difficulty, usageStats, analyticsSummary, activeConfig, forceEcoLite = true
                    )
                },
                onAnalyzeLocal = {
                    aiViewModel.runAimCoachLocally(gameStats, difficulty, usageStats, analyticsSummary, activeConfig)
                },
                onClaimGrace = { aiViewModel.claimDailyGraceCredits() },
                onOpenUnlockSheet = { aiViewModel.openValueReceiptSheet() },
                onEquipConfig = applyAiConfig
            )
        }
    }

    if (showValueReceipt) {
        ValueReceiptAndUpgradeDialog(
            quota = quota,
            isAuthBusy = isAuthBusy,
            initialByokKey = aiViewModel.getByokApiKey(),
            onSignInWithGoogle = { aiViewModel.signInWithGoogle(context) },
            onSignOutGoogle = { aiViewModel.signOutGoogle(context) },
            onClaimGraceCredits = { aiViewModel.claimDailyGraceCredits() },
            onSaveByokKey = { aiViewModel.saveByokApiKey(it) },
            onDismiss = { aiViewModel.closeValueReceiptSheet() }
        )
    }
}

@Composable
private fun AiToolTabStrip(
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val tabs = listOf(
        Triple(0, R.string.ai_tab_architect, Icons.Default.AutoAwesome),
        Triple(1, R.string.ai_tab_contrast, Icons.Default.ImageSearch),
        Triple(2, R.string.ai_tab_coach, Icons.Default.Psychology)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEach { (idx, titleRes, icon) ->
            val selected = selectedIndex == idx
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .clickable { onSelect(idx) }
                    .tvFocusable(RoundedCornerShape(12.dp), MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 6.dp)
                    .testTag("ai_tool_tab_$idx"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = stringResource(titleRes),
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ArchitectToolSection(
    prompt: String,
    onPromptChange: (String) -> Unit,
    state: AiTaskState<AiArchitectResult>,
    quota: QuotaSnapshot,
    onGenerateCloud: () -> Unit,
    onGenerateEco: () -> Unit,
    onGenerateLocal: () -> Unit,
    onClaimGrace: () -> Unit,
    onOpenUnlockSheet: () -> Unit,
    onEquipConfig: (CrosshairConfig) -> Unit
) {
    val quickPrompts = listOf(
        stringResource(R.string.ai_prompt_chip_valorant),
        stringResource(R.string.ai_prompt_chip_pubg),
        stringResource(R.string.ai_prompt_chip_codm),
        stringResource(R.string.ai_prompt_chip_apex)
    )

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitleRow(
                icon = Icons.Default.AutoAwesome,
                title = stringResource(R.string.ai_architect_title),
                subtitle = stringResource(R.string.ai_architect_subtitle)
            )

            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                placeholder = { Text(stringResource(R.string.ai_architect_hint), fontSize = 12.sp) },
                minLines = 2,
                maxLines = 3,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_architect_prompt_input")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickPrompts.forEach { chip ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .clickable { onPromptChange(chip) }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = chip,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onGenerateCloud,
                    enabled = state !is AiTaskState.Loading,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp)
                        .testTag("ai_architect_generate_btn")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.ai_architect_generate_btn),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                }

                OutlinedButton(
                    onClick = onGenerateLocal,
                    enabled = state !is AiTaskState.Loading,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier
                        .weight(0.9f)
                        .height(48.dp)
                        .testTag("ai_architect_local_btn")
                ) {
                    Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.ai_btn_local_zero),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    when (state) {
        is AiTaskState.Idle -> Unit
        is AiTaskState.Loading -> AiLoadingCard(state.plannedTier)
        is AiTaskState.Error -> AiErrorCard(state.message)
        is AiTaskState.RecoveryNeeded -> SmartQuotaRecoveryCard(
            feature = state.feature,
            reason = state.reason,
            message = state.message,
            quota = quota,
            canRetryEcoLite = state.canRetryEcoLite,
            onClaimGraceCredits = onClaimGrace,
            onRetryEcoLite = onGenerateEco,
            onRunLocalEngine = onGenerateLocal,
            onOpenUnlockSheet = onOpenUnlockSheet
        )
        is AiTaskState.Success -> {
            val res = state.data
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_architect_result_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AiTierBadge(
                        tier = res.tierUsed,
                        fromCache = res.fromCache,
                        autoCascaded = res.autoCascaded
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(ArenaDarkCanvas)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            CrosshairView(config = res.config, overrideSize = 56.dp)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = res.config.name,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(
                                    R.string.ai_architect_spec_summary,
                                    stringResource(res.config.style.nameRes),
                                    res.config.sizeDp.toInt(),
                                    res.config.gapDp.toInt()
                                ),
                                fontFamily = MonoFontFamily,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = res.rationale,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.ai_pro_tip_prefix, res.proTip),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )
                    }

                    Button(
                        onClick = { onEquipConfig(res.config) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("ai_architect_equip_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.ai_equip_save_reticle_btn),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VisionContrastToolSection(
    screenshot: Bitmap?,
    state: AiTaskState<AiVisionResult>,
    quota: QuotaSnapshot,
    onPickScreenshot: () -> Unit,
    onReanalyzeEco: () -> Unit,
    onAnalyzeLocal: () -> Unit,
    onClaimGrace: () -> Unit,
    onOpenUnlockSheet: () -> Unit,
    onEquipConfig: (CrosshairConfig) -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitleRow(
                icon = Icons.Default.ImageSearch,
                title = stringResource(R.string.ai_vision_title),
                subtitle = stringResource(R.string.ai_vision_subtitle)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPickScreenshot,
                    enabled = state !is AiTaskState.Loading,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.35f)
                        .height(48.dp)
                        .testTag("ai_vision_pick_btn")
                ) {
                    Icon(Icons.Default.ImageSearch, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.ai_vision_pick_screenshot_btn),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                if (screenshot != null) {
                    OutlinedButton(
                        onClick = onAnalyzeLocal,
                        enabled = state !is AiTaskState.Loading,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier
                            .weight(0.95f)
                            .height(48.dp)
                            .testTag("ai_vision_local_btn")
                    ) {
                        Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.ai_btn_local_zero),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Live Screenshot Preview with Generated Reticle Overlaid right in center!
            if (screenshot != null) {
                val previewConfig = (state as? AiTaskState.Success)?.data?.config
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ArenaDarkCanvas)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = screenshot.asImageBitmap(),
                        contentDescription = stringResource(R.string.ai_vision_screenshot_cd),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (previewConfig != null) {
                        CrosshairView(config = previewConfig, overrideSize = 56.dp)
                    }
                }
            }
        }
    }

    when (state) {
        is AiTaskState.Idle -> Unit
        is AiTaskState.Loading -> AiLoadingCard(state.plannedTier)
        is AiTaskState.Error -> AiErrorCard(state.message)
        is AiTaskState.RecoveryNeeded -> SmartQuotaRecoveryCard(
            feature = state.feature,
            reason = state.reason,
            message = state.message,
            quota = quota,
            canRetryEcoLite = state.canRetryEcoLite,
            onClaimGraceCredits = onClaimGrace,
            onRetryEcoLite = onReanalyzeEco,
            onRunLocalEngine = onAnalyzeLocal,
            onOpenUnlockSheet = onOpenUnlockSheet
        )
        is AiTaskState.Success -> {
            val res = state.data
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_vision_result_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AiTierBadge(
                        tier = res.tierUsed,
                        fromCache = res.fromCache,
                        autoCascaded = res.autoCascaded
                    )

                    Text(
                        text = res.mapLightingSummary,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Palette swatches + Recommended Reticle Color
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.ai_vision_palette_label),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        res.dominantPaletteHex.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.symbol_arrow_right),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(res.recommendedColorHex))
                                .border(2.dp, Color.Black, CircleShape)
                        )
                        Text(
                            text = String.format(java.util.Locale.US, "#%06X", res.recommendedColorHex and 0xFFFFFF),
                            fontFamily = MonoFontFamily,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = res.contrastReasoning,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Button(
                        onClick = { onEquipConfig(res.config) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("ai_vision_apply_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.ai_vision_apply_btn),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AimCoachToolSection(
    lastDrill: AimGameStats,
    difficulty: AimDifficulty,
    usageStats: UsageStats,
    summary: AnalyticsSummary,
    activeConfig: CrosshairConfig,
    state: AiTaskState<AiCoachResult>,
    quota: QuotaSnapshot,
    onAnalyzeCloud: () -> Unit,
    onAnalyzeEco: () -> Unit,
    onAnalyzeLocal: () -> Unit,
    onClaimGrace: () -> Unit,
    onOpenUnlockSheet: () -> Unit,
    onEquipConfig: (CrosshairConfig) -> Unit
) {
    val accuracy = if (lastDrill.totalTargets > 0) {
        ((lastDrill.targetsHit.toFloat() / lastDrill.totalTargets.toFloat()) * 100f).toInt()
    } else 0

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitleRow(
                icon = Icons.Default.Psychology,
                title = stringResource(R.string.ai_coach_title),
                subtitle = stringResource(R.string.ai_coach_subtitle)
            )

            // Real Telemetry Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryPill(
                    label = stringResource(R.string.stat_accuracy),
                    value = if (lastDrill.totalTargets > 0) {
                        stringResource(R.string.percent_format, accuracy)
                    } else {
                        stringResource(R.string.stat_empty_placeholder)
                    },
                    modifier = Modifier.weight(1f)
                )
                TelemetryPill(
                    label = stringResource(R.string.stat_reaction),
                    value = if (lastDrill.avgReactionTimeMs > 0) {
                        stringResource(R.string.unit_ms_format, lastDrill.avgReactionTimeMs)
                    } else {
                        stringResource(R.string.stat_empty_placeholder)
                    },
                    modifier = Modifier.weight(1f)
                )
                TelemetryPill(
                    label = stringResource(R.string.metric_best_score),
                    value = "${maxOf(summary.bestAimScore, lastDrill.score)}",
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = stringResource(
                    R.string.ai_coach_context_line,
                    stringResource(activeConfig.style.nameRes),
                    usageStats.totalSessionsCount,
                    stringResource(difficulty.titleRes)
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAnalyzeCloud,
                    enabled = state !is AiTaskState.Loading,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp)
                        .testTag("ai_coach_analyze_btn")
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.ai_coach_analyze_btn),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                }

                OutlinedButton(
                    onClick = onAnalyzeLocal,
                    enabled = state !is AiTaskState.Loading,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier
                        .weight(0.9f)
                        .height(48.dp)
                        .testTag("ai_coach_local_btn")
                ) {
                    Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.ai_btn_local_zero),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    when (state) {
        is AiTaskState.Idle -> Unit
        is AiTaskState.Loading -> AiLoadingCard(state.plannedTier)
        is AiTaskState.Error -> AiErrorCard(state.message)
        is AiTaskState.RecoveryNeeded -> SmartQuotaRecoveryCard(
            feature = state.feature,
            reason = state.reason,
            message = state.message,
            quota = quota,
            canRetryEcoLite = state.canRetryEcoLite,
            onClaimGraceCredits = onClaimGrace,
            onRetryEcoLite = onAnalyzeEco,
            onRunLocalEngine = onAnalyzeLocal,
            onOpenUnlockSheet = onOpenUnlockSheet
        )
        is AiTaskState.Success -> {
            val res = state.data
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_coach_result_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AiTierBadge(
                        tier = res.tierUsed,
                        fromCache = res.fromCache,
                        autoCascaded = res.autoCascaded
                    )

                    Text(
                        text = res.headline,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = res.diagnosis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.ai_coach_drill_focus_label),
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = MonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = res.drillFocus,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(ArenaDarkCanvas)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            CrosshairView(config = res.suggestedConfig, overrideSize = 48.dp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.ai_coach_reticle_tweak_label),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = res.tweakSummary,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Button(
                        onClick = { onEquipConfig(res.suggestedConfig) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("ai_coach_apply_tweak_btn")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.ai_coach_apply_tweak_btn),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitleRow(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.5.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun TelemetryPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 8.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontFamily = MonoFontFamily,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AiLoadingCard(plannedTier: AiExecutionTier) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = stringResource(R.string.ai_loading_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (plannedTier) {
                        AiExecutionTier.PRIMARY_CLOUD -> stringResource(R.string.ai_loading_primary)
                        AiExecutionTier.ECONOMY_LITE -> stringResource(R.string.ai_loading_eco)
                        AiExecutionTier.BYOK_DIRECT -> stringResource(R.string.ai_loading_byok)
                        AiExecutionTier.LOCAL_ENGINE -> stringResource(R.string.ai_loading_local)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp
                )
            }
        }
    }
}

@Composable
private fun AiErrorCard(message: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontSize = 12.sp,
            modifier = Modifier.padding(14.dp)
        )
    }
}

private fun decodeUriToBitmap(context: android.content.Context, uri: Uri): Bitmap? {
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }
    }.getOrNull()
}
