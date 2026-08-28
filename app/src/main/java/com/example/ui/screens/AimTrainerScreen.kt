package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AimDifficulty
import com.example.ui.AimGameState
import com.example.ui.CrosshairViewModel
import com.example.ui.PreviewBackground
import com.example.ui.components.CrosshairView
import com.example.ui.components.GameBackgroundCanvas
import com.example.ui.theme.ArenaDarkCanvas
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
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SolarYellow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun AimTrainerScreen(
    viewModel: CrosshairViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.gameState.collectAsState()
    val config by viewModel.activeConfig.collectAsState()
    val background by viewModel.selectedBackground.collectAsState()
    val difficulty by viewModel.selectedDifficulty.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 90.dp)
            .padding(horizontal = 16.dp)
    ) {
        // Score & Live Stats HUD
        StatsHudBar(stats)

        Spacer(modifier = Modifier.height(10.dp))

        // Background Selector Pill Row for Fast Map Switching
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Wallpaper,
                contentDescription = null,
                tint = MinimalPurple,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Map:",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            PreviewBackground.values().forEach { bg ->
                val isSelected = background == bg
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MinimalPurple else MinimalSurface)
                        .border(1.dp, if (isSelected) MinimalPurple else MinimalBorder, RoundedCornerShape(12.dp))
                        .clickable { viewModel.setBackground(bg) }
                        .tvFocusable(RoundedCornerShape(12.dp), MinimalPurple)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = bg.displayName,
                        color = if (isSelected) Color.White else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Interactive Target Shooting Arena with Clean Minimalist styled frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(ArenaDarkCanvas)
                .border(2.5.dp, if (stats.state == AimGameState.PLAYING) MinimalPurple else MinimalBorder, RoundedCornerShape(24.dp))
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val arenaWidth = maxWidth
                val arenaHeight = maxHeight

                // Arena Background
                GameBackgroundCanvas(background = background)

                // The User's Active Crosshair centered
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CrosshairView(
                        config = config,
                        overrideSize = (config.sizeDp * 1.5f).dp
                    )
                }

                // Miss click detector on arena floor when playing
                if (stats.state == AimGameState.PLAYING) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                viewModel.onTargetMiss()
                            }
                    )
                }

                // Active Target
                if (stats.state == AimGameState.PLAYING && stats.currentTarget != null) {
                    val target = stats.currentTarget!!
                    val targetSizeDp = (target.radiusDp * 2).dp
                    val targetX = arenaWidth * target.xFraction - (targetSizeDp / 2)
                    val targetY = arenaHeight * target.yFraction - (targetSizeDp / 2)

                    Box(
                        modifier = Modifier
                            .offset(x = targetX, y = targetY)
                            .size(targetSizeDp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                viewModel.onTargetHit(isHeadshot = target.isHeadshotRing)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(targetSizeDp)) {
                            // Outer Target Ring
                            drawCircle(
                                color = LaserCrimson,
                                radius = size.minDimension / 2f
                            )
                            drawCircle(
                                color = Color.White,
                                radius = size.minDimension / 2.8f
                            )
                            drawCircle(
                                color = LaserCrimson,
                                radius = size.minDimension / 4.2f
                            )
                            // Headshot Bullseye
                            drawCircle(
                                color = if (target.isHeadshotRing) SolarYellow else Color.White,
                                radius = size.minDimension / 8f
                            )
                        }

                        if (target.isHeadshotRing) {
                            Text(
                                text = "CRIT",
                                color = Color.Black,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // Overlays for Idle / Finished
                if (stats.state == AimGameState.IDLE) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.78f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MinimalPurpleLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = MinimalPurpleDark,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "REFLEX AIM DRILL",
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Test your reticle contrast and response speed in a 20-second target drill.",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Difficulty Picker Pills
                            Text(
                                text = "DIFFICULTY",
                                color = MinimalPurpleLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AimDifficulty.values().forEach { diff ->
                                    val isDiffSelected = difficulty == diff
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isDiffSelected) MinimalPurple else Color.White.copy(alpha = 0.12f))
                                            .border(
                                                1.dp,
                                                if (isDiffSelected) MinimalPurpleLight else Color.White.copy(alpha = 0.2f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { viewModel.setDifficulty(diff) }
                                            .tvFocusable(RoundedCornerShape(12.dp), MinimalPurple)
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = diff.title,
                                            color = if (isDiffSelected) Color.White else Color.White.copy(alpha = 0.8f),
                                            fontSize = 11.sp,
                                            fontWeight = if (isDiffSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = { viewModel.startAimTrainer() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MinimalPurple,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .fillMaxWidth(0.75f)
                                    .tvFocusable(RoundedCornerShape(14.dp), MinimalPurple)
                                    .testTag("start_aim_trainer_btn")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "START DRILL",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else if (stats.state == AimGameState.FINISHED) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        SummaryScoreCard(
                            stats = stats,
                            onRestart = { viewModel.startAimTrainer() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatsHudBar(stats: com.example.ui.AimGameStats) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MinimalSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MinimalBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timer
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = if (stats.timeLeftSec <= 5) LaserCrimson else MinimalPurple,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${stats.timeLeftSec}s",
                    color = if (stats.timeLeftSec <= 5) LaserCrimson else MinimalPurple,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Score
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "SCORE",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${stats.score}",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Streak
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = SolarYellow,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${stats.currentStreak}x",
                    color = SolarYellow,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Reaction Time
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "AVG REACTION",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (stats.avgReactionTimeMs > 0) "${stats.avgReactionTimeMs}ms" else "--",
                    color = MinimalPurpleDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun SummaryScoreCard(
    stats: com.example.ui.AimGameStats,
    onRestart: () -> Unit
) {
    val accuracy = if (stats.totalTargets > 0) {
        ((stats.targetsHit.toFloat() / stats.totalTargets.toFloat()) * 100).toInt()
    } else 0

    val rank = when {
        stats.score >= 2500 && accuracy >= 85 -> "SSS • PRO ESPORTS"
        stats.score >= 1800 -> "DIAMOND AIM"
        stats.score >= 1200 -> "PLATINUM SHARPSHOOTER"
        else -> "GOLD RANGER"
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MinimalSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MinimalBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "DRILL COMPLETE",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${stats.score} PTS",
                color = MinimalPurple,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MinimalPurpleContainer)
                    .border(1.dp, MinimalPurpleLight, RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = rank,
                    color = MinimalPurpleDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Stat breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "Accuracy", value = "$accuracy%")
                StatItem(label = "Reaction", value = "${stats.avgReactionTimeMs}ms")
                StatItem(label = "Headshots", value = "${stats.headshots}")
                StatItem(label = "Best Streak", value = "${stats.bestStreak}x")
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MinimalPurple,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .tvFocusable(RoundedCornerShape(14.dp), MinimalPurple)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PLAY AGAIN",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = TextTertiary, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

