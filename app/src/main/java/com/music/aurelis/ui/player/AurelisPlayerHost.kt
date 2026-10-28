package com.music.aurelis.ui.player

import kotlinx.coroutines.flow.StateFlow

interface SystemVolume {
    val level: StateFlow<Float>
    fun set(level: Float)
    fun refresh()
}
