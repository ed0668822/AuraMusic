package com.music.aura.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Subject
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.music.aura.playback.PlaybackViewModel
import com.music.aura.ui.components.AnimatedArtwork
import com.music.aura.ui.components.LyricsSheet
import com.music.aura.ui.components.PlayerControls
import com.music.aura.ui.components.ThinSlider
import com.music.aura.ui.theme.ArtworkColors
import com.music.aura.ui.theme.DynamicAuraTheme
import com.music.aura.ui.theme.DynamicThemeViewModel
import com.music.aura.ui.theme.MeshGradient

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

    var showLyrics by remember { mutableStateOf(false) }

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

        DynamicAuraTheme(colors = artworkColors) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                // Fondo: MeshGradient animado con colores del artwork
                MeshGradient(
                    colors = artworkColors ?: ArtworkColors(),
                    isPlaying = state.isPlaying,
                    modifier = Modifier.fillMaxSize(),
                )

                // Scrim vertical MUY suave
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
                    // Header
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

                        IconButton(onClick = { showLyrics = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Subject,
                                contentDescription = "Ver letras",
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

                    Spacer(modifier = Modifier.height(40.dp))

                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

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

                    if (!song.album.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = song.album,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Usamos el nuevo ThinSlider de BitChord
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
                    LyricsSheet(
                        lyrics = state.lyrics,
                        positionMs = state.positionMs,
                        onDismiss = { showLyrics = false },
                    )
                }
            }
        }
    }
}
