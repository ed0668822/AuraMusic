// Aurelis © 2026. GPL-3.0-or-later.

package com.music.aurelis

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.music.aurelis.data.AudioPermission
import com.music.aurelis.data.LibraryState
import com.music.aurelis.data.LocalMediaRepository
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.data.model.LocalAlbum
import com.music.aurelis.data.model.LocalArtist
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.SortOption
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class LibraryUiState(
    val libraryState: LibraryState = LibraryState.Loading,
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.TITLE_ASC,
    val filteredSongs: List<LocalSong> = emptyList(),
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private var allSongs: List<LocalSong> = emptyList()

    @Volatile
    private var scanned: Boolean = false

    private var scanJob: Job? = null

    fun loadLibrary(force: Boolean = false) {
        if (scanned && !force) return
        if (scanJob?.isActive == true) return

        scanned = true

        scanJob = viewModelScope.launch {
            _uiState.update { it.copy(libraryState = LibraryState.Loading) }

            val context = getApplication<Application>()

            if (!AudioPermission.isGranted(context)) {
                scanned = false
                _uiState.update {
                    it.copy(
                        libraryState = LibraryState.Error(
                            "Permiso de acceso a música no concedido. " +
                                "Concédelo en Ajustes para escanear el dispositivo.",
                        ),
                    )
                }
                return@launch
            }

            try {
                val filterNonMusic = SettingsRepository.filterNonMusicAudio.value
                val result = LocalMediaRepository.getAllSongs(context, filterNonMusic)
                allSongs = result
                val (albums, artists, recent) = deriveGroupings(result)
                _uiState.update {
                    it.copy(
                        libraryState = LibraryState.Success(
                            songs = result,
                            albums = albums,
                            artists = artists,
                            recentlyAdded = recent,
                        ),
                    )
                }
                applyFilterAndSort()
            } catch (e: Exception) {
                scanned = false
                _uiState.update {
                    it.copy(
                        libraryState = LibraryState.Error(
                            e.message ?: "Error desconocido al escanear la biblioteca.",
                        ),
                    )
                }
            }
        }
    }

    fun refresh() = loadLibrary(force = true)

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilterAndSort()
    }

    fun onSortOptionChanged(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
        applyFilterAndSort()
    }

    private fun deriveGroupings(
        songs: List<LocalSong>,
    ): Triple<List<LocalAlbum>, List<LocalArtist>, List<LocalSong>> {
        val albums = songs
            .filter { !it.album.isNullOrBlank() }
            .groupBy { (it.album ?: "") to it.artist }
            .map { (key, list) ->
                LocalAlbum(
                    name = key.first,
                    artist = key.second,
                    artworkUri = list.firstNotNullOfOrNull { it.artworkUri },
                    songCount = list.size,
                    songs = list.sortedBy { it.trackNumber ?: Int.MAX_VALUE },
                )
            }
            .sortedBy { it.name.lowercase(Locale.getDefault()) }

        val artists = songs
            .groupBy { it.artist }
            .map { (name, list) ->
                LocalArtist(
                    name = name,
                    artworkUri = list.firstNotNullOfOrNull { it.artworkUri },
                    songCount = list.size,
                    albumCount = list.mapNotNull { it.album }.distinct().size,
                    songs = list,
                )
            }
            .sortedBy { it.name.lowercase(Locale.getDefault()) }

        val recent = songs
            .filter { it.dateAddedMs > 0L }
            .sortedByDescending { it.dateAddedMs }
            .take(20)

        return Triple(albums, artists, recent)
    }

    private fun applyFilterAndSort() {
        val state = _uiState.value
        val query = state.searchQuery.trim().lowercase(Locale.getDefault())
        val sort = state.sortOption

        var result = allSongs

        if (query.isNotBlank()) {
            result = result.filter { song ->
                song.title.lowercase(Locale.getDefault()).contains(query) ||
                    song.artist.lowercase(Locale.getDefault()).contains(query) ||
                    (song.album?.lowercase(Locale.getDefault())?.contains(query) == true)
            }
        }

        result = when (sort) {
            SortOption.TITLE_ASC -> result.sortedBy { it.title.lowercase(Locale.getDefault()) }
            SortOption.TITLE_DESC -> result.sortedByDescending { it.title.lowercase(Locale.getDefault()) }
            SortOption.ARTIST_ASC -> result.sortedBy { it.artist.lowercase(Locale.getDefault()) }
            SortOption.ARTIST_DESC -> result.sortedByDescending { it.artist.lowercase(Locale.getDefault()) }
            SortOption.ALBUM_ASC -> result.sortedBy { (it.album ?: "").lowercase(Locale.getDefault()) }
            SortOption.ALBUM_DESC -> result.sortedByDescending { (it.album ?: "").lowercase(Locale.getDefault()) }
            SortOption.DURATION_ASC -> result.sortedBy { it.durationMs }
            SortOption.DURATION_DESC -> result.sortedByDescending { it.durationMs }
        }

        _uiState.update { it.copy(filteredSongs = result) }
    }
}
