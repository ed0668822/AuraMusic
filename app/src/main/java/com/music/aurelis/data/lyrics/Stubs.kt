// Aurelis stubs. GPL-3.0-or-later.
package com.music.aurelis.data.lyrics

object SimpMusicLyrics {
    suspend fun lyrics(videoId: String, durationMs: Long): List<LyricLine>? = null
}

object YouTubeMusicLyrics {
    suspend fun lyrics(videoId: String): List<LyricLine>? = null
}

object YouTubeTranscriptLyrics {
    suspend fun lyrics(videoId: String): List<LyricLine>? = null
}

object PaxSenix {
    @Volatile private var apiKey: String = ""
    fun setApiKey(value: String) { apiKey = value.trim() }
    suspend fun lyrics(title: String, artist: String, durationMs: Long, album: String? = null): List<LyricLine>? = null
    suspend fun spotifyLyrics(title: String, artist: String, durationMs: Long): List<LyricLine>? = null
    suspend fun musixmatchLyrics(title: String, artist: String, durationMs: Long): List<LyricLine>? = null
}
