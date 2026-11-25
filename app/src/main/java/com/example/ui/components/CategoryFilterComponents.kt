package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CrosshairCollectionCategory
import com.example.model.DesignCategory
import com.example.ui.theme.LaserCrimson
import com.example.ui.theme.tvFocusable

fun collectionCategoryIcon(category: CrosshairCollectionCategory): ImageVector = when (category) {
    CrosshairCollectionCategory.ALL -> Icons.Default.GridView
    CrosshairCollectionCategory.FAVORITES -> Icons.Default.Favorite
    CrosshairCollectionCategory.COMPETITIVE -> Icons.Default.EmojiEvents
    CrosshairCollectionCategory.MINIMALIST -> Icons.Default.Adjust
    CrosshairCollectionCategory.TACTICAL -> Icons.Default.GpsFixed
    CrosshairCollectionCategory.FUN -> Icons.Default.AutoAwesome
    CrosshairCollectionCategory.CUSTOM -> Icons.Default.Bookmark
}

@Composable
fun FavoriteHeartButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 18.dp,
    touchSize: Dp = 48.dp,
    unselectedTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedTint: Color = LaserCrimson,
    testTagValue: String? = null
) {
    val heartScale by animateFloatAsState(
        targetValue = if (isFavorite) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "favorite_heart_scale"
    )
    IconButton(
        onClick = onToggle,
        modifier = modifier
            .size(touchSize)
            .clip(CircleShape)
            .tvFocusable(CircleShape, MaterialTheme.colorScheme.primary)
            .let { if (testTagValue != null) it.testTag(testTagValue) else it }
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = stringResource(
                if (isFavorite) R.string.cd_remove_favorite else R.string.cd_add_favorite
            ),
            tint = if (isFavorite) selectedTint else unselectedTint,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    scaleX = heartScale
                    scaleY = heartScale
                }
        )
    }
}

@Composable
fun CollectionCategoryFilterBar(
    categories: List<CrosshairCollectionCategory>,
    selectedCategory: CrosshairCollectionCategory,
    categoryCounts: Map<CrosshairCollectionCategory, Int>,
    onSelectCategory: (CrosshairCollectionCategory) -> Unit,
    testTagPrefix: String,
    showSecondaryToggle: Boolean = false,
    isSecondaryExpanded: Boolean = false,
    hasActiveSecondaryFilter: Boolean = false,
    onToggleSecondary: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showSecondaryToggle && onToggleSecondary != null) {
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, end = 6.dp)
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSecondaryExpanded || hasActiveSecondaryFilter) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isSecondaryExpanded || hasActiveSecondaryFilter) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    ),
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(role = Role.Button) { onToggleSecondary() }
                        .tvFocusable(RoundedCornerShape(14.dp), MaterialTheme.colorScheme.primary)
                        .testTag("${testTagPrefix}_toggle_filters_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.filter_more_toggle),
                            tint = if (isSecondaryExpanded || hasActiveSecondaryFilter) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        if (hasActiveSecondaryFilter) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .padding(
                    start = if (showSecondaryToggle) 2.dp else 16.dp,
                    end = 16.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { category ->
                val isSelected = selectedCategory == category
                val count = categoryCounts[category] ?: 0
                val labelText = stringResource(category.labelRes)
                val chipText = stringResource(R.string.filter_chip_count_format, labelText, count)
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface,
                    animationSpec = tween(180),
                    label = "cat_chip_bg_${category.name}"
                )

                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable(role = Role.Tab) { onSelectCategory(category) }
                        .semantics { selected = isSelected }
                        .testTag("${testTagPrefix}_${category.name}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(bgColor)
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(19.dp)
                            )
                            .tvFocusable(RoundedCornerShape(19.dp), MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = collectionCategoryIcon(category),
                            contentDescription = null,
                            tint = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                category == CrosshairCollectionCategory.FAVORITES -> LaserCrimson
                                else -> MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = chipText,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandableShapeAndSearchPanel(
    visible: Boolean,
    selectedShape: DesignCategory,
    onSelectShape: (DesignCategory) -> Unit,
    searchQuery: String? = null,
    onSearchQueryChange: ((String) -> Unit)? = null,
    searchPlaceholderRes: Int = R.string.designs_search_placeholder,
    testTagPrefix: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(tween(200)) + fadeIn(tween(180)),
        exit = shrinkVertically(tween(180)) + fadeOut(tween(150)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (searchQuery != null && onSearchQueryChange != null) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = stringResource(searchPlaceholderRes),
                            fontSize = 12.5.sp,
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
                            IconButton(onClick = { onSearchQueryChange("") }) {
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
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("${testTagPrefix}_search_input")
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DesignCategory.entries.forEach { shape ->
                    val isSelected = selectedShape == shape
                    Box(
                        modifier = Modifier
                            .heightIn(min = 42.dp)
                            .clickable(role = Role.Tab) { onSelectShape(shape) }
                            .semantics { selected = isSelected }
                            .testTag("${testTagPrefix}_shape_${shape.name}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainer
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                                    RoundedCornerShape(16.dp)
                                )
                                .tvFocusable(RoundedCornerShape(16.dp), MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(shape.labelRes),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterSummaryStrip(
    resultCount: Int,
    hasActiveFilters: Boolean,
    onResetFilters: () -> Unit,
    resetTestTag: String,
    modifier: Modifier = Modifier
) {
    if (!hasActiveFilters) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.filter_results_count_format, resultCount),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium
        )
        TextButton(
            onClick = onResetFilters,
            modifier = Modifier
                .heightIn(min = 40.dp)
                .testTag(resetTestTag)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.filter_reset_btn),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
