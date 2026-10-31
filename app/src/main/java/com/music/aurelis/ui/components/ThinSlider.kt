// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.music.aurelis.data.settings.MixBlend
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos

@Composable
fun ThinSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    mixing: Boolean = false,
    mixPulse: MixPulse? = null,
    loading: Boolean = false,
    transitionWindow: ClosedFloatingPointRange<Float>? = null,
    idleHeight: Dp = 7.dp,
    activeHeight: Dp = 12.dp,
    activeColor: Color = Color.White.copy(alpha = 0.92f),
    inactiveColor: Color = Color.White.copy(alpha = 0.26f),
    markerColor: Color = Color.White.copy(alpha = 0.5f),
) {
    var dragging by remember { mutableStateOf(false) }
    val height by animateDpAsState(
        targetValue = if (dragging) activeHeight else idleHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "sliderHeight",
    )
    var shownLoading by remember { mutableStateOf(loading) }
    var sheenStart by remember { mutableStateOf(System.nanoTime()) }
    LaunchedEffect(loading) {
        if (loading) {
            sheenStart = System.nanoTime()
            shownLoading = true
        } else {
            val remaining = LOADING_MIN_MS - (System.nanoTime() - sheenStart) / 1_000_000L
            if (remaining > 0) delay(remaining)
            shownLoading = false
        }
    }
    val sheenVisible = shownLoading || (mixing && mixPulse?.active != true)
    var shownWindow by remember { mutableStateOf(transitionWindow) }
    LaunchedEffect(transitionWindow) {
        if (transitionWindow != null) shownWindow = transitionWindow
    }
    val windowAlpha by animateFloatAsState(
        targetValue = if (transitionWindow != null) 1f else 0f,
        animationSpec = tween(durationMillis = WINDOW_FADE_MS, easing = FastOutSlowInEasing),
        label = "transitionWindowAlpha",
    )
    val fillFactor by animateFloatAsState(
        targetValue = if (sheenVisible) 0f else 1f,
        animationSpec = tween(durationMillis = MORPH_MS, easing = FastOutSlowInEasing),
        label = "fillFactor",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(activeHeight + 22.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    dragging = true
                    onValueChange((down.position.x / size.width).coerceIn(0f, 1f))

                    while (true) {
                        val event = awaitPointerEvent()
                        val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!pointer.pressed) {
                            pointer.consume()
                            break
                        }
                        if (pointer.positionChanged()) {
                            onValueChange((pointer.position.x / size.width).coerceIn(0f, 1f))
                            pointer.consume()
                        }
                    }

                    dragging = false
                    onValueChangeFinished?.invoke()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height),
        ) {
            val radius = CornerRadius(size.height / 2f)
            drawRoundRect(color = inactiveColor, cornerRadius = radius)
            shownWindow?.takeIf { windowAlpha > 0f }?.let { window ->
                val from = size.width * window.start.coerceIn(0f, 1f)
                val to = size.width * window.endInclusive.coerceIn(0f, 1f)
                val span = to - from
                if (span > 0f) {
                    val feather = (span * WINDOW_FEATHER_SHARE)
                        .coerceIn(WINDOW_FEATHER_MIN.toPx(), WINDOW_FEATHER_MAX.toPx())
                        .coerceAtMost(span / 2f)
                    val f = feather / span
                    val full = markerColor.alpha * windowAlpha
                    val ramp = (0..WINDOW_FEATHER_STEPS).map { step ->
                        val t = step / WINDOW_FEATHER_STEPS.toFloat()
                        t to markerColor.copy(alpha = full * t * t * (3f - 2f * t))
                    }
                    val stops = ramp.map { (t, c) -> f * t to c } +
                        ramp.reversed().map { (t, c) -> 1f - f * t to c }
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colorStops = stops.toTypedArray(),
                            startX = from,
                            endX = to,
                        ),
                        size = size,
                        cornerRadius = radius,
                    )
                }
            }
            val cover = if (dragging) 0f else mixPulse?.cover ?: 0f
            val base = value.coerceIn(0f, 1f)
            val fraction = base + (1f - base) * cover
            val filled = size.width * fraction * fillFactor
            if (filled > 0f) {
                val alpha = if (dragging) activeColor.alpha else mixPulse?.alpha(activeColor.alpha) ?: activeColor.alpha
                drawRoundRect(
                    color = activeColor.copy(alpha = alpha),
                    size = Size(
                        filled.coerceAtLeast(size.height * fillFactor).coerceAtMost(size.width),
                        size.height,
                    ),
                    cornerRadius = radius,
                )
            }
        }
        AnimatedVisibility(
            visible = sheenVisible,
            enter = fadeIn(tween(durationMillis = MORPH_MS, easing = FastOutSlowInEasing)) +
                scaleIn(
                    animationSpec = tween(durationMillis = MORPH_MS, easing = FastOutSlowInEasing),
                    initialScale = 0f,
                    transformOrigin = TransformOrigin(0f, 0.5f),
                ),
            exit = fadeOut(tween(durationMillis = MORPH_MS, easing = FastOutSlowInEasing)),
        ) {
            MixSheen(height = height, color = activeColor)
        }
    }
}

