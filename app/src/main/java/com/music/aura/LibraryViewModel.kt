package com.music.aura

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.music.aura.data.AudioPermission
import com.music.aura.data.LibraryState
import com.music.aura.data.LocalMediaRepository
import com.music.aura.data.model.LocalAlbum
import com.music.aura.data.model.LocalArtist
import com.music.aura.data.model.LocalSong
import com.music.aura.data.model.SortOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Estado de la UI de biblioteca incluyendo búsqueda y ordenamiento.
 */
data class LibraryUiState(
    val libraryState: LibraryState = LibraryState.Loading,
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.TITLE_ASC,
    val filteredSongs: List<LocalSong> = emptyList(),
)

/**
 * ViewModel que gestiona la biblioteca musical con búsqueda y ordenamiento.
 */
class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    /** Todas las canciones cargadas (sin filtro). */
    private var allSongs: List<LocalSong> = emptyList()

    /**
     * Guard contra doble escaneo. Como MainActivity comparte esta instancia
     * entre HomeScreen y LibraryScreen, ambos pueden llamar a [loadLibrary]
     * en su LaunchedEffect inicial; sin este flag, se escanearía dos veces.
     */
    private var scanned: Boolean = false

    /**
     * Inicia el escaneo de MediaStore.
     * Si ya se escaneó, no hace nada salvo que [force] sea true.
     */
    fun loadLibrary(force: Boolean = false) {
        if (scanned && !force) return
        scanned = true

        viewModelScope.launch {
            _uiState.update { it.copy(libraryState = LibraryState.Loading) }

            val context = getApplication<Application>()

            if (!AudioPermission.isGranted(context)) {
                scanned = false
                _uiState.update {
                    it.copy(
                        libraryState = LibraryState.Error(
                            "Permiso de acceso a música no concedido. " +
                            "Ve a Ajustes y permite el acceso a archivos de audio."
                        )
                    )
                }
                return@launch
            }

            try {
                val result = LocalMediaRepository.getAllSongs(context)
                allSongs = result
                val (albums, artists, recent) = deriveGroupings(result)
                _uiState.update {
                    it.copy(
                        libraryState = LibraryState.Success(
                            songs = result,
                            albums = albums,
                            artists = artists,
                            recentlyAdded = recent,
                        )
                    )
                }
                applyFilterAndSort()
            } catch (e: Exception) {
                scanned = false
                _uiState.update {
                    it.copy(
                        libraryState = LibraryState.Error(
                            e.message ?: "Error desconocido al escanear la biblioteca."
                        )
                    )
                }
            }
        }
    }

    /**
     * Actualiza la consulta de búsqueda y re-filtra.
     */
    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilterAndSort()
    }

    /**
     * Cambia la opción de ordenamiento y re-ordena.
     */
    fun onSortOptionChanged(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
        applyFilterAndSort()
    }

    /**
     * Deriva álbumes, artistas y la lista de "añadidos recientemente" desde
     * las canciones crudas. Se ejecuta una sola vez por escaneo.
     */
    private fun deriveGroupings(
        songs: List<LocalSong>
    ): Triple<List<LocalAlbum>, List<LocalArtist>, List<LocalSong>> {
        // Álbumes: agrupamos por (album + artist) para evitar colisiones
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

        // Artistas
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

        // Recientes: 20 más recientes por dateAddedMs (excluye 0).
        val recent = songs
            .filter { it.dateAddedMs > 0L }
            .sortedByDescending { it.dateAddedMs }
            .take(20)

        return Triple(albums, artists, recent)
    }

    /**
     * Aplica filtro de búsqueda + ordenamiento sobre [allSongs].
     */
    private fun applyFilterAndSort() {
        val state = _uiState.value
        val query = state.searchQuery.trim().lowercase(Locale.getDefault())
        val sort = state.sortOption

        var result = allSongs

        // Filtrar por búsqueda
        if (query.isNotBlank()) {
            result = result.filter { song ->
                song.title.lowercase(Locale.getDefault()).contains(query) ||
                song.artist.lowercase(Locale.getDefault()).contains(query) ||
                (song.album?.lowercase(Locale.getDefault())?.contains(query) == true)
            }
        }

        // Ordenar
        result = when (sort) {
            SortOption.TITLE_ASC -> result.sortedBy {
                it.title.lowercase(Locale.getDefault())
            }
            SortOption.TITLE_DESC -> result.sortedByDescending {
                it.title.lowercase(Locale.getDefault())
            }
            SortOption.ARTIST_ASC -> result.sortedBy {
                it.artist.lowercase(Locale.getDefault())
            }
            SortOption.ARTIST_DESC -> result.sortedByDescending {
                it.artist.lowercase(Locale.getDefault())
            }
            SortOption.ALBUM_ASC -> result.sortedBy {
                (it.album ?: "").lowercase(Locale.getDefault())
            }
            SortOption.ALBUM_DESC -> result.sortedByDescending {
                (it.album ?: "").lowercase(Locale.getDefault())
            }
            SortOption.DURATION_ASC -> result.sortedBy { it.durationMs }
            SortOption.DURATION_DESC -> result.sortedByDescending { it.durationMs }
        }

        _uiState.update { it.copy(filteredSongs = result) }
    }
}
