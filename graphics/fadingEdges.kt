// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.ui.graphics

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

/**
 * Recorta los bordes de un Composable con un degradado suave. Usado por
 * `FadingLine` (SongText.kt) para hacer que el texto que se desborda
 * se desvanezca en el borde en vez de cortarse con puntos suspensivos.
 *
 * [start] y [end] reciben un lambda que devuelve la fracción de opacidad
 * (0f a 1f) con la que el degradado llega al borde. `null` significa que
 * ese borde no se difumina.
 */
fun Modifier.fadingEdges(
    fade: Dp,
    start: (() -> Float)? = null,
    end: (() -> Float)? = null,
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val fadePx = fade.toPx()
        if (fadePx <= 0f) return@drawWithContent

        if (start != null) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = 0f,
                    endY = fadePx,
                ),
                blendMode = BlendMode.DstIn,
            )
        }
        if (end != null) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startY = size.height - fadePx,
                    endY = size.height,
                ),
                blendMode = BlendMode.DstIn,
            )
        }
    }
