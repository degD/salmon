package net.dege.salmon.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SalmonPrimary,
    onPrimary = SalmonOnPrimary,
    primaryContainer = SalmonPrimaryContainer,
    onPrimaryContainer = SalmonOnPrimaryContainer,
    secondary = SalmonSecondary,
    onSecondary = SalmonOnSecondary,
    secondaryContainer = SalmonSecondaryContainer,
    onSecondaryContainer = SalmonOnSecondaryContainer,
    tertiary = SalmonTertiary,
    onTertiary = SalmonOnTertiary,
    tertiaryContainer = SalmonTertiaryContainer,
    onTertiaryContainer = SalmonOnTertiaryContainer,
    background = SalmonBackground,
    onBackground = SalmonOnBackground,
    surface = SalmonSurface,
    onSurface = SalmonOnSurface,
    surfaceVariant = SalmonSurfaceVariant,
    onSurfaceVariant = SalmonOnSurfaceVariant,
    outline = SalmonOutline,
    outlineVariant = SalmonOutlineVariant,
    inverseSurface = SalmonInverseSurface,
    inverseOnSurface = SalmonInverseOnSurface,
    inversePrimary = SalmonInversePrimary,
    surfaceContainerHighest = SalmonSurfaceContainerHighest,
    surfaceContainerHigh = SalmonSurfaceContainerHigh,
    surfaceContainer = SalmonSurfaceContainer,
    surfaceContainerLow = SalmonSurfaceContainerLow,
    surfaceContainerLowest = SalmonSurfaceContainerLowest,
)

private val LightColorScheme = lightColorScheme(
    primary = SalmonPrimary,
    onPrimary = SalmonOnPrimary,
    primaryContainer = SalmonLightPrimaryContainer,
    onPrimaryContainer = SalmonLightOnPrimaryContainer,
    secondary = SalmonSecondary,
    onSecondary = SalmonOnSecondary,
    secondaryContainer = SalmonLightSecondaryContainer,
    onSecondaryContainer = SalmonLightOnSecondaryContainer,
    tertiary = SalmonTertiary,
    onTertiary = SalmonOnTertiary,
    tertiaryContainer = SalmonLightTertiaryContainer,
    onTertiaryContainer = SalmonLightOnTertiaryContainer,
    background = SalmonLightBackground,
    onBackground = SalmonLightOnBackground,
    surface = SalmonLightSurface,
    onSurface = SalmonLightOnSurface,
    surfaceVariant = SalmonLightSurfaceVariant,
    onSurfaceVariant = SalmonLightOnSurfaceVariant,
    outline = SalmonLightOutline,
    outlineVariant = SalmonLightOutlineVariant,
    inverseSurface = SalmonBackground,
    inverseOnSurface = SalmonOnBackground,
    inversePrimary = SalmonPrimary,
    surfaceContainerHighest = SalmonLightSurfaceContainerHighest,
    surfaceContainerHigh = SalmonLightSurfaceContainerHigh,
    surfaceContainer = SalmonLightSurfaceContainer,
    surfaceContainerLow = SalmonLightSurfaceContainerLow,
    surfaceContainerLowest = SalmonLightSurfaceContainerLowest,
)

@Composable
fun SalmonTheme(
    content: @Composable () -> Unit
) {
    val isDark = when (AppTheme.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
