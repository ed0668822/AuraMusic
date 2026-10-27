package com.music.aura.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.PlatformContext
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.music.aura.data.model.LocalSong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

/** How many further goes a cover that failed to load gets. */
internal const val ART_RETRIES = 3

/** How long to leave it before trying a failed cover again. */
internal const val ART_RETRY_DELAY_MS = 1_500L

/**
 * How close the player's reported position has to get to a released scrub
 * handle before the handle stops being drawn where it was dropped.
 */
internal const val SEEK_SETTLE_TOLERANCE_MS = 1_500L

/**
 * How long that handle is held at the drop point regardless.
 */
internal const val SEEK_SETTLE_TIMEOUT_MS = 4_000L

/**
 * The player's cover: the one request every surface draws it from, whether it
 * has loaded, and the bounded retries after a failure.
 */
@Stable
internal class PlayerArtwork(private val context: PlatformContext) {
    var loaded by mutableStateOf(false)
        private set

    var attempt by mutableIntStateOf(0)
        private set

    internal var failed by mutableStateOf(false)

    /** The URI to load, set per track. */
    var uri: String? by mutableStateOf(null)

    val request: ImageRequest by derivedStateOf {
        val attempt = attempt
        ImageRequest.Builder(context)
            .data(uri)
            .size(ART_PX)
            .apply { if (attempt > 0) memoryCacheKeyExtra("attempt", attempt.toString()) }
            .build()
    }

    fun onState(state: AsyncImagePainter.State) {
        loaded = state is AsyncImagePainter.State.Success
        if (state is AsyncImagePainter.State.Success) failed = false
        if (state is AsyncImagePainter.State.Error) failed = true
    }

    internal fun retry() {
        failed = false
        attempt++
    }
}

/** Artwork pixel size for the player. */
internal const val ART_PX = 512

@Composable
internal fun rememberPlayerArtwork(song: LocalSong): PlayerArtwork {
    val context = LocalPlatformContext.current
    val art = remember { PlayerArtwork(context) }
    val artUri = song.artworkUri?.toString()
    LaunchedEffect(artUri) {
        art.uri = artUri
        art.loaded = false
        art.failed = false
    }
    LaunchedEffect(artUri, art.failed) {
        if (artUri == null || !art.failed || art.attempt >= ART_RETRIES) return@LaunchedEffect
        delay(ART_RETRY_DELAY_MS)
        art.retry()
    }
    return art
}

/**
 * The seek bar's own state: a drag in progress, and where the handle was
 * dropped until the player's position catches up with it.
 */
@Stable
internal class PlayerScrub {
    var scrubbing by mutableStateOf(false)
        private set
    var value by mutableFloatStateOf(0f)
        private set

    internal var pendingSeek by mutableStateOf<Float?>(null)

    fun shown(positionMs: Long, durationMs: Long): Float {
        val fraction = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
        return when {
            scrubbing -> value
            pendingSeek != null -> pendingSeek!!
            else -> fraction.coerceIn(0f, 1f)
        }
    }

    fun drag(to: Float) {
        scrubbing = true
        value = to
    }

    fun release(seekTo: (Float) -> Unit) {
        pendingSeek = value
        seekTo(value)
        scrubbing = false
    }
}

@Composable
internal fun rememberPlayerScrub(
    trackId: Long,
    positionMs: Long,
    durationMs: Long,
): PlayerScrub {
    val scrub = remember { PlayerScrub() }
    var pendingSeek by scrub::pendingSeek

    LaunchedEffect(durationMs, pendingSeek) {
        val target = pendingSeek ?: return@LaunchedEffect
        if (durationMs <= 0) return@LaunchedEffect
        snapshotFlow { positionMs }.first { pos ->
            abs(pos - (target * durationMs).toLong()) < SEEK_SETTLE_TOLERANCE_MS
        }
        pendingSeek = null
    }
    LaunchedEffect(pendingSeek) {
        if (pendingSeek == null) return@LaunchedEffect
        delay(SEEK_SETTLE_TIMEOUT_MS)
        pendingSeek = null
    }
    LaunchedEffect(trackId) { pendingSeek = null }

    return scrub
}

/**
 * The volume bar's state: the system output level, tweened to whatever the
 * system reports, and followed exactly under a finger.
 */
@Stable
internal class PlayerVolume(
    private val system: SystemVolume,
    private val scope: CoroutineScope,
) {
    val level = Animatable(system.level.value)
    var dragging by mutableStateOf(false)
        private set

    fun drag(to: Float) {
        dragging = true
        scope.launch { level.snapTo(to) }
        system.set(to)
    }

    fun release() {
        dragging = false
    }
}

@Composable
internal fun rememberPlayerVolume(systemVolume: SystemVolume): PlayerVolume {
    val scope = rememberCoroutineScope()
    val volume = remember(systemVolume) { PlayerVolume(systemVolume, scope) }
    val systemLevel by systemVolume.level.collectAsStateWithLifecycle()

    LaunchedEffect(systemLevel) {
        if (!volume.dragging) {
            volume.level.animateTo(systemLevel, tween(durationMillis = 220, easing = FastOutSlowInEasing))
        }
    }

    return volume
}
