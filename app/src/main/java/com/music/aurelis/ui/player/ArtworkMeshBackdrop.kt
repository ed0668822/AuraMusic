// Aurelis © 2026. GPL-3.0-or-later.

package com.music.aurelis.ui.player

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.music.aurelis.ui.graphics.forPixelAccess
import com.music.aurelis.ui.graphics.toImageBitmap
import kotlin.math.roundToInt

@Immutable
class ArtworkMesh internal constructor(internal val image: ImageBitmap)

@Composable
fun rememberArtworkMesh(
    artworkUri: Uri?,
): ArtworkMesh? {
    val context = LocalPlatformContext.current
    var mesh by remember(artworkUri) { mutableStateOf<ArtworkMesh?>(null) }
    LaunchedEffect(artworkUri) {
        mesh = null
        if (artworkUri == null) return@LaunchedEffect
        val request = ImageRequest.Builder(context)
            .data(artworkUri)
            .size(MESH_SAMPLE_PX)
            .forPixelAccess()
            .build()
        val result = runCatching {
            SingletonImageLoader.get(context).execute(request)
        }.getOrNull()
        val bitmap = (result as? SuccessResult)?.image?.toImageBitmap()
            ?: return@LaunchedEffect
        mesh = ArtworkMesh(bitmap)
    }
    return mesh
}

@Composable
fun ArtworkMeshBackdrop(
    mesh: ArtworkMesh?,
    modifier: Modifier = Modifier,
    seam: Dp = 0.dp,
    fallback: Color = FallbackBackdrop,
    blurRadius: Dp = 32.dp,
    reduceAnimation: Boolean = false,
    reduceDynamicBlur: Boolean = false,
) {
    val canBlur = !reduceDynamicBlur &&
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S

    var shown by remember { mutableStateOf(mesh) }
    var incoming by remember { mutableStateOf<ArtworkMesh?>(null) }
    val fade = remember { Animatable(0f) }

    LaunchedEffect(mesh) {
        val next = mesh ?: return@LaunchedEffect
        incoming?.let { shown = it }
        incoming = null
        val current = shown
        if (next === current) return@LaunchedEffect

        if (current == null || reduceAnimation) {
            shown = next
            return@LaunchedEffect
        }
        incoming = next
        fade.snapTo(0f)
        fade.animateTo(1f, tween(MESH_FADE_MS, easing = FastOutSlowInEasing))
        shown = next
        incoming = null
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(fallback)
            .then(if (canBlur) Modifier.blur(blurRadius) else Modifier),
    ) {
        val seamY = seam.toPx().coerceIn(0f, size.height)
        shown?.let { drawMesh(it, seamY, alpha = 1f) }
        incoming?.let { drawMesh(it, seamY, alpha = fade.value) }

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.06f),
                    Color.Black.copy(alpha = 0.30f),
                ),
            ),
        )
    }
}

private fun DrawScope.drawMesh(mesh: ArtworkMesh, seamY: Float, alpha: Float) {
    if (alpha <= 0.001f) return
    val image = mesh.image
    val width = size.width.roundToInt()

    if (seamY > 0.5f) {
        drawImage(
            image = image,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(image.width, 1),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(width, seamY.roundToInt()),
            alpha = alpha,
            filterQuality = FilterQuality.Low,
        )
    }

    drawImage(
        image = image,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(image.width, image.height),
        dstOffset = IntOffset(0, seamY.roundToInt()),
        dstSize = IntSize(width, (size.height - seamY).roundToInt()),
        alpha = alpha,
        filterQuality = FilterQuality.Low,
    )
}

private val FallbackBackdrop = Color(0xFF121212)
private const val MESH_FADE_MS = 900
private const val MESH_SAMPLE_PX = 512
