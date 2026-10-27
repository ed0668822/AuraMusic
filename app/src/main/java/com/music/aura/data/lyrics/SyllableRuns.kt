package com.music.aura.data.lyrics

/**
 * One stamped run of text as a provider wrote it, its spacing untouched.
 */
internal class TimedRun(val startMs: Long, val endMs: Long, val raw: String)

/**
 * Glues [runs] into words, each one keeping its runs as [LyricSyllable]s.
 */
internal fun wordsFromRuns(runs: List<TimedRun>, spacingIsExplicit: Boolean = false): List<LyricWord> {
    val glue = spacingIsExplicit || runs.any { run -> run.raw.any(Char::isWhitespace) }

    val words = ArrayList<LyricWord>()
    val current = StringBuilder()
    val parts = ArrayList<LyricSyllable>()
    var start = 0L
    var end = 0L

    fun flush() {
        if (current.isNotEmpty()) {
            val text = current.toString()
            words += LyricWord(start, maxOf(start, end), text, LyricSyllable.normalized(parts, text.length))
        }
        current.setLength(0)
        parts.clear()
    }

    for (run in runs) {
        val content = run.raw.trim()
        if (content.isEmpty()) {
            flush()
            continue
        }
        val joins = glue && current.isNotEmpty() && !run.raw.first().isWhitespace() &&
            !current.last().isUnspacedScript() && !content.first().isUnspacedScript()
        if (!joins) flush()
        if (current.isEmpty()) {
            start = run.startMs
            end = run.startMs
        }
        val from = current.length
        current.append(content)
        val runEnd = maxOf(run.startMs, run.endMs)
        parts += LyricSyllable(run.startMs, runEnd, from, current.length)
        end = maxOf(end, runEnd)
        if (!glue || run.raw.last().isWhitespace()) flush()
    }
    flush()
    return words
}

/** Scripts written without spaces between words. */
private fun Char.isUnspacedScript(): Boolean =
    this in '\u3400'..'\u9FFF' ||
        this in '\u3040'..'\u30FF' ||
        this in '\u3000'..'\u303F' ||
        this in '\uFF00'..'\uFFEF' ||
        this in '\u0E00'..'\u0E7F'
