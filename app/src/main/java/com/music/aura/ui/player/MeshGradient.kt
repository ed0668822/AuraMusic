package com.music.aura.ui.player

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.music.aura.ui.graphics.ColorUtils
import com.music.aura.ui.graphics.paletteSwatches
import com.music.aura.ui.graphics.toImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val FallbackColors = listOf(
    Color(0xFF3A1C71),
    Color(0xFFD76D77),
    Color(0xFF2B5876),
    Color(0xFFFFAF7B),
)

@Immutable
data class MeshPalette(val colors: List<Color>)

@Composable
fun MeshGradientBackground(
    palette: MeshPalette,
    modifier: Modifier = Modifier,
    trackKey: Any? = null,
    driftMillis: Int = 8_000,
    continuous: Boolean = false,
    blurRadius: Dp = 64.dp,
    animated: Boolean = true,
    colorFadeMillis: Int = 1400,
    reduceAnimation: Boolean = false,
) {
    val tuned = (palette.colors.ifEmpty { FallbackColors } + FallbackColors)
        .take(4)
        .map { it.tuned() }

    val colorSpec: AnimationSpec<Color> = if (reduceAnimation || !animated) snap() else tween(colorFadeMillis)
    val animatedColors = tuned.mapIndexed { index, color ->
        animateColorAsState(color, colorSpec, label = "meshColor$index").value
    }
    val baseColor by animateColorAsState(tuned.first().dimmed(), colorSpec, label = "meshBase")

    val phase = remember { Animatable(0f) }
    LaunchedEffect(trackKey, reduceAnimation, continuous, animated) {
        when {
            !animated || reduceAnimation -> phase.snapTo(0f)
            continuous -> while (isActive) {
                phase.animateTo(
                    targetValue = phase.value + (2 * PI).toFloat(),
                    animationSpec = tween(driftMillis * 4, easing = LinearEasing),
                )
            }
            else -> phase.animateTo(
                targetValue = phase.value + DRIFT_RADIANS,
                animationSpec = tween(driftMillis, easing = FastOutSlowInEasing),
            )
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .graphicsLayer {
                scaleX = 1.3f
                scaleY = 1.3f
            }
            .background(baseColor)
            .blur(blurRadius),
    ) {
        val anchors = listOf(
            Offset(0.20f, 0.25f),
            Offset(0.80f, 0.20f),
            Offset(0.75f, 0.80f),
            Offset(0.25f, 0.75f),
        )
        val speeds = listOf(1f, -0.7f, 0.85f, -1.15f)
        val drift = phase.value

        animatedColors.forEachIndexed { index, color ->
            val anchor = anchors[index]
            val center = Offset(
                x = (anchor.x + 0.16f * cos(drift * speeds[index] + index * 1.7f)) * size.width,
                y = (anchor.y + 0.16f * sin(drift * speeds[index] * 0.9f + index * 2.3f)) * size.height,
            )
            val radius = size.maxDimension * 0.62f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.85f), color.copy(alpha = 0f)),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.10f),
                    Color.Black.copy(alpha = 0.38f),
                ),
            ),
        )
    }
}

const val MESH_CYCLE_MILLIS = 3_600

val WelcomeMeshPalettes: List<MeshPalette> = listOf(
    MeshPalette(listOf(Color(0xFFFA2D48), Color(0xFF7B2FF7), Color(0xFFFF7A45), Color(0xFF2E1A8A))),
    MeshPalette(listOf(Color(0xFFFF4E8A), Color(0xFFFF9A3C), Color(0xFFE0245E), Color(0xFF6A1B9A))),
    MeshPalette(listOf(Color(0xFF1E88E5), Color(0xFF00BFA5), Color(0xFF5E35B1), Color(0xFF0D47A1))),
    MeshPalette(listOf(Color(0xFF00C853), Color(0xFF00BFA5), Color(0xFF7C4DFF), Color(0xFF1A237E))),
    MeshPalette(listOf(Color(0xFFD500F9), Color(0xFFFF4081), Color(0xFF651FFF), Color(0xFF4A148C))),
    MeshPalette(listOf(Color(0xFFFFB300), Color(0xFFFF5252), Color(0xFFF06292), Color(0xFF6D2E46))),
)

