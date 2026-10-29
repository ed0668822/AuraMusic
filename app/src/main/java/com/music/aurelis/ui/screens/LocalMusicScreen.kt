// Aurelis © 2026. GPL-3.0-or-later.

package com.music.aurelis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.music.aurelis.LibraryViewModel
import com.music.aurelis.data.LibraryState
import com.music.aurelis.data.model.LocalAlbum
import com.music.aurelis.data.model.LocalArtist
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.SortOption
import com.music.aurelis.playback.PlaybackViewModel
import com.music.aurelis.ui.components.SegmentedControl
import com.music.aurelis.ui.components.SongListItem

@Composable
fun LocalMusicScreen(
    playbackViewModel: PlaybackViewModel,
    onBackClick: () -> Unit,
    libraryViewModel: LibraryViewModel = viewModel(),
) {
    val uiState by libraryViewModel.uiState.collectAsState()
    val playbackState by playbackViewModel.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val tabs = listOf("Canciones", "Artistas", "Álbumes")

    val success = uiState.libraryState as? LibraryState.Success
    val allSongs = success?.songs ?: emptyList()

    val displaySongs: List<LocalSong> = remember(uiState.filteredSongs, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.filteredSongs
        } else {
            uiState.filteredSongs.filter {
                it.title.contains(searchQuery, true) ||
                    it.artist.contains(searchQuery, true) ||
                    it.album?.contains(searchQuery, true) == true
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(top = 40.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                showSearch = !showSearch
                if (!showSearch) searchQuery = ""
            }) {
                Icon(Icons.Filled.Search, contentDescription = "Buscar")
            }
            Box {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.Filled.Sort, contentDescription = "Ordenar")
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                ) {
                    SortOption.entries.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt.label) },
                            onClick = {
                                showSortMenu = false
                                libraryViewModel.onSortOptionChanged(opt)
                            },
                        )
                    }
                }
            }
            IconButton(onClick = { libraryViewModel.loadLibrary(force = true) }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Recargar")
            }
        }

        if (showSearch) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar canciones") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("Local Music", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Archivos de audio en el dispositivo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${allSongs.size} canciones",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = {
                        if (displaySongs.isNotEmpty()) {
                            playbackViewModel.playSongs(displaySongs, 0)
                        }
                    },
                    enabled = displaySongs.isNotEmpty(),
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Play", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = {
                        if (displaySongs.isNotEmpty()) {
                            val shuffled = displaySongs.shuffled()
                            playbackViewModel.playSongs(shuffled, 0)
                        }
                    },
                    enabled = displaySongs.isNotEmpty(),
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Icon(Icons.Filled.Shuffle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Shuffle", fontWeight = FontWeight.Bold)
                }
            }
        }

        SegmentedControl(
            options = tabs,
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it },
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )

        Spacer(Modifier.height(8.dp))

        when (val state = uiState.libraryState) {
            is LibraryState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is LibraryState.Error -> {
                Box(
                    Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { libraryViewModel.loadLibrary(force = true) }) {
                            Text("Reintentar")
                        }
                    }
                }
            }
            is LibraryState.Success -> {
                when (selectedTab) {
                    0 -> {
                        if (displaySongs.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    if (searchQuery.isBlank()) "No hay canciones"
                                    else "Sin resultados",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            LazyColumn(contentPadding = PaddingValues(bottom = 40.dp)) {
                                itemsIndexed(
                                    items = displaySongs,
                                    key = { _, s -> s.id },
                                ) { index, song ->
                                    val isCurrent = playbackState.currentSong?.id == song.id
                                    SongListItem(
                                        song = song,
                                        isPlaying = isCurrent && playbackState.isPlaying,
                                        onClick = {
                                            playbackViewModel.playSongs(displaySongs, index)
                                        },
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        if (state.artists.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No hay artistas", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            ArtistList(
                                artists = state.artists,
                                onArtistClick = { artist ->
                                    if (artist.songs.isNotEmpty()) {
                                        playbackViewModel.playSongs(artist.songs, 0)
                                    }
                                },
                            )
                        }
                    }
                    2 -> {
                        if (state.albums.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No hay álbumes", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            AlbumList(
                                albums = state.albums,
                                onAlbumClick = { album ->
                                    if (album.songs.isNotEmpty()) {
                                        playbackViewModel.playSongs(album.songs, 0)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistList(
    artists: List<LocalArtist>,
    onArtistClick: (LocalArtist) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 40.dp)) {
        items(artists, key = { it.name }) { artist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onArtistClick(artist) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        artist.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${artist.songCount} canciones · ${artist.albumCount} álbumes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumList(
    albums: List<LocalAlbum>,
    onAlbumClick: (LocalAlbum) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 40.dp)) {
        items(albums, key = { it.key }) { album ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAlbumClick(album) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Album,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        album.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${album.artist} · ${album.songCount} canciones",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
