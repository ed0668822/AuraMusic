// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
    if (lines.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Letras no disponibles",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(lines) { line ->
            Text(
                text = line.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
