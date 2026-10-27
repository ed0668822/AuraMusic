package com.music.aura.ui.theme

import android.content.Context
import android.net.Uri
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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.music.aura.ui.graphics.ColorUtils
import com.music.aura.ui.graphics.paletteSwatches
import com.music.aura.ui.graphics.toImageBitmap
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
) {
    companion object {
        suspend fun extractColors(context: Context, artworkUri: String): ArtworkColors? =
            withContext(Dispatchers.IO) {
                val uri = runCatching { Uri.parse(artworkUri) }.getOrNull() ?: return@withContext null
                val request = ImageRequest.Builder(context)
                    .data(uri)
                    .size(PALETTE_PX)
                    .build()
                val result = runCatching {
                    SingletonImageLoader.get(context).execute(request)
                }.getOrNull()
                val bitmap = (result as? SuccessResult)?.image?.toImageBitmap() ?: return@withContext null
                val swatches = paletteSwatches(bitmap, SWATCH_COUNT, clearFilters = false)
                if (swatches.isEmpty()) return@withContext null
                val dominant = swatches.maxByOrNull { it.population }?.rgb ?: return@withContext null
                val vibrant = swatches.maxByOrNull { swatch ->
                    val hsl = FloatArray(3).also { ColorUtils.colorToHSL(swatch.rgb, it) }
                    hsl[1] * sqrt(swatch.population.toFloat())
                }?.rgb ?: dominant
                ArtworkColors(
                    primary = Color(vibrant),
                    secondary = Color(dominant),
                    background = Color(0xFF0E0E14),
                )
            }
    }
}

data class ArtworkColors(
    val primary: Color = Color(0xFFB4A7F5),
    val onPrimary: Color = Color(0xFF1F1147),
    val primaryContainer: Color = Color(0xFF3B2D6B),
    val onPrimaryContainer: Color = Color(0xFFE8DEFF),
    val secondary: Color = Color(0xFF8ECFF5),
    val onSecondary: Color = Color(0xFF00344B),
    val secondaryContainer: Color = Color(0xFF004C6B),
    val onSecondaryContainer: Color = Color(0xFFC8E9FF),
    val background: Color = Color(0xFF0E0E14),
    val onBackground: Color = Color(0xFFE4E1EC),
    val surface: Color = Color(0xFF0E0E14),
    val onSurface: Color = Color(0xFFE4E1EC),
    val surfaceVariant: Color = Color(0xFF2A2833),
    val onSurfaceVariant: Color = Color(0xFFC8C4D4),
)

val LocalPlayerTint = staticCompositionLocalOf { Color.White }

@Composable
fun rememberArtworkPalette(
    artworkUri: Uri?,
    dark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f,
    reduceAnimation: Boolean = false,
): ArtworkPalette {
    val scheme = MaterialTheme.colorScheme
    val seed = rememberArtworkSeed(artworkUri)
    val knownUpFront = remember(artworkUri) { seed != null }

    val target = seed?.toPalette(dark) ?: ArtworkPalette(
        background = scheme.background,
        wash = scheme.background,
        elevated = scheme.surfaceVariant,
        accent = scheme.primary,
        onBackground = scheme.onBackground,
        onBackgroundVariant = scheme.onSurfaceVariant,
        divider = scheme.outline,
    )

    val spec: AnimationSpec<Color> = if (reduceAnimation || knownUpFront) snap() else tween(TINT_FADE_MS)
    return ArtworkPalette(
        background = animateColorAsState(target.background, spec, label = "tintBackground").value,
        wash = animateColorAsState(target.wash, spec, label = "tintWash").value,
        elevated = animateColorAsState(target.elevated, spec, label = "tintElevated").value,
        accent = animateColorAsState(target.accent, spec, label = "tintAccent").value,
        onBackground = animateColorAsState(target.onBackground, spec, label = "tintOn").value,
        onBackgroundVariant = animateColorAsState(target.onBackgroundVariant, spec, label = "tintOnVariant").value,
        divider = animateColorAsState(target.divider, spec, label = "tintDivider").value,
    )
}

