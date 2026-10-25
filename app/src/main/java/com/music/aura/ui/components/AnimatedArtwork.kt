package com.music.aura.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

/**
 * Artwork con crossfade al cambiar de canción y rotación 3D sutil.
 *
 * - [songKey] dispara el crossfade.
 * - La rotación 3D en Y ocurre al cambiar de canción (efecto "voltear disco").
 * - La escala respira suavemente con [progressFraction].
 * - Sombra amplia y redondeada para efecto premium.
 */
@Composable
fun AnimatedArtwork(
    songKey: String,
    artworkUri: String?,
    title: String,
    size: Dp,
    progressFraction: Float,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = 1f + (progressFraction.coerceIn(0f, 1f) * 0.02f),
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "artworkScale",
    )

    val shape = RoundedCornerShape(24.dp)

    Crossfade(
        targetState = songKey,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        modifier = modifier,
        label = "artworkCrossfade",
    ) { key ->
        // Rotación breve al entrar una canción nueva
        val rotationY by animateFloatAsState(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            label = "artworkRotation_$key",
        )

        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.rotationY = rotationY
                }
                .shadow(elevation = 24.dp, shape = shape, clip = false)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            if (artworkUri != null) {
                AsyncImage(
                    model = artworkUri,
                    contentDescription = "Artwork de $title",
                    modifier = Modifier.size(size),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = "♪",
                    fontSize = (size.value * 0.2f).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
