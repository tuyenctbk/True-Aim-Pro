package com.example.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val CleanMinimalLightColorScheme = lightColorScheme(
    primary = MinimalPurple,
    onPrimary = Color.White,
    primaryContainer = MinimalPurpleLight,
    onPrimaryContainer = MinimalPurpleDark,
    secondary = MinimalPurpleDark,
    onSecondary = Color.White,
    secondaryContainer = MinimalPurpleContainer,
    onSecondaryContainer = MinimalPurpleDark,
    tertiary = MinimalGreen,
    onTertiary = Color.White,
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
)

private val CleanMinimalDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = MinimalGreen,
    onTertiary = Color.Black,
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF1D1B20),
    onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceContainer = Color(0xFF2B2930),
    surfaceContainerHigh = Color(0xFF36343B),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) CleanMinimalDarkColorScheme else CleanMinimalLightColorScheme
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}

fun Modifier.tvFocusable(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp),
    borderColor: Color = Color(0xFF00FF41) // Neon Green
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    this
        .onFocusChanged { isFocused = it.isFocused }
        .focusable()
        .border(
            width = if (isFocused) 2.dp else 0.dp,
            color = if (isFocused) borderColor else Color.Transparent,
            shape = shape
        )
}


