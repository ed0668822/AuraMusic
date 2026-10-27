package com.music.aura.ui.player

import com.music.aura.data.lyrics.LyricLine

internal fun activeLyricRows(lines: List<LyricLine>, positionMs: Long): List<Int> {
    val latest = lines.indexOfLast { it.timestampMs <= positionMs }
    if (latest < 0) return emptyList()
    return listOf(latest)
}
