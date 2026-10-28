package com.music.aurelis.playback

import androidx.media3.common.MediaItem
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.QueueTier
import java.util.concurrent.ConcurrentHashMap

internal const val EXTRA_QUEUE_ENTRY_ID = "bitchord.queueEntryId"
internal const val EXTRA_CANONICAL_INDEX = "bitchord.canonicalIndex"
internal const val EXTRA_QUEUE_TIER = "bitchord.queueTier"

internal val mediaItemEntryIds = ConcurrentHashMap<MediaItem, String>()
internal val mediaItemCanonicalIndices = ConcurrentHashMap<MediaItem, Int>()
internal val mediaItemTiers = ConcurrentHashMap<MediaItem, QueueTier>()

val MediaItem.queueEntryId: String?
    get() = mediaItemEntryIds[this] ?: mediaMetadata.extras?.getString(EXTRA_QUEUE_ENTRY_ID)

val MediaItem.canonicalIndex: Int?
    get() = mediaItemCanonicalIndices[this] ?: mediaMetadata.extras?.getInt(EXTRA_CANONICAL_INDEX)

val MediaItem.queueTier: QueueTier
    get() = mediaItemTiers[this] ?: mediaMetadata.extras?.getString(EXTRA_QUEUE_TIER)?.let { 
        runCatching { QueueTier.valueOf(it) }.getOrNull() 
    } ?: QueueTier.CONTEXT

fun LocalSong.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(contentUri)
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(artworkUri?.let { android.net.Uri.parse(it) })
                .build()
        )
        .build()
}
