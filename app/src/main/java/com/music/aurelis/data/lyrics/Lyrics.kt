// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.data.lyrics

data class Lyrics(
    val lines: List<LyricLine>,
    val isSynced: Boolean,
) {
    fun activeIndexAt(positionMs: Long): Int {
        if (!isSynced || lines.isEmpty()) return -1
        var idx = -1
        for (i in lines.indices) {
            if (lines[i].timeMs <= positionMs) idx = i else break
        }
        return idx
    }

    companion object {
        private val LINE_TIMESTAMP_REGEX =
            Regex("""\[(\d+):(\d{2})(?:[.:](\d{1,3}))?]""")

        private val WORD_TIMESTAMP_REGEX =
            Regex("""<\d+:\d{2}(?:\.\d{1,3})?>""")

        private val VERSE_PREFIX_REGEX =
            Regex("""^v\d+:""", RegexOption.IGNORE_CASE)

        fun parse(raw: String): Lyrics? {
            if (raw.isBlank()) return null

            val timed = mutableListOf<LyricLine>()
            val plain = mutableListOf<String>()
            var sawAnyTimestamp = false

            raw.lines().forEach { rawLine ->
                var line = rawLine.trim()
                if (line.isEmpty()) return@forEach

                if (line.length > 3 && line[0] == '[' && !line[1].isDigit()) return@forEach

                val lineMatches = LINE_TIMESTAMP_REGEX.findAll(line).toList()
                if (lineMatches.isNotEmpty()) {
                    sawAnyTimestamp = true
                    val lastMatch = lineMatches.last()
                    var text = line.substring(lastMatch.range.last + 1).trim()
                    text = WORD_TIMESTAMP_REGEX.replace(text, "").trim()
                    text = VERSE_PREFIX_REGEX.replace(text, "").trim()

                    if (text.isNotEmpty()) {
                        val firstMatch = lineMatches.first()
                        val min = firstMatch.groupValues[1].toLongOrNull() ?: 0L
                        val sec = firstMatch.groupValues[2].toLongOrNull() ?: 0L
                        val fracStr = firstMatch.groupValues[3]
                        val frac = when (fracStr.length) {
                            0 -> 0L
                            1 -> fracStr.toLong() * 100
                            2 -> fracStr.toLong() * 10
                            else -> fracStr.take(3).toLong()
                        }
                        val ms = min * 60_000 + sec * 1_000 + frac
                        timed.add(LyricLine(timeMs = ms, text = text))
                    }
                } else {
                    var text = WORD_TIMESTAMP_REGEX.replace(line, "").trim()
                    text = VERSE_PREFIX_REGEX.replace(text, "").trim()
                    if (text.isNotEmpty()) {
                        plain.add(text)
                    }
                }
            }

            return when {
                sawAnyTimestamp && timed.isNotEmpty() -> Lyrics(
                    lines = timed.sortedBy { it.timeMs },
                    isSynced = true,
                )
                plain.isNotEmpty() -> Lyrics(
                    lines = plain.mapIndexed { i, text -> LyricLine(timeMs = i * 3_000L, text = text) },
                    isSynced = false,
                )
                else -> null
            }
        }
    }
}
