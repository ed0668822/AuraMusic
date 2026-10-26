package com.music.aura.playback

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
class PlaybackPosition {
    var positionMs by mutableLongStateOf(0L)
        private set

    var sampledAtNanos by mutableLongStateOf(0L)
        private set

    var seeks by mutableIntStateOf(0)

    var advancing by mutableStateOf(true)

    fun report(positionMs: Long, sampledAtNanos: Long = System.nanoTime()) {
        this.positionMs = positionMs
        this.sampledAtNanos = sampledAtNanos
    }
}
