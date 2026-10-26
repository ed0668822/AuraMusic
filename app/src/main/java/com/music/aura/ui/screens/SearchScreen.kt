package com.music.aura.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.music.aura.LibraryViewModel
import com.music.aura.data.LibraryState
import com.music.aura.data.model.LocalSong
import com.music.aura.playback.PlaybackViewModel
import com.music.aura.ui.components.SearchField
import com.music.aura.ui.components.SongListItem

@Composable
fun SearchScreen(
    playbackViewModel: PlaybackViewModel,
    libraryViewModel: LibraryViewModel = viewModel(),
) {
    val uiState by libraryViewModel.uiState.collectAsState()
    val playbackState by playbackViewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // Filtrado local en tiempo real
    val allSongs = (uiState.libraryState as? LibraryState.Success)?.songs ?: emptyList()
    val filteredSongs = remember(searchQuery, allSongs) {
        if (searchQuery.isBlank()) allSongs
        else allSongs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true) ||
            (it.album?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(top = 60.dp)) {
        SearchField(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSubmit = { focusManager.clearFocus() },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 260.dp, top = 8.dp),
        ) {
            itemsIndexed(filteredSongs, key = { _, song -> song.id }) { index, song ->
                val isCurrent = playbackState.currentSong?.id == song.id
                SongListItem(
                    song = song,
                    isPlaying = isCurrent && playbackState.isPlaying,
                    onClick = {
                        playbackViewModel.playSongs(filteredSongs, index)
                    },
                )
            }
        }
    }
}
