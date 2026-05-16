package org.lingolocal.project.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette LingoLocal: accento giallo caldo, sfondi neutri (bianco / quasi nero).
 * Pensata per leggibilità su entrambi i temi.
 */
internal object LingoColors {
    // Light theme
    val PrimaryLight = Color(0xFFE5B800)            // mustard, leggibile su bianco
    val OnPrimaryLight = Color(0xFF1F1A00)
    val PrimaryContainerLight = Color(0xFFFFF1AA)
    val OnPrimaryContainerLight = Color(0xFF1F1500)
    val SecondaryLight = Color(0xFF6B5B3F)
    val OnSecondaryLight = Color(0xFFFFFFFF)
    val BackgroundLight = Color(0xFFFFFFFF)
    val OnBackgroundLight = Color(0xFF1B1B1F)
    val SurfaceLight = Color(0xFFFFFFFF)
    val OnSurfaceLight = Color(0xFF1B1B1F)
    val SurfaceVariantLight = Color(0xFFF5F1E4)
    val OnSurfaceVariantLight = Color(0xFF4B4838)
    val OutlineLight = Color(0xFF7C7866)
    val ErrorLight = Color(0xFFBA1A1A)

    // Dark theme
    val PrimaryDark = Color(0xFFFFD740)             // giallo vivido su sfondo scuro
    val OnPrimaryDark = Color(0xFF2A2300)
    val PrimaryContainerDark = Color(0xFF4D3F00)
    val OnPrimaryContainerDark = Color(0xFFFFE07A)
    val SecondaryDark = Color(0xFFD8C49E)
    val OnSecondaryDark = Color(0xFF3A2E15)
    val BackgroundDark = Color(0xFF121212)
    val OnBackgroundDark = Color(0xFFE6E1E5)
    val SurfaceDark = Color(0xFF1B1B1F)
    val OnSurfaceDark = Color(0xFFE6E1E5)
    val SurfaceVariantDark = Color(0xFF302E26)
    val OnSurfaceVariantDark = Color(0xFFCCC6B0)
    val OutlineDark = Color(0xFF96907D)
    val ErrorDark = Color(0xFFFFB4AB)
}
