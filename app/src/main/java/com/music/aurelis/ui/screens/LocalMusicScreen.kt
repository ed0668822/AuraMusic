package com.music.aurelis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.music.aurelis.LibraryViewModel
import com.music.aurelis.data.LibraryState
import com.music.aurelis.playback.PlaybackViewModel
import com.music.aurelis.ui.components.SegmentedControl
import com.music.aurelis.ui.components.SongListItem
import com.music.aurelis.ui.components.home.AlbumCarousel
import com.music.aurelis.ui.components.home.ArtistCarousel

@Composable
fun LocalMusicScreen(
    playbackViewModel: PlaybackViewModel,
    onBackClick: () -> Unit,
    libraryViewModel: LibraryViewModel = viewModel(),
) {
    val uiState by libraryViewModel.uiState.collectAsState()
    val playbackState by playbackViewModel.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Songs", "Artists", "Albums")

    Column(modifier = Modifier.fillMaxSize().padding(top = 40.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { }) { Icon(Icons.Filled.Search, "Buscar") }
            IconButton(onClick = { }) { Icon(Icons.Filled.MoreVert, "Más") }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("Local Music", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Archivos de audio en el dispositivo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (uiState.libraryState is LibraryState.Success) {
                val count = (uiState.libraryState as LibraryState.Success).songs.size
                Text(
                    "$count Tracks",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = {
                        val songs = (uiState.libraryState as? LibraryState.Success)?.songs ?: return@Button
                        playbackViewModel.playSongs(songs, 0)
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Play", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = MaterialTheme.shapes.extraLarge
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
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        Spacer(Modifier.height(8.dp))

        val state = uiState.libraryState
        if (state is LibraryState.Success) {
            when (selectedTab) {
                0 -> {
                    LazyColumn(contentPadding = PaddingValues(bottom = 40.dp)) {
                        val displaySongs = uiState.filteredSongs
                        itemsIndexed(items = displaySongs, key = { _, song -> song.id }) { index, song ->
                            val isCurrent = playbackState.currentSong?.id == song.id
                            SongListItem(
                                song = song,
                                isPlaying = isCurrent && playbackState.isPlaying,
                                onClick = { playbackViewModel.playSongs(displaySongs, index) },
                            )
                        }
                    }
                }
                1 -> {
                    ArtistCarousel(
                        title = "",
                        artists = state.artists,
                        onArtistClick = { artist -> playbackViewModel.playSongs(artist.songs, 0) }
                    )
                }
                2 -> {
                    AlbumCarousel(
                        title = "",
                        albums = state.albums,
                        onAlbumClick = { album -> playbackViewModel.playSongs(album.songs, 0) }
                    )
                }
            }
        }
    }
}
