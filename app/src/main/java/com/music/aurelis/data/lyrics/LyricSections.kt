package com.music.aurelis.data.lyrics

/**
 * Whether a plain lyric line is a section marker — `[Chorus]`,
 * `[Verse 2: Artist]` — rather than words to sing.
 */
fun isSectionHeader(text: String): Boolean {
    val trimmed = text.trim()
    return trimmed.startsWith("[") && trimmed.endsWith("]") && trimmed.length in 3..60
}
