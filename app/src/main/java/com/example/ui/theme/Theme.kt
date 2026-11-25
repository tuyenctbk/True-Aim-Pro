package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

fun Modifier.tvFocusable(
    shape: Shape,
    focusColor: Color = NeonGreen
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    this
        .onFocusChanged { isFocused = it.isFocused }
        .focusable()
        .then(
            if (isFocused) Modifier.border(2.5.dp, focusColor, shape) else Modifier
        )
}

private val TacticalLightColorScheme = lightColorScheme(
    primary = MinimalPurple,
    onPrimary = Color.White,
    primaryContainer = MinimalPurpleLight,
    onPrimaryContainer = MinimalPurpleDark,
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF075985),
    tertiary = MinimalAmber,
    onTertiary = Color.White,
    tertiaryContainer = MinimalAmberContainer,
    onTertiaryContainer = Color(0xFF78350F),
    background = MinimalCanvas,
    onBackground = TextPrimary,
    surface = MinimalSurface,
    onSurface = TextPrimary,
    surfaceVariant = MinimalSurfaceCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = MinimalSurfaceContainer,
    surfaceContainerHigh = MinimalSurfaceHigh,
    outline = MinimalBorder,
    outlineVariant = MinimalBorderSubtle,
    error = LaserCrimson,
    onError = Color.White
)

private val TacticalDarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = Color(0xFF042615),
    primaryContainer = Color(0xFF0D3324),
    onPrimaryContainer = Color(0xFF6EE7B7),
    secondary = CyanLaser,
    onSecondary = Color(0xFF002430),
    secondaryContainer = Color(0xFF0E2A3A),
    onSecondaryContainer = Color(0xFF7DD3FC),
    tertiary = SolarYellow,
    onTertiary = Color(0xFF2B2300),
    tertiaryContainer = Color(0xFF332B08),
    onTertiaryContainer = Color(0xFFFDE68A),
    background = ObsidianCanvas,
    onBackground = ObsidianTextPrimary,
    surface = ObsidianSurface,
    onSurface = ObsidianTextPrimary,
    surfaceVariant = ObsidianSurfaceContainer,
    onSurfaceVariant = ObsidianTextSecondary,
    surfaceContainer = ObsidianSurfaceContainer,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    outline = ObsidianBorder,
    outlineVariant = Color(0xFF1F2B47),
    error = Color(0xFFFF526B),
    onError = Color(0xFF2B0008)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) TacticalDarkColorScheme else TacticalLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                @Suppress("DEPRECATION")
                window.statusBarColor = Color.Transparent.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = Color.Transparent.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
