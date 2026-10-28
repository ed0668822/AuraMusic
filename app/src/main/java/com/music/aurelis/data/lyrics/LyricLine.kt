package com.music.aurelis.data.lyrics

/**
 * One word of a line, with the stretch of the song it is sung over.
 */
data class LyricWord(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val syllables: List<LyricSyllable> = emptyList(),
) {
    fun charsSungAt(positionMs: Long): Float {
        if (positionMs <= startMs) return 0f
        if (positionMs >= endMs) return text.length.toFloat()
        if (syllables.isEmpty()) {
            val span = (endMs - startMs).coerceAtLeast(1L)
            return (positionMs - startMs).toFloat() / span * text.length
        }
        for (syllable in syllables) {
            if (positionMs < syllable.startMs) return syllable.startChar.toFloat()
            if (positionMs < syllable.endMs) {
                val span = (syllable.endMs - syllable.startMs).coerceAtLeast(1L)
                return ((positionMs - syllable.startMs).toFloat() / span * (syllable.endChar - syllable.startChar)).toFloat() + syllable.startChar
            }
        }
        return text.length.toFloat()
    }
}

/**
 * One separately timed piece of a word.
 */
data class LyricSyllable(
    val startMs: Long,
    val endMs: Long,
    val startChar: Int,
    val endChar: Int,
) {
    companion object {
        fun normalized(items: List<LyricSyllable>, length: Int): List<LyricSyllable> = items
    }
}

enum class LyricAlignment { Start, End }

/**
 * One synced lyric line.
 */
data class LyricLine(
    val timeMs: Long,
    val text: String,
    val words: List<LyricWord> = emptyList(),
    val sungUntilMs: Long? = null,
    val background: LyricLine? = null,
    val alignment: LyricAlignment = LyricAlignment.Start,
    val timingSource: LyricLine? = null,
) {
    val isGap: Boolean get() = text.isEmpty()
    val isWordSynced: Boolean get() = words.isNotEmpty()
    val timestampMs: Long get() = timeMs
}
