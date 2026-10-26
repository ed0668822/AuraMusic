package com.music.aura.ui.screens

import android.media.AudioManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.music.aura.R
import com.music.aura.playback.PlaybackViewModel
import com.music.aura.ui.components.AnimatedArtwork
import com.music.aura.ui.components.PlayerControls
import com.music.aura.ui.components.ThinSlider
import com.music.aura.ui.theme.ArtworkColors
import com.music.aura.ui.theme.DynamicAuraTheme
import com.music.aura.ui.theme.DynamicThemeViewModel
import com.music.aura.ui.theme.MeshGradient

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

        DynamicAuraTheme(colors = artworkColors ?: fallbackColors) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                // Fondo animado
                MeshGradient(
                    colors = artworkColors ?: fallbackColors,
                    isPlaying = state.isPlaying,
                    modifier = Modifier.fillMaxSize(),
                )

                // Scrim
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
                        size = 340.dp,
                        progressFraction = progressFraction,
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Título, Artista, Álbum
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = song.album ?: "Unknown Album",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Texto Técnico (FLAC · 16-bit...)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "FLAC · 16-bit · 44.1 kHz · Stereo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Text(
                            text = "Automix · this song: analysed · next: analysed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Slider de Progreso
                    ThinSlider(
                        value = progressFraction,
                        onValueChange = { fraction ->
                            val newPos = (fraction * state.durationMs).toLong()
                            playbackViewModel.seekTo(newPos)
                        },
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(state.positionMs),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                             Image(
                                painter = painterResource(id = R.drawable.ic_lossless),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp, 10.dp),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Lossless",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Text(
                            text = "-${formatTime(state.durationMs - state.positionMs)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Controles
                    PlayerControls(
                        isPlaying = state.isPlaying,
                        isLoading = state.isLoading,
                        onPlayPause = { playbackViewModel.togglePlayPause() },
                        onNext = { playbackViewModel.next() },
                        onPrevious = { playbackViewModel.previous() },
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Slider de Volumen
                    SystemVolumeSlider()

                    Spacer(modifier = Modifier.height(24.dp))

                    // Bottom Row (Lyrics, Audio, Profile, Queue)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showLyrics = true }) {
                            Icon(Icons.Rounded.FormatQuote, contentDescription = "Letras", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = { /* TODO: Output */ }) {
                            Icon(Icons.Rounded.Smartphone, contentDescription = "Salida", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = { /* TODO: Profile */ }) {
                            Icon(Icons.Rounded.Person, contentDescription = "Perfil", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = onOpenQueue) {
                            Icon(Icons.Rounded.FormatListBulleted, contentDescription = "Cola", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                if (showLyrics) {
                    LyricsScreen(
                        lyrics = state.lyrics,
                        positionMs = state.positionMs,
                        artworkUri = song.artworkUri,
                        onDismiss = { showLyrics = false }
                    )
                }
            }
        }
    }
}

@Composable
fun SystemVolumeSlider() {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    var volume by remember { mutableFloatStateOf(0f) }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }

    LaunchedEffect(Unit) {
        volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.VolumeMute, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
        Slider(
            value = volume,
            onValueChange = { 
                volume = it
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, it.toInt(), 0)
            },
            valueRange = 0f..maxVolume,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                activeTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            ),
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        )
        Icon(Icons.Rounded.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
