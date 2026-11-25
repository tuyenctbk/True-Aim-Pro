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
        drawCrosshairOnScope(
            scope = this,
            config = config,
            scaleStrokesToCanvas = overrideSize != null
        )
    }
}

fun drawCrosshairOnScope(
    scope: DrawScope,
    config: CrosshairConfig,
    scaleStrokesToCanvas: Boolean = false
) {
    val center = scope.center
    val safeOpacity = config.opacity.coerceIn(0.05f, 1.0f)
    val baseColor = Color(config.primaryColorHex or 0xFF000000L).copy(alpha = safeOpacity)
    val outlineColor = Color(config.outlineColorHex or 0xFF000000L).copy(alpha = safeOpacity)

    val density = scope.drawContext.density.density
    val minDim = minOf(scope.size.width, scope.size.height)
    val radius = minDim / 2f
    if (radius <= 0f) return

    // Proportionally scale strokes/gaps when rendering inside compact thumbnail previews
    val scale = if (scaleStrokesToCanvas && config.sizeDp > 0f) {
        (minDim / (config.sizeDp * density)).coerceIn(0.38f, 1.5f)
    } else {
        1f
    }

    val lineThickness = (config.thicknessDp * density * scale).coerceAtLeast(1f)
    val outlinePad = (config.outlineThicknessDp * density * scale).coerceAtLeast(0.75f)
    val outlineThickness = lineThickness + outlinePad * 2f
    val gap = (config.gapDp * density * scale).coerceIn(0f, radius * 0.78f)
    val dotRadius = (config.centerDotSizeDp * density * scale / 2f).coerceAtMost(radius * 0.45f)

    scope.rotate(degrees = config.rotationDeg, pivot = center) {
        when (config.style) {
            CrosshairStyle.CLASSIC_CROSS, CrosshairStyle.T_SHAPE -> {
                val isT = config.tStyle || config.style == CrosshairStyle.T_SHAPE

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
                val outerRingRadius = (radius * 0.7f).coerceAtLeast(dotRadius + lineThickness)
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
                val circleR = radius * 0.52f
                val armStart = (circleR + gap * 0.45f).coerceAtMost(radius)
                val drawArms = armStart < radius - lineThickness * 0.4f
                if (config.outlineEnabled) {
                    drawCircle(
                        color = outlineColor,
                        radius = circleR,
                        center = center,
                        style = Stroke(width = outlineThickness)
                    )
                    if (drawArms) {
                        drawCrossLines(this, center, radius, armStart, outlineThickness, outlineColor, config.tStyle)
                    }
                }
                drawCircle(
                    color = baseColor,
                    radius = circleR,
                    center = center,
                    style = Stroke(width = lineThickness)
                )
                if (drawArms) {
                    drawCrossLines(this, center, radius, armStart, lineThickness, baseColor, config.tStyle)
                }
            }

            CrosshairStyle.MIL_DOT_SNIPER -> {
                if (config.outlineEnabled) {
                    drawCrossLines(this, center, radius, gap, outlineThickness, outlineColor, false)
                }
                drawCrossLines(this, center, radius, gap, lineThickness, baseColor, false)

                val milRadius = lineThickness * 0.85f
                for (i in 1..3) {
                    val d = gap + i * (radius - gap) / 4f
                    if (d < radius - milRadius) {
                        val pts = listOf(
                            Offset(center.x + d, center.y),
                            Offset(center.x - d, center.y),
                            Offset(center.x, center.y + d),
                            Offset(center.x, center.y - d)
                        )
                        if (config.outlineEnabled) {
                            pts.forEach { pt ->
                                drawCircle(outlineColor, radius = milRadius + outlinePad * 0.75f, center = pt)
                            }
                        }
                        pts.forEach { pt ->
                            drawCircle(baseColor, radius = milRadius, center = pt)
                        }
                    }
                }
            }

            CrosshairStyle.CYBER_HEX -> {
                val hexR = radius * 0.62f
                val outerSpikeR = (hexR + gap * 0.6f).coerceAtMost(radius - lineThickness * 0.5f)
                val path = createPolygonPath(center, hexR, 6)
                if (config.outlineEnabled) {
                    drawPath(path, outlineColor, style = Stroke(width = outlineThickness, join = StrokeJoin.Round))
                }
                drawPath(path, baseColor, style = Stroke(width = lineThickness, join = StrokeJoin.Round))

                // Aligned precisely with hexagon vertices (i * 60 - 90)
                if (outerSpikeR > hexR + 1f) {
                    for (i in 0 until 6) {
                        val angle = Math.toRadians((i * 60 - 90).toDouble())
                        val p1 = Offset(center.x + (hexR * cos(angle)).toFloat(), center.y + (hexR * sin(angle)).toFloat())
                        val p2 = Offset(center.x + (outerSpikeR * cos(angle)).toFloat(), center.y + (outerSpikeR * sin(angle)).toFloat())
                        if (config.outlineEnabled) {
                            drawLine(outlineColor, p1, p2, strokeWidth = outlineThickness, cap = StrokeCap.Round)
                        }
                        drawLine(baseColor, p1, p2, strokeWidth = lineThickness, cap = StrokeCap.Round)
                    }
                }
            }

            CrosshairStyle.CHEVRON_ARROW -> {
                val path = Path().apply {
                    moveTo(center.x - radius * 0.5f, center.y + radius * 0.35f)
                    lineTo(center.x, center.y - radius * 0.35f)
                    lineTo(center.x + radius * 0.5f, center.y + radius * 0.35f)
                }
                val anchorStart = Offset(center.x, center.y + radius * 0.45f)
                val anchorEnd = Offset(center.x, center.y + radius * 0.92f)
                if (config.outlineEnabled) {
                    drawPath(path, outlineColor, style = Stroke(width = outlineThickness, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawLine(outlineColor, anchorStart, anchorEnd, strokeWidth = outlineThickness, cap = StrokeCap.Round)
                }
                drawPath(path, baseColor, style = Stroke(width = lineThickness, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLine(baseColor, anchorStart, anchorEnd, strokeWidth = lineThickness, cap = StrokeCap.Round)
            }

            CrosshairStyle.SHOTGUN_BOX -> {
                val boxHalf = radius * 0.48f
                val extent = (boxHalf + gap * 0.45f).coerceAtMost((radius - outlineThickness * 0.5f).coerceAtLeast(boxHalf))
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

                drawCorner(center.x - extent, center.y - extent, 1f, 1f)
                drawCorner(center.x + extent, center.y - extent, -1f, 1f)
                drawCorner(center.x + extent, center.y + extent, -1f, -1f)
                drawCorner(center.x - extent, center.y + extent, 1f, -1f)
            }

            CrosshairStyle.DYNAMIC_RING -> {
                val arcR = radius * 0.7f
                val strokeW = lineThickness * 1.15f
                val outlineW = strokeW + outlinePad * 2f
                val gapAngle = 24f
                val sweep = (360f - (gapAngle * 4)) / 4f

                for (i in 0 until 4) {
                    val startAngle = i * (sweep + gapAngle) + gapAngle / 2f
                    if (config.outlineEnabled) {
                        drawArc(
                            color = outlineColor,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = Offset(center.x - arcR, center.y - arcR),
                            size = Size(arcR * 2, arcR * 2),
                            style = Stroke(width = outlineW, cap = StrokeCap.Round)
                        )
                    }
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
                val diaR = radius * 0.56f
                val extent = (diaR + gap * 0.4f).coerceAtMost((radius - outlineThickness * 0.5f).coerceAtLeast(diaR))
                val path = Path().apply {
                    moveTo(center.x, center.y - extent)
                    lineTo(center.x + extent, center.y)
                    lineTo(center.x, center.y + extent)
                    lineTo(center.x - extent, center.y)
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
                if (config.outlineEnabled) {
                    drawCircle(outlineColor, radius = r2, center = center, style = Stroke(width = outlineThickness))
                    drawCrossLines(this, center, radius, r1, outlineThickness, outlineColor, false)
                }
                drawCircle(baseColor.copy(alpha = baseColor.alpha * 0.55f), radius = r1, center = center, style = Stroke(width = lineThickness * 0.8f))
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
                    radius = dotRadius + outlinePad,
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
    if (gap >= radius) return
    val cap = StrokeCap.Round

    scope.drawLine(
        color = color,
        start = Offset(center.x + gap, center.y),
        end = Offset(center.x + radius, center.y),
        strokeWidth = thickness,
        cap = cap
    )

    scope.drawLine(
        color = color,
        start = Offset(center.x - gap, center.y),
        end = Offset(center.x - radius, center.y),
        strokeWidth = thickness,
        cap = cap
    )

    scope.drawLine(
        color = color,
        start = Offset(center.x, center.y + gap),
        end = Offset(center.x, center.y + radius),
        strokeWidth = thickness,
        cap = cap
    )

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
