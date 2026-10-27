package com.music.aura.ui.player

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import com.music.aura.data.model.LocalSong
import com.music.aura.data.model.PlaybackSourceType
import com.music.aura.playback.PlaybackPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The tint every secondary line and glyph in the player reads.
 * White by default; set from the artwork's palette when available.
 */
val LocalPlayerTint: CompositionLocal<Color> = staticCompositionLocalOf { Color.White }

/**
 * Holds the screen on while [enabled], releasing it when disabled or disposed.
 */
@Composable
internal fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        if (enabled) {
            view.keepScreenOn = true
        }
        onDispose {
            view.keepScreenOn = false
        }
    }
}

/**
 * Back handler scoped to the player sheet.
 * Only active while [enabled]; when active, intercepts Back before the sheet.
 */
@Composable
internal fun PlayerBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}

/**
 * Whether the player's controls are locked (e.g. during a party session).
 * For local playback this is always false.
 */
@Composable
internal fun rememberControlsLocked(): Boolean = false

/**
 * The lyric playhead: a flow of the current position in milliseconds,
 * readable only where it is drawn so position ticks don't recompose the player.
 */
internal class LyricPlayhead(val positionMs: StateFlow<Long>)

@Composable
internal fun rememberLyricPlayhead(position: PlaybackPosition): LyricPlayhead {
    return LyricPlayhead(position.positionFlow)
}

/**
 * Adjusts a lyric line's timestamp by the user's offset before seeking.
 */
internal fun adjustedLyricsSeekTarget(lineTimeMs: Long, offsetMs: Int): Long =
    (lineTimeMs - offsetMs).coerceAtLeast(0L)

/**
 * The text for the "Playing from" caption above the player.
 */
internal fun playbackOriginText(song: LocalSong): String? {
    val source = song.playbackSource
    if (!source.isNullOrBlank()) return source
    val album = song.album
    if (!album.isNullOrBlank()) return album
    return null
}

/** How long the artist marquee waits after the title starts scrolling. */
internal const val MARQUEE_ARTIST_STAGGER_MS = 1_200L

/**
 * Modifier extension: makes the composable clickable only when [browseId] is non-null.
 * For local music, album navigation uses the album name.
 */
internal fun androidx.compose.ui.Modifier.opensPage(
    album: String?,
    onOpenAlbum: (String) -> Unit,
): androidx.compose.ui.Modifier {
    if (album.isNullOrBlank()) return this
    return this.then(
        androidx.compose.foundation.clickable(
            interactionSource = null,
            indication = null,
            onClick = { onOpenAlbum(album) },
        )
    )
}
