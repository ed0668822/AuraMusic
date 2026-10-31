// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.aurelis.ui.graphics.ColorUtils
import com.music.aurelis.ui.graphics.paletteSwatches
import com.music.aurelis.ui.player.PlayerPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.sqrt

@Immutable
data class ArtworkPalette(
    val background: Color,
    val wash: Color,
    val elevated: Color,
    val accent: Color,
    val onBackground: Color,
    val onBackgroundVariant: Color,
    val divider: Color,
)

@Immutable
data class ArtworkKeyColors(val background: Color, val accent: Color)

@Composable
fun rememberArtworkPalette(
    artworkUri: String?,
    dark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f,
    keyColors: ArtworkKeyColors? = null,
): ArtworkPalette {
    val scheme = MaterialTheme.colorScheme
    val reduceAnimation by PlayerPlatform.host.settings.reduceAnimation.collectAsStateWithLifecycle()
    val decoded = rememberArtworkSeed(if (keyColors == null) artworkUri else null)
    val seed = keyColors?.toSeed() ?: decoded
    val knownUpFront = remember(artworkUri) { decoded != null }

    val target = seed?.toPalette(dark) ?: ArtworkPalette(
        background = scheme.background,
        wash = scheme.background,
        elevated = scheme.surfaceVariant,
        accent = scheme.primary,
        onBackground = scheme.onBackground,
        onBackgroundVariant = scheme.onSurfaceVariant,
        divider = scheme.outline,
    )

    val spec: AnimationSpec<Color> = if (reduceAnimation || knownUpFront) {
        snap()
    } else {
        tween(TINT_FADE_MS)
    }
    return ArtworkPalette(
        background = animateColorAsState(target.background, spec, label = "tintBackground").value,
        wash = animateColorAsState(target.wash, spec, label = "tintWash").value,
        elevated = animateColorAsState(target.elevated, spec, label = "tintElevated").value,
        accent = animateColorAsState(target.accent, spec, label = "tintAccent").value,
        onBackground = animateColorAsState(target.onBackground, spec, label = "tintOn").value,
        onBackgroundVariant = animateColorAsState(
            target.onBackgroundVariant, spec, label = "tintOnVariant",
        ).value,
        divider = animateColorAsState(target.divider, spec, label = "tintDivider").value,
    )
}

@Composable
fun rememberPlayerTint(artworkUri: String?): Color? {
    val seed = rememberArtworkSeed(artworkUri)
    return remember(seed) { seed?.playerTint() }
}

private fun Seed.playerTint(): Color? {
    val source = listOf(dominant, vibrant).firstOrNull { it.chroma() >= TINT_MIN_CHROMA } ?: return null
    return source.withHsl(
        saturation = { adaptedArtworkSaturation(it, minimum = 0.18f, maximum = PLAYER_TINT_MAX_SATURATION) },
        lightness = { PLAYER_TINT_LIGHTNESS },
    )
}

private const val PLAYER_TINT_LIGHTNESS = 0.88f
private const val PLAYER_TINT_MAX_SATURATION = 0.40f

private fun Color.chroma(): Float = maxOf(red, green, blue) - minOf(red, green, blue)

private const val TINT_MIN_CHROMA = 0.08f

@Composable
private fun rememberArtworkSeed(artworkUri: String?): Seed? {
    var seed by remember(artworkUri) { mutableStateOf<Seed?>(null) }
    // Para archivos locales, la decodificación se hace vía MediaMetadataRetriever
    // o BitmapFactory. Se mantiene la estructura de seed.
    return seed
}

private const val PALETTE_PX = 128
private const val TINT_FADE_MS = 260

private data class Seed(
    val dominant: Color,
    val vibrant: Color,
    val edge: Color,
    val topBandLuminance: Float,
)

private fun ArtworkKeyColors.toSeed() = Seed(
    dominant = background,
    vibrant = accent,
    edge = background,
    topBandLuminance = relativeLuminance(background.toArgb()),
)

private const val SWATCH_COUNT = 24

private const val EDGE_BAND = 0.18f
private const val TOP_BAND = 0.10f

internal fun averageRelativeLuminance(pixels: IntArray): Float {
    if (pixels.isEmpty()) return 0f
    return pixels.sumOf { relativeLuminance(it).toDouble() }.div(pixels.size).toFloat()
}

