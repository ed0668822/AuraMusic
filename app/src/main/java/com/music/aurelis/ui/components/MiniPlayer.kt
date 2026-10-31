// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import com.music.aurelis.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.AppSettings
import com.music.aurelis.ui.haptics.Haptic
import com.music.aurelis.ui.haptics.rememberHaptics
import com.music.aurelis.ui.player.PlayerDock
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.fillMaxSize
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

private val GLYPH_SLOT = 40.dp
private val GLYPH_SIZE = 32.dp
private val SPINNER_SIZE = 22.dp
private val TRANSPORT_GAP = 8.dp
private val ROW_PADDING_VERTICAL = 8.dp
private val ROW_PADDING_HORIZONTAL = 12.dp
private val ART_CORNER = 8.dp
private val TRACK_SWIPE_THRESHOLD = 72.dp
private const val PULL_BIAS = 1.5f
private val PULL_LIFT_EASE = 72.dp
private const val PULL_LIFT_RATE = 0.5f

private fun pullLift(pulled: Float, ease: Float): Float =
    if (pulled <= 0f || ease <= 0f) {
        0f
    } else {
        PULL_LIFT_RATE * 2f * ease * (sqrt(1f + pulled / ease) - 1f)
    }

interface MiniPlayerPull {
    fun drag(delta: Float)
    fun release(velocity: Float)
}

@Composable
internal fun Modifier.miniPlayerGestures(
    pull: MiniPlayerPull?,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    locked: Boolean = false,
    onBlocked: () -> Unit = {},
): Modifier {
    val currentPull by rememberUpdatedState(pull)
    val currentOnNext by rememberUpdatedState(onNext)
    val currentOnPrevious by rememberUpdatedState(onPrevious)
    val currentLocked by rememberUpdatedState(locked)
    val currentOnBlocked by rememberUpdatedState(onBlocked)
    val scope = rememberCoroutineScope()
    val lift = remember { mutableFloatStateOf(0f) }
    val settle = remember { arrayOfNulls<Job>(1) }
    return pointerInput(Unit) {
        val trackThreshold = TRACK_SWIPE_THRESHOLD.toPx()
        val liftEase = PULL_LIFT_EASE.toPx()
        val maxVelocity = viewConfiguration.maximumFlingVelocity
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val pointer = down.id
            var start: PointerInputChange? = null
            var vertical = false
            while (start == null) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == pointer }
                    ?: return@awaitEachGesture
                if (!change.pressed || change.isConsumed) return@awaitEachGesture
                val moved = change.position - down.position
                if (moved.getDistance() < viewConfiguration.touchSlop) continue
                vertical = moved.y < 0f && abs(moved.x) < -moved.y * PULL_BIAS
                if (!vertical && abs(moved.x) < abs(moved.y)) return@awaitEachGesture
                if (vertical && currentPull == null) return@awaitEachGesture
                change.consume()
                start = change
            }
            val slopped = start ?: return@awaitEachGesture

            if (!vertical) {
                var dragged = slopped.position.x - down.position.x
                val finished = drag(pointer) { change ->
                    dragged += change.positionChange().x
                    change.consume()
                }
                if (!finished) return@awaitEachGesture
                val crossed = dragged <= -trackThreshold || dragged >= trackThreshold
                when {
                    currentLocked -> if (crossed) currentOnBlocked()
                    dragged <= -trackThreshold -> currentOnNext()
                    dragged >= trackThreshold -> currentOnPrevious()
                }
                return@awaitEachGesture
            }

            val target = currentPull ?: return@awaitEachGesture
            settle[0]?.cancel()
            val tracker = VelocityTracker()
            tracker.addPointerInputChange(down)
            tracker.addPointerInputChange(slopped)
            target.drag(slopped.position.y - down.position.y)
            var pulled = 0f
            val finished = drag(pointer) { change ->
                val delta = change.positionChange().y
                change.consume()
                tracker.addPointerInputChange(change)
                pulled -= delta
                target.drag(delta)
                lift.floatValue = pullLift(pulled, liftEase)
            }
            target.release(
                if (finished) tracker.calculateVelocity().y.coerceIn(-maxVelocity, maxVelocity) else 0f,
            )
            val from = lift.floatValue
            settle[0] = scope.launch {
                animate(
                    initialValue = from,
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.65f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ) { value, _ -> lift.floatValue = value }
            }
        }
    }.graphicsLayer { translationY = -lift.floatValue }
}

