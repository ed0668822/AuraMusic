package com.music.aura.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.music.aura.data.SettingsRepository

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
)

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
)

@Composable
fun AuraTheme(
    content: @Composable () -> Unit,
) {
    val themeMode by SettingsRepository.themeMode.collectAsState()
    val darkTheme = when (themeMode) {
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
