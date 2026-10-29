// Ported from BitChord. © 2026 Kushagra Singh (BitChord/Aurelis). GPL-3.0-or-later.
package com.music.aurelis.data.lyrics

/**
 * The databases [LyricsRepository] can ask, in the order it asks them.
 */
enum class LyricsSource(
    val label: String,
    val detail: String,
    val wordSynced: Boolean,
    val hidden: Boolean = false,
) {
    LRC_RED(
        label = "lrc.red",
        detail = "Apple timings by recording, syllable by syllable",
        wordSynced = true,
    ),
    BINI_LYRICS(
        label = "BiniLyrics",
        detail = "The same Apple timings, matched on the recording itself",
        wordSynced = true,
    ),
    BETTER_LYRICS(
        label = "BetterLyrics",
        detail = "Apple Music timings, word by word",
        wordSynced = true,
    ),
    BETTER_LYRICS_PORTATO(
        label = "BetterLyrics Portato",
        detail = "QQ Music karaoke timings through BetterLyrics",
        wordSynced = true,
    ),
    PAXSENIX(
        label = "PaxSenix",
        detail = "Apple Music timings through the original keyless provider",
        wordSynced = true,
    ),
    PAXSENIX_SPOTIFY(
        label = "PaxSenix: Spotify",
        detail = "Spotify lyrics with PaxSenix fallback; API key required",
        wordSynced = false,
    ),
    PAXSENIX_MUSIXMATCH(
        label = "PaxSenix: Musixmatch",
        detail = "Musixmatch timings with PaxSenix fallback; API key required",
        wordSynced = true,
    ),
    LYRICS_PLUS(
        label = "LyricsPlus",
        detail = "Syllable by syllable, on community mirrors",
        wordSynced = true,
    ),
    SIMP_MUSIC(
        label = "SimpMusic",
        detail = "Matched on the video, so never the wrong edit",
        wordSynced = true,
        hidden = true,
    ),
    UNISON(
        label = "Unison",
        detail = "Contributed by listeners, so it has what nobody licensed",
        wordSynced = true,
    ),
    YOUTUBE_TRANSCRIPT(
        label = "YouTube captions",
        detail = "Timed captions matched to the exact playing video",
        wordSynced = false,
        hidden = true,
    ),
    YOUTUBE_MUSIC(
        label = "YouTube Music",
        detail = "Plain lyrics from the playing video's Lyrics tab",
        wordSynced = false,
        hidden = true,
    ),
    MEGALOBIZ(
        label = "Megalobiz",
        detail = "Community-made, whole-line LRC",
        wordSynced = false,
        hidden = true,
    ),
    KUGOU(
        label = "KuGou",
        detail = "Whole lines, strong outside the English catalogue",
        wordSynced = false,
    ),
    LRCLIB(
        label = "LRCLIB",
        detail = "Whole lines only, and always up",
        wordSynced = false,
    ),
    MUSIXMATCH(
        label = "Musixmatch",
        detail = "Whole lines, from the biggest lyrics database there is",
        wordSynced = false,
    ),
    GENIUS(
        label = "Genius",
        detail = "Plain text fallback, massive web catalogue",
        wordSynced = false,
    ),
    ;

    companion object {
        val offered: List<LyricsSource> = entries.filterNot { it.hidden }

        fun ordered(saved: List<LyricsSource>): List<LyricsSource> {
            val result = saved.filter { it in offered }.distinct().toMutableList()
            offered.forEachIndexed { index, source ->
                if (source in result) return@forEachIndexed
                val before = offered.subList(0, index).lastOrNull { it in result }
                result.add(before?.let { result.indexOf(it) + 1 } ?: 0, source)
            }
            return result
        }
    }
}
