package com.music.aura.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.music.aura.playback.PlaybackViewModel
import com.music.aura.ui.components.FrostedTopBar
import com.music.aura.ui.components.SearchBar
import com.music.aura.ui.components.SongListItem
import com.music.aura.ui.components.SortMenu
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
fun LibraryScreen(
    libraryViewModel: LibraryViewModel = viewModel(),
    playbackViewModel: PlaybackViewModel,
    onSettingsClick: () -> Unit = {},
) {
    val uiState by libraryViewModel.uiState.collectAsState()
    val playbackState by playbackViewModel.state.collectAsState()
    val hazeState = remember { HazeState() }

    LaunchedEffect(Unit) {
        if (AudioPermission.isGranted(libraryViewModel.getApplication())) {
            libraryViewModel.loadLibrary()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val current = uiState.libraryState) {
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 80.dp, bottom = 8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                SearchBar(
                                    query = uiState.searchQuery,
                                    onQueryChanged = { libraryViewModel.onSearchQueryChanged(it) },
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                SortMenu(
                                    currentSort = uiState.sortOption,
                                    onSortSelected = { libraryViewModel.onSortOptionChanged(it) },
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .hazeSource(hazeState),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                bottom = 120.dp,
                            ),
                        ) {
                            val displaySongs = uiState.filteredSongs

                            if (displaySongs.isEmpty() && uiState.searchQuery.isNotBlank()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Sin resultados",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            Text(
                                                text = "No se encontró nada para \"${uiState.searchQuery}\"",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(top = 4.dp),
                                            )
                                        }
                                    }
                                }
                            } else {
                                itemsIndexed(
                                    items = displaySongs,
                                    key = { _, song -> song.id }
                                ) { index, song ->
                                    val isCurrentSong = playbackState.currentSong?.id == song.id
                                    SongListItem(
                                        song = song,
                                        isPlaying = isCurrentSong && playbackState.isPlaying,
                                        onClick = {
                                            playbackViewModel.playSongs(displaySongs, index)
                                        },
                                    )
                                }
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

        if (uiState.libraryState is LibraryState.Success) {
            val songCount = (uiState.libraryState as LibraryState.Success).songs.size
            val displayCount = uiState.filteredSongs.size
            val subtitle = if (uiState.searchQuery.isNotBlank()) {
                "$displayCount de $songCount canciones"
            } else {
                "$songCount canciones"
            }
            FrostedTopBar(
                title = "Aura Music",
                subtitle = subtitle,
                hazeState = hazeState,
                onSettingsClick = onSettingsClick,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}
