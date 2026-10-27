package com.music.aura.ui.player

import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato mínimo del host del player para Aura Music.
 * Adaptado de BitChord: sin Canvas, Party, Cast, Watch, streaming ni traducción.
 */
interface SystemVolume {
    val level: StateFlow<Float>
    fun set(level: Float)
    fun refresh()
}
