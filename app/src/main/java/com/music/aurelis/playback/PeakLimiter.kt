// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.playback

import kotlin.math.exp

class PeakLimiter {

    private var releaseCoef = 1f
    private var reduction = 1f

    fun configure(sampleRate: Int) {
        releaseCoef = if (sampleRate <= 0) 1f
        else (1.0 - exp(-1.0 / (RELEASE_SECONDS * sampleRate))).toFloat()
        reduction = 1f
    }

    fun reset() { reduction = 1f }

    fun gainFor(peak: Float): Float {
        val wanted = if (peak > CEILING) CEILING / peak else 1f
        reduction = if (wanted < reduction) wanted else reduction + (wanted - reduction) * releaseCoef
        return reduction
    }

    companion object {
        const val CEILING = 0.985f
        private const val RELEASE_SECONDS = 0.1
    }
}
