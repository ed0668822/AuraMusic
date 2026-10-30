// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.ui.components

import androidx.compose.animation.core.EaseInOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.aurelis.data.SettingsRepository
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

private val FADE_RUN = 16.dp
private val BLUR_MAX = 24.dp
private const val FOOT_FADE_START = 0.6f
private const val FOOT_FADE_STOPS = 8

private val FootFade = Brush.verticalGradient(
    colorStops = Array(FOOT_FADE_STOPS + 1) { i ->
        val t = i / FOOT_FADE_STOPS.toFloat()
        val at = FOOT_FADE_START + (1f - FOOT_FADE_START) * t
        at to Color.Black.copy(alpha = 1f - EaseInOut.transform(t))
    },
)

/**
 * Blur progresivo del borde superior de la pantalla: lo que se desliza bajo
 * la barra de estado y los controles flotantes se difumina, más fuerte arriba
 * y desvaneciéndose a la altura de los controles. Sin tinte ni scrim — solo
 * blur. Se apaga con "Reduce dynamic blur" en ajustes.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun TopFadeBlur(
    hazeState: HazeState,
    pageColor: Color,
    modifier: Modifier = Modifier,
) {
    val reduceDynamicBlur by SettingsRepository.reduceDynamicBlur.collectAsStateWithLifecycle()
    if (reduceDynamicBlur) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(topBarHeight() + FADE_RUN)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(FootFade, blendMode = BlendMode.DstIn)
            }
            .hazeEffect(state = hazeState) {
                backgroundColor = pageColor
                tints = emptyList()
                blurRadius = BLUR_MAX
                noiseFactor = 0f
                inputScale = HazeInputScale.Fixed(0.33f)
                progressive = HazeProgressive.verticalGradient(
                    easing = EaseInOut,
                    startIntensity = 1f,
                    endIntensity = 0f,
                )
            },
    )
}
