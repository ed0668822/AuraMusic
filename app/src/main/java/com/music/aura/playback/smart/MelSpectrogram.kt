package com.music.aura.playback.smart

object MelSpectrogram {

    val available: Boolean get() = NativeAnalysisLibrary.available

    val mels: Int by lazy { if (available) nativeMelCount() else 128 }

    val sampleRate: Double by lazy { if (available) nativeSampleRate() else 22_050.0 }

    val hop: Int by lazy { if (available) nativeHop() else 441 }

    val frameRate: Double get() = sampleRate / hop

    fun compute(samples: FloatArray, sampleRate: Double = this.sampleRate): Spectrogram? {
        if (!available || samples.isEmpty()) return null
        val values = nativeCompute(samples, sampleRate)
        if (values.isEmpty()) return null
        return Spectrogram(values = values, frames = values.size / mels, mels = mels)
    }

    data class Spectrogram(val values: FloatArray, val frames: Int, val mels: Int) {
        val durationSeconds: Double get() = frames / (sampleRate / hop)

        override fun equals(other: Any?): Boolean =
            this === other ||
                (other is Spectrogram && frames == other.frames && mels == other.mels &&
                    values.contentEquals(other.values))

        override fun hashCode(): Int = 31 * (31 * values.contentHashCode() + frames) + mels
    }

    fun resample(samples: FloatArray, inputRate: Double, outputRate: Double = sampleRate): FloatArray? {
        if (!available || samples.isEmpty() || inputRate <= 0 || outputRate <= 0) return null
        return nativeResample(samples, inputRate, outputRate).takeIf { it.isNotEmpty() }
    }

    @JvmStatic private external fun nativeCompute(samples: FloatArray, sampleRate: Double): FloatArray
    @JvmStatic private external fun nativeResample(samples: FloatArray, inputRate: Double, outputRate: Double): FloatArray
    @JvmStatic private external fun nativeMelCount(): Int
    @JvmStatic private external fun nativeSampleRate(): Double
    @JvmStatic private external fun nativeHop(): Int
}
