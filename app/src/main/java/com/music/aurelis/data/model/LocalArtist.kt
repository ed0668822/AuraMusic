package com.music.aurelis.data.model

/**
 * Artista derivado de las canciones locales.
 * Se agrupa por el campo artist de MediaStore.
 */
data class LocalArtist(
    val name: String,
    val artworkUri: String?,
    val songCount: Int,
    val albumCount: Int,
    val songs: List<LocalSong>,
)
