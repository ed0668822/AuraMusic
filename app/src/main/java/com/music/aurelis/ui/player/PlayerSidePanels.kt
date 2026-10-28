package com.music.aurelis.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.music.aurelis.data.lyrics.LyricLine
import com.music.aurelis.data.model.LocalSong

@Composable
fun LyricsSidePanel(
    song: LocalSong,
    isPlaying: Boolean,
    positionMs: Long,
    lyrics: List<LyricLine>?,
    lyricsUnavailable: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        Box(Modifier.fillMaxSize()) {
            // Estructura visual del panel de letras.
        }
    }
}

@Composable
fun QueueSidePanel(
    queue: List<LocalSong>,
    queueIndex: Int,
    onJumpTo: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InlineQueue(
        queue = queue,
        currentIndex = queueIndex,
        autoplayEnabled = true,
        controlsLocked = false,
        onJumpTo = onJumpTo,
        onRemove = onRemove,
        onMove = onMove,
        onClear = onClear,
        modifier = modifier,
    )
}
