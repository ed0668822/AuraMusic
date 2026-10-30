// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.aurelis.data.lyrics.LyricLine
import com.music.aurelis.data.lyrics.LyricsSource
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.playback.PlaybackPosition
import com.music.aurelis.ui.LyricsProviderState
import com.music.aurelis.ui.haptics.rememberHaptics
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

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
    val haptics = rememberHaptics()
    val lyricsOffsetMs by PlayerSettings.lyricsOffsetMs.collectAsStateWithLifecycle()
    val lyricsPlayhead = rememberLyricPlayhead(position)
    val seekToLyric: (Long) -> Unit = { lineTimeMs ->
        onSeek(adjustedLyricsSeekTarget(lineTimeMs, lyricsOffsetMs))
    }
    val lyricsLoadingLines = listOf("Buscando letras...", "Cargando...", "Esperando...")
    val lyricsLoadingText = remember(song.id, lyricsLoadingLines) {
        lyricsLoadingLines.randomOrNull().orEmpty()
    }
    val lyricsTranslation = rememberLyricsTranslation(
        trackId = song.id,
        lyrics = lyrics,
        lyricsSource = lyricsSource,
        lyricsUnavailable = lyricsUnavailable,
        loadingText = lyricsLoadingText,
        haptics = haptics,
    )
    val haze = remember { HazeState() }
    var showLyricsProviders by remember { mutableStateOf(false) }

    Box(modifier) {
        Box(Modifier.fillMaxSize().hazeSource(haze)) {
            LandscapeLyricsPane(
                hasLyrics = lyricsTranslation.displayedLyrics.isNotEmpty(),
                placeholder = if (lyricsUnavailable) "Letras no disponibles" else lyricsLoadingText,
                status = lyricsTranslation.status,
                onStatusClick = { showLyricsProviders = true },
                romanizationToggle = {
                    RomanizationToggleButton(
                        state = lyricsTranslation.romanizationState,
                        showingRomanization = lyricsTranslation.showingRomanization,
                        enabled = !lyrics.isNullOrEmpty(),
                        onClick = lyricsTranslation.toggleRomanization,
                    )
                },
                translationToggle = {
                    TranslationToggleButton(
                        state = lyricsTranslation.translationState,
                        showingTranslation = lyricsTranslation.showingTranslation,
                        enabled = !lyrics.isNullOrEmpty(),
                        onClick = lyricsTranslation.toggleTranslation,
                    )
                },
            ) { panelModifier ->
                LyricsTranslationMotion(
                    trigger = lyricsTranslation.transition,
                    reduceMotion = lyricsTranslation.reduceMotion,
                    modifier = panelModifier,
                ) { particleProgress ->
                    LyricsPanel(
                        lines = lyrics.orEmpty(),
                        subLines = lyricsTranslation.subLines,
                        trackKey = song.id,
                        playhead = lyricsPlayhead,
                        looking = !lyricsUnavailable,
                        isPlaying = isPlaying && position.advancing,
                        onSeekToLine = seekToLyric,
                        controlsOpen = true,
                        onRevealControls = {},
                        onHideControls = {},
                        translationProgress = particleProgress,
                        centerFocus = true,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        if (showLyricsProviders) {
            LyricsProviderSheet(
                hazeState = haze,
                currentSource = lyricsSource,
                states = lyricsProviderStates,
                onSelect = onSelectLyricsProvider,
                onDismiss = { showLyricsProviders = false },
            )
        }
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
