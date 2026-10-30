// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.music.aurelis.data.model.LocalSong

/**
 * Envoltorio histórico sobre `SongRow`. Se conserva por compatibilidad con
 * pantallas que aún lo invocan directamente (LocalMusicScreen, etc.). En
 * internamente delega al `SongRow` completo para que la fila se vea idéntica
 * en todas partes.
 */
@Composable
fun SongListItem(
    song: LocalSong,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SongRow(
        song = song,
        onClick = onClick,
        modifier = modifier,
        isCurrent = isPlaying,
        isPlaying = isPlaying,
        searchPlayingStyle = false,
    )
}
