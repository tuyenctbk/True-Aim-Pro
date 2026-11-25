package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CrosshairCollectionCategory
import com.example.model.DesignCategory
import com.example.model.PredefinedCrosshairCatalog
import com.example.model.PredefinedCrosshairDesign
import com.example.ui.CrosshairViewModel
import com.example.ui.PreviewBackground
import com.example.ui.components.CollectionCategoryFilterBar
import com.example.ui.components.CrosshairColorPickerDialog
import com.example.ui.components.CrosshairView
import com.example.ui.components.ExpandableShapeAndSearchPanel
import com.example.ui.components.FavoriteHeartButton
import com.example.ui.components.FilterSummaryStrip
import com.example.ui.components.GameBackgroundCanvas
import com.example.ui.components.formatHexRgb
import com.example.ui.theme.ArenaDarkCanvas
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.tvFocusable

@Composable
fun PredefinedDesignsScreen(
    viewModel: CrosshairViewModel,
    onNavigateBack: () -> Unit,
    onCustomizeInStudio: () -> Unit,
    onToggleOverlay: () -> Unit,
    onEnsureOverlayStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)

    val context = LocalContext.current
    val activeConfig by viewModel.activeConfig.collectAsState()
    val isOverlayActive by viewModel.isOverlayActive.collectAsState()
    val currentBg by viewModel.selectedBackground.collectAsState()
    val recentColors by viewModel.recentCustomColors.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()

    val allDesigns = remember { PredefinedCrosshairCatalog.DESIGNS }
    var selectedCategory by remember { mutableStateOf(CrosshairCollectionCategory.ALL) }
    var selectedShape by remember { mutableStateOf(DesignCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var showSecondaryFilters by remember { mutableStateOf(false) }

    var selectedDesign by remember {
        mutableStateOf(
            allDesigns.firstOrNull {
                it.config.id == activeConfig.id || it.config.style == activeConfig.style
            } ?: allDesigns.first()
        )
    }
    var customColorsByDesignId by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var customSizesByDesignId by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var customOpacitiesByDesignId by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var customRotationsByDesignId by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var showColorPickerDialog by remember { mutableStateOf(false) }

    val effectiveSelectedColorHex = customColorsByDesignId[selectedDesign.config.id]
        ?: if (activeConfig.id == selectedDesign.config.id) activeConfig.primaryColorHex
        else selectedDesign.config.primaryColorHex

    val effectiveSelectedSizeDp = customSizesByDesignId[selectedDesign.config.id]
        ?: if (activeConfig.id == selectedDesign.config.id) activeConfig.sizeDp
        else selectedDesign.config.sizeDp

    val effectiveSelectedOpacity = customOpacitiesByDesignId[selectedDesign.config.id]
        ?: if (activeConfig.id == selectedDesign.config.id) activeConfig.opacity
        else selectedDesign.config.opacity

    val effectiveSelectedRotationDeg = customRotationsByDesignId[selectedDesign.config.id]
        ?: if (activeConfig.id == selectedDesign.config.id) activeConfig.rotationDeg
        else selectedDesign.config.rotationDeg

    val effectiveSelectedDesign = remember(
        selectedDesign,
        effectiveSelectedColorHex,
        effectiveSelectedSizeDp,
        effectiveSelectedOpacity,
        effectiveSelectedRotationDeg
    ) {
        selectedDesign.copy(
            config = selectedDesign.config.copy(
                primaryColorHex = effectiveSelectedColorHex,
                sizeDp = effectiveSelectedSizeDp,
                opacity = effectiveSelectedOpacity,
                rotationDeg = effectiveSelectedRotationDeg
            )
        )
    }

    val shapeAndSearchFiltered = remember(selectedShape, searchQuery, context) {
        val q = searchQuery.trim().lowercase()
        allDesigns.filter { design ->
            val shapeMatches = selectedShape == DesignCategory.ALL || design.category == selectedShape
            val queryMatches = q.isEmpty() ||
                context.getString(design.titleRes).lowercase().contains(q) ||
                context.getString(design.summaryRes).lowercase().contains(q) ||
                context.getString(design.roleTagRes).lowercase().contains(q) ||
                context.getString(design.collectionCategory.labelRes).lowercase().contains(q) ||
                context.getString(design.config.style.nameRes).lowercase().contains(q)
            shapeMatches && queryMatches
        }
    }

    val categoryCounts = remember(shapeAndSearchFiltered, favoriteIds) {
        CrosshairCollectionCategory.PREDEFINED_TABS.associateWith { cat ->
            shapeAndSearchFiltered.count {
                PredefinedCrosshairCatalog.matchesCollectionCategory(it.config, cat, favoriteIds)
            }
        }
    }

    val filteredDesigns = remember(shapeAndSearchFiltered, selectedCategory, favoriteIds) {
        shapeAndSearchFiltered.filter {
            PredefinedCrosshairCatalog.matchesCollectionCategory(it.config, selectedCategory, favoriteIds)
        }
    }

    val toggleFavoriteWithToast: (String) -> Unit = { designId ->
        val added = viewModel.toggleFavorite(designId)
        Toast.makeText(
            context,
            context.getString(
                if (added) R.string.favorite_added_toast else R.string.favorite_removed_toast
            ),
            Toast.LENGTH_SHORT
        ).show()
    }

    val hasActiveSecondaryFilter = selectedShape != DesignCategory.ALL || searchQuery.isNotBlank()
    val hasAnyFilter = selectedCategory != CrosshairCollectionCategory.ALL || hasActiveSecondaryFilter
    val resetAllFilters = {
        selectedCategory = CrosshairCollectionCategory.ALL
        selectedShape = DesignCategory.ALL
        searchQuery = ""
    }

    val isSelectedApplied = activeConfig.id == effectiveSelectedDesign.config.id &&
        activeConfig.style == effectiveSelectedDesign.config.style &&
        activeConfig.primaryColorHex == effectiveSelectedDesign.config.primaryColorHex &&
        activeConfig.sizeDp == effectiveSelectedDesign.config.sizeDp &&
        activeConfig.opacity == effectiveSelectedDesign.config.opacity &&
        activeConfig.rotationDeg == effectiveSelectedDesign.config.rotationDeg

    val selectedTitle = stringResource(effectiveSelectedDesign.titleRes)

    val applySelectedToOverlay = {
        viewModel.applyDesignToOverlay(
            design = effectiveSelectedDesign.config.copy(name = selectedTitle),
            onEnsureOverlayStarted = onEnsureOverlayStarted
        )
        Toast.makeText(
            context,
            context.getString(R.string.designs_applied_toast, selectedTitle),
            Toast.LENGTH_SHORT
        ).show()
    }

    val cycleBackground = {
        val entries = PreviewBackground.entries
        val next = entries[(entries.indexOf(currentBg) + 1) % entries.size]
        viewModel.setBackground(next)
    }

    val resetSelectedDesignToDefaults = {
        val id = selectedDesign.config.id
        customColorsByDesignId = customColorsByDesignId - id
        customSizesByDesignId = customSizesByDesignId - id
        customOpacitiesByDesignId = customOpacitiesByDesignId - id
        customRotationsByDesignId = customRotationsByDesignId - id
        if (activeConfig.id == id) {
            viewModel.resetSelectedCrosshairSettings()
        } else {
            viewModel.triggerDesignPreviewFeedback()
        }
        Toast.makeText(
            context,
            context.getString(R.string.crosshair_settings_reset_toast),
            Toast.LENGTH_SHORT
        ).show()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("predefined_designs_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PredefinedDesignsTopBar(
                totalCount = allDesigns.size,
                isOverlayActive = isOverlayActive,
                onBack = onNavigateBack,
                onToggleOverlay = onToggleOverlay
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth >= 600.dp

            if (isWideScreen) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1.35f)
                            .fillMaxHeight()
                    ) {
                        CollectionCategoryFilterBar(
                            categories = CrosshairCollectionCategory.PREDEFINED_TABS,
                            selectedCategory = selectedCategory,
                            categoryCounts = categoryCounts,
                            onSelectCategory = { selectedCategory = it },
                            testTagPrefix = "designs_cat",
                            showSecondaryToggle = true,
                            isSecondaryExpanded = showSecondaryFilters,
                            hasActiveSecondaryFilter = hasActiveSecondaryFilter,
                            onToggleSecondary = { showSecondaryFilters = !showSecondaryFilters }
                        )
                        ExpandableShapeAndSearchPanel(
                            visible = showSecondaryFilters || hasActiveSecondaryFilter,
                            selectedShape = selectedShape,
                            onSelectShape = { selectedShape = it },
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            testTagPrefix = "designs"
                        )
                        FilterSummaryStrip(
                            resultCount = filteredDesigns.size,
                            hasActiveFilters = hasAnyFilter,
                            onResetFilters = resetAllFilters,
                            resetTestTag = "designs_reset_filters_btn"
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        DesignsSelectionGrid(
                            designs = filteredDesigns,
                            selectedId = selectedDesign.config.id,
                            activeConfigId = activeConfig.id,
                            favoriteIds = favoriteIds,
                            isOverlayActive = isOverlayActive,
                            isFavoritesEmpty = selectedCategory == CrosshairCollectionCategory.FAVORITES && !hasActiveSecondaryFilter,
                            onSelectDesign = { design ->
                                selectedDesign = design
                                viewModel.triggerDesignPreviewFeedback()
                            },
                            onToggleFavorite = { design -> toggleFavoriteWithToast(design.config.id) },
                            onResetFilters = resetAllFilters,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(0.95f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SelectedDesignPreviewStage(
                            design = effectiveSelectedDesign,
                            currentBg = currentBg,
                            isOverlayActive = isOverlayActive,
                            isApplied = isSelectedApplied,
                            isFavorite = effectiveSelectedDesign.config.id in favoriteIds,
                            stageHeightDp = 210,
                            onCycleBg = cycleBackground,
                            onOpenColorPicker = { showColorPickerDialog = true },
                            onToggleFavorite = { toggleFavoriteWithToast(effectiveSelectedDesign.config.id) },
                            onSizeChange = { newSize ->
                                customSizesByDesignId = customSizesByDesignId + (selectedDesign.config.id to newSize)
                                if (activeConfig.id == selectedDesign.config.id) {
                                    viewModel.setReticleSizeDp(newSize)
                                }
                            },
                            onOpacityChange = { newOpacity ->
                                val clamped = newOpacity.coerceIn(0.10f, 1.00f)
                                customOpacitiesByDesignId = customOpacitiesByDesignId + (selectedDesign.config.id to clamped)
                                if (activeConfig.id == selectedDesign.config.id) {
                                    viewModel.setReticleOpacity(clamped)
                                }
                            },
                            onRotationChange = { newRotation ->
                                val clamped = ((newRotation % 360f) + 360f) % 360f
                                customRotationsByDesignId = customRotationsByDesignId + (selectedDesign.config.id to clamped)
                                if (activeConfig.id == selectedDesign.config.id) {
                                    viewModel.setReticleRotationDeg(clamped)
                                }
                            },
                            onResetDefaults = resetSelectedDesignToDefaults
                        )

                        ApplyDesignActionPanel(
                            isOverlayActive = isOverlayActive,
                            isApplied = isSelectedApplied,
                            selectedColorHex = effectiveSelectedDesign.config.primaryColorHex,
                            onOpenColorPicker = { showColorPickerDialog = true },
                            onApplyToOverlay = applySelectedToOverlay,
                            onCustomizeInStudio = {
                                viewModel.applyPreset(effectiveSelectedDesign.config.copy(name = selectedTitle))
                                onCustomizeInStudio()
                            }
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        SelectedDesignPreviewStage(
                            design = effectiveSelectedDesign,
                            currentBg = currentBg,
                            isOverlayActive = isOverlayActive,
                            isApplied = isSelectedApplied,
                            isFavorite = effectiveSelectedDesign.config.id in favoriteIds,
                            stageHeightDp = 136,
                            onCycleBg = cycleBackground,
                            onOpenColorPicker = { showColorPickerDialog = true },
                            onToggleFavorite = { toggleFavoriteWithToast(effectiveSelectedDesign.config.id) },
                            onSizeChange = { newSize ->
                                customSizesByDesignId = customSizesByDesignId + (selectedDesign.config.id to newSize)
                                if (activeConfig.id == selectedDesign.config.id) {
                                    viewModel.setReticleSizeDp(newSize)
                                }
                            },
                            onOpacityChange = { newOpacity ->
                                val clamped = newOpacity.coerceIn(0.10f, 1.00f)
                                customOpacitiesByDesignId = customOpacitiesByDesignId + (selectedDesign.config.id to clamped)
                                if (activeConfig.id == selectedDesign.config.id) {
                                    viewModel.setReticleOpacity(clamped)
                                }
                            },
                            onRotationChange = { newRotation ->
                                val clamped = ((newRotation % 360f) + 360f) % 360f
                                customRotationsByDesignId = customRotationsByDesignId + (selectedDesign.config.id to clamped)
                                if (activeConfig.id == selectedDesign.config.id) {
                                    viewModel.setReticleRotationDeg(clamped)
                                }
                            },
                            onResetDefaults = resetSelectedDesignToDefaults
                        )
                    }

                    CollectionCategoryFilterBar(
                        categories = CrosshairCollectionCategory.PREDEFINED_TABS,
                        selectedCategory = selectedCategory,
                        categoryCounts = categoryCounts,
                        onSelectCategory = { selectedCategory = it },
                        testTagPrefix = "designs_cat",
                        showSecondaryToggle = true,
                        isSecondaryExpanded = showSecondaryFilters,
                        hasActiveSecondaryFilter = hasActiveSecondaryFilter,
                        onToggleSecondary = { showSecondaryFilters = !showSecondaryFilters },
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    ExpandableShapeAndSearchPanel(
                        visible = showSecondaryFilters || hasActiveSecondaryFilter,
                        selectedShape = selectedShape,
                        onSelectShape = { selectedShape = it },
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        testTagPrefix = "designs"
                    )

                    FilterSummaryStrip(
                        resultCount = filteredDesigns.size,
                        hasActiveFilters = hasAnyFilter,
                        onResetFilters = resetAllFilters,
                        resetTestTag = "designs_reset_filters_btn"
                    )

                    DesignsSelectionGrid(
                        designs = filteredDesigns,
                        selectedId = selectedDesign.config.id,
                        activeConfigId = activeConfig.id,
                        favoriteIds = favoriteIds,
                        isOverlayActive = isOverlayActive,
                        isFavoritesEmpty = selectedCategory == CrosshairCollectionCategory.FAVORITES && !hasActiveSecondaryFilter,
                        onSelectDesign = { design ->
                            selectedDesign = design
                            viewModel.triggerDesignPreviewFeedback()
                        },
                        onToggleFavorite = { design -> toggleFavoriteWithToast(design.config.id) },
                        onResetFilters = resetAllFilters,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    )

                    CompactApplyBottomBar(
                        design = effectiveSelectedDesign,
                        isOverlayActive = isOverlayActive,
                        isApplied = isSelectedApplied,
                        onOpenColorPicker = { showColorPickerDialog = true },
                        onApplyToOverlay = applySelectedToOverlay,
                        onCustomizeInStudio = {
                            viewModel.applyPreset(effectiveSelectedDesign.config.copy(name = selectedTitle))
                            onCustomizeInStudio()
                        }
                    )
                }
            }
        }
    }

    if (showColorPickerDialog) {
        CrosshairColorPickerDialog(
            config = effectiveSelectedDesign.config,
            recentColors = recentColors,
            onColorChanged = { newHex ->
                customColorsByDesignId = customColorsByDesignId + (selectedDesign.config.id to newHex)
                if (activeConfig.id == selectedDesign.config.id) {
                    viewModel.setPrimaryColorHex(newHex)
                }
            },
            onColorCommitted = { newHex ->
                viewModel.recordRecentColor(newHex)
            },
            onDismiss = { showColorPickerDialog = false }
        )
    }
}

@Composable
private fun PredefinedDesignsTopBar(
    totalCount: Int,
    isOverlayActive: Boolean,
    onBack: () -> Unit,
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
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .tvFocusable(CircleShape, MaterialTheme.colorScheme.primary)
                        .testTag("designs_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_designs_back),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = stringResource(R.string.designs_screen_title),
                        fontFamily = DisplayFontFamily,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.designs_screen_subtitle, totalCount),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isOverlayActive) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(
                    1.dp,
                    if (isOverlayActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onToggleOverlay() }
                    .tvFocusable(RoundedCornerShape(20.dp), MaterialTheme.colorScheme.primary)
                    .testTag("designs_overlay_status_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isOverlayActive) NeonGreen
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isOverlayActive) stringResource(R.string.hud_on) else stringResource(R.string.hud_off),
                        fontFamily = MonoFontFamily,
                        color = if (isOverlayActive) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedDesignPreviewStage(
    design: PredefinedCrosshairDesign,
    currentBg: PreviewBackground,
    isOverlayActive: Boolean,
    isApplied: Boolean,
    isFavorite: Boolean,
    stageHeightDp: Int,
    onCycleBg: () -> Unit,
    onOpenColorPicker: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSizeChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onRotationChange: (Float) -> Unit,
    onResetDefaults: () -> Unit
) {
    val cfg = design.config
    val animatedSize by animateFloatAsState(
        targetValue = cfg.sizeDp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "selected_design_size"
    )
    val animatedGap by animateFloatAsState(
        targetValue = cfg.gapDp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "selected_design_gap"
    )

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("designs_preview_card")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(stageHeightDp.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ArenaDarkCanvas)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                GameBackgroundCanvas(background = currentBg)

                CrosshairView(
                    config = cfg.copy(sizeDp = animatedSize, gapDp = animatedGap),
                    modifier = Modifier.align(Alignment.Center)
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.68f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = stringResource(design.roleTagRes).uppercase(),
                        fontFamily = MonoFontFamily,
                        color = NeonGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isApplied && isOverlayActive) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.designs_live_badge),
                                    fontFamily = MonoFontFamily,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.68f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        FavoriteHeartButton(
                            isFavorite = isFavorite,
                            onToggle = onToggleFavorite,
                            iconSize = 18.dp,
                            touchSize = 34.dp,
                            unselectedTint = Color.White,
                            testTagValue = "designs_preview_favorite_btn"
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.68f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenColorPicker() }
                        .tvFocusable(RoundedCornerShape(10.dp), MaterialTheme.colorScheme.primary)
                        .testTag("designs_preview_color_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(Color(cfg.primaryColorHex))
                                .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatHexRgb(cfg.primaryColorHex),
                            fontFamily = MonoFontFamily,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.68f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onCycleBg() }
                        .tvFocusable(RoundedCornerShape(10.dp), MaterialTheme.colorScheme.primary)
                        .testTag("designs_cycle_bg_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wallpaper,
                            contentDescription = stringResource(R.string.cd_switch_preview_bg),
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = stringResource(currentBg.nameRes),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(design.titleRes),
                        fontFamily = DisplayFontFamily,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(design.summaryRes),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onResetDefaults() }
                        .tvFocusable(RoundedCornerShape(10.dp), MaterialTheme.colorScheme.primary)
                        .testTag("designs_reset_settings_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.cd_reset_crosshair_settings),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.reset),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Text(
                        text = stringResource(R.string.designs_spec_chip, cfg.sizeDp.toInt(), cfg.gapDp.toInt()),
                        fontFamily = MonoFontFamily,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomOutMap,
                    contentDescription = stringResource(R.string.overlay_scale_label),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Slider(
                    value = cfg.sizeDp.coerceIn(14f, 90f),
                    onValueChange = onSizeChange,
                    valueRange = 14f..90f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("designs_scale_slider")
                )
                Text(
                    text = stringResource(
                        R.string.overlay_scale_badge_format,
                        (cfg.sizeDp / 36f).coerceIn(0.3f, 2.6f),
                        cfg.sizeDp.toInt()
                    ),
                    fontFamily = MonoFontFamily,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Opacity,
                    contentDescription = stringResource(R.string.overlay_opacity_label),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Slider(
                    value = cfg.opacity.coerceIn(0.10f, 1.00f),
                    onValueChange = onOpacityChange,
                    valueRange = 0.10f..1.00f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("designs_opacity_slider")
                )
                Text(
                    text = stringResource(
                        R.string.percent_format,
                        (cfg.opacity.coerceIn(0.10f, 1.00f) * 100).toInt()
                    ),
                    fontFamily = MonoFontFamily,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RotateRight,
                    contentDescription = stringResource(R.string.overlay_rotation_label),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Slider(
                    value = cfg.rotationDeg.coerceIn(0f, 360f),
                    onValueChange = onRotationChange,
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("designs_rotation_slider")
                )
                Text(
                    text = stringResource(
                        R.string.unit_deg_int,
                        cfg.rotationDeg.toInt()
                    ),
                    fontFamily = MonoFontFamily,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DesignsSelectionGrid(
    designs: List<PredefinedCrosshairDesign>,
    selectedId: String,
    activeConfigId: String,
    favoriteIds: Set<String>,
    isOverlayActive: Boolean,
    isFavoritesEmpty: Boolean,
    onSelectDesign: (PredefinedCrosshairDesign) -> Unit,
    onToggleFavorite: (PredefinedCrosshairDesign) -> Unit,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (designs.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(
                        if (isFavoritesEmpty) R.string.favorites_empty_title
                        else R.string.designs_empty_title
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        if (isFavoritesEmpty) R.string.favorites_empty_hint
                        else R.string.designs_empty_hint
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = onResetFilters,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("designs_empty_reset_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.filter_reset_btn),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 152.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.testTag("designs_grid")
    ) {
        items(designs, key = { it.config.id }) { design ->
            val isSelected = design.config.id == selectedId
            val isOnOverlay = isOverlayActive && design.config.id == activeConfigId
            val isFavorite = design.config.id in favoriteIds

            PredefinedDesignCard(
                design = design,
                isSelected = isSelected,
                isOnOverlay = isOnOverlay,
                isFavorite = isFavorite,
                onClick = { onSelectDesign(design) },
                onToggleFavorite = { onToggleFavorite(design) }
            )
        }
    }
}

@Composable
private fun PredefinedDesignCard(
    design: PredefinedCrosshairDesign,
    isSelected: Boolean,
    isOnOverlay: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.015f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "card_scale_${design.config.id}"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outline,
        animationSpec = tween(180),
        label = "card_border_${design.config.id}"
    )
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .tvFocusable(RoundedCornerShape(20.dp), MaterialTheme.colorScheme.primary)
            .testTag("design_card_${design.config.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Text(
                        text = stringResource(design.roleTagRes).uppercase(),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.primary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    FavoriteHeartButton(
                        isFavorite = isFavorite,
                        onToggle = onToggleFavorite,
                        iconSize = 16.dp,
                        touchSize = 30.dp,
                        testTagValue = "design_favorite_btn_${design.config.id}"
                    )

                    when {
                        isOnOverlay -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = stringResource(R.string.designs_live_badge),
                                tint = NeonGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        isSelected -> {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(R.string.cd_selected_design),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ArenaDarkCanvas)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                TacticalGridOverlay()
                CrosshairView(
                    config = design.config,
                    overrideSize = (design.config.sizeDp * 1.35f).coerceIn(28f, 68f).dp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(design.titleRes),
                fontFamily = DisplayFontFamily,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(
                    R.string.event_meta_format,
                    stringResource(design.collectionCategory.labelRes),
                    stringResource(design.config.style.nameRes)
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.5.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TacticalGridOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val step = size.minDimension / 4f
        val lineColor = Color.White.copy(alpha = 0.06f)
        for (i in 1..3) {
            drawLine(lineColor, Offset(step * i, 0f), Offset(step * i, size.height), strokeWidth = 1f)
            drawLine(lineColor, Offset(0f, step * i), Offset(size.width, step * i), strokeWidth = 1f)
        }
    }
}

@Composable
private fun CompactApplyBottomBar(
    design: PredefinedCrosshairDesign,
    isOverlayActive: Boolean,
    isApplied: Boolean,
    onOpenColorPicker: () -> Unit,
    onApplyToOverlay: () -> Unit,
    onCustomizeInStudio: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = onOpenColorPicker,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    .tvFocusable(RoundedCornerShape(14.dp), MaterialTheme.colorScheme.primary)
                    .testTag("designs_color_picker_btn")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(design.config.primaryColorHex))
                            .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                    )
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = stringResource(R.string.cd_pick_color),
                        tint = Color.Transparent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            IconButton(
                onClick = onCustomizeInStudio,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    .tvFocusable(RoundedCornerShape(14.dp), MaterialTheme.colorScheme.primary)
                    .testTag("designs_customize_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = stringResource(R.string.cd_edit_studio),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Button(
                onClick = onApplyToOverlay,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .tvFocusable(RoundedCornerShape(14.dp), MaterialTheme.colorScheme.primary)
                    .testTag("designs_apply_overlay_btn")
            ) {
                Icon(
                    imageVector = if (isApplied && isOverlayActive) Icons.Default.CheckCircle else Icons.Default.Layers,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        isApplied && isOverlayActive -> stringResource(R.string.designs_active_on_overlay_btn)
                        isOverlayActive -> stringResource(R.string.designs_update_overlay_btn)
                        else -> stringResource(R.string.designs_apply_overlay_btn)
                    },
                    fontFamily = DisplayFontFamily,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ApplyDesignActionPanel(
    isOverlayActive: Boolean,
    isApplied: Boolean,
    selectedColorHex: Long,
    onOpenColorPicker: () -> Unit,
    onApplyToOverlay: () -> Unit,
    onCustomizeInStudio: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onApplyToOverlay,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .tvFocusable(RoundedCornerShape(14.dp), MaterialTheme.colorScheme.primary)
                .testTag("designs_apply_overlay_btn")
        ) {
            Icon(
                imageVector = if (isApplied && isOverlayActive) Icons.Default.CheckCircle else Icons.Default.Layers,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when {
                    isApplied && isOverlayActive -> stringResource(R.string.designs_active_on_overlay_btn)
                    isOverlayActive -> stringResource(R.string.designs_update_overlay_btn)
                    else -> stringResource(R.string.designs_apply_overlay_btn)
                },
                fontFamily = DisplayFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onOpenColorPicker,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .tvFocusable(RoundedCornerShape(14.dp), MaterialTheme.colorScheme.primary)
                    .testTag("designs_color_picker_btn")
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(selectedColorHex))
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formatHexRgb(selectedColorHex),
                    fontFamily = MonoFontFamily,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            OutlinedButton(
                onClick = onCustomizeInStudio,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .tvFocusable(RoundedCornerShape(14.dp), MaterialTheme.colorScheme.primary)
                    .testTag("designs_customize_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.cd_edit_studio),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
