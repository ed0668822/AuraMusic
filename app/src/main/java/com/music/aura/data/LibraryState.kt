package com.music.aura.data

import com.music.aura.data.model.LocalAlbum
import com.music.aura.data.model.LocalArtist
import com.music.aura.data.model.LocalSong

/**
 * Estado de la biblioteca musical para la UI.
 * Representa las tres fases posibles de la carga.
 */
sealed interface LibraryState {

    /** Aún no se ha iniciado la carga o está en progreso. */
    data object Loading : LibraryState

    /**
     * Carga completada con éxito.
     *
     * [albums], [artists] y [recentlyAdded] son campos derivados que se calculan
     * una sola vez al escanear. Se guardan precalculados para que el Home no
     * tenga que agrupar en cada recomposición.
     */
    data class Success(
        val songs: List<LocalSong>,
        val albums: List<LocalAlbum> = emptyList(),
        val artists: List<LocalArtist> = emptyList(),
        val recentlyAdded: List<LocalSong> = emptyList(),
    ) : LibraryState

    /** Error durante la carga (permiso denegado, fallo de MediaStore, etc.). */
    data class Error(val message: String) : LibraryState
}
