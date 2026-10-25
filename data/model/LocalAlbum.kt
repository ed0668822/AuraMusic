package com.music.aura.data.model

/**
 * Álbum derivado de las canciones locales.
 * Agrupa por (nombre de álbum + artista) para no mezclar artistas distintos
 * que casualmente compartan el nombre del álbum.
 */
data class LocalAlbum(
    val name: String,
    val artist: String,
    val artworkUri: String?,
    val songCount: Int,
    val songs: List<LocalSong>,
) {
    /** Clave única dentro de la biblioteca. */
    val key: String get() = "$name|$artist"
}
