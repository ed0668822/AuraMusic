package com.music.aurelis.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.music.aurelis.data.SettingsRepository

// Apple Music's signature red. No longer the primary accent, but kept for the
// spots (Replay's rank badge) that want that specific red regardless of theme.
val AccentRed = Color(0xFFFA2D48)

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

/**
 * Heavy, tight typography — the backbone of the Apple Music look. Bitchord
 * ships SF Pro Display; Aurelis uses the system default font family with the same
 * weights and letter-spacing so the layout reads identically.
 */
private val AurelisTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.W800, fontSize = 34.sp, letterSpacing = (-0.8).sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.W800, fontSize = 30.sp, letterSpacing = (-0.7).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.W700, fontSize = 22.sp, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.W700, fontSize = 20.sp, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.W600, fontSize = 16.sp, letterSpacing = (-0.2).sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.W400, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.W400, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.W600, fontSize = 12.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.W600, fontSize = 11.sp),
)

@Composable
fun AurelisTheme(
    content: @Composable () -> Unit,
) {
    val themeMode by SettingsRepository.themeMode.collectAsState()
    val darkTheme = when (themeMode) {
        0 -> true
        1 -> false
        2 -> isSystemInDarkTheme()
        else -> true
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = AurelisTypography,
        content = content,
    )
}

/**
 * Draws the status and navigation bar glyphs dark or light.
 *
 * `enableEdgeToEdge()` decides this from the *system* dark-mode setting, which
 * is the wrong input the moment the in-app theme disagrees with it. The bars
 * have to follow the content the app is actually painting. The player supplies
 * its own stable light-icon value and paints contrast behind it; every other
 * surface follows the theme. Hence a parameter rather than reading it here.
 */
@Composable
fun SystemBarIcons(dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = findWindow(view) ?: return
    SideEffect {
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = dark
            isAppearanceLightNavigationBars = dark
        }
    }
}
