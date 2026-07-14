package com.medfusion.ai.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Blue600,
    onPrimary = SurfaceElevated,
    primaryContainer = Blue100,
    onPrimaryContainer = Blue700,
    secondary = Teal500,
    onSecondary = SurfaceElevated,
    secondaryContainer = Teal100,
    onSecondaryContainer = Color(0xFF06403F),
    background = Surface,
    onBackground = OnSurface,
    surface = SurfaceElevated,
    onSurface = OnSurface,
    surfaceVariant = Blue50,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = Outline,
    error = ErrorRed,
    onError = SurfaceElevated,
)

private val DarkColors = darkColorScheme(
    primary = Blue500,
    onPrimary = Color(0xFF06213F),
    primaryContainer = Blue700,
    onPrimaryContainer = Blue100,
    secondary = Teal500,
    onSecondary = Color(0xFF06403F),
    secondaryContainer = Color(0xFF0A5C5C),
    onSecondaryContainer = Teal100,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceElevatedDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = Color(0xFF1B2740),
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineDark,
    error = ErrorRed,
    onError = SurfaceElevated,
)

@Composable
fun MedFusionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalSemanticColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MedFusionTypography,
            shapes = MedFusionShapes,
            content = content,
        )
    }
}
