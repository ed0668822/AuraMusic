package com.music.aurelis.playback

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow

object SleepTimer {

    val deadline = MutableStateFlow<Long?>(null)

    val minutes = MutableStateFlow<Int?>(null)

    val afterTrack = MutableStateFlow(false)

    val PRESETS = listOf(15, 30, 45, 60)

    val isRunning: Boolean get() = deadline.value != null || afterTrack.value

    fun start(minutes: Int) {
        afterTrack.value = false
        this.minutes.value = minutes
        deadline.value = SystemClock.elapsedRealtime() + minutes * 60_000L
    }

    fun startAfterTrack() {
        minutes.value = null
        deadline.value = null
        afterTrack.value = true
    }

    fun cancel() {
        minutes.value = null
        deadline.value = null
        afterTrack.value = false
    }

    fun remainingMs(): Long? =
        deadline.value?.let { (it - SystemClock.elapsedRealtime()).coerceAtLeast(0L) }
}
