package com.music.aura

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.music.aura.data.AudioPermission
import com.music.aura.data.LibraryState
import com.music.aura.data.LocalMediaRepository
import com.music.aura.data.model.LocalSong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel que gestiona el estado de la biblioteca musical.
 *
 * Expone un [LibraryState] que la UI observa para mostrar
 * la lista de canciones, un loader o un error.
 */
class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<LibraryState>(LibraryState.Loading)
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    /** Lista plana de canciones (acceso rápido una vez cargado). */
    val songs: List<LocalSong>
        get() = (_state.value as? LibraryState.Success)?.songs ?: emptyList()

    /**
     * Inicia el escaneo de MediaStore.
     * Verifica permiso antes de consultar.
     */
    fun loadLibrary() {
        viewModelScope.launch {
            _state.value = LibraryState.Loading

            val context = getApplication<Application>()

            if (!AudioPermission.isGranted(context)) {
                _state.value = LibraryState.Error(
                    "Permiso de acceso a música no concedido. " +
                    "Ve a Ajustes y permite el acceso a archivos de audio."
                )
                return@launch
            }

            try {
                val result = LocalMediaRepository.getAllSongs(context)
                if (result.isEmpty()) {
                    _state.value = LibraryState.Success(emptyList())
                } else {
                    _state.value = LibraryState.Success(result)
                }
            } catch (e: Exception) {
                _state.value = LibraryState.Error(
                    e.message ?: "Error desconocido al escanear la biblioteca."
                )
            }
        }
    }
}
