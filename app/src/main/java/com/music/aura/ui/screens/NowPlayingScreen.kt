package com.music.aura.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Subject
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.music.aura.R
import com.music.aura.playback.PlaybackViewModel
import com.music.aura.ui.components.*
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
                MeshGradient(
                    colors = artworkColors ?: ArtworkColors(),
                    isPlaying = state.isPlaying,
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

                    Spacer(modifier = Modifier.height(32.dp))

                    // Título con Iconos de Calidad
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
                        // Icono Lossless (puedes condicionarlo a si es FLAC o WAV)
                        if (song.filePath?.endsWith(".flac", ignoreCase = true) == true) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_lossless),
                                contentDescription = "Lossless",
                                modifier = Modifier.size(20.dp, 12.dp),
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
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
                    ModalBottomSheet(
                        onDismissRequest = { showLyrics = false },
                        containerColor = MaterialTheme.colorScheme.background,
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ) {
                        if (state.lyrics != null && state.lyrics!!.isSynced) {
                            SyncedLyricsPanel(
                                lyrics = state.lyrics!!,
                                positionMs = state.positionMs,
                                modifier = Modifier.fillMaxHeight(0.9f)
                            )
                        } else {
                            Box(Modifier.fillMaxHeight(0.9f), contentAlignment = Alignment.Center) {
                                Text("Letras no sincronizadas", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
