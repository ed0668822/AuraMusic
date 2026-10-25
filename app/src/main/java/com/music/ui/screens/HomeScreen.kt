package com.music.aura.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.music.aura.LibraryViewModel
import com.music.aura.data.AudioPermission
import com.music.aura.data.LibraryState
import com.music.aura.playback.PlaybackViewModel
import com.music.aura.ui.components.home.AlbumCarousel
import com.music.aura.ui.components.home.ArtistCarousel
import com.music.aura.ui.components.home.HomeHero
import com.music.aura.ui.components.home.SongCarousel

/**
 * Pantalla de inicio de Aura Music.
 *
 * Estilo portado de BitChord: hero card con artwork difuminado + carruseles
 * horizontales de canciones, álbumes y artistas. 100% local — todos los datos
 * vienen del MediaStore del dispositivo.
 */
@Composable
fun HomeScreen(
    playbackViewModel: PlaybackViewModel,
    libraryViewModel: LibraryViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by libraryViewModel.uiState.collectAsState()
    val playbackState by playbackViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        if (AudioPermission.isGranted(libraryViewModel.getApplication())) {
            libraryViewModel.loadLibrary()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState.libraryState) {

            is LibraryState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Escaneando tu música…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                }
            }

            is LibraryState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp),
                    ) {
                        Text(
                            text = "⚠",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Button(
                            onClick = { libraryViewModel.loadLibrary(force = true) },
                            modifier = Modifier.padding(top = 24.dp),
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
            }

            is LibraryState.Success -> {
                if (state.songs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "♪",
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "No se encontró música",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                            Text(
                                text = "Añade canciones a tu dispositivo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                } else {
                    val latest = state.recentlyAdded.firstOrNull() ?: state.songs.first()

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            top = 56.dp,
                            bottom = 180.dp,
                        ),
                    ) {
                        item {
                            HomeHero(
                                song = latest,
                                onPlayClick = {
                                    val idx = state.songs.indexOfFirst { it.id == latest.id }
                                    playbackViewModel.playSongs(
                                        state.songs,
                                        if (idx >= 0) idx else 0,
                                    )
                                },
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            )
                        }

                        if (state.recentlyAdded.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(8.dp))
                                SongCarousel(
                                    title = "Añadido recientemente",
                                    songs = state.recentlyAdded,
                                    currentSongId = playbackState.currentSong?.id,
                                    isPlaying = playbackState.isPlaying,
                                    onSongClick = { index ->
                                        playbackViewModel.playSongs(state.recentlyAdded, index)
                                    },
                                )
                            }
                        }

                        if (state.albums.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(8.dp))
                                AlbumCarousel(
                                    title = "Álbumes",
                                    albums = state.albums,
                                    onAlbumClick = { album ->
                                        playbackViewModel.playSongs(album.songs, 0)
                                    },
                                )
                            }
                        }

                        if (state.artists.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(8.dp))
                                ArtistCarousel(
                                    title = "Artistas",
                                    artists = state.artists,
                                    onArtistClick = { artist ->
                                        playbackViewModel.playSongs(artist.songs, 0)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
