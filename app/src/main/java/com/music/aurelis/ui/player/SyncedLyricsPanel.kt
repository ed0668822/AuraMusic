package com.music.aurelis.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.music.aurelis.data.lyrics.LyricLine
import com.music.aurelis.playback.PlaybackPosition

/**
 * Panel de letras sincronizadas para Aurelis Music.
 * Adaptado de BitChord: sin traducción ni romanización.
 * Las letras se leen localmente (embebidas o archivo .lrc).
 */
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
    // La implementación completa del panel de letras se conecta
    // con el SyncedLyricsPanel.kt ya existente en Aurelis
    // (app/src/main/java/com/music/aurelis/ui/components/SyncedLyricsPanel.kt).
    // Este wrapper proporciona la interfaz compatible con el player.
    //
    // Se integrará completamente cuando se porte LyricsPanel
    // del sharedUi de BitChord.
}
