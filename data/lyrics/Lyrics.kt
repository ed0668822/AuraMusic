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
 *
 * [isSynced] es true cuando las líneas tienen timestamps reales (LRC).
 * Cuando es false, cada línea tiene un timestamp artificial equiespaciado
 * para que la UI pueda hacer scroll pasivo sin resaltar ninguna.
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
        /** Parser LRC: `[mm:ss.xx]texto` (con fracciones de 1–3 dígitos). */
        private val TIMESTAMP_REGEX =
            Regex("""\[(\d+):(\d{2})(?:[.:](\d{1,3}))?]""")

        /**
         * Parsea texto LRC o letra plana.
         * - Si contiene timestamps → [isSynced] true, ordenado por tiempo.
         * - Si no → cada línea recibe un timestamp artificial equiespaciado.
         */
        fun parse(raw: String): Lyrics? {
            if (raw.isBlank()) return null

            val timed = mutableListOf<LyricLine>()
            val plain = mutableListOf<String>()
            var sawAnyTimestamp = false

            raw.lines().forEach { rawLine ->
                val line = rawLine.trim()
                if (line.isEmpty()) return@forEach

                // Saltar metadata tipo [ar:Artist], [ti:Title]
                if (line.length > 3 && line[0] == '[' && !line[1].isDigit()) return@forEach

                val matches = TIMESTAMP_REGEX.findAll(line).toList()
                if (matches.isEmpty()) {
                    plain.add(line)
                    return@forEach
                }
                sawAnyTimestamp = true

                val text = line.substring(matches.last().range.last + 1).trim()
                if (text.isEmpty()) return@forEach

                matches.forEach { m ->
                    val min = m.groupValues[1].toLongOrNull() ?: return@forEach
                    val sec = m.groupValues[2].toLongOrNull() ?: return@forEach
                    val fracStr = m.groupValues[3]
                    val frac = when (fracStr.length) {
                        0 -> 0L
                        1 -> fracStr.toLong() * 100
                        2 -> fracStr.toLong() * 10
                        else -> fracStr.take(3).toLong()
                    }
                    val ms = min * 60_000 + sec * 1_000 + frac
                    timed.add(LyricLine(ms, text))
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
