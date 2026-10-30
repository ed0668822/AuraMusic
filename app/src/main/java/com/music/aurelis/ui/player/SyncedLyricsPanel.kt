// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.music.aurelis.data.lyrics.LyricLine
import com.music.aurelis.playback.PlaybackPosition

@Composable
fun SyncedLyricsPanel(
    lines: List<LyricLine>,
    trackKey: String,
    position: PlaybackPosition,
    looking: Boolean,
    isPlaying: Boolean,
    onSeekToLine: (Long) -> Unit,
    centerFocus: Boolean = false,
    modifier: Modifier = Modifier,
) {
    LyricsPanel(
        lines = lines,
        trackKey = trackKey,
        playhead = rememberLyricPlayhead(position),
        looking = looking,
        isPlaying = isPlaying,
        onSeekToLine = onSeekToLine,
        controlsOpen = true,
        onRevealControls = {},
        onHideControls = {},
        centerFocus = centerFocus,
        modifier = modifier,
    )
}
