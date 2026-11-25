package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.AimDifficulty
import com.example.ui.AimGameState
import com.example.ui.AimGameStats
import com.example.ui.CrosshairViewModel
import com.example.ui.PreviewBackground
import com.example.ui.components.CrosshairView
import com.example.ui.components.GameBackgroundCanvas
import com.example.ui.theme.ArenaDarkCanvas
import com.example.ui.theme.LaserCrimson
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SolarYellow
import com.example.ui.theme.tvFocusable

@Composable
fun AimTrainerScreen(
    viewModel: CrosshairViewModel,
    onAnalyzeWithAiCoach: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.gameState.collectAsState()
    val config by viewModel.activeConfig.collectAsState()
    val background by viewModel.selectedBackground.collectAsState()
    val difficulty by viewModel.selectedDifficulty.collectAsState()
    val lastHit by viewModel.lastHitFeedback.collectAsState()
    val lastMissTs by viewModel.lastMissTimestampMs.collectAsState()

    val missFlashAlpha = remember { Animatable(0f) }
    LaunchedEffect(lastMissTs) {
        if (lastMissTs > 0L && stats.state == AimGameState.PLAYING) {
            missFlashAlpha.snapTo(0.85f)
            missFlashAlpha.animateTo(0f, tween(340, easing = FastOutSlowInEasing))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 16.dp)
            .padding(horizontal = 16.dp)
            .testTag("aim_trainer_screen")
    ) {
        StatsHudBar(stats)

        Spacer(modifier = Modifier.height(10.dp))

        // Background Selector Pill Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Wallpaper,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(R.string.trainer_map_label),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            PreviewBackground.entries.forEach { bg ->
                val isSelected = background == bg
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface
                        )
                        .border(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.setBackground(bg) }
                        .tvFocusable(RoundedCornerShape(12.dp), MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(bg.nameRes),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Interactive Target Shooting Arena
        val arenaBorderColor by animateColorAsState(
            targetValue = when {
                missFlashAlpha.value > 0.1f -> LaserCrimson
                stats.state == AimGameState.PLAYING -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outline
            },
            animationSpec = tween(140),
            label = "arena_border_color"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(ArenaDarkCanvas)
                .border(
                    width = if (missFlashAlpha.value > 0.1f) 3.dp else 2.dp,
                    color = arenaBorderColor,
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val arenaWidth = maxWidth
                val arenaHeight = maxHeight

                GameBackgroundCanvas(background = background)

                // Subtle crimson miss feedback flash overlay
                if (missFlashAlpha.value > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(LaserCrimson.copy(alpha = 0.16f * missFlashAlpha.value))
                    )
                }

                // User's Active Crosshair centered
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CrosshairView(
                        config = config,
                        overrideSize = config.sizeDp.coerceIn(22f, 56f).dp
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

                // Active Target with Spring Pop-In Animation
                if (stats.state == AimGameState.PLAYING && stats.currentTarget != null) {
                    val target = stats.currentTarget!!
                    val targetScale = remember(target.id) { Animatable(0.25f) }
                    LaunchedEffect(target.id) {
                        targetScale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }

                    val targetSizeDp = (target.radiusDp * 2f).dp
                    val targetX = arenaWidth * target.xFraction - (targetSizeDp / 2)
                    val targetY = arenaHeight * target.yFraction - (targetSizeDp / 2)

                    Box(
                        modifier = Modifier
                            .offset(x = targetX, y = targetY)
                            .size(targetSizeDp)
                            .graphicsLayer {
                                scaleX = targetScale.value
                                scaleY = targetScale.value
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                viewModel.onTargetHit(isHeadshot = target.isHeadshotRing)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(targetSizeDp)) {
                            drawCircle(color = LaserCrimson, radius = size.minDimension / 2f)
                            drawCircle(color = Color.White, radius = size.minDimension / 2.8f)
                            drawCircle(color = LaserCrimson, radius = size.minDimension / 4.2f)
                            drawCircle(
                                color = if (target.isHeadshotRing) SolarYellow else Color.White,
                                radius = size.minDimension / 8f
                            )
                        }

                        if (target.isHeadshotRing) {
                            Text(
                                text = stringResource(R.string.trainer_crit_label),
                                color = Color.Black,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // Animated Hit Feedback Ripple + Floating Score Popup
                lastHit?.let { hit ->
                    val hitAnimProgress = remember(hit.id) { Animatable(0f) }
                    LaunchedEffect(hit.id) {
                        hitAnimProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing)
                        )
                    }

                    if (hitAnimProgress.value < 1f) {
                        val rippleSize = (28f + 44f * hitAnimProgress.value).dp
                        val rippleAlpha = (1f - hitAnimProgress.value).coerceIn(0f, 1f)
                        Canvas(
                            modifier = Modifier
                                .offset(
                                    x = arenaWidth * hit.xFraction - rippleSize / 2,
                                    y = arenaHeight * hit.yFraction - rippleSize / 2
                                )
                                .size(rippleSize)
                        ) {
                            drawCircle(
                                color = (if (hit.isHeadshot) SolarYellow else NeonGreen).copy(alpha = rippleAlpha),
                                radius = size.minDimension / 2f,
                                style = Stroke(width = 2.5.dp.toPx())
                            )
                        }

                        val floatY = ((arenaHeight * hit.yFraction) - (36 * hitAnimProgress.value).dp)
                            .coerceAtLeast(6.dp)
                        val floatX = (arenaWidth * hit.xFraction - 34.dp)
                            .coerceIn(4.dp, (arenaWidth - 88.dp).coerceAtLeast(4.dp))
                        val scale = 0.85f + (0.3f * hitAnimProgress.value)

                        Box(
                            modifier = Modifier
                                .offset(x = floatX, y = floatY)
                                .graphicsLayer {
                                    alpha = rippleAlpha
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (hit.isHeadshot) SolarYellow.copy(alpha = 0.94f)
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (hit.isHeadshot)
                                    stringResource(R.string.trainer_hit_crit, hit.scoreDelta)
                                else
                                    stringResource(R.string.trainer_hit_regular, hit.scoreDelta),
                                fontFamily = MonoFontFamily,
                                color = if (hit.isHeadshot) Color.Black else MaterialTheme.colorScheme.onPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Idle Overlay
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
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = stringResource(R.string.reflex_aim_drill_title),
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.reflex_aim_drill_desc),
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = stringResource(R.string.difficulty_label),
                                color = NeonGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AimDifficulty.entries.forEach { diff ->
                                    val isDiffSelected = difficulty == diff
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isDiffSelected) MaterialTheme.colorScheme.primary
                                                else Color.White.copy(alpha = 0.12f)
                                            )
                                            .border(
                                                1.dp,
                                                if (isDiffSelected) MaterialTheme.colorScheme.primary
                                                else Color.White.copy(alpha = 0.22f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { viewModel.setDifficulty(diff) }
                                            .tvFocusable(RoundedCornerShape(12.dp), MaterialTheme.colorScheme.primary)
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(diff.titleRes),
                                            color = if (isDiffSelected) MaterialTheme.colorScheme.onPrimary
                                            else Color.White.copy(alpha = 0.85f),
                                            fontSize = 11.sp,
                                            fontWeight = if (isDiffSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = { viewModel.startAimTrainer() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .fillMaxWidth(0.75f)
                                    .tvFocusable(RoundedCornerShape(14.dp), MaterialTheme.colorScheme.primary)
                                    .testTag("start_aim_trainer_btn")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.start_drill_btn),
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
                            onRestart = { viewModel.startAimTrainer() },
                            onDone = { viewModel.dismissAimSummaryToIdle() },
                            onAnalyzeWithAiCoach = onAnalyzeWithAiCoach
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatsHudBar(stats: AimGameStats) {
    val isLowTime = stats.timeLeftSec in 1..5 && stats.state == AimGameState.PLAYING
    val timerPulseScale = remember { Animatable(1f) }
    LaunchedEffect(isLowTime, stats.timeLeftSec) {
        if (isLowTime) {
            timerPulseScale.snapTo(1.16f)
            timerPulseScale.animateTo(1f, tween(340, easing = FastOutSlowInEasing))
        } else {
            timerPulseScale.snapTo(1f)
        }
    }

    val animatedScore by animateIntAsState(
        targetValue = stats.score,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "hud_score_anim"
    )

    val streakScale by animateFloatAsState(
        targetValue = if (stats.currentStreak > 0) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "streakScale"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stats_hud_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.graphicsLayer {
                    scaleX = timerPulseScale.value
                    scaleY = timerPulseScale.value
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = if (isLowTime) LaserCrimson else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.unit_sec_short_format, stats.timeLeftSec),
                    fontFamily = MonoFontFamily,
                    color = if (isLowTime) LaserCrimson else MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.score_label),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$animatedScore",
                    fontFamily = MonoFontFamily,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.graphicsLayer {
                    scaleX = streakScale
                    scaleY = streakScale
                }
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = SolarYellow,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.trainer_streak_x_format, stats.currentStreak),
                    fontFamily = MonoFontFamily,
                    color = SolarYellow,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.avg_reaction_label),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (stats.avgReactionTimeMs > 0) {
                        stringResource(R.string.unit_ms_format, stats.avgReactionTimeMs)
                    } else stringResource(R.string.stat_empty_placeholder),
                    fontFamily = MonoFontFamily,
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SummaryScoreCard(
    stats: AimGameStats,
    onRestart: () -> Unit,
    onDone: () -> Unit,
    onAnalyzeWithAiCoach: (() -> Unit)? = null
) {
    val accuracy = if (stats.totalTargets > 0) {
        ((stats.targetsHit.toFloat() / stats.totalTargets.toFloat()) * 100).toInt()
    } else 0

    val rank = when {
        stats.score >= 2500 && accuracy >= 85 -> stringResource(R.string.rank_sss_pro_esports)
        stats.score >= 1800 -> stringResource(R.string.rank_diamond_aim)
        stats.score >= 1200 -> stringResource(R.string.rank_platinum_sharpshooter)
        else -> stringResource(R.string.rank_gold_ranger)
    }

    val cardScale = remember { Animatable(0.88f) }
    val cardAlpha = remember { Animatable(0f) }
    val animatedFinalScore = remember { Animatable(0f) }
    LaunchedEffect(stats.score) {
        cardAlpha.animateTo(1f, tween(220))
    }
    LaunchedEffect(Unit) {
        cardScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }
    LaunchedEffect(stats.score) {
        animatedFinalScore.animateTo(
            targetValue = stats.score.toFloat(),
            animationSpec = tween(550, easing = FastOutSlowInEasing)
        )
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .graphicsLayer {
                scaleX = cardScale.value
                scaleY = cardScale.value
                alpha = cardAlpha.value
            }
            .testTag("aim_summary_card")
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.drill_complete_title),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.trainer_score_pts_format, animatedFinalScore.value.toInt()),
                fontFamily = MonoFontFamily,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = rank,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = stringResource(R.string.stat_accuracy), value = stringResource(R.string.percent_format, accuracy))
                StatItem(
                    label = stringResource(R.string.stat_reaction),
                    value = stringResource(R.string.unit_ms_format, stats.avgReactionTimeMs)
                )
                StatItem(label = stringResource(R.string.stat_headshots), value = "${stats.headshots}")
                StatItem(label = stringResource(R.string.stat_best_streak), value = stringResource(R.string.trainer_streak_x_format, stats.bestStreak))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDone,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .weight(0.85f)
                        .height(48.dp)
                        .testTag("aim_summary_done_btn")
                ) {
                    Text(
                        text = stringResource(R.string.btn_done),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.35f)
                        .height(48.dp)
                        .tvFocusable(RoundedCornerShape(14.dp), MaterialTheme.colorScheme.primary)
                        .testTag("aim_summary_restart_btn")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.play_again_btn),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (onAnalyzeWithAiCoach != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onAnalyzeWithAiCoach,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("aim_summary_ai_coach_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.ai_coach_from_summary_btn),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontFamily = MonoFontFamily,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
