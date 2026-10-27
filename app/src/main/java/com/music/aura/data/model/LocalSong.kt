package com.music.aura.data.model

enum class QueueTier {
    USER_QUEUE,
    CONTEXT,
    AUTOPLAY,
}

enum class PlaybackSourceType {
    HOME, SEARCH, HISTORY, REPLAY, EXPLORE, BROWSE, SHARED_LINK, QUEUE,
}

data class LocalSong(
    val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val contentUri: String,
    val artworkUri: String?,
    val durationMs: Long,
    val filePath: String?,
    val trackNumber: Int?,
    val year: Int?,
    val dateAddedMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val queueTier: QueueTier = QueueTier.CONTEXT,
    val queueEntryId: String? = null,
    val playbackSource: String? = null,
    val playbackSourceType: PlaybackSourceType? = null,
    val playbackSourceId: String? = null,
) {
    val videoId: String get() = id
    val thumbnailUrl: String? get() = artworkUri
    val albumName: String? get() = album
    val fromAutoplay: Boolean get() = queueTier == QueueTier.AUTOPLAY

    val durationText: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}

fun LocalSong.isSameTrackAs(other: LocalSong?): Boolean {
    other ?: return false
    return title == other.title && artist == other.artist
}
