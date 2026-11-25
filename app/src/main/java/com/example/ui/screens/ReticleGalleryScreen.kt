package com.example.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CrosshairCollectionCategory
import com.example.model.CrosshairConfig
import com.example.model.CrosshairStyle
import com.example.model.DesignCategory
import com.example.model.PredefinedCrosshairCatalog
import com.example.ui.CrosshairViewModel
import com.example.ui.components.CollectionCategoryFilterBar
import com.example.ui.components.CrosshairView
import com.example.ui.components.ExpandableShapeAndSearchPanel
import com.example.ui.components.FavoriteHeartButton
import com.example.ui.components.FilterSummaryStrip
import com.example.ui.theme.ArenaDarkCanvas
import com.example.ui.theme.LaserCrimson
import com.example.ui.theme.tvFocusable

private fun matchesShapeFilter(preset: CrosshairConfig, shape: DesignCategory): Boolean = when (shape) {
    DesignCategory.ALL -> true
    DesignCategory.CLASSIC_FPS -> preset.style in listOf(
        CrosshairStyle.CLASSIC_CROSS,
        CrosshairStyle.T_SHAPE,
        CrosshairStyle.CROSS_CIRCLE
    ) || preset.category.equals(CrosshairConfig.CATEGORY_CLASSIC, ignoreCase = true)

    DesignCategory.PRECISION_DOTS -> preset.style in listOf(
        CrosshairStyle.PRECISION_DOT,
        CrosshairStyle.MICRO_PLUS,
        CrosshairStyle.HOLLOW_DIAMOND
    ) || preset.category.equals(CrosshairConfig.CATEGORY_DOT, ignoreCase = true)

    DesignCategory.TACTICAL_CQB -> preset.style in listOf(
        CrosshairStyle.TACTICAL_CIRCLE,
        CrosshairStyle.CHEVRON_ARROW,
        CrosshairStyle.SHOTGUN_BOX,
        CrosshairStyle.CORNER_BRACKETS,
        CrosshairStyle.DYNAMIC_RING
    ) || preset.category.equals(CrosshairConfig.CATEGORY_TACTICAL, ignoreCase = true)

    DesignCategory.SNIPER_SCOPES -> preset.style == CrosshairStyle.MIL_DOT_SNIPER ||
        preset.category.equals(CrosshairConfig.CATEGORY_SNIPER, ignoreCase = true)

    DesignCategory.SCIFI_HUD -> preset.style in listOf(
        CrosshairStyle.CYBER_HEX,
        CrosshairStyle.TRI_BLADE,
        CrosshairStyle.SONAR_RADAR,
        CrosshairStyle.HEARTBEAT
    ) || preset.category.equals(CrosshairConfig.CATEGORY_SCIFI, ignoreCase = true)
}

