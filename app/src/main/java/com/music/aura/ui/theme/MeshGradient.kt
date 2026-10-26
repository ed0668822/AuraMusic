package com.music.aura.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun MeshGradient(
    colors: ArtworkColors,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    // Animación continua infinita
    val transition = rememberInfiniteTransition(label = "mesh")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        // Siempre dura 20 segundos. No cambiamos la duración si pausa para evitar saltos.
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart, // Restart asegura que el ciclo vuelva a empezar suavemente
        ),
        label = "meshPhase",
    )

    val base = colors.background
    val accentA = colors.primary.copy(alpha = 0.85f)
    val accentB = colors.secondary.copy(alpha = 0.75f)
    val accentC = colors.primaryContainer.copy(alpha = 0.70f)
    val accentD = colors.secondaryContainer.copy(alpha = 0.65f)

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(color = base)
        val w = size.width
        val h = size.height
        val diagonal = hypot(w, h)
        val pi2 = 2 * Math.PI.toFloat()

        // Blob 1
        drawBlob(
            center = Offset(w * (0.3f + 0.2f * cos(phase * pi2)), h * (0.2f + 0.15f * sin(phase * pi2))),
            radius = diagonal * 0.7f,
            color = accentA,
        )
        // Blob 2
        drawBlob(
            center = Offset(w * (0.7f + 0.2f * cos(phase * pi2 + 2f)), h * (0.3f + 0.15f * sin(phase * pi2 + 2f))),
            radius = diagonal * 0.6f,
            color = accentB,
        )
        // Blob 3
        drawBlob(
            center = Offset(w * (0.2f + 0.15f * cos(phase * pi2 + 4f)), h * (0.8f + 0.15f * sin(phase * pi2 + 4f))),
            radius = diagonal * 0.75f,
            color = accentC,
        )
        // Blob 4
        drawBlob(
            center = Offset(w * (0.8f + 0.15f * cos(phase * pi2 + 1f)), h * (0.85f + 0.15f * sin(phase * pi2 + 1f))),
            radius = diagonal * 0.55f,
            color = accentD,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBlob(center: Offset, radius: Float, color: Color) {
    drawCircle(
        brush = Brush.radialGradient(colors = listOf(color, Color.Transparent), center = center, radius = radius),
        radius = radius,
        center = center,
    )
}
