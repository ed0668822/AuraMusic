// Ported from BitChord. © 2026 Kushagra Singh (BitChord/Aurelis). GPL-3.0-or-later.
package com.music.aurelis.data.lyrics

private const val PERSON = "person"
private const val GROUP = "group"
private const val OTHER = "other"
private const val GROUP_AGENT = "v1000"
private const val OTHER_AGENT = "v2000"
private const val MOSTLY_RIGHT = 0.85f

internal fun lineAlignments(
    singers: List<String?>,
    types: Map<String, String>,
): List<LyricAlignment> {
    var left = true
    var lastVoice: String? = null
    var rightward = 0
    var placed = 0

    val sides = singers.map { singer ->
        if (singer.isNullOrEmpty()) return@map LyricAlignment.Start
        val type = types[singer] ?: when (singer) {
            GROUP_AGENT -> GROUP
            OTHER_AGENT -> OTHER
            else -> PERSON
        }
        placed += 1
        if (type == GROUP) return@map LyricAlignment.Start

        when {
            lastVoice == null -> left = type != OTHER
            singer != lastVoice -> left = !left
        }
        lastVoice = singer

        if (!left) rightward += 1
        if (left) LyricAlignment.Start else LyricAlignment.End
    }

    if (placed == 0 || rightward.toFloat() / placed < MOSTLY_RIGHT) return sides
    return sides.map {
        if (it == LyricAlignment.Start) LyricAlignment.End else LyricAlignment.Start
    }
}
