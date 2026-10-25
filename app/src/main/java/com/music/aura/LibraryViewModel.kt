package com.music.aura

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.music.aura.data.AudioPermission
import com.music.aura.data.LibraryState
import com.music.aura.data.LocalMediaRepository
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
     * Inicia el escaneo de MediaStore.
     */
    fun loadLibrary() {
        viewModelScope.launch {
            _uiState.update { it.copy(libraryState = LibraryState.Loading) }

            val context = getApplication<Application>()

            if (!AudioPermission.isGranted(context)) {
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
                _uiState.update {
                    it.copy(libraryState = LibraryState.Success(result))
                }
                applyFilterAndSort()
            } catch (e: Exception) {
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
