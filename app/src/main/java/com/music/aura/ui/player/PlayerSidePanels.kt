package com.music.aura.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.music.aura.data.lyrics.LyricLine
import com.music.aura.data.model.LocalSong

/**
 * Panel lateral de letras para landscape/tablet.
 * Adaptado de BitChord: se elimina traducción y romanización (streaming).
 * Las letras se leen localmente (embedidas o archivo .lrc).
 */
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
            // La implementación completa del panel de letras sincronizadas
            // se conecta con SyncedLyricsPanel.kt ya existente en Aura.
            // Se integrará en el Lote 3 con LyricClock y LyricFocus.
        }
    }
}

/**
 * Panel lateral de cola para landscape/tablet.
 * Muestra InlineQueue sin el player alrededor.
 */
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
        onJumpTo = onJumpTo,
        onRemove = onRemove,
        onMove = onMove,
        onClear = onClear,
        modifier = modifier,
    )
}
