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
    val transition = rememberInfiniteTransition(label = "mesh")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 12_000 else 1, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "meshPhase",
    )

    val base = colors.background
    val accentA = colors.primary.copy(alpha = 0.90f)
    val accentB = colors.secondary.copy(alpha = 0.85f)
    val accentC = colors.primaryContainer.copy(alpha = 0.80f)
    val accentD = colors.secondaryContainer.copy(alpha = 0.75f)

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(color = base)
        val w = size.width
        val h = size.height
        val diagonal = hypot(w, h)

        drawBlob(Offset(w * (0.25f + 0.2f * cos(phase * 6.28f)), h * (0.20f + 0.15f * sin(phase * 6.28f))), diagonal * 0.7f, accentA)
        drawBlob(Offset(w * (0.75f + 0.2f * cos(phase * 6.28f + 1.7f)), h * (0.30f + 0.15f * sin(phase * 6.28f + 1.7f))), diagonal * 0.65f, accentB)
        drawBlob(Offset(w * (0.20f + 0.15f * cos(phase * 4.0f + 3.2f)), h * (0.78f + 0.15f * sin(phase * 4.0f + 3.2f))), diagonal * 0.75f, accentC)
        drawBlob(Offset(w * (0.80f + 0.15f * cos(phase * 4.0f + 4.8f)), h * (0.85f + 0.15f * sin(phase * 4.0f + 4.8f))), diagonal * 0.6f, accentD)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBlob(center: Offset, radius: Float, color: Color) {
    drawCircle(
        brush = Brush.radialGradient(colors = listOf(color, Color.Transparent), center = center, radius = radius),
        radius = radius,
        center = center,
    )
}
