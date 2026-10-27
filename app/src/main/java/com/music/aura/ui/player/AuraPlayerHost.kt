package com.music.aura.ui.player

import kotlinx.coroutines.flow.StateFlow

interface SystemVolume {
    val level: StateFlow<Float>
    fun set(level: Float)
    fun refresh()
}
