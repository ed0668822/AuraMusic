// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.playback

/**
 * Aurelis reproduce únicamente archivos locales (MediaStore / SAF). No hay
 * fuentes remotas con calidades alternativas de las que caer, por lo que este
 * objeto conserva la API de BitChord por compatibilidad futura pero no hace
 * nada en la reproducción actual.
 */
internal object PlaybackFallback {
    fun isAlternative(uri: String, substitutedYouTube: Boolean): Boolean = false
    fun directYouTubeSourceUri(uri: String): String = uri
}
