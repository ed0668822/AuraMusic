package com.music.aura.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.music.aura.LibraryViewModel
import com.music.aura.data.AudioPermission
import com.music.aura.data.LibraryState
import com.music.aura.data.model.LocalSong
import com.music.aura.playback.PlaybackViewModel
import com.music.aura.ui.components.FrostedTopBar
import com.music.aura.ui.components.SongListItem
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/**
 * Pantalla principal de la biblioteca.
 * Recibe el PlaybackViewModel para lanzar reproducción al hacer clic.
 */
@Composable
fun LibraryScreen(
    libraryViewModel: LibraryViewModel = viewModel(),
    playbackViewModel: PlaybackViewModel,
) {
    val state by libraryViewModel.state.collectAsState()
    val playbackState by playbackViewModel.state.collectAsState()
    val hazeState = remember { HazeState() }

    // Cargar biblioteca al entrar
    LaunchedEffect(Unit) {
        if (AudioPermission.isGranted(libraryViewModel.getApplication())) {
            libraryViewModel.loadLibrary()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val current = state) {
            is LibraryState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Escaneando música…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                }
            }

            is LibraryState.Success -> {
                if (current.songs.isEmpty()) {
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
                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .hazeSource(hazeState),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                top = 80.dp,
                                bottom = 120.dp,
                            ),
                        ) {
                            itemsIndexed(
                                items = current.songs,
                                key = { _, song -> song.id }
                            ) { index, song ->
                                val isCurrentSong = playbackState.currentSong?.id == song.id
                                SongListItem(
                                    song = song,
                                    isPlaying = isCurrentSong && playbackState.isPlaying,
                                    onClick = {
                                        playbackViewModel.playSongs(current.songs, index)
                                    },
                                )
                            }
                        }
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
                            text = current.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Button(
                            onClick = { libraryViewModel.loadLibrary() },
                            modifier = Modifier.padding(top = 24.dp),
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
            }
        }

        // Top bar con blur (siempre visible encima del contenido)
        if (state is LibraryState.Success) {
            val songCount = (state as LibraryState.Success).songs.size
            FrostedTopBar(
                title = "Aura Music",
                subtitle = "$songCount canciones",
                hazeState = hazeState,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}
