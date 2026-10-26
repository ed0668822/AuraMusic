package com.music.aura.data.lyrics

/**
 * Una línea de letra con su marca de tiempo (0 si la letra no está sincronizada).
 */
data class LyricLine(
    val timestampMs: Long,
    val text: String,
)

/**
 * Letras de una canción, sincronizadas o planas.
 */
data class Lyrics(
    val lines: List<LyricLine>,
    val isSynced: Boolean,
) {
    /** Devuelve el índice de la línea activa en [positionMs], o -1. */
    fun activeIndexAt(positionMs: Long): Int {
        if (!isSynced || lines.isEmpty()) return -1
        var idx = -1
        for (i in lines.indices) {
            if (lines[i].timestampMs <= positionMs) idx = i else break
        }
        return idx
    }

    companion object {
        // Coincide con [00:31.495] o [00:31.49]
        private val LINE_TIMESTAMP_REGEX =
            Regex("""\[(\d+):(\d{2})(?:[.:](\d{1,3}))?]""")
        
        // Coincide con <00:31.495> (Enhanced LRC)
        private val WORD_TIMESTAMP_REGEX = 
            Regex("""<\d+:\d{2}(?:\.\d{1,3})?>""")
            
        // Coincide con "v1:", "v2:", etc.
        private val VERSE_PREFIX_REGEX = 
            Regex("""^v\d+:""", RegexOption.IGNORE_CASE)

        /**
         * Parsea texto LRC (estándar o Enhanced) o letra plana.
         */
        fun parse(raw: String): Lyrics? {
            if (raw.isBlank()) return null

            val timed = mutableListOf<LyricLine>()
            val plain = mutableListOf<String>()
            var sawAnyTimestamp = false

            raw.lines().forEach { rawLine ->
                var line = rawLine.trim()
                if (line.isEmpty()) return@forEach

                // Saltar metadata tipo [ar:Artist], [ti:Title]
                if (line.length > 3 && line[0] == '[' && !line[1].isDigit()) return@forEach

                // Buscar timestamp de línea estándar
                val lineMatches = LINE_TIMESTAMP_REGEX.findAll(line).toList()
                
                if (lineMatches.isNotEmpty()) {
                    sawAnyTimestamp = true
                    
                    // El texto de la línea es todo lo que está DESPUÉS del último timestamp de línea
                    val lastMatch = lineMatches.last()
                    var text = line.substring(lastMatch.range.last + 1).trim()

                    // Limpieza para Enhanced LRC
                    text = WORD_TIMESTAMP_REGEX.replace(text, "").trim()
                    text = VERSE_PREFIX_REGEX.replace(text, "").trim()

                    if (text.isNotEmpty()) {
                        // Tomamos el primer timestamp para la línea (el resto son redundantes si hay varios)
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
                        timed.add(LyricLine(ms, text))
                    }
                } else {
                    // Texto plano o Enhanced LRC sin timestamp de línea
                    var text = WORD_TIMESTAMP_REGEX.replace(line, "").trim()
                    text = VERSE_PREFIX_REGEX.replace(text, "").trim()
                    if (text.isNotEmpty()) {
                        plain.add(text)
                    }
                }
            }

            return when {
                sawAnyTimestamp && timed.isNotEmpty() -> Lyrics(
                    lines = timed.sortedBy { it.timestampMs },
                    isSynced = true,
                )
                plain.isNotEmpty() -> Lyrics(
                    lines = plain.mapIndexed { i, text -> LyricLine(i * 3_000L, text) },
                    isSynced = false,
                )
                else -> null
            }
        }
    }
}
