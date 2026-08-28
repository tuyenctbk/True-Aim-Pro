package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.CrosshairConfig
import com.example.model.CrosshairStyle
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CrosshairView(
    config: CrosshairConfig,
    modifier: Modifier = Modifier,
    overrideSize: Dp? = null
) {
    val displaySize = overrideSize ?: config.sizeDp.dp

    Canvas(
        modifier = modifier.size(displaySize)
    ) {
        drawCrosshairOnScope(this, config)
    }
}

fun drawCrosshairOnScope(scope: DrawScope, config: CrosshairConfig) {
    val center = scope.center
    val baseColor = Color(config.primaryColorHex).copy(alpha = config.opacity)
    val outlineColor = Color(config.outlineColorHex).copy(alpha = config.opacity)

    // Calculate scale factor relative to standard 100x100 canvas
    val density = scope.drawContext.density.density
    val width = scope.size.width
    val height = scope.size.height
    val minDim = minOf(width, height)
    val radius = minDim / 2f

    // Size parameters in px
    val lineThickness = config.thicknessDp * density
    val outlineThickness = (config.thicknessDp + config.outlineThicknessDp * 2) * density
    val gap = (config.gapDp * density).coerceAtMost(radius * 0.8f)
    val dotRadius = (config.centerDotSizeDp * density / 2f)

    scope.rotate(degrees = config.rotationDeg, pivot = center) {
        when (config.style) {
            CrosshairStyle.CLASSIC_CROSS, CrosshairStyle.T_SHAPE -> {
                val isT = config.tStyle || config.style == CrosshairStyle.T_SHAPE

                // Draw Outlines if enabled
                if (config.outlineEnabled) {
                    drawCrossLines(
                        scope = this,
                        center = center,
                        radius = radius,
                        gap = gap,
                        thickness = outlineThickness,
                        color = outlineColor,
                        skipTop = isT
                    )
                }

                // Draw Primary lines
                drawCrossLines(
                    scope = this,
                    center = center,
                    radius = radius,
                    gap = gap,
                    thickness = lineThickness,
                    color = baseColor,
                    skipTop = isT
                )
            }

            CrosshairStyle.PRECISION_DOT -> {
                // Precision Dot with optional micro ring
                val outerRingRadius = radius * 0.7f
                if (config.outlineEnabled) {
                    drawCircle(
                        color = outlineColor,
                        radius = outerRingRadius,
                        center = center,
                        style = Stroke(width = outlineThickness)
                    )
                }
                drawCircle(
                    color = baseColor,
                    radius = outerRingRadius,
                    center = center,
                    style = Stroke(width = lineThickness)
                )
            }

            CrosshairStyle.TACTICAL_CIRCLE -> {
                val circleR = radius * 0.55f
                if (config.outlineEnabled) {
                    drawCircle(
                        color = outlineColor,
                        radius = circleR,
                        center = center,
                        style = Stroke(width = outlineThickness)
                    )
                    drawCrossLines(this, center, radius, circleR + gap * 0.5f, outlineThickness, outlineColor, config.tStyle)
                }
                drawCircle(
                    color = baseColor,
                    radius = circleR,
                    center = center,
                    style = Stroke(width = lineThickness)
                )
                drawCrossLines(this, center, radius, circleR + gap * 0.5f, lineThickness, baseColor, config.tStyle)
            }

            CrosshairStyle.MIL_DOT_SNIPER -> {
                if (config.outlineEnabled) {
                    drawCrossLines(this, center, radius, gap, outlineThickness, outlineColor, false)
                }
                drawCrossLines(this, center, radius, gap, lineThickness, baseColor, false)

                // Draw Mil-Dots along bars
                val step = radius / 4f
                for (i in 1..3) {
                    val d = gap + i * (radius - gap) / 4f
                    if (d < radius) {
                        drawCircle(baseColor, radius = lineThickness * 0.9f, center = Offset(center.x + d, center.y))
                        drawCircle(baseColor, radius = lineThickness * 0.9f, center = Offset(center.x - d, center.y))
                        drawCircle(baseColor, radius = lineThickness * 0.9f, center = Offset(center.x, center.y + d))
                    }
                }
            }

            CrosshairStyle.CYBER_HEX -> {
                val hexR = radius * 0.65f
                val path = createPolygonPath(center, hexR, 6)
                if (config.outlineEnabled) {
                    drawPath(path, outlineColor, style = Stroke(width = outlineThickness, join = StrokeJoin.Round))
                }
                drawPath(path, baseColor, style = Stroke(width = lineThickness, join = StrokeJoin.Round))
                // Corner accents
                for (i in 0 until 6) {
                    val angle = Math.toRadians((i * 60).toDouble())
                    val p1 = Offset(center.x + (hexR * cos(angle)).toFloat(), center.y + (hexR * sin(angle)).toFloat())
                    val p2 = Offset(center.x + ((hexR + gap) * cos(angle)).toFloat(), center.y + ((hexR + gap) * sin(angle)).toFloat())
                    drawLine(baseColor, p1, p2, strokeWidth = lineThickness, cap = StrokeCap.Round)
                }
            }

            CrosshairStyle.CHEVRON_ARROW -> {
                val path = Path().apply {
                    moveTo(center.x - radius * 0.5f, center.y + radius * 0.35f)
                    lineTo(center.x, center.y - radius * 0.35f)
                    lineTo(center.x + radius * 0.5f, center.y + radius * 0.35f)
                }
                if (config.outlineEnabled) {
                    drawPath(path, outlineColor, style = Stroke(width = outlineThickness, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                drawPath(path, baseColor, style = Stroke(width = lineThickness, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // Bottom anchor line
                drawLine(baseColor, Offset(center.x, center.y + radius * 0.45f), Offset(center.x, center.y + radius), strokeWidth = lineThickness, cap = StrokeCap.Round)
            }

            CrosshairStyle.SHOTGUN_BOX -> {
                val boxHalf = radius * 0.5f
                val bracketLen = radius * 0.25f

                fun drawCorner(x: Float, y: Float, dx: Float, dy: Float) {
                    val p = Path().apply {
                        moveTo(x, y + dy * bracketLen)
                        lineTo(x, y)
                        lineTo(x + dx * bracketLen, y)
                    }
                    if (config.outlineEnabled) {
                        drawPath(p, outlineColor, style = Stroke(width = outlineThickness, cap = StrokeCap.Square, join = StrokeJoin.Miter))
                    }
                    drawPath(p, baseColor, style = Stroke(width = lineThickness, cap = StrokeCap.Square, join = StrokeJoin.Miter))
                }

                drawCorner(center.x - boxHalf - gap, center.y - boxHalf - gap, 1f, 1f)
                drawCorner(center.x + boxHalf + gap, center.y - boxHalf - gap, -1f, 1f)
                drawCorner(center.x + boxHalf + gap, center.y + boxHalf + gap, -1f, -1f)
                drawCorner(center.x - boxHalf - gap, center.y + boxHalf + gap, 1f, -1f)
            }

            CrosshairStyle.DYNAMIC_RING -> {
                val arcR = radius * 0.7f
                val strokeW = lineThickness * 1.2f
                val gapAngle = 24f
                val sweep = (360f - (gapAngle * 4)) / 4f

                for (i in 0 until 4) {
                    val startAngle = i * (sweep + gapAngle) + gapAngle / 2f
                    drawArc(
                        color = baseColor,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - arcR, center.y - arcR),
                        size = Size(arcR * 2, arcR * 2),
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }
            }

            CrosshairStyle.TRI_BLADE -> {
                for (i in 0 until 3) {
                    val angle = Math.toRadians((i * 120 - 90).toDouble())
                    val pStart = Offset(
                        center.x + (gap * cos(angle)).toFloat(),
                        center.y + (gap * sin(angle)).toFloat()
                    )
                    val pEnd = Offset(
                        center.x + (radius * cos(angle)).toFloat(),
                        center.y + (radius * sin(angle)).toFloat()
                    )
                    if (config.outlineEnabled) {
                        drawLine(outlineColor, pStart, pEnd, strokeWidth = outlineThickness, cap = StrokeCap.Round)
                    }
                    drawLine(baseColor, pStart, pEnd, strokeWidth = lineThickness, cap = StrokeCap.Round)
                }
            }

            CrosshairStyle.HOLLOW_DIAMOND -> {
                val diaR = radius * 0.6f
                val path = Path().apply {
                    moveTo(center.x, center.y - diaR - gap)
                    lineTo(center.x + diaR + gap, center.y)
                    lineTo(center.x, center.y + diaR + gap)
                    lineTo(center.x - diaR - gap, center.y)
                    close()
                }
                if (config.outlineEnabled) {
                    drawPath(path, outlineColor, style = Stroke(width = outlineThickness, join = StrokeJoin.Round))
                }
                drawPath(path, baseColor, style = Stroke(width = lineThickness, join = StrokeJoin.Round))
            }

            CrosshairStyle.CROSS_CIRCLE -> {
                val circleR = radius * 0.5f
                if (config.outlineEnabled) {
                    drawCircle(outlineColor, radius = circleR, center = center, style = Stroke(width = outlineThickness))
                    drawCrossLines(this, center, radius, 0f, outlineThickness, outlineColor, false)
                }
                drawCircle(baseColor, radius = circleR, center = center, style = Stroke(width = lineThickness))
                drawCrossLines(this, center, radius, 0f, lineThickness, baseColor, false)
            }

            CrosshairStyle.CORNER_BRACKETS -> {
                val len = radius * 0.35f
                val dist = radius * 0.7f
                fun drawBracket(x: Float, y: Float, dx: Float, dy: Float) {
                    val p = Path().apply {
                        moveTo(x, y - dy * len)
                        lineTo(x, y)
                        lineTo(x - dx * len, y)
                    }
                    if (config.outlineEnabled) {
                        drawPath(p, outlineColor, style = Stroke(width = outlineThickness, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    drawPath(p, baseColor, style = Stroke(width = lineThickness, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                drawBracket(center.x - dist, center.y - dist, -1f, -1f)
                drawBracket(center.x + dist, center.y - dist, 1f, -1f)
                drawBracket(center.x + dist, center.y + dist, 1f, 1f)
                drawBracket(center.x - dist, center.y + dist, -1f, 1f)
            }

            CrosshairStyle.SONAR_RADAR -> {
                val r1 = radius * 0.35f
                val r2 = radius * 0.75f
                drawCircle(baseColor.copy(alpha = baseColor.alpha * 0.5f), radius = r1, center = center, style = Stroke(width = lineThickness * 0.8f))
                drawCircle(baseColor, radius = r2, center = center, style = Stroke(width = lineThickness))
                drawCrossLines(this, center, radius, r1, lineThickness, baseColor, false)
            }

            CrosshairStyle.MICRO_PLUS -> {
                val microLen = radius * 0.45f
                if (config.outlineEnabled) {
                    drawCrossLines(this, center, microLen, 0f, outlineThickness, outlineColor, false)
                }
                drawCrossLines(this, center, microLen, 0f, lineThickness, baseColor, false)
            }

            CrosshairStyle.HEARTBEAT -> {
                val path = Path().apply {
                    moveTo(center.x - radius, center.y)
                    lineTo(center.x - radius * 0.4f, center.y)
                    lineTo(center.x - radius * 0.2f, center.y - radius * 0.45f)
                    lineTo(center.x, center.y + radius * 0.45f)
                    lineTo(center.x + radius * 0.2f, center.y - radius * 0.3f)
                    lineTo(center.x + radius * 0.4f, center.y)
                    lineTo(center.x + radius, center.y)
                }
                if (config.outlineEnabled) {
                    drawPath(path, outlineColor, style = Stroke(width = outlineThickness, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                drawPath(path, baseColor, style = Stroke(width = lineThickness, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }

        // Draw Center Laser Dot if enabled
        if (config.centerDot && dotRadius > 0f) {
            if (config.outlineEnabled) {
                drawCircle(
                    color = outlineColor,
                    radius = dotRadius + (config.outlineThicknessDp * density),
                    center = center,
                    style = Fill
                )
            }
            drawCircle(
                color = baseColor,
                radius = dotRadius,
                center = center,
                style = Fill
            )
        }
    }
}

private fun drawCrossLines(
    scope: DrawScope,
    center: Offset,
    radius: Float,
    gap: Float,
    thickness: Float,
    color: Color,
    skipTop: Boolean
) {
    val cap = StrokeCap.Round

    // Right arm
    scope.drawLine(
        color = color,
        start = Offset(center.x + gap, center.y),
        end = Offset(center.x + radius, center.y),
        strokeWidth = thickness,
        cap = cap
    )

    // Left arm
    scope.drawLine(
        color = color,
        start = Offset(center.x - gap, center.y),
        end = Offset(center.x - radius, center.y),
        strokeWidth = thickness,
        cap = cap
    )

    // Bottom arm
    scope.drawLine(
        color = color,
        start = Offset(center.x, center.y + gap),
        end = Offset(center.x, center.y + radius),
        strokeWidth = thickness,
        cap = cap
    )

    // Top arm (optional)
    if (!skipTop) {
        scope.drawLine(
            color = color,
            start = Offset(center.x, center.y - gap),
            end = Offset(center.x, center.y - radius),
            strokeWidth = thickness,
            cap = cap
        )
    }
}

private fun createPolygonPath(center: Offset, radius: Float, sides: Int): Path {
    val path = Path()
    for (i in 0 until sides) {
        val angle = Math.toRadians((i * (360.0 / sides) - 90).toDouble())
        val x = center.x + (radius * cos(angle)).toFloat()
        val y = center.y + (radius * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