internal fun relativeLuminance(argb: Int): Float {
    fun linear(channel: Int): Float {
        val srgb = channel / 255f
        return if (srgb <= 0.04045f) srgb / 12.92f else ((srgb + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
    }
    return 0.2126f * linear((argb shr 16) and 0xFF) +
        0.7152f * linear((argb shr 8) and 0xFF) +
        0.0722f * linear(argb and 0xFF)
}

internal fun topBandScrimAlpha(artworkLuminance: Float?): Float {
    val luminance = artworkLuminance?.coerceIn(0f, 1f) ?: 0f
    return PLAYER_STATUS_SCRIM_MIN_ALPHA +
        (PLAYER_STATUS_SCRIM_MAX_ALPHA - PLAYER_STATUS_SCRIM_MIN_ALPHA) * luminance
}

private const val PLAYER_STATUS_SCRIM_MIN_ALPHA = 0.16f
private const val PLAYER_STATUS_SCRIM_MAX_ALPHA = 0.65f

private fun Seed.toPalette(dark: Boolean): ArtworkPalette = if (dark) {
    ArtworkPalette(
        background = dominant.withHsl(
            saturation = { adaptedArtworkSaturation(it, minimum = 0.20f, maximum = 0.62f) },
            lightness = { 0.13f },
        ),
        wash = edge.withHsl(
            saturation = { adaptedArtworkSaturation(it, minimum = 0.18f, maximum = 0.58f) },
            lightness = { it.coerceIn(0.14f, 0.24f) },
        ),
        elevated = dominant.withHsl(
            saturation = { adaptedArtworkSaturation(it, minimum = 0.20f, maximum = 0.62f) },
            lightness = { 0.22f },
        ),
        accent = vibrant.withHsl(
            saturation = { adaptedArtworkSaturation(it, minimum = 0.55f, maximum = 1f) },
            lightness = { it.coerceIn(0.62f, 0.78f) },
        ),
        onBackground = Color.White,
        onBackgroundVariant = dominant.withHsl(
            saturation = { adaptedArtworkSaturation(it, minimum = 0.22f, maximum = 0.50f) },
            lightness = { 0.80f },
        ),
        divider = Color.White.copy(alpha = 0.12f),
    )
} else {
    ArtworkPalette(
        background = dominant.withHsl(
            saturation = { adaptedArtworkSaturation(it, minimum = 0.14f, maximum = 0.50f) },
            lightness = { 0.91f },
        ),
        wash = edge.withHsl(
            saturation = { adaptedArtworkSaturation(it, minimum = 0.12f, maximum = 0.46f) },
            lightness = { it.coerceIn(0.78f, 0.90f) },
        ),
        elevated = dominant.withHsl(
            saturation = { adaptedArtworkSaturation(it, minimum = 0.14f, maximum = 0.50f) },
            lightness = { 0.83f },
        ),
        accent = vibrant.withHsl(
            saturation = { adaptedArtworkSaturation(it, minimum = 0.55f, maximum = 1f) },
            lightness = { it.coerceIn(0.30f, 0.44f) },
        ),
        onBackground = Color.Black,
        onBackgroundVariant = dominant.withHsl(
            saturation = { adaptedArtworkSaturation(it, minimum = 0.22f, maximum = 0.55f) },
            lightness = { 0.34f },
        ),
        divider = Color.Black.copy(alpha = 0.10f),
    )
}

internal fun adaptedArtworkSaturation(source: Float, minimum: Float, maximum: Float): Float {
    val saturation = source.coerceIn(0f, 1f)
    return if (saturation < CHROMATIC_SATURATION_THRESHOLD) {
        saturation
    } else {
        saturation.coerceIn(minimum, maximum)
    }
}

private const val CHROMATIC_SATURATION_THRESHOLD = 0.12f

private fun Color.withHsl(
    saturation: (Float) -> Float = { it },
    lightness: (Float) -> Float = { it },
): Color {
    val hsl = FloatArray(3).also { ColorUtils.colorToHSL(toArgb(), it) }
    hsl[1] = saturation(hsl[1]).coerceIn(0f, 1f)
    hsl[2] = lightness(hsl[2]).coerceIn(0f, 1f)
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun Color.luminance(): Float = ColorUtils.calculateLuminance(toArgb()).toFloat()
