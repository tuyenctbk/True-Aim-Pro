package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.ui.theme.CyanLaser
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianCanvas
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emblemScale = remember { Animatable(0.55f) }
    val emblemAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(24f) }
    val ringSweep = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_ring_rotation")
    val outerAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerAngle"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    LaunchedEffect(Unit) {
        launch {
            emblemAlpha.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
        }
        launch {
            emblemScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            ringSweep.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        }
        delay(220L)
        launch {
            textAlpha.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        }
        launch {
            textOffsetY.animateTo(0f, tween(380, easing = FastOutSlowInEasing))
        }
        delay(950L)
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianCanvas)
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Subtle Tactical Blueprint Grid & Radial Glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridStep = 44.dp.toPx()
            val gridColor = CyanLaser.copy(alpha = 0.045f)
            var x = 0f
            while (x < size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                x += gridStep
            }
            var y = 0f
            while (y < size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                y += gridStep
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonGreen.copy(alpha = 0.16f * glowPulse),
                        CyanLaser.copy(alpha = 0.08f * glowPulse),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.55f
                ),
                radius = size.minDimension * 0.55f,
                center = center
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Animated Precision Lock-On Reticle Emblem
            Box(
                modifier = Modifier.size(164.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokePx = 2.5.dp.toPx()
                    val outerRadius = size.minDimension / 2f - strokePx
                    val sweep = 68f * ringSweep.value
                    for (i in 0 until 4) {
                        val start = outerAngle + (i * 90f) + 10f
                        drawArc(
                            color = if (i % 2 == 0) NeonGreen else CyanLaser,
                            startAngle = start,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }

                    // Counter-rotating inner precision calibration arcs
                    val innerR = outerRadius * 0.84f
                    val innerSweep = 34f * ringSweep.value
                    val innerInset = outerRadius - innerR + strokePx
                    for (i in 0 until 4) {
                        val innerStart = -(outerAngle * 1.4f) + (i * 90f) + 28f
                        drawArc(
                            color = CyanLaser.copy(alpha = 0.55f * ringSweep.value),
                            startAngle = innerStart,
                            sweepAngle = innerSweep,
                            useCenter = false,
                            topLeft = Offset(innerInset, innerInset),
                            size = androidx.compose.ui.geometry.Size(innerR * 2f, innerR * 2f),
                            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Expanding subtle lock-on pulse ring
                    drawCircle(
                        color = NeonGreen.copy(alpha = 0.14f * glowPulse),
                        radius = outerRadius * (0.90f + 0.08f * glowPulse),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.img_trueaim_app_icon_1795534801161),
                    contentDescription = stringResource(R.string.app_name),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(116.dp)
                        .graphicsLayer {
                            scaleX = emblemScale.value
                            scaleY = emblemScale.value
                            alpha = emblemAlpha.value
                        }
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = textAlpha.value
                    translationY = textOffsetY.value
                }
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontFamily = DisplayFontFamily,
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.splash_tagline),
                    fontFamily = MonoFontFamily,
                    color = NeonGreen.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }

        // Bottom version watermark
        Text(
            text = stringResource(R.string.app_version_label, BuildConfig.VERSION_NAME),
            fontFamily = MonoFontFamily,
            color = Color.White.copy(alpha = 0.42f),
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
                .graphicsLayer { alpha = textAlpha.value }
        )
    }
}
