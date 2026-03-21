package com.emanueledipietro.remodex.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.emanueledipietro.remodex.model.RemodexAppearanceMode

private val LightScheme = lightColorScheme(
    primary = OliveLight,
    onPrimary = SurfaceLight,
    primaryContainer = SandLight,
    onPrimaryContainer = InkLight,
    secondary = RustLight,
    onSecondary = SurfaceLight,
    secondaryContainer = SurfaceAltLight,
    onSecondaryContainer = InkLight,
    tertiary = RustLight,
    onTertiary = SurfaceLight,
    tertiaryContainer = Color(0xFFFFD9C8),
    onTertiaryContainer = InkLight,
    background = CanvasLight,
    onBackground = InkLight,
    surface = SurfaceLight,
    onSurface = InkLight,
    surfaceVariant = SurfaceAltLight,
    onSurfaceVariant = Color(0xFF5B5F68),
)

private val DarkScheme = darkColorScheme(
    primary = OliveDark,
    onPrimary = CanvasDark,
    primaryContainer = SandDark,
    onPrimaryContainer = InkDark,
    secondary = RustDark,
    onSecondary = CanvasDark,
    secondaryContainer = SurfaceAltDark,
    onSecondaryContainer = InkDark,
    tertiary = RustDark,
    onTertiary = CanvasDark,
    tertiaryContainer = Color(0xFF563528),
    onTertiaryContainer = InkDark,
    background = CanvasDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    surfaceVariant = SurfaceAltDark,
    onSurfaceVariant = Color(0xFFCAC5BC),
)

@Composable
fun RemodexTheme(
    appearanceMode: RemodexAppearanceMode = RemodexAppearanceMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (appearanceMode) {
        RemodexAppearanceMode.SYSTEM -> isSystemInDarkTheme()
        RemodexAppearanceMode.LIGHT -> false
        RemodexAppearanceMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = RemodexTypography,
        content = content,
    )
}
