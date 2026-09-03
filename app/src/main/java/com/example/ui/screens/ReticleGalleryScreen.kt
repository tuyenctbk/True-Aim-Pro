package com.example.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CrosshairConfig
import com.example.model.CrosshairStyle
import com.example.ui.CrosshairViewModel
import com.example.ui.components.CrosshairView
import com.example.ui.theme.ArenaDarkCanvas
import com.example.ui.theme.LaserCrimson
import com.example.ui.theme.tvFocusable


enum class GalleryTab(val labelRes: Int) {
    ALL(R.string.tab_gallery),
    DOTS(R.string.style_dot),
    CIRCLES(R.string.style_circle),
    CROSSES(R.string.style_cross),
    TACTICAL(R.string.style_tactical),
    SNIPER(R.string.preset_sniper),
    SCI_FI(R.string.preset_sci_fi),
    MY_PRESETS(R.string.title_presets)
}

@Composable
fun ReticleGalleryScreen(
    viewModel: CrosshairViewModel,
    onNavigateToStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presets by viewModel.allPresets.collectAsState()
    val activeConfig by viewModel.activeConfig.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var selectedTab by remember { mutableStateOf(GalleryTab.ALL) }

    val filteredList = remember(presets, selectedTab, searchQuery) {
        val base = presets.filter { preset ->
            when (selectedTab) {
                GalleryTab.ALL -> true
                GalleryTab.DOTS -> preset.style in listOf(
                    CrosshairStyle.PRECISION_DOT,
                    CrosshairStyle.MICRO_PLUS,
                    CrosshairStyle.HOLLOW_DIAMOND
                ) || preset.category.equals("Dot", ignoreCase = true)

                GalleryTab.CIRCLES -> preset.style in listOf(
                    CrosshairStyle.TACTICAL_CIRCLE,
                    CrosshairStyle.DYNAMIC_RING,
                    CrosshairStyle.CROSS_CIRCLE,
                    CrosshairStyle.SONAR_RADAR
                ) || preset.category.equals("Circle", ignoreCase = true)

                GalleryTab.CROSSES -> preset.style in listOf(
                    CrosshairStyle.CLASSIC_CROSS,
                    CrosshairStyle.T_SHAPE,
                    CrosshairStyle.MICRO_PLUS,
                    CrosshairStyle.CROSS_CIRCLE,
                    CrosshairStyle.HEARTBEAT
                ) || preset.category.equals("Classic", ignoreCase = true)

                GalleryTab.TACTICAL -> preset.style in listOf(
                    CrosshairStyle.SHOTGUN_BOX,
                    CrosshairStyle.CORNER_BRACKETS,
                    CrosshairStyle.CHEVRON_ARROW,
                    CrosshairStyle.TACTICAL_CIRCLE
                ) || preset.category.equals("Tactical", ignoreCase = true)

                GalleryTab.SNIPER -> preset.style in listOf(
                    CrosshairStyle.MIL_DOT_SNIPER,
                    CrosshairStyle.HOLLOW_DIAMOND
                ) || preset.category.equals("Sniper", ignoreCase = true)

                GalleryTab.SCI_FI -> preset.style in listOf(
                    CrosshairStyle.CYBER_HEX,
                    CrosshairStyle.TRI_BLADE,
                    CrosshairStyle.HEARTBEAT,
                    CrosshairStyle.SONAR_RADAR
                ) || preset.category.equals("Sci-Fi", ignoreCase = true)

                GalleryTab.MY_PRESETS -> preset.isCustom || preset.category == "Custom" ||
                        preset.category in listOf("Valorant", "Apex Legends", "CS2", "CoD: Mobile", "PUBG Mobile", "Free Fire")
            }
        }

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            base.filter {
                it.name.lowercase().contains(q) ||
                        it.category.lowercase().contains(q) ||
                        it.style.displayName.lowercase().contains(q)
            }
        } else {
            base
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 90.dp)
            .testTag("reticle_gallery_screen")
    ) {
        // Search and Action Bar
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
                placeholder = { Text(stringResource(R.string.search_placeholder), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
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
                    .height(50.dp)
                    .testTag("gallery_search_input")
            )

            Button(
                onClick = { viewModel.openImportDialog() },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier
                    .height(50.dp)
                    .testTag("gallery_import_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = stringResource(R.string.btn_import), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GalleryTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedTab = tab }
                        .tvFocusable(RoundedCornerShape(20.dp), MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("gallery_tab_${tab.name}")
                ) {
                    Text(
                        text = stringResource(tab.labelRes),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Gallery Grid View
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
                    Text(
                        text = if (searchQuery.isNotEmpty())
                            stringResource(R.string.gallery_empty_search, searchQuery)
                        else
                            stringResource(R.string.gallery_empty_default),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.gallery_empty_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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
    onEquip: () -> Unit,
    onCustomize: () -> Unit,
    onShare: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEquipped) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (isEquipped) 1.5.dp else 1.dp,
            if (isEquipped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("gallery_card_${preset.name}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Category Badge & Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isEquipped) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = preset.category.uppercase(),
                        color = if (isEquipped) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share preset",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    if (onDelete != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete preset",
                                tint = LaserCrimson,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Crosshair Vector Box
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
                text = preset.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
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
                        containerColor = if (isEquipped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (isEquipped) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .tvFocusable(RoundedCornerShape(10.dp), MaterialTheme.colorScheme.primary)
                        .testTag("equip_btn_${preset.name}")
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
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .tvFocusable(RoundedCornerShape(10.dp), MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Edit in Studio",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