private const val SHEEN_BAND_FRACTION = 0.34f
private const val WINDOW_FEATHER_SHARE = 0.4f
private val WINDOW_FEATHER_MIN = 6.dp
private val WINDOW_FEATHER_MAX = 40.dp
private const val WINDOW_FEATHER_STEPS = 6
private const val WINDOW_FADE_MS = 500

@Stable
class MixPulse internal constructor(private val blend: () -> MixBlend?) {
    val active: Boolean by derivedStateOf { blend() != null }
    internal val playing: Boolean by derivedStateOf { blend()?.playing == true }
    internal val coverAnim = Animatable(0f)
    internal val depthAnim = Animatable(0f)
    internal var level by mutableFloatStateOf(0f)
    internal var phase = 0.0
    internal var locked = false

    val cover: Float get() = coverAnim.value

    fun alpha(fullAlpha: Float, restShare: Float = PULSE_REST): Float {
        val rest = fullAlpha * restShare
        val pulsed = rest + (fullAlpha - rest) * level
        return fullAlpha + (pulsed - fullAlpha) * depthAnim.value
    }

    internal fun blendNow(): MixBlend? = blend()
}

@Composable
fun rememberMixPulse(mixBlend: () -> MixBlend?, enabled: Boolean): MixPulse {
    val current by rememberUpdatedState(mixBlend)
    val pulse = remember { MixPulse { current() } }
    val active = pulse.active
    val playing = pulse.playing
    LaunchedEffect(active, playing, enabled) {
        launch {
            pulse.coverAnim.animateTo(
                if (active) 1f else 0f,
                tween(durationMillis = COVER_MS, easing = FastOutSlowInEasing),
            )
        }
        launch {
            pulse.depthAnim.animateTo(
                if (active && playing && enabled) 1f else 0f,
                tween(durationMillis = PULSE_FADE_MS, easing = FastOutSlowInEasing),
            )
        }
        if (!enabled) return@LaunchedEffect
        var last = System.nanoTime()
        while ((active && playing) || pulse.depthAnim.value > 0f) {
            withFrameNanos {
                val now = System.nanoTime()
                val dt = (now - last).coerceIn(0L, MAX_FRAME_NANOS)
                last = now
                val blend = pulse.blendNow()
                val beatNanos = blend?.beatMs?.takeIf { it > 0f }?.let { it * 1_000_000.0 }
                var phase = pulse.phase
                if (beatNanos != null && blend != null) {
                    phase += dt / beatNanos
                    val target = Math.floorMod(now - blend.beatAnchorNanos, beatNanos.toLong()) / beatNanos
                    val error = ((target - phase) % 1.0 + 1.5) % 1.0 - 0.5
                    phase += if (pulse.locked) error * PHASE_PULL else error
                    pulse.locked = true
                } else {
                    phase += dt / BREATH_NANOS
                    if (blend != null) pulse.locked = false
                }
                phase -= kotlin.math.floor(phase)
                pulse.phase = phase
                pulse.level = beatShape(phase.toFloat())
            }
        }
        pulse.locked = false
    }
    return pulse
}

private const val COVER_MS = 700
private const val PULSE_FADE_MS = 900
private const val PULSE_REST = 0.1f
private const val PHASE_PULL = 0.06
private const val BREATH_NANOS = 1_800_000_000.0
private const val MAX_FRAME_NANOS = 100_000_000L

private fun beatShape(phase: Float): Float {
    val c = 0.5f + 0.5f * cos(2f * PI.toFloat() * phase)
    return c * c
}

private const val MORPH_MS = 450
private const val LOADING_MIN_MS = 600L

@Composable
private fun MixSheen(height: Dp, color: Color) {
    val transition = rememberInfiniteTransition(label = "mixSheen")
    val phase by transition.animateFloat(
        initialValue = -SHEEN_BAND_FRACTION,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mixSheenPhase",
    )
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val band = size.width * SHEEN_BAND_FRACTION
        val x = phase * size.width
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    0.5f to color,
                    1f to Color.Transparent,
                ),
                startX = x,
                endX = x + band,
            ),
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(size.height / 2f),
        )
    }
}
