package net.dege.salmon.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SalmonColorScheme = darkColorScheme(
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

@Composable
fun SalmonTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = SalmonColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
