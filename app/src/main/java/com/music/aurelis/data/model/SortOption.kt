package com.music.aurelis.data.model

/**
 * Opciones de ordenamiento disponibles para la biblioteca.
 */
enum class SortOption(val label: String) {
    TITLE_ASC("Título (A-Z)"),
    TITLE_DESC("Título (Z-A)"),
    ARTIST_ASC("Artista (A-Z)"),
    ARTIST_DESC("Artista (Z-A)"),
    ALBUM_ASC("Álbum (A-Z)"),
    ALBUM_DESC("Álbum (Z-A)"),
    DURATION_ASC("Duración (corta)"),
    DURATION_DESC("Duración (larga)"),
}
