package com.music.aura.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.music.aura.data.model.LocalSong
import kotlinx.coroutines.guava.await

/**
 * Conexión al AuraPlaybackService desde la UI.
 */
object PlayerConnection {

    /**
     * Crea un MediaController conectado al servicio de Aura.
     */
    suspend fun connect(context: Context): MediaController {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, AuraPlaybackService::class.java)
        )
        return MediaController.Builder(context, sessionToken).buildAsync().await()
    }

    /**
     * Reproduce una lista de canciones locales desde [startIndex].
     */
    suspend fun playSongs(
        controller: MediaController,
        songs: List<LocalSong>,
        startIndex: Int = 0
    ) {
        if (songs.isEmpty()) return

        val mediaItems = songs.map { it.toMediaItem() }
        controller.setMediaItems(mediaItems, startIndex.coerceIn(0, mediaItems.size - 1), 0L)
        controller.prepare()
        controller.play()
    }

    /**
     * Convierte un LocalSong a MediaItem para Media3.
     * Almacena metadatos extra en el Bundle para poder reconstruir
     * el LocalSong desde el MediaItem (ver PlaybackViewModel).
     */
    private fun LocalSong.toMediaItem(): MediaItem {
        val extras = Bundle().apply {
            putLong("durationMs", durationMs)
            putString("filePath", filePath)
            trackNumber?.let { putInt("trackNumber", it) }
            year?.let { putInt("year", it) }
        }

        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(contentUri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(artworkUri?.let { android.net.Uri.parse(it) })
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .setExtras(extras)
                    .build()
            )
            .build()
    }
}
