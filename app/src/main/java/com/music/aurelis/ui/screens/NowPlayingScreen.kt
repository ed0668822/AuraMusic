// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Subject
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.music.aurelis.R
import com.music.aurelis.playback.LyricsState
import com.music.aurelis.playback.PlaybackViewModel
import com.music.aurelis.playback.TranslationState
import com.music.aurelis.ui.components.AnimatedArtwork
import com.music.aurelis.ui.components.PlayerControls
import com.music.aurelis.ui.components.ThinSlider
import com.music.aurelis.ui.components.LyricsSourcesDialog
import com.music.aurelis.ui.components.TranslationLanguageDialog
import com.music.aurelis.ui.player.LyricsOffsetSheet
import com.music.aurelis.ui.player.MeshGradientBackground
import com.music.aurelis.ui.player.SyncedLyricsPanel
import com.music.aurelis.ui.player.rememberArtworkColors
import com.music.aurelis.ui.theme.ArtworkColors
import com.music.aurelis.ui.theme.DynamicAurelisTheme
import com.music.aurelis.ui.theme.DynamicThemeViewModel
import com.music.aurelis.data.SettingsRepository
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playbackViewModel: PlaybackViewModel,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onOpenQueue: () -> Unit,
) {
    val state by playbackViewModel.state.collectAsState()
    val song = state.currentSong
    val themeViewModel: DynamicThemeViewModel = viewModel()

    LaunchedEffect(song?.artworkUri) {
        themeViewModel.extractForArtwork(song?.artworkUri)
    }
    val artworkColors by themeViewModel.artworkColors.collectAsState()

    val fallbackColors = ArtworkColors(
        primary = Color(0xFFFA2D48),
        secondary = Color(0xFF8B5CF6),
        background = Color(0xFF0E0E14),
    )

    var showLyrics by remember { mutableStateOf(false) }
    var showLyricsSources by remember { mutableStateOf(false) }
    var showTranslationLanguages by remember { mutableStateOf(false) }
    var showLyricsOffset by remember { mutableStateOf(false) }

    val lyricsOffset by SettingsRepository.lyricsOffsetMs.collectAsState()
    val hazeState = remember { HazeState() }

    AnimatedVisibility(
        visible = isVisible && song != null,
        enter = slideInVertically(
            animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
            initialOffsetY = { it },
        ) + fadeIn(animationSpec = tween(280)),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            targetOffsetY = { it },
        ) + fadeOut(animationSpec = tween(220)),
    ) {
        if (song == null) return@AnimatedVisibility

        DynamicAurelisTheme(colors = artworkColors ?: fallbackColors) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .hazeSource(hazeState),
            ) {
                val meshPalette = rememberArtworkColors(song.artworkUri?.let { Uri.parse(it) })
                MeshGradientBackground(
                    palette = meshPalette,
                    trackKey = song.id,
                    continuous = state.isPlaying,
                    modifier = Modifier.fillMaxSize(),
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.15f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.25f),
                                ),
                            ),
                        ),
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Cerrar reproductor",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(32.dp),
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(onClick = { showLyricsSources = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreHoriz,
                                contentDescription = "Fuentes de letras",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(26.dp),
                            )
                        }

                        IconButton(onClick = onOpenQueue) {
                            Icon(
                                imageVector = Icons.Rounded.QueueMusic,
                                contentDescription = "Abrir cola",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val progressFraction = if (state.durationMs > 0) {
                        (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    AnimatedArtwork(
                        songKey = song.id,
                        artworkUri = song.artworkUri,
                        title = song.title,
                        size = 320.dp,
                        progressFraction = progressFraction,
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (song.filePath?.endsWith(".flac", ignoreCase = true) == true) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_lossless),
                                contentDescription = "Lossless",
                                modifier = Modifier.size(20.dp, 12.dp),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showLyrics = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Rounded.Subject, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ver Letras")
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    ThinSlider(
                        value = progressFraction,
                        onValueChange = { fraction ->
                            val newPos = (fraction * state.durationMs).toLong()
                            playbackViewModel.seekTo(newPos)
                        },
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    PlayerControls(
                        isPlaying = state.isPlaying,
                        isLoading = state.isLoading,
                        onPlayPause = { playbackViewModel.togglePlayPause() },
                        onNext = { playbackViewModel.next() },
                        onPrevious = { playbackViewModel.previous() },
                    )

                    Spacer(modifier = Modifier.height(48.dp))
                }

                if (showLyrics) {
                    val lyricsState = state.lyrics
                    val lines = when (lyricsState) {
                        is LyricsState.Loaded -> {
                            val trans = lyricsState.translation
                            if (trans is TranslationState.Done) trans.lines else lyricsState.lines
                        }
                        else -> emptyList()
                    }

                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                            .statusBarsPadding()
                    ) {
                        Column(Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { showLyrics = false }) {
                                    Icon(Icons.Rounded.KeyboardArrowDown, "Cerrar letras", tint = Color.White)
                                }
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { showTranslationLanguages = true }) {
                                    Icon(Icons.Rounded.Translate, "Traducir", tint = Color.White)
                                }
                            }

                            if (lines.isEmpty()) {
                                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = when(lyricsState) {
                                            is LyricsState.Loading -> "Buscando letras..."
                                            is LyricsState.Missing -> "No se encontraron letras."
                                            else -> "Cargando..."
                                        },
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            } else {
                                val position = remember { com.music.aurelis.playback.PlaybackPosition() }
                                LaunchedEffect(state.positionMs, lyricsOffset) {
                                    position.report(state.positionMs + lyricsOffset)
                                    position.advancing = state.isPlaying
                                }

                                SyncedLyricsPanel(
                                    lines = lines,
                                    trackKey = song.id,
                                    position = position,
                                    looking = true,
                                    isPlaying = state.isPlaying,
                                    onSeekToLine = { playbackViewModel.seekTo(it) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                if (showLyricsSources) {
                    LyricsSourcesDialog(hazeState = hazeState, onDismiss = { showLyricsSources = false })
                }
                if (showTranslationLanguages) {
                    TranslationLanguageDialog(
                        hazeState = hazeState,
                        onDismiss = {
                            showTranslationLanguages = false
                            val currentLang = SettingsRepository.translationLanguage.value
                            if (currentLang.isNotBlank()) {
                                playbackViewModel.translateLyrics(currentLang)
                            }
                        }
                    )
                }
                if (showLyricsOffset) {
                    LyricsOffsetSheet(
                        hazeState = hazeState,
                        offsetMs = lyricsOffset,
                        onOffsetChange = { SettingsRepository.setLyricsOffsetMs(it) },
                        onDismiss = { showLyricsOffset = false }
                    )
                }
            }
        }
    }
}
