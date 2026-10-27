package com.music.aura.data.lyrics

/**
 * One word of a line, with the stretch of the song it is sung over.
 *
 * Apple's TTML splits long words into syllables. The word is still the unit
 * the line is laid out, lifted and lit in — "e" and "nough" are one word on
 * screen — so [startMs] is the first syllable's start and [endMs] the last
 * one's end. But the syllables keep their own timing in [syllables], and the
 * sweep follows those: "e" is clipped and "nough" is held, and a wipe spread
 * evenly over the pair runs ahead of the first half and behind the second.
 *
 * Empty for a word nobody split, which is most of them — the word is then its
 * own single syllable.
 */
data class LyricWord(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val syllables: List<LyricSyllable> = emptyList(),
) {
    /**
     * How many of [text]'s characters have been sung at [positionMs], as a
     * fraction. Each syllable fills its own characters over its own time, and
     * a pause between two of them rests on the boundary rather than creeping
     * into the next one before it is sung.
     */
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
                val through = (positionMs - syllable.startMs).toFloat() / span
                return syllable.startChar + through * (syllable.endChar - syllable.startChar)
            }
        }
        return text.length.toFloat()
    }

    /**
     * This word with every character [drop] matches taken out of its text, and
     * the syllables moved to keep covering the same letters. Null if nothing
     * is left.
     */
    fun withoutChars(drop: (Char) -> Boolean): LyricWord? {
        if (text.none(drop)) return this
        val moved = IntArray(text.length + 1)
        val kept = StringBuilder()
        text.forEachIndexed { index, char ->
            moved[index] = kept.length
            if (!drop(char)) kept.append(char)
        }
        moved[text.length] = kept.length
        val lead = kept.indexOfFirst { !it.isWhitespace() }.takeIf { it >= 0 } ?: return null
        val trimmed = kept.toString().trim()
        val parts = syllables.map {
            LyricSyllable(
                startMs = it.startMs,
                endMs = it.endMs,
                startChar = (moved[it.startChar] - lead).coerceAtLeast(0),
                endChar = (moved[it.endChar] - lead).coerceAtMost(trimmed.length),
            )
        }
        return copy(text = trimmed, syllables = LyricSyllable.normalized(parts, trimmed.length))
    }
}

/**
 * One separately timed piece of a [LyricWord]: the characters [startChar]
 * until [endChar] of the word's text, sung from [startMs] to [endMs].
 */
data class LyricSyllable(
    val startMs: Long,
    val endMs: Long,
    val startChar: Int,
    val endChar: Int,
) {
    companion object {
        /**
         * Tidies a word's syllables, as a parser collected them, into what
         * [LyricWord.charsSungAt] expects: in order, end to end with no
         * characters between them, and covering the whole of a
         * [length]-character word.
         */
        fun normalized(parts: List<LyricSyllable>, length: Int): List<LyricSyllable> {
            val usable = parts
                .filter { it.endChar > it.startChar && it.startChar < length }
                .sortedBy { it.startChar }
            if (usable.size < 2) return emptyList()
            return usable.mapIndexed { index, part ->
                LyricSyllable(
                    startMs = part.startMs,
                    endMs = maxOf(part.startMs, part.endMs),
                    startChar = if (index == 0) 0 else part.startChar,
                    endChar = usable.getOrNull(index + 1)?.startChar ?: length,
                )
            }
        }
    }
}

/**
 * Which side of the panel a line is sung from.
 */
enum class LyricAlignment { Start, End }

/**
 * One synced line. [timeMs] is when it starts; a blank [text] is an
 * instrumental stretch — LRC files mark those with a bare timestamp.
 *
 * [words] is populated only by the providers that carry word-level timing.
 * LRCLIB has none, so a line from there highlights whole; see [isWordSynced].
 *
 * [sungUntilMs] is the line's own end where a line-synced provider states one.
 *
 * [background] is the answering vocal — the "(ooh)" or the echoed half-phrase
 * a second voice sings over the lead.
 */
