package com.music.aura.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.music.aura.data.model.LocalSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositorio que escanea el MediaStore del dispositivo para obtener
 * todas las canciones de audio locales.
 *
 * 100% offline: solo lee la base de datos local de Android.
 * No realiza ninguna petición de red.
 */
object LocalMediaRepository {

    /** Duración mínima para considerar un archivo como "música" (30 segundos). */
    private const val MIN_DURATION_MS = 30_000L

    /**
     * Escanea MediaStore y devuelve todas las canciones locales.
     * Debe llamarse desde una coroutine (usa Dispatchers.IO internamente).
     */
    suspend fun getAllSongs(context: Context): List<LocalSong> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<LocalSong>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND " +
                "${MediaStore.Audio.Media.DURATION} >= ?"

        val selectionArgs = arrayOf(MIN_DURATION_MS.toString())

        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val albumArtBaseUri = Uri.parse("content://media/external/audio/albumart")

        context.contentResolver.query(
            contentUri,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val trackCol = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
            val yearCol = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
            val dateAddedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol) ?: continue
                val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                val album = cursor.getString(albumCol)?.takeIf { it.isNotBlank() && it != "<unknown>" }
                val albumId = cursor.getLong(albumIdCol)
                val durationMs = cursor.getLong(durationCol)
                val filePath = cursor.getString(dataCol)
                val trackNumber = if (trackCol >= 0 && !cursor.isNull(trackCol)) {
                    parseTrackNumber(cursor.getInt(trackCol))
                } else null
                val year = if (yearCol >= 0 && !cursor.isNull(yearCol)) {
                    cursor.getInt(yearCol).takeIf { it > 0 }
                } else null
                // DATE_ADDED viene en segundos; lo convertimos a ms.
                val dateAddedMs = if (dateAddedCol >= 0 && !cursor.isNull(dateAddedCol)) {
                    cursor.getLong(dateAddedCol) * 1000L
                } else 0L

                val songContentUri = ContentUris.withAppendedId(contentUri, id).toString()
                val artworkUri = if (albumId > 0) {
                    ContentUris.withAppendedId(albumArtBaseUri, albumId).toString()
                } else null

                songs.add(
                    LocalSong(
                        id = id.toString(),
                        title = title,
                        artist = artist,
                        album = album,
                        contentUri = songContentUri,
                        artworkUri = artworkUri,
                        durationMs = durationMs,
                        filePath = filePath,
                        trackNumber = trackNumber,
                        year = year,
                        dateAddedMs = dateAddedMs,
                    )
                )
            }
        }

        songs
    }

    /**
     * MediaStore almacena el track como (disc * 1000 + track).
     * Extraemos solo el número de pista.
     */
    private fun parseTrackNumber(raw: Int): Int? {
        val track = if (raw >= 1000) raw % 1000 else raw
        return track.takeIf { it in 1..999 }
    }
}
