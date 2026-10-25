package com.music.aura.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.music.aura.data.model.LocalSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.withContext

/**
 * Conexión al AuraPlaybackService desde la UI.
 *
 * Crea un MediaController que se comunica con el MediaSessionService
 * en el proceso de reproducción.
 */
object PlayerConnection {

    /**
     * Crea un MediaController conectado al servicio de Aura.
     * Debe llamarse desde una coroutine (suspend).
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
     */
    private fun LocalSong.toMediaItem(): MediaItem {
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
                    .build()
            )
            .build()
    }
}
