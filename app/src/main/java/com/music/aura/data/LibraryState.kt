package com.music.aura.data

import com.music.aura.data.model.LocalSong

/**
 * Estado de la biblioteca musical para la UI.
 * Representa las tres fases posibles de la carga.
 */
sealed interface LibraryState {

    /** Aún no se ha iniciado la carga o está en progreso. */
    data object Loading : LibraryState

    /** Carga completada con éxito. */
    data class Success(val songs: List<LocalSong>) : LibraryState

    /** Error durante la carga (permiso denegado, fallo de MediaStore, etc.). */
    data class Error(val message: String) : LibraryState
}