@Composable
fun cyclingMeshPalette(
    palettes: List<MeshPalette>,
    stepMillis: Int = MESH_CYCLE_MILLIS,
    reduceAnimation: Boolean = false,
): MeshPalette {
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(reduceAnimation, stepMillis) {
        if (reduceAnimation) return@LaunchedEffect
        while (isActive) {
            delay(stepMillis.toLong())
            step++
        }
    }
    return palettes[step % palettes.size]
}

/**
 * Paleta de colores del artwork local.
 * Adaptado de BitChord: usa content:// URI en vez de URL remota.
 */
@Composable
fun rememberArtworkColors(artworkUri: Uri?): MeshPalette {
    val context = LocalPlatformContext.current
    var palette by remember(artworkUri) { mutableStateOf(MeshPalette(FallbackColors)) }

    LaunchedEffect(artworkUri) {
        if (artworkUri == null) return@LaunchedEffect
        val request = ImageRequest.Builder(context)
            .data(artworkUri)
            .size(128)
            .build()
        val result = runCatching {
            SingletonImageLoader.get(context).execute(request)
        }.getOrNull()
        val bitmap = (result as? SuccessResult)?.image?.toImageBitmap() ?: return@LaunchedEffect
        val colors = withContext(Dispatchers.Default) { paletteOf(bitmap) }
        palette = MeshPalette(colors)
    }
    return palette
}

private const val DRIFT_RADIANS = (PI * 0.45f).toFloat()

fun artworkMeshColors(bitmap: ImageBitmap): List<Color> = paletteOf(bitmap)

private fun paletteOf(bitmap: ImageBitmap): List<Color> {
    fun swatchesOf(clearFilters: Boolean): List<Color> =
        paletteSwatches(bitmap, 24, clearFilters)
            .sortedByDescending { it.population }
            .map { Color(it.rgb) }

    val found = swatchesOf(clearFilters = false).ifEmpty {
        swatchesOf(clearFilters = true)
    }

    val distinct = found.distinctEnough()
    return when {
        distinct.isEmpty() -> FallbackColors
        distinct.size >= 4 -> distinct.take(4)
        else -> distinct.expandedToFour()
    }
}

private fun List<Color>.distinctEnough(): List<Color> {
    val kept = mutableListOf<Color>()
    forEach { color -> if (kept.none { it.isCloseTo(color) }) kept += color }
    return kept
}

private fun Color.isCloseTo(other: Color): Boolean {
    val a = hsl()
    val b = other.hsl()
    val hueGap = abs(a[0] - b[0]).let { min(it, 360f - it) }
    return hueGap < 15f && abs(a[2] - b[2]) < 0.12f
}

private fun List<Color>.expandedToFour(): List<Color> {
    val out = toMutableList()
    var step = 1
    while (out.size < 4) {
        out += this[(out.size - size) % size].shifted(24f * step, 0.12f * step)
        step++
    }
    return out
}

private fun Color.shifted(hue: Float, lightness: Float): Color {
    val hsl = hsl()
    hsl[0] = (hsl[0] + hue) % 360f
    hsl[2] = (hsl[2] + lightness).coerceIn(0.2f, 0.7f)
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun Color.hsl(): FloatArray =
    FloatArray(3).also { ColorUtils.colorToHSL(toArgb(), it) }

private fun Color.tuned(): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(toArgb(), hsl)
    hsl[1] = (hsl[1] * 1.35f).coerceAtMost(1f)
    hsl[2] = hsl[2].coerceIn(0.28f, 0.58f)
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun Color.dimmed(): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(toArgb(), hsl)
    hsl[2] = 0.12f
    return Color(ColorUtils.HSLToColor(hsl))
}
