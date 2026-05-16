package org.lingolocal.project.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import org.lingolocal.project.domain.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = LingoColors.PrimaryLight,
    onPrimary = LingoColors.OnPrimaryLight,
    primaryContainer = LingoColors.PrimaryContainerLight,
    onPrimaryContainer = LingoColors.OnPrimaryContainerLight,
    secondary = LingoColors.SecondaryLight,
    onSecondary = LingoColors.OnSecondaryLight,
    background = LingoColors.BackgroundLight,
    onBackground = LingoColors.OnBackgroundLight,
    surface = LingoColors.SurfaceLight,
    onSurface = LingoColors.OnSurfaceLight,
    surfaceVariant = LingoColors.SurfaceVariantLight,
    onSurfaceVariant = LingoColors.OnSurfaceVariantLight,
    outline = LingoColors.OutlineLight,
    error = LingoColors.ErrorLight
)

private val DarkColors = darkColorScheme(
    primary = LingoColors.PrimaryDark,
    onPrimary = LingoColors.OnPrimaryDark,
    primaryContainer = LingoColors.PrimaryContainerDark,
    onPrimaryContainer = LingoColors.OnPrimaryContainerDark,
    secondary = LingoColors.SecondaryDark,
    onSecondary = LingoColors.OnSecondaryDark,
    background = LingoColors.BackgroundDark,
    onBackground = LingoColors.OnBackgroundDark,
    surface = LingoColors.SurfaceDark,
    onSurface = LingoColors.OnSurfaceDark,
    surfaceVariant = LingoColors.SurfaceVariantDark,
    onSurfaceVariant = LingoColors.OnSurfaceVariantDark,
    outline = LingoColors.OutlineDark,
    error = LingoColors.ErrorDark
)

/**
 * Tema dell'app. La scelta tra Light/Dark dipende da [themeMode]:
 * - SYSTEM: segue [isSystemInDarkTheme]
 * - LIGHT / DARK: forzati esplicitamente dall'utente.
 */
@Composable
fun AppTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (useDark) DarkColors else LightColors,
        content = content
    )
}