@Composable
internal fun Modifier.playerDockArt(dock: PlayerDock?, corner: Dp): Modifier {
    if (dock == null) return this
    val placed = remember { arrayOfNulls<LayoutCoordinates>(1) }
    DisposableEffect(dock) {
        onDispose { placed[0]?.let(dock::releaseMiniArt) }
    }
    return this
        .onPlaced { coordinates ->
            placed[0] = coordinates
            dock.reportMiniArt(coordinates, corner)
        }
        .graphicsLayer { alpha = if (dock.coversMiniArt) 0f else 1f }
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun MiniPlayer(
    song: LocalSong,
    isPlaying: Boolean,
    isLoading: Boolean,
    hazeState: HazeState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onExpand: () -> Unit,
    controlsLocked: Boolean = false,
    onBlockedControl: () -> Unit = {},
    dock: PlayerDock? = null,
    pull: MiniPlayerPull? = null,
    modifier: Modifier = Modifier,
) {
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = modifier
            .padding(horizontal = BAR_GUTTER)
            .miniPlayerGestures(
                pull = pull,
                onNext = {
                    haptics.play(Haptic.SkipNext)
                    onNext()
                },
                onPrevious = {
                    haptics.play(Haptic.SkipPrevious)
                    onPrevious()
                },
                locked = controlsLocked,
                onBlocked = onBlockedControl,
            )
            .clip(shape)
            .then(
                if (reduceDynamicBlur) {
                    Modifier.background(MaterialTheme.colorScheme.surface)
                } else {
                    Modifier.optimizedHazeEffect(
                        state = hazeState,
                        style = HazeMaterials.thin(MaterialTheme.colorScheme.surface),
                    )
                },
            )
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), shape)
            .clickable(onClick = onExpand),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = ROW_PADDING_HORIZONTAL,
                    vertical = ROW_PADDING_VERTICAL,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .playerDockArt(dock, ART_CORNER)
                    .clip(RoundedCornerShape(ART_CORNER))
                    .thumbnailBorder(RoundedCornerShape(ART_CORNER))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                AsyncImage(
                    model = song.artworkUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                ExplicitSongTitle(
                    song = song,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    badgeColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    fadeOverflow = true,
                )
                FadingLine(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isLoading) {
                Box(Modifier.size(GLYPH_SLOT), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onBackground,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(SPINNER_SIZE),
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        haptics.play(if (isPlaying) Haptic.Pause else Haptic.Resume)
                        onPlayPause()
                    },
                    modifier = Modifier.size(GLYPH_SLOT),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(GLYPH_SIZE),
                    )
                }
            }
            Spacer(Modifier.width(TRANSPORT_GAP))
            IconButton(
                onClick = {
                    haptics.play(Haptic.SkipNext)
                    onNext()
                },
                enabled = !controlsLocked,
                modifier = Modifier.size(GLYPH_SLOT),
            ) {
                MiniPlayerNextGlyph(
                    tint = MaterialTheme.colorScheme.onBackground
                        .copy(alpha = if (controlsLocked) 0.3f else 1f),
                )
            }
        }
    }
}

@Composable
internal fun MiniPlayerNextGlyph(tint: Color) {
    Icon(
        imageVector = AurelisIcons.SkipNext,
        contentDescription = stringResource(R.string.next),
        tint = tint,
        modifier = Modifier
            .size(NEXT_GLYPH_SIZE)
            .graphicsLayer { scaleY = 0.85f },
    )
}

private val NEXT_GLYPH_SIZE = 25.dp
