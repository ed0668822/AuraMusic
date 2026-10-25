package com.music.aura.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

/**
 * Artwork con crossfade al cambiar de canción y escala sutil
 * vinculada al progreso de reproducción.
 *
 * El crossfade usa [songKey] para detectar el cambio de canción.
 * La escala va de 1.0f a 1.02f a lo largo del track, dando
 * una sensación de "respiración" muy sutil.
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
    // Escala sutil: 1.0 → 1.02 a lo largo de la canción
    val scale by animateFloatAsState(
        targetValue = 1f + (progressFraction.coerceIn(0f, 1f) * 0.02f),
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "artworkScale",
    )

    Crossfade(
        targetState = songKey,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        modifier = modifier,
        label = "artworkCrossfade",
    ) { key ->
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            contentAlignment = Alignment.Center,
        ) {
            if (artworkUri != null) {
                AsyncImage(
                    model = artworkUri,
                    contentDescription = "Artwork de $title",
                    modifier = Modifier
                        .size(size)
                        .clip(RoundedCornerShape(20.dp)),
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
