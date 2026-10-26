package com.music.aura.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.music.aura.LibraryViewModel
import com.music.aura.data.LibraryState
import com.music.aura.playback.PlaybackViewModel
import com.music.aura.ui.components.SearchBar
import com.music.aura.ui.components.SongListItem
import com.music.aura.ui.components.SortMenu

@Composable
fun LocalMusicScreen(
    playbackViewModel: PlaybackViewModel,
    onBackClick: () -> Unit,
    libraryViewModel: LibraryViewModel = viewModel(),
) {
    val uiState by libraryViewModel.uiState.collectAsState()
    val playbackState by playbackViewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(top = 40.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
            }
            Text(text = "Local Music", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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

        when (val state = uiState.libraryState) {
            is LibraryState.Success -> {
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
            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
