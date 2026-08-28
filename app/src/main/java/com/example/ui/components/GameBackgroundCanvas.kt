package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.ui.PreviewBackground
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHigh
import com.example.ui.theme.LaserCrimson
import com.example.ui.theme.NeonGreen

@Composable
fun GameBackgroundCanvas(
    background: PreviewBackground,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        when (background) {
            PreviewBackground.CQB_DEPOT -> {
                // Dark Industrial CQB Warehouse
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0B0F19))
                    )
                )
                // Warehouse steel girders / roof beams
                drawLine(Color(0xFF334155), Offset(0f, 0f), Offset(w * 0.4f, h * 0.4f), strokeWidth = 8f)
                drawLine(Color(0xFF334155), Offset(w, 0f), Offset(w * 0.6f, h * 0.4f), strokeWidth = 8f)
                drawLine(Color(0xFF1E293B), Offset(0f, h * 0.35f), Offset(w, h * 0.35f), strokeWidth = 6f)

                // Shipping containers silhouette
                drawRect(
                    color = Color(0xFF152238),
                    topLeft = Offset(w * 0.05f, h * 0.45f),
                    size = Size(w * 0.35f, h * 0.55f)
                )
                drawRect(
                    color = Color(0xFF2C3E50),
                    topLeft = Offset(w * 0.65f, h * 0.4f),
                    size = Size(w * 0.35f, h * 0.6f)
                )

                // Industrial amber & blue warning lights
                drawCircle(Color(0xFFFFB703).copy(alpha = 0.4f), radius = 24f, center = Offset(w * 0.2f, h * 0.3f))
                drawCircle(Color(0xFF00E5FF).copy(alpha = 0.3f), radius = 18f, center = Offset(w * 0.8f, h * 0.25f))

                // Floor grid
                for (i in 1..4) {
                    val y = h * 0.65f + (i * h * 0.08f)
                    drawLine(Color(0xFF1E293B).copy(alpha = 0.6f), Offset(0f, y), Offset(w, y), strokeWidth = 2f)
                }
            }

            PreviewBackground.DESERT_OUTPOST -> {
                // Desert Mountain military outpost
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF78909C), Color(0xFFD7CCC8), Color(0xFFC7A779))
                    )
                )
                // Mountains in distance
                val mtnPath = Path().apply {
                    moveTo(0f, h * 0.55f)
                    lineTo(w * 0.25f, h * 0.35f)
                    lineTo(w * 0.5f, h * 0.48f)
                    lineTo(w * 0.75f, h * 0.32f)
                    lineTo(w, h * 0.5f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(mtnPath, Color(0xFF8D6E63))

                // Watchtower silhouette
                drawRect(Color(0xFF455A64), Offset(w * 0.7f, h * 0.38f), Size(w * 0.08f, h * 0.22f))
                drawLine(Color(0xFF37474F), Offset(w * 0.68f, h * 0.38f), Offset(w * 0.8f, h * 0.38f), strokeWidth = 4f)

                // Sun
                drawCircle(Color(0xFFFFF9C4).copy(alpha = 0.8f), radius = 36f, center = Offset(w * 0.82f, h * 0.18f))
            }

            PreviewBackground.CYBER_NIGHT -> {
                // Cyberpunk City Night Skyline
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF050510), Color(0xFF0F0B24), Color(0xFF1B0B2E))
                    )
                )
                // Skyscraper buildings
                drawRect(Color(0xFF0A0F24), Offset(w * 0.05f, h * 0.35f), Size(w * 0.2f, h * 0.65f))
                drawRect(Color(0xFF140D2E), Offset(w * 0.3f, h * 0.25f), Size(w * 0.25f, h * 0.75f))
                drawRect(Color(0xFF0D1B2A), Offset(w * 0.6f, h * 0.3f), Size(w * 0.35f, h * 0.7f))

                // Neon glow lines
                drawLine(CyberCyan, Offset(w * 0.3f, h * 0.25f), Offset(w * 0.55f, h * 0.25f), strokeWidth = 4f)
                drawLine(LaserCrimson, Offset(w * 0.05f, h * 0.35f), Offset(w * 0.25f, h * 0.35f), strokeWidth = 3f)
                drawLine(NeonGreen, Offset(w * 0.6f, h * 0.3f), Offset(w * 0.95f, h * 0.3f), strokeWidth = 3f)

                // Cyber grid floor
                for (i in 1..5) {
                    val y = h * 0.7f + (i * h * 0.06f)
                    drawLine(CyberCyan.copy(alpha = 0.3f), Offset(0f, y), Offset(w, y), strokeWidth = 1.5f)
                }
            }

            PreviewBackground.TARGET_RANGE -> {
                // IPSC Paper Target Range
                drawRect(Color(0xFF263238))

                // Target Stand
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFD7CCC8), Color(0xFFBCAAA4))
                    ),
                    topLeft = Offset(w * 0.22f, h * 0.15f),
                    size = Size(w * 0.56f, h * 0.7f)
                )

                val targetCenter = Offset(w * 0.5f, h * 0.5f)
                // Head zone
                drawRect(
                    color = Color(0xFFA1887F),
                    topLeft = Offset(w * 0.38f, h * 0.15f),
                    size = Size(w * 0.24f, h * 0.18f)
                )
                // Headshot A-Box
                drawRect(
                    color = Color(0xFF5D4037),
                    topLeft = Offset(w * 0.42f, h * 0.18f),
                    size = Size(w * 0.16f, h * 0.11f),
                    style = Stroke(width = 3f)
                )

                // Body Bullseye Rings
                drawCircle(Color(0xFF8D6E63), radius = w * 0.22f, center = targetCenter, style = Stroke(width = 3f))
                drawCircle(Color(0xFF6D4C41), radius = w * 0.15f, center = targetCenter, style = Stroke(width = 3f))
                drawCircle(Color(0xFF4E342E), radius = w * 0.08f, center = targetCenter, style = Stroke(width = 3f))
                drawCircle(Color(0xFFFF1744).copy(alpha = 0.8f), radius = w * 0.03f, center = targetCenter, style = Fill)
            }

            PreviewBackground.SNIPER_MOUNTAIN -> {
                // Mountain Range Sniper View
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF455A64), Color(0xFF607D8B), Color(0xFF37474F))
                    )
                )
                // Distance mountains
                val path = Path().apply {
                    moveTo(0f, h * 0.5f)
                    lineTo(w * 0.3f, h * 0.35f)
                    lineTo(w * 0.6f, h * 0.45f)
                    lineTo(w * 0.85f, h * 0.3f)
                    lineTo(w, h * 0.42f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(path, Color(0xFF263238))

                // Horizon and range markers
                drawLine(Color(0xFF78909C).copy(alpha = 0.5f), Offset(0f, h * 0.5f), Offset(w, h * 0.5f), strokeWidth = 1.5f)
            }

            PreviewBackground.TACTICAL_DARK -> {
                // Carbon Dark Grid
                drawRect(DarkCanvas)
                val step = 32f
                var x = 0f
                while (x < w) {
                    drawLine(DarkSurfaceHigh.copy(alpha = 0.5f), Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                    x += step
                }
                var y = 0f
                while (y < h) {
                    drawLine(DarkSurfaceHigh.copy(alpha = 0.5f), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                    y += step
                }

                // Center cross guides
                drawLine(CyberCyan.copy(alpha = 0.25f), Offset(w / 2f, 0f), Offset(w / 2f, h), strokeWidth = 1.5f)
                drawLine(CyberCyan.copy(alpha = 0.25f), Offset(0f, h / 2f), Offset(w, h / 2f), strokeWidth = 1.5f)
                drawCircle(CyberCyan.copy(alpha = 0.15f), radius = w * 0.2f, center = Offset(w / 2f, h / 2f), style = Stroke(width = 1.5f))
            }
        }
    }
}
