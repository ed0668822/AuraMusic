package com.music.aura.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Fondo MeshGradient animado, inspirado en BitChord.
 *
 * Dibuja 4 manchas de color radiales que orbitan lentamente sobre un color
 * base. Los colores vienen del artwork de la canción ([ArtworkColors]).
 * Cada mancha tiene su propio período y dirección, generando una mezcla
 * orgánica que nunca se repite exactamente.
 *
 * Coste: 4 radialGradients por frame en un Canvas. Barato (sin blur real,
 * sin RenderEffect), apto para cualquier dispositivo.
 */
@Composable
fun MeshGradient(
    colors: ArtworkColors,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    // Si está en pausa, congelamos la animación. Si no, los blobs orbitan.
    val transition = rememberInfiniteTransition(label = "mesh")

    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 25_000 else 1),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "meshPhase",
    )

    val base = colors.background
    val accentA = colors.primary.copy(alpha = 0.55f)
    val accentB = colors.secondary.copy(alpha = 0.50f)
    val accentC = colors.primaryContainer.copy(alpha = 0.45f)
    val accentD = colors.secondaryContainer.copy(alpha = 0.40f)

    Canvas(modifier = modifier.fillMaxSize()) {
        // 1. Base
        drawRect(color = base)

        val w = size.width
        val h = size.height
        val diagonal = kotlin.math.hypot(w, h)

        // 2. Blob 1 — arriba-izquierda, orbita en círculo pequeño.
        drawBlob(
            center = Offset(
                x = w * (0.25f + 0.08f * kotlin.math.cos(phase * 6.28f)),
                y = h * (0.20f + 0.06f * kotlin.math.sin(phase * 6.28f)),
            ),
            radius = diagonal * 0.55f,
            color = accentA,
        )

        // 3. Blob 2 — arriba-derecha, orbita en sentido opuesto.
        drawBlob(
            center = Offset(
                x = w * (0.75f + 0.06f * kotlin.math.cos(phase * 6.28f + 1.7f)),
                y = h * (0.30f + 0.05f * kotlin.math.sin(phase * 6.28f + 1.7f)),
            ),
            radius = diagonal * 0.50f,
            color = accentB,
        )

        // 4. Blob 3 — abajo-izquierda, más lento (usa phase * 0.7).
        val p3 = phase * 0.7f
        drawBlob(
            center = Offset(
                x = w * (0.20f + 0.05f * kotlin.math.cos(p3 * 6.28f + 3.2f)),
                y = h * (0.78f + 0.04f * kotlin.math.sin(p3 * 6.28f + 3.2f)),
            ),
            radius = diagonal * 0.60f,
            color = accentC,
        )

        // 5. Blob 4 — abajo-derecha, más lento aún.
        val p4 = phase * 0.5f
        drawBlob(
            center = Offset(
                x = w * (0.80f + 0.05f * kotlin.math.cos(p4 * 6.28f + 4.8f)),
                y = h * (0.85f + 0.05f * kotlin.math.sin(p4 * 6.28f + 4.8f)),
            ),
            radius = diagonal * 0.45f,
            color = accentD,
        )
    }
}

/** Un blob: gradiente radial del color al transparente. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBlob(
    center: Offset,
    radius: Float,
    color: Color,
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}
