package com.music.aura.data.model

/**
 * Modelo de una canción local del dispositivo.
 * Todos los datos provienen de MediaStore (escaneo local, sin red).
 */
data class LocalSong(
    /** ID único del MediaStore */
    val id: String,
    /** Título de la canción */
    val title: String,
    /** Artista */
    val artist: String,
    /** Álbum */
    val album: String?,
    /** URI content:// del archivo de audio */
    val contentUri: String,
    /** URI del artwork del álbum (content://media/external/audio/albumart/ID) */
    val artworkUri: String?,
    /** Duración en milisegundos */
    val durationMs: Long,
    /** Ruta absoluta del archivo (para fallback) */
    val filePath: String?,
    /** Número de pista en el álbum */
    val trackNumber: Int?,
    /** Año de la pista */
    val year: Int?,
    /** Fecha de adición al MediaStore, en milisegundos (0 si no disponible). */
    val dateAddedMs: Long = 0L,
) {
    /** Duración formateada como "M:SS" */
    val durationText: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}
