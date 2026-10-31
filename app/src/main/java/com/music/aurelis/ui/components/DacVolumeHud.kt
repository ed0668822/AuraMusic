// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Icon
import android.view.KeyEvent
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.aurelis.data.AppSettings
import com.music.aurelis.playback.audio.bitperfect.DacVolume
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

object VolumeHud {
    data class Pulse(val direction: Int, val atLimit: Boolean)

    private val _pulses = MutableSharedFlow<Pulse>(extraBufferCapacity = 8)
    val pulses: SharedFlow<Pulse> = _pulses.asSharedFlow()

    @Volatile var suppressed = false

    fun pulse(direction: Int, atLimit: Boolean) {
        _pulses.tryEmit(Pulse(direction, atLimit))
    }
}

fun handleDacVolumeKey(event: KeyEvent): Boolean {
    val code = event.keyCode
    if (code != KeyEvent.KEYCODE_VOLUME_UP && code != KeyEvent.KEYCODE_VOLUME_DOWN) return false
    if (DacVolume.level.value == null) return false
    if (event.action == KeyEvent.ACTION_DOWN) {
        val direction = if (code == KeyEvent.KEYCODE_VOLUME_UP) 1 else -1
        VolumeHud.pulse(direction, atLimit = !DacVolume.step(direction))
    }
    return true
}

@Composable
fun DacVolumeKeys() {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window ?: return
    DisposableEffect(window) {
        val original = window.callback
        val intercepting = object : Window.Callback by original {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean =
                handleDacVolumeKey(event) || original.dispatchKeyEvent(event)
        }
        window.callback = intercepting
        onDispose {
            if (window.callback === intercepting) window.callback = original
        }
    }
}

private const val HIDE_AFTER_MS = 1500L
private val HUD_HEIGHT = 156.dp
private val COMPACT_WIDTH = 14.dp
private val EXPANDED_WIDTH = 50.dp
private val EDGE_GAP = 10.dp

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun DacVolumeHud(hazeState: HazeState) {
    val level by DacVolume.level.collectAsStateWithLifecycle()
    val current = level ?: return

    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    var present by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var hideJob by remember { mutableStateOf<Job?>(null) }
    val slide = remember { Animatable(1f) }
    val stretch = remember { Animatable(1f) }
    var stretchFromTop by remember { mutableStateOf(false) }

    val fill by animateFloatAsState(
        targetValue = current,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 520f),
        label = "fill",
    )
    val width by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (dragging) EXPANDED_WIDTH else COMPACT_WIDTH,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "width",
    )

    fun scheduleHide() {
        hideJob?.cancel()
        hideJob = scope.launch {
            delay(HIDE_AFTER_MS)
            if (dragging) return@launch
            slide.animateTo(1f, spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow))
            present = false
        }
    }

    fun show() {
        if (VolumeHud.suppressed) return
        present = true
        scope.launch { slide.animateTo(0f, spring(dampingRatio = 0.72f, stiffness = 420f)) }
        scheduleHide()
    }

    fun rubberBand(direction: Int) {
        stretchFromTop = direction < 0
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        scope.launch {
            stretch.animateTo(1.07f, tween(durationMillis = 90))
            stretch.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 380f))
        }
    }

    LaunchedEffect(Unit) {
        VolumeHud.pulses.collect { pulse ->
            show()
            if (pulse.atLimit) rubberBand(pulse.direction)
        }
    }

    if (!present || VolumeHud.suppressed) return

    val reduceBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val offscreenPx = with(density) { (EXPANDED_WIDTH + EDGE_GAP * 2).roundToPx() }
    val heightPx = with(density) { HUD_HEIGHT.toPx() }
    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = screenHeight * 0.2f, end = EDGE_GAP)
                .height(HUD_HEIGHT)
                .width(EXPANDED_WIDTH),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = slide.value * offscreenPx
                        alpha = 1f - slide.value
                        scaleY = stretch.value
                        scaleX = 1f - (stretch.value - 1f) * 1.2f
                        transformOrigin = TransformOrigin(0.5f, if (stretchFromTop) 0f else 1f)
                    }
                    .width(width)
                    .fillMaxHeight()
                    .shadow(10.dp, RoundedCornerShape(percent = 50), clip = false)
                    .clip(RoundedCornerShape(percent = 50))
                    .then(
                        if (reduceBlur) {
                            Modifier.background(Color(0xFF1C1C1E).copy(alpha = 0.82f))
                        } else {
                            Modifier.optimizedHazeEffect(
                                state = hazeState,
                                style = HazeMaterials.ultraThin(Color(0xFF1C1C1E)),
                            )
                        },
                    )
                    .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(percent = 50))
                    .pointerInput(Unit) {
                        var start = 0f
                        var travelled = 0f
                        detectVerticalDragGestures(
                            onDragStart = {
                                dragging = true
                                hideJob?.cancel()
                                start = DacVolume.level.value ?: 0f
                                travelled = 0f
                            },
                            onDragEnd = {
                                dragging = false
                                scope.launch { stretch.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 380f)) }
                                scheduleHide()
                            },
                            onDragCancel = {
                                dragging = false
                                scope.launch { stretch.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 380f)) }
                                scheduleHide()
                            },
                        ) { change, dragAmount ->
                            change.consume()
                            travelled -= dragAmount
                            val wanted = start + travelled / heightPx
                            DacVolume.set(wanted.coerceIn(0f, 1f))
                            val over = when {
                                wanted > 1f -> wanted - 1f
                                wanted < 0f -> -wanted
                                else -> 0f
                            }
                            stretchFromTop = wanted < 0f
                            scope.launch { stretch.snapTo(1f + minOf(0.12f, over * 0.35f)) }
                        }
                    },
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(fill.coerceIn(0f, 1f))
                        .background(Color.White),
                )
                if (width > COMPACT_WIDTH + 18.dp) {
                    Icon(
                        imageVector = when {
                            current <= 0f -> Icons.AutoMirrored.Rounded.VolumeMute
                            current < 0.5f -> Icons.AutoMirrored.Rounded.VolumeDown
                            else -> Icons.AutoMirrored.Rounded.VolumeUp
                        },
                        contentDescription = null,
                        tint = if (fill > 0.12f) Color(0xFF3A3A3C) else Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .size(20.dp),
                    )
                }
            }
        }
    }
}
