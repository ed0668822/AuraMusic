// Ported from BitChord. © 2026 Kushagra Singh (BitChord/Aurelis). GPL-3.0-or-later.
package com.music.aurelis.data.lyrics

internal const val MIN_GAP_MS = 4_000L

internal fun List<LyricLine>.withInstrumentalGaps(): List<LyricLine> {
    if (isEmpty()) return this
    val out = ArrayList<LyricLine>(size + 4)
    if (first().timeMs >= MIN_GAP_MS) out += LyricLine(0L, "")
    forEachIndexed { index, line ->
        out += line
        val next = getOrNull(index + 1) ?: return@forEachIndexed
        if (!line.hasKnownEnd) return@forEachIndexed
        val silence = next.timeMs - line.endMs
        if (silence >= MIN_GAP_MS && line.endMs > line.timeMs) out += LyricLine(line.endMs, "")
    }
    return out
}
