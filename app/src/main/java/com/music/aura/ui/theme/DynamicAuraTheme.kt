package com.music.aura.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Tema dinámico de Aura Music.
 * Genera un ColorScheme Material 3 a partir de ArtworkColors.
 *
 * Si [colors] es null, usa la paleta oscura por defecto.
 */
@Composable
fun DynamicAuraTheme(
    colors: ArtworkColors?,
    content: @Composable () -> Unit
) {
    val colorScheme = remember(colors) {
        if (colors == null) {
            // Fallback: paleta oscura por defecto
            darkColorScheme(
                primary = androidx.compose.ui.graphics.Color(0xFFB4A7F5),
                onPrimary = androidx.compose.ui.graphics.Color(0xFF1F1147),
                primaryContainer = androidx.compose.ui.graphics.Color(0xFF3B2D6B),
                onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFE8DEFF),
                secondary = androidx.compose.ui.graphics.Color(0xFF8ECFF5),
                onSecondary = androidx.compose.ui.graphics.Color(0xFF00344B),
                secondaryContainer = androidx.compose.ui.graphics.Color(0xFF004C6B),
                onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFC8E9FF),
                tertiary = androidx.compose.ui.graphics.Color(0xFFF5A9B8),
                onTertiary = androidx.compose.ui.graphics.Color(0xFF4D1226),
                tertiaryContainer = androidx.compose.ui.graphics.Color(0xFF67293C),
                onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFFFFD9E0),
                background = androidx.compose.ui.graphics.Color(0xFF0E0E14),
                onBackground = androidx.compose.ui.graphics.Color(0xFFE4E1EC),
                surface = androidx.compose.ui.graphics.Color(0xFF0E0E14),
                onSurface = androidx.compose.ui.graphics.Color(0xFFE4E1EC),
                surfaceVariant = androidx.compose.ui.graphics.Color(0xFF2A2833),
                onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFC8C4D4),
                outline = androidx.compose.ui.graphics.Color(0xFF928E9E),
                outlineVariant = androidx.compose.ui.graphics.Color(0xFF47454F),
                scrim = androidx.compose.ui.graphics.Color(0xFF000000),
                inverseSurface = androidx.compose.ui.graphics.Color(0xFFE4E1EC),
                inverseOnSurface = androidx.compose.ui.graphics.Color(0xFF2B2930),
                inversePrimary = androidx.compose.ui.graphics.Color(0xFF5B4A9E),
                surfaceDim = androidx.compose.ui.graphics.Color(0xFF0E0E14),
                surfaceBright = androidx.compose.ui.graphics.Color(0xFF34323B),
                surfaceContainerLowest = androidx.compose.ui.graphics.Color(0xFF09090F),
                surfaceContainerLow = androidx.compose.ui.graphics.Color(0xFF1A1920),
                surfaceContainer = androidx.compose.ui.graphics.Color(0xFF1E1D25),
                surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFF28272F),
                surfaceContainerHighest = androidx.compose.ui.graphics.Color(0xFF33323A),
            )
        } else {
            darkColorScheme(
                primary = colors.primary,
                onPrimary = colors.onPrimary,
                primaryContainer = colors.primaryContainer,
                onPrimaryContainer = colors.onPrimaryContainer,
                secondary = colors.secondary,
                onSecondary = colors.onSecondary,
                secondaryContainer = colors.secondaryContainer,
                onSecondaryContainer = colors.onSecondaryContainer,
                tertiary = colors.primary.copy(alpha = 0.8f),
                onTertiary = colors.onPrimary,
                tertiaryContainer = colors.primaryContainer.copy(alpha = 0.8f),
                onTertiaryContainer = colors.onPrimaryContainer,
                background = colors.background,
                onBackground = colors.onBackground,
                surface = colors.surface,
                onSurface = colors.onSurface,
                surfaceVariant = colors.surfaceVariant,
                onSurfaceVariant = colors.onSurfaceVariant,
                outline = colors.onSurfaceVariant.copy(alpha = 0.5f),
                outlineVariant = colors.surfaceVariant,
                scrim = androidx.compose.ui.graphics.Color(0xFF000000),
                inverseSurface = colors.onSurface,
                inverseOnSurface = colors.surface,
                inversePrimary = colors.primaryContainer,
                surfaceDim = colors.background,
                surfaceBright = colors.surfaceVariant,
                surfaceContainerLowest = colors.background,
                surfaceContainerLow = colors.surface,
                surfaceContainer = colors.surface,
                surfaceContainerHigh = colors.surfaceVariant,
                surfaceContainerHighest = colors.surfaceVariant.copy(alpha = 0.8f),
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
