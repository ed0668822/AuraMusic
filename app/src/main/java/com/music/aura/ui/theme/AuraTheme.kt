package com.music.aura.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.music.aura.data.SettingsRepository

/**
 * Paleta oscura de Aura Music.
 */
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB4A7F5),
    onPrimary = Color(0xFF1F1147),
    primaryContainer = Color(0xFF3B2D6B),
    onPrimaryContainer = Color(0xFFE8DEFF),
    secondary = Color(0xFF8ECFF5),
    onSecondary = Color(0xFF00344B),
    secondaryContainer = Color(0xFF004C6B),
    onSecondaryContainer = Color(0xFFC8E9FF),
    tertiary = Color(0xFFF5A9B8),
    onTertiary = Color(0xFF4D1226),
    tertiaryContainer = Color(0xFF67293C),
    onTertiaryContainer = Color(0xFFFFD9E0),
    background = Color(0xFF0E0E14),
    onBackground = Color(0xFFE4E1EC),
    surface = Color(0xFF0E0E14),
    onSurface = Color(0xFFE4E1EC),
    surfaceVariant = Color(0xFF2A2833),
    onSurfaceVariant = Color(0xFFC8C4D4),
    outline = Color(0xFF928E9E),
    outlineVariant = Color(0xFF47454F),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE4E1EC),
    inverseOnSurface = Color(0xFF2B2930),
    inversePrimary = Color(0xFF5B4A9E),
    surfaceDim = Color(0xFF0E0E14),
    surfaceBright = Color(0xFF34323B),
    surfaceContainerLowest = Color(0xFF09090F),
    surfaceContainerLow = Color(0xFF1A1920),
    surfaceContainer = Color(0xFF1E1D25),
    surfaceContainerHigh = Color(0xFF28272F),
    surfaceContainerHighest = Color(0xFF33323A),
)

/**
 * Paleta clara de Aura Music.
 */
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF5B4A9E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8DEFF),
    onPrimaryContainer = Color(0xFF1F1147),
    secondary = Color(0xFF00658A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC8E9FF),
    onSecondaryContainer = Color(0xFF001E2C),
    tertiary = Color(0xFF834257),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9E0),
    onTertiaryContainer = Color(0xFF350E1F),
    background = Color(0xFFFDF8FF),
    onBackground = Color(0xFF1A1B20),
    surface = Color(0xFFFDF8FF),
    onSurface = Color(0xFF1A1B20),
    surfaceVariant = Color(0xFFE6E0EC),
    onSurfaceVariant = Color(0xFF47454F),
    outline = Color(0xFF787580),
    outlineVariant = Color(0xFFC8C4D4),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF2B2930),
    inverseOnSurface = Color(0xFFF1EFF7),
    inversePrimary = Color(0xFFB4A7F5),
    surfaceDim = Color(0xFFDDD8E0),
    surfaceBright = Color(0xFFFDF8FF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F2FA),
    surfaceContainer = Color(0xFFF1ECF4),
    surfaceContainerHigh = Color(0xFFEBE7EE),
    surfaceContainerHighest = Color(0xFFE6E1E9),
)

/**
 * Tema principal de Aura Music.
 * Respeta la preferencia del usuario guardada en SettingsRepository:
 * 0 = Oscuro, 1 = Claro, 2 = Seguir sistema.
 */
@Composable
fun AuraTheme(
    content: @Composable () -> Unit,
) {
    val darkTheme = when (SettingsRepository.themeMode) {
        0 -> true
        1 -> false
        2 -> isSystemInDarkTheme()
        else -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