data class LyricLine(
    val timeMs: Long,
    val text: String,
    val words: List<LyricWord> = emptyList(),
    val sungUntilMs: Long? = null,
    val background: LyricLine? = null,
    val alignment: LyricAlignment = LyricAlignment.Start,
    /** Display-only translation: drive its sweep and bloom from the original vocal. */
    val timingSource: LyricLine? = null,
) {
    val isGap: Boolean get() = text.isEmpty()

    val isWordSynced: Boolean get() = words.isNotEmpty()

    /**
     * Where each of [words] sits in [text], as character ranges.
     */
    val wordSpans: List<IntRange> by lazy(LazyThreadSafetyMode.NONE) {
        var offset = 0
        words.map { word ->
            val start = text.indexOf(word.text, offset).takeIf { it >= 0 } ?: offset
            val end = start + word.text.length
            offset = end
            start until end
        }
    }

    /**
     * Where each separately timed piece of the line sits in [text]: a word's
     * syllables where the provider split it, the word itself where it did not.
     */
    val sweepSpans: List<IntRange> by lazy(LazyThreadSafetyMode.NONE) {
        words.flatMapIndexed { index, word ->
            val span = wordSpans[index]
            if (word.syllables.isEmpty()) {
                listOf(span)
            } else {
                word.syllables.map { (span.first + it.startChar) until (span.first + it.endChar) }
            }
        }
    }

    /**
     * Whether anything on this line is off the floor at [positionMs].
     */
    fun isLifted(positionMs: Long): Boolean {
        val first = words.firstOrNull() ?: return false
        if (positionMs <= first.startMs) return false
        return positionMs < words.last().endMs + RISE_MS || isGrowing(positionMs)
    }

    /**
     * The stretch of the song over which anything drawn for this line moves.
     */
    val animatesFromMs: Long by lazy(LazyThreadSafetyMode.NONE) {
        minOf(timeMs, words.firstOrNull()?.startMs ?: timeMs)
    }

    /** See [animatesFromMs]. */
    val animatesUntilMs: Long by lazy(LazyThreadSafetyMode.NONE) {
        val settled = words.lastOrNull()?.let { it.endMs + RISE_MS.toLong() } ?: endMs
        val grown = growingWords.maxOfOrNull { it.restsAtMs } ?: Long.MIN_VALUE
        maxOf(endMs, settled, grown)
    }

    /**
     * How far the word covering [positionMs] has lifted, 0..1.
     */
    fun wordLift(index: Int, positionMs: Long): Float {
        val word = words.getOrNull(index) ?: return 0f
        val riseMs = (word.endMs - word.startMs).toFloat().coerceIn(RISE_MIN_MS, RISE_MS)
        val rising = ((positionMs - word.startMs) / riseMs).coerceIn(0f, 1f)
        val falling = (1f - (positionMs - word.endMs) / RISE_MS).coerceIn(0f, 1f)
        return liftHeight(word) * smooth(minOf(rising, falling))
    }

    /**
     * Whether anything actually told us when the singing stops.
     */
    val hasKnownEnd: Boolean get() = words.isNotEmpty() || sungUntilMs != null

    /**
     * When the last word finishes.
     */
    val endMs: Long
        get() {
            val lead = words.lastOrNull()?.endMs ?: sungUntilMs ?: timeMs
            return maxOf(lead, background?.endMs ?: lead)
        }

    /**
     * How far through the line the singing has got, 0..1, as a fractional
     * index into [text].
     */
    fun revealedChars(positionMs: Long): Float {
        timingSource?.let { source ->
            if (source.text.isEmpty()) return 0f
            return (source.revealedChars(positionMs) / source.text.length)
                .coerceIn(0f, 1f) * text.length
        }
        if (words.isEmpty()) return if (positionMs >= timeMs) text.length.toFloat() else 0f
        var offset = 0
        words.forEachIndexed { index, word ->
            val start = text.indexOf(word.text, offset).takeIf { it >= 0 } ?: offset
            val end = start + word.text.length
            if (positionMs < word.startMs) return start.toFloat()
            if (positionMs < word.endMs) return start + word.charsSungAt(positionMs)
            val next = words.getOrNull(index + 1)
            if (next != null && positionMs < next.startMs) {
                val gapStart = text.indexOf(next.text, end).takeIf { it >= 0 } ?: end
                val pause = (next.startMs - word.endMs).coerceAtLeast(1L)
                val through = (positionMs - word.endMs).toFloat() / pause
                return end + through * (gapStart - end)
            }
            offset = end
        }
        return text.length.toFloat()
    }

    /**
     * How much of a word's lift is left at [positionMs].
     */
    fun wordFall(index: Int, positionMs: Long): Float {
        val word = words.getOrNull(index) ?: return 0f
        val falling = (1f - (positionMs - word.endMs) / RISE_MS).coerceIn(0f, 1f)
        return liftHeight(word) * smooth(falling)
    }

    /**
     * The words held long enough to be worth animating letter by letter.
     */
    val growingWords: List<GrowingWord> by lazy(LazyThreadSafetyMode.NONE) {
        words.mapIndexedNotNull { index, word ->
            if (word.canGrow()) GrowingWord(index, word) else null
        }
    }

    /** The letter-by-letter treatment for word [index], where it has earned one. */
    fun growingAt(index: Int): GrowingWord? =
        growingWords.firstOrNull { it.index == index }

    /**
     * Whether any word on this line is mid-flight at [positionMs].
     */
    fun isGrowing(positionMs: Long): Boolean = growingWords.any {
        positionMs >= it.startMs && positionMs <= it.restsAtMs
    }
}