@Composable
fun ReticleGalleryScreen(
    viewModel: CrosshairViewModel,
    onNavigateToStudio: () -> Unit,
    onNavigateToDesigns: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presets by viewModel.allPresets.collectAsState()
    val activeConfig by viewModel.activeConfig.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    var selectedCategory by remember { mutableStateOf(CrosshairCollectionCategory.ALL) }
    var selectedShape by remember { mutableStateOf(DesignCategory.ALL) }
    var showSecondaryFilters by remember { mutableStateOf(false) }

    val shapeFiltered = remember(presets, selectedShape) {
        presets.filter { matchesShapeFilter(it, selectedShape) }
    }

    val categoryCounts = remember(shapeFiltered, favoriteIds) {
        CrosshairCollectionCategory.GALLERY_TABS.associateWith { cat ->
            shapeFiltered.count { PredefinedCrosshairCatalog.matchesCollectionCategory(it, cat, favoriteIds) }
        }
    }

    val filteredList = remember(shapeFiltered, selectedCategory, favoriteIds) {
        shapeFiltered.filter { PredefinedCrosshairCatalog.matchesCollectionCategory(it, selectedCategory, favoriteIds) }
    }

    val hasAnyFilter = selectedCategory != CrosshairCollectionCategory.ALL ||
        selectedShape != DesignCategory.ALL ||
        searchQuery.isNotBlank()

    val resetAllFilters = {
        selectedCategory = CrosshairCollectionCategory.ALL
        selectedShape = DesignCategory.ALL
        viewModel.setSearchQuery("")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("reticle_gallery_screen")
    ) {
        // Search and Import Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text(
                        stringResource(R.string.search_placeholder),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.cd_search),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.cd_clear),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("gallery_search_input")
            )

            Button(
                onClick = { viewModel.openImportDialog() },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier
                    .height(52.dp)
                    .testTag("gallery_import_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.btn_import), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Quick Banner to Pre-defined Crosshair Designs Screen
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onNavigateToDesigns() }
                .tvFocusable(RoundedCornerShape(16.dp), MaterialTheme.colorScheme.primary)
                .testTag("gallery_open_designs_btn")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.designs_banner_title),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.designs_banner_desc),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.cd_open_designs),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Collection Category Filter Chips + Shape Filter Toggle
        CollectionCategoryFilterBar(
            categories = CrosshairCollectionCategory.GALLERY_TABS,
            selectedCategory = selectedCategory,
            categoryCounts = categoryCounts,
            onSelectCategory = { selectedCategory = it },
            testTagPrefix = "gallery_tab",
            showSecondaryToggle = true,
            isSecondaryExpanded = showSecondaryFilters,
            hasActiveSecondaryFilter = selectedShape != DesignCategory.ALL,
            onToggleSecondary = { showSecondaryFilters = !showSecondaryFilters },
            modifier = Modifier.padding(top = 4.dp)
        )

        ExpandableShapeAndSearchPanel(
            visible = showSecondaryFilters || selectedShape != DesignCategory.ALL,
            selectedShape = selectedShape,
            onSelectShape = { selectedShape = it },
            testTagPrefix = "gallery"
        )

        FilterSummaryStrip(
            resultCount = filteredList.size,
            hasActiveFilters = hasAnyFilter,
            onResetFilters = resetAllFilters,
            resetTestTag = "gallery_summary_reset_btn"
        )

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val isFavoritesEmpty = selectedCategory == CrosshairCollectionCategory.FAVORITES &&
                        selectedShape == DesignCategory.ALL &&
                        searchQuery.isBlank()
                    Text(
                        text = when {
                            isFavoritesEmpty -> stringResource(R.string.favorites_empty_title)
                            searchQuery.isNotEmpty() -> stringResource(R.string.gallery_empty_search, searchQuery)
                            else -> stringResource(R.string.gallery_empty_default)
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            if (isFavoritesEmpty) R.string.favorites_empty_hint
                            else R.string.gallery_empty_hint
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    if (hasAnyFilter) {
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = resetAllFilters,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("gallery_reset_filters_btn")
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
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 158.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("gallery_grid_view")
            ) {
                items(filteredList, key = { it.id }) { preset ->
                    val isEquipped = activeConfig.id == preset.id ||
                        (activeConfig.style == preset.style && activeConfig.name == preset.name)

                    GalleryReticleCard(
                        preset = preset,
                        isEquipped = isEquipped,
                        isFavorite = preset.id in favoriteIds,
                        onToggleFavorite = { viewModel.toggleFavorite(preset.id) },
                        onEquip = { viewModel.applyPreset(preset) },
                        onCustomize = {
                            viewModel.applyPreset(preset)
                            onNavigateToStudio()
                        },
                        onShare = { viewModel.openShareDialog(preset) },
                        onDelete = if (preset.isCustom) {
                            { viewModel.deletePreset(preset.id) }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
fun GalleryReticleCard(
    preset: CrosshairConfig,
    isEquipped: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onEquip: () -> Unit,
    onCustomize: () -> Unit,
    onShare: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val builtInTitleRes = PredefinedCrosshairCatalog.resolveTitleResById(preset.id)
    val displayTitle = when {
        builtInTitleRes != null -> stringResource(builtInTitleRes)
        preset.name.isNotBlank() -> preset.name
        else -> stringResource(R.string.default_custom_crosshair_name)
    }
    val collectionLabel = stringResource(PredefinedCrosshairCatalog.resolveCollectionCategory(preset).labelRes)
    val categoryRes = CrosshairConfig.resolveCategoryLabelRes(preset.category)
    val displayCategory = if (preset.isCustom && categoryRes != null) {
        stringResource(categoryRes)
    } else {
        collectionLabel
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEquipped) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            if (isEquipped) 1.5.dp else 1.dp,
            if (isEquipped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("gallery_card_${preset.id}")
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
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isEquipped) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainer
                        )
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = displayCategory.uppercase(),
                        color = if (isEquipped) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.primary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FavoriteHeartButton(
                        isFavorite = isFavorite,
                        onToggle = onToggleFavorite,
                        iconSize = 16.dp,
                        touchSize = 30.dp,
                        testTagValue = "gallery_favorite_btn_${preset.id}"
                    )

                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.cd_share_preset),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.cd_delete_preset),
                                tint = LaserCrimson,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ArenaDarkCanvas)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .clickable { onEquip() },
                contentAlignment = Alignment.Center
            ) {
                CrosshairView(
                    config = preset,
                    overrideSize = (preset.sizeDp * 1.4f).dp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = displayTitle,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onEquip,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEquipped) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (isEquipped) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .tvFocusable(RoundedCornerShape(10.dp), MaterialTheme.colorScheme.primary)
                        .testTag("equip_btn_${preset.id}")
                ) {
                    if (isEquipped) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(R.string.status_active), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text(text = stringResource(R.string.btn_equip), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                IconButton(
                    onClick = onCustomize,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .tvFocusable(RoundedCornerShape(10.dp), MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = stringResource(R.string.cd_edit_studio),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
