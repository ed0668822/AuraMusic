// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.music.aurelis.data.model.LocalSong
import kotlinx.coroutines.guava.await

object PlayerConnection {

    suspend fun connect(context: Context): MediaController {
        val sessionToken = SessionToken(context, ComponentName(context, AurelisPlaybackService::class.java))
        return MediaController.Builder(context, sessionToken).buildAsync().await()
    }

    suspend fun playSongs(controller: MediaController, songs: List<LocalSong>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        val mediaItems = songs.map { it.toMediaItem() }
        controller.setMediaItems(mediaItems, startIndex.coerceIn(0, mediaItems.size - 1), 0L)
        controller.prepare()
        controller.play()
    }

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