/**
 * One word held long enough to be animated a letter at a time.
 */
class GrowingWord internal constructor(
    val index: Int,
    word: LyricWord,
) {
    val startMs: Long = word.startMs
    val endMs: Long = word.endMs

    private val chars: Int = word.text.length
    private val scalePeak = FloatArray(chars)
    private val shiftPeak = FloatArray(chars)
    private val risePeak = FloatArray(chars)
    private val bloomPeak = FloatArray(chars)

    /** When the last letter has finished moving and is just sitting lifted. */
    val restsAtMs: Long

    init {
        val held = (endMs - startMs).coerceAtLeast(1L).toFloat()
        val earned = ((held - GROW_RAMP_MIN_MS) / (GROW_RAMP_MAX_MS - GROW_RAMP_MIN_MS))
            .coerceIn(0f, 1f)
            .let { it * it * it }
        val decay = decayRate(chars, held)
        val bloomPace = minOf(GROW_BLOOM_PACE_MAX, held / GROW_BLOOM_PACE_MS)
        val bloomSpread = when {
            chars <= 3 -> GROW_BLOOM_SHORT
            chars >= 6 -> GROW_BLOOM_LONG
            else -> 1f
        }
        val base = if (chars <= 3) GROW_BASE_SHORT else GROW_BASE_LONG
        val liftPace = (held / GROW_LIFT_PACE_MS).coerceIn(GROW_LIFT_FLOOR, 1f)

        for (i in 0 until chars) {
            val place = if (chars > 1) i.toFloat() / (chars - 1) else 0f
            val reach = earned * (1f - place * decay)
            val scale = 1f + base + reach * GROW_SCALE_RANGE
            scalePeak[i] = scale * GROW_SCALE_TRIM
            bloomPeak[i] = (GROW_BLOOM_FLOOR + reach * GROW_BLOOM_RANGE) * bloomPace * bloomSpread
            risePeak[i] = ((scale - 1f) / GROW_SCALE_CEILING) * liftPace
            val centre = (i + 0.5f) / chars
            shiftPeak[i] = (centre - 0.5f) * 2f * (scale - 1f) * GROW_SHIFT_EM * GROW_SCALE_TRIM
        }

        val last = (chars - 1).coerceAtLeast(0) * GROW_STAGGER + GROW_SPAN
        restsAtMs = startMs + (held * last).toLong()
    }

    /**
     * Where letter [charIndex] has got to at [positionMs], written into [into].
     */
    fun sampleInto(charIndex: Int, positionMs: Long, into: CharGrowth) {
        val span = (endMs - startMs).coerceAtLeast(1L).toFloat()
        val elapsed = positionMs - startMs - charIndex * span * GROW_STAGGER
        val phase = (elapsed / (span * GROW_SPAN)).coerceIn(0f, 1f)
        val peakScale = scalePeak[charIndex]
        when {
            phase < GROW_IN -> {
                val t = smooth(phase / GROW_IN)
                into.scale = 1f + (peakScale - 1f) * t
                into.shift = shiftPeak[charIndex] * t
                into.rise = risePeak[charIndex] * t
                into.bloom = bloomPeak[charIndex] * t
            }
            phase < GROW_HOLD -> {
                into.scale = peakScale
                into.shift = shiftPeak[charIndex]
                into.rise = risePeak[charIndex]
                into.bloom = bloomPeak[charIndex]
            }
            phase < GROW_OUT -> {
                val t = smooth((phase - GROW_HOLD) / (GROW_OUT - GROW_HOLD))
                into.scale = peakScale + (1f - peakScale) * t
                into.shift = shiftPeak[charIndex] * (1f - t)
                into.rise = risePeak[charIndex] + (GROW_REST - risePeak[charIndex]) * t
                into.bloom = bloomPeak[charIndex] * (1f - t)
            }
            else -> {
                into.scale = 1f
                into.shift = 0f
                into.rise = GROW_REST
                into.bloom = 0f
            }
        }
    }
}

