// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.data.lyrics.LyricLine
import com.music.aurelis.data.lyrics.LyricsSource
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.playback.PlaybackPosition
import com.music.aurelis.ui.LyricsProviderState

@Composable
fun LyricsSidePanel(
    song: LocalSong,
    isPlaying: Boolean,
    position: PlaybackPosition,
    lyrics: List<LyricLine>?,
    lyricsSource: LyricsSource?,
    lyricsProviderStates: Map<LyricsSource, LyricsProviderState>,
    onSelectLyricsProvider: (LyricsSource) -> Unit,
    lyricsUnavailable: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lyricsOffsetMs by SettingsRepository.lyricsOffsetMs.collectAsState()
    val seekToLyric: (Long) -> Unit = { lineTimeMs ->
        onSeek(lineTimeMs + lyricsOffsetMs)
    }

    Box(modifier) {
        SyncedLyricsPanel(
            lines = lyrics.orEmpty(),
            trackKey = song.id,
            position = position,
            looking = !lyricsUnavailable,
            isPlaying = isPlaying,
            onSeekToLine = seekToLyric,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun QueueSidePanel(
    queue: List<LocalSong>,
    queueIndex: Int,
    autoplayEnabled: Boolean,
    onJumpTo: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InlineQueue(
        queue = queue,
        currentIndex = queueIndex,
        autoplayEnabled = autoplayEnabled,
        controlsLocked = false,
        onJumpTo = onJumpTo,
        onRemove = onRemove,
        onMove = onMove,
        onClear = onClear,
        modifier = modifier,
    )
}