@Composable
fun rememberArtworkTopBandLuminance(artworkUri: Uri?): Float? =
    rememberArtworkSeed(artworkUri)?.topBandLuminance

@Composable
fun rememberPlayerTint(artworkUri: Uri?): Color? {
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
private fun rememberArtworkSeed(artworkUri: Uri?): Seed? {
    val context = LocalPlatformContext.current
    var seed by remember(artworkUri) { mutableStateOf(artworkUri?.let { seedCache[it.toString()] }) }

    LaunchedEffect(artworkUri) {
        if (artworkUri == null || seed != null) return@LaunchedEffect
        val request = ImageRequest.Builder(context)
            .data(artworkUri)
            .size(PALETTE_PX)
            .build()
        val result = runCatching {
            SingletonImageLoader.get(context).execute(request)
        }.getOrNull()
        val bitmap = (result as? SuccessResult)?.image?.toImageBitmap() ?: return@LaunchedEffect
        val found = withContext(Dispatchers.Default) { seedOf(bitmap) } ?: return@LaunchedEffect
        seedCache[artworkUri.toString()] = found
        seed = found
    }
    return seed
}

private val seedCache = object : LinkedHashMap<String, Seed>(0, 0.75f, true) {
    override fun removeEldestEntry(eldest: Map.Entry<String, Seed>) = size > SEED_CACHE_ENTRIES
}

private const val SEED_CACHE_ENTRIES = 128
private const val PALETTE_PX = 128
private const val TINT_FADE_MS = 260

private data class Seed(
    val dominant: Color,
    val vibrant: Color,
    val edge: Color,
    val topBandLuminance: Float,
)

private fun seedOf(bitmap: ImageBitmap): Seed? {
    fun swatches(clearFilters: Boolean) = paletteSwatches(bitmap, SWATCH_COUNT, clearFilters)

    val all = swatches(clearFilters = true)
    if (all.isEmpty()) return null
    val accentCandidates = swatches(clearFilters = false).ifEmpty { all }

    val dominant = all.maxBy { it.population }
    val vibrant = accentCandidates.maxBy { swatch ->
        val hsl = FloatArray(3).also { ColorUtils.colorToHSL(swatch.rgb, it) }
        hsl[1] * sqrt(swatch.population.toFloat())
    }
    return Seed(
        dominant = Color(dominant.rgb),
        vibrant = Color(vibrant.rgb),
        edge = bitmap.bottomEdgeColor(),
        topBandLuminance = bitmap.topBandRelativeLuminance(),
    )
}

private const val SWATCH_COUNT = 24

private fun ImageBitmap.bottomEdgeColor(): Color {
    val band = (height * EDGE_BAND).toInt().coerceIn(1, height)
    val pixels = IntArray(width * band)
    readPixels(pixels, startX = 0, startY = height - band, width = width, height = band)

    var red = 0L
    var green = 0L
    var blue = 0L
    pixels.forEach { pixel ->
        red += (pixel shr 16) and 0xFF
        green += (pixel shr 8) and 0xFF
        blue += pixel and 0xFF
    }
    val count = pixels.size.coerceAtLeast(1)
    return Color(
        red = (red / count).toInt(),
        green = (green / count).toInt(),
        blue = (blue / count).toInt(),
    )
}

private const val EDGE_BAND = 0.18f
private const val TOP_BAND = 0.10f

private fun ImageBitmap.topBandRelativeLuminance(): Float {
    val band = (height * TOP_BAND).toInt().coerceIn(1, height)
    val pixels = IntArray(width * band)
    readPixels(pixels, startX = 0, startY = 0, width = width, height = band)
    return averageRelativeLuminance(pixels)
}

internal fun averageRelativeLuminance(pixels: IntArray): Float {
    if (pixels.isEmpty()) return 0f
    return pixels.sumOf { relativeLuminance(it).toDouble() }.div(pixels.size).toFloat()
}

internal fun relativeLuminance(argb: Int): Float {
    fun linear(channel: Int): Float {
        val srgb = channel / 255f
        return if (srgb <= 0.04045f) srgb / 12.9f else ((srgb + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
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

fun Color.luminance(): Float = ColorUtils.calculateLuminance(toArgb()).toFloat()
