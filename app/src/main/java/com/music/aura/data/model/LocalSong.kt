package com.music.aura.data.model

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
) {
    val durationText: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}
