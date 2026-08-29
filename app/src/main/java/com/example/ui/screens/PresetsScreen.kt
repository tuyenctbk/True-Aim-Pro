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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.model.CrosshairConfig
import com.example.R
import com.example.ui.CrosshairViewModel
import com.example.ui.components.CrosshairView
import com.example.ui.theme.ArenaDarkCanvas
import com.example.ui.theme.LaserCrimson
import com.example.ui.theme.MinimalBorder
import com.example.ui.theme.MinimalCanvas
import com.example.ui.theme.MinimalGreen
import com.example.ui.theme.MinimalPurple
import com.example.ui.theme.MinimalPurpleContainer
import com.example.ui.theme.MinimalPurpleDark
import com.example.ui.theme.MinimalPurpleLight
import com.example.ui.theme.MinimalSurface
import com.example.ui.theme.MinimalSurfaceContainer
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun PresetsScreen(
    viewModel: CrosshairViewModel,
    onNavigateToStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presets by viewModel.allPresets.collectAsState()
    val activeConfig by viewModel.activeConfig.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val categories = listOf("All", "Classic", "Dot", "Tactical", "Sniper", "Sci-Fi", "Custom")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 90.dp)
    ) {
        // Search & Import Bar
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
                placeholder = { Text(stringResource(R.string.search_presets_placeholder), fontSize = 13.sp, color = TextTertiary) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MinimalPurple,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MinimalPurple,
                    unfocusedBorderColor = MinimalBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = MinimalSurface,
                    unfocusedContainerColor = MinimalSurface
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("preset_search_input")
            )

            Button(
                onClick = { viewModel.openImportDialog() },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MinimalPurpleContainer,
                    contentColor = MinimalPurpleDark
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier
                    .height(50.dp)
                    .testTag("import_preset_btn")
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

        // Category Filter Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                val isSelected = selectedCategory == category
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) MinimalPurple else MinimalSurface)
                        .border(
                            1.dp,
                            if (isSelected) MinimalPurple else MinimalBorder,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { viewModel.setCategory(category) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("preset_cat_$category")
                ) {
                    Text(
                        text = category,
                        color = if (isSelected) Color.White else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Preset Grid
        if (presets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No Crosshairs Found for \"$searchQuery\"" else "No Saved Custom Crosshairs Yet",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try clearing the search or save new reticles in Studio",
                        color = TextTertiary,
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
                modifier = Modifier.fillMaxSize()
            ) {
                items(presets, key = { it.id }) { preset ->
                    val isEquipped = activeConfig.id == preset.id ||
                            (activeConfig.style == preset.style && activeConfig.name == preset.name)

                    PresetCard(
                        preset = preset,
                        isEquipped = isEquipped,
                        onEquip = {
                            viewModel.applyPreset(preset)
                        },
                        onCustomize = {
                            viewModel.applyPreset(preset)
                            onNavigateToStudio()
                        },
                        onShare = {
                            viewModel.openShareDialog(preset)
                        },
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
fun PresetCard(
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
            containerColor = if (isEquipped) MinimalPurpleContainer else MinimalSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (isEquipped) 1.5.dp else 1.dp,
            if (isEquipped) MinimalPurple else MinimalBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("preset_card_${preset.name}")
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
                        .background(if (isEquipped) MinimalPurpleLight else MinimalSurfaceContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = preset.category.uppercase(),
                        color = if (isEquipped) MinimalPurpleDark else MinimalPurple,
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
                            contentDescription = "Share preset code",
                            tint = TextTertiary,
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
                                contentDescription = "Delete custom preset",
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
                    .size(82.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ArenaDarkCanvas)
                    .border(1.dp, MinimalBorder, RoundedCornerShape(16.dp))
                    .clickable { onEquip() },
                contentAlignment = Alignment.Center
            ) {
                CrosshairView(
                    config = preset,
                    overrideSize = (preset.sizeDp * 1.5f).dp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = preset.name,
                color = if (isEquipped) MinimalPurpleDark else TextPrimary,
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
                        containerColor = if (isEquipped) MinimalPurple else MinimalPurpleLight,
                        contentColor = if (isEquipped) Color.White else MinimalPurpleDark
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    if (isEquipped) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(R.string.btn_active), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text(text = stringResource(R.string.btn_equip), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                IconButton(
                    onClick = onCustomize,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MinimalSurfaceContainer)
                        .border(1.dp, MinimalBorder, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Edit in Studio",
                        tint = MinimalPurple,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}