/** Where one letter of a [GrowingWord] is, filled in by [GrowingWord.sampleInto]. */
class CharGrowth {
    var scale: Float = 1f
    var shift: Float = 0f
    var rise: Float = 0f
    var bloom: Float = 0f
}

private const val RISE_MS = 700f
private const val RISE_MIN_MS = 250f

private fun liftHeight(word: LyricWord): Float {
    val held = (word.endMs - word.startMs).toFloat()
    val through = ((held - LIFT_QUICK_MS) / (LIFT_HELD_MS - LIFT_QUICK_MS)).coerceIn(0f, 1f)
    return LIFT_FLOOR + (1f - LIFT_FLOOR) * smooth(through)
}

private const val LIFT_QUICK_MS = 150f
private const val LIFT_HELD_MS = 900f
private const val LIFT_FLOOR = 0.25f

private fun smooth(fraction: Float) = fraction * fraction * (3f - 2f * fraction)

private fun LyricWord.canGrow(): Boolean {
    val length = text.length
    if (length == 0 || length > GROW_MAX_CHARS) return false
    if ('-' in text || text.any { it.isBlockScript() || it.isJoinedScript() }) return false
    val held = endMs - startMs
    return when {
        length == 1 -> held >= GROW_MIN_SOLO_MS
        length <= 3 -> held >= GROW_MIN_SHORT_MS + (length - 2) * GROW_SHORT_STEP_MS
        length == 4 -> held >= GROW_MIN_FOUR_MS
        else -> held >= GROW_MIN_LONG_MS && held >= length * GROW_MS_PER_CHAR
    }
}

private fun decayRate(length: Int, heldMs: Float): Float {
    val long = length > GROW_DECAY_LONG_CHARS
    val quick = heldMs < GROW_DECAY_QUICK_MS
    if (!long && !quick) return 0f
    var strength = 0f
    if (long) {
        strength += minOf((length - GROW_DECAY_LONG_CHARS) / 5f, 1f) * GROW_DECAY_LONG
    }
    if (quick) {
        val short = maxOf(0f, 1f - (heldMs - GROW_DECAY_QUICK_FLOOR_MS) / 400f)
        strength += short * if (length > 3) GROW_DECAY_QUICK else GROW_DECAY_QUICK_TINY
    }
    return minOf(strength, GROW_DECAY_MAX)
}

private fun Char.isBlockScript(): Boolean =
    this in '一'..'鿿' || this in '぀'..'ゟ' ||
        this in '゠'..'ヿ' || this in '가'..'힯'

private fun Char.isJoinedScript(): Boolean =
    this in '֐'..'ࣿ'

private const val GROW_MAX_CHARS = 7
private const val GROW_MIN_SOLO_MS = 1_100L
private const val GROW_MIN_SHORT_MS = 1_360L
private const val GROW_SHORT_STEP_MS = 140L
private const val GROW_MIN_FOUR_MS = 1_050L
private const val GROW_MIN_LONG_MS = 900L
private const val GROW_MS_PER_CHAR = 200L
private const val GROW_DECAY_LONG_CHARS = 5
private const val GROW_DECAY_QUICK_MS = 1_200f
private const val GROW_DECAY_QUICK_FLOOR_MS = 800f
private const val GROW_DECAY_LONG = 0.4f
private const val GROW_DECAY_QUICK = 0.3f
private const val GROW_DECAY_QUICK_TINY = 0.1f
private const val GROW_DECAY_MAX = 0.7f
private const val GROW_STAGGER = 0.09f
private const val GROW_SPAN = 1.5f
private const val GROW_IN = 0.25f
private const val GROW_HOLD = 0.30f
private const val GROW_OUT = 0.75f
private const val GROW_REST = 1f
private const val GROW_RAMP_MIN_MS = 400f
private const val GROW_RAMP_MAX_MS = 3_000f
private const val GROW_BASE_SHORT = 0.05f
private const val GROW_BASE_LONG = 0.04f
private const val GROW_SCALE_RANGE = 0.08f
private const val GROW_SCALE_CEILING = 0.1f
private const val GROW_SCALE_TRIM = 0.98f
private const val GROW_SHIFT_EM = 25f / 34f
private const val GROW_BLOOM_FLOOR = 0.35f
private const val GROW_BLOOM_RANGE = 0.45f
private const val GROW_BLOOM_PACE_MS = 1_500f
private const val GROW_BLOOM_PACE_MAX = 1.1f
private const val GROW_BLOOM_SHORT = 0.85f
private const val GROW_BLOOM_LONG = 1.1f
private const val GROW_LIFT_PACE_MS = 2_000f
private const val GROW_LIFT_FLOOR = 0.3f
