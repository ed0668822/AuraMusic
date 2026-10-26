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

// ... (tus esquemas de color DarkColorScheme y LightColorScheme sin cambios) ...

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
