package com.music.aura.playback.smart

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.ceil
import kotlin.math.floor

object VocalSpectrogram {
    val available: Boolean get() = MelSpectrogram.available
    val bins: Int by lazy { if (available) nativeBins() else 2049 }
    val sampleRate: Double by lazy { if (available) nativeSampleRate() else 44_100.0 }
    val hop: Int by lazy { if (available) nativeHop() else 1024 }
    val fftSize: Int by lazy { if (available) nativeFftSize() else 4096 }
    val frameRate: Double get() = sampleRate / hop

    fun compute(left: FloatArray, right: FloatArray, rate: Double = sampleRate): Spectrogram? {
        if (!available || left.isEmpty() || left.size != right.size) return null
        val values = nativeCompute(left, right, rate)
        if (values.isEmpty()) return null
        return Spectrogram(values, frames = values.size / (CHANNELS * bins), bins = bins)
    }

    data class Spectrogram(val values: FloatArray, val frames: Int, val bins: Int) {
        override fun equals(other: Any?): Boolean =
            this === other || (other is Spectrogram && frames == other.frames &&
                bins == other.bins && values.contentEquals(other.values))

        override fun hashCode(): Int = 31 * (31 * values.contentHashCode() + frames) + bins
    }

    const val CHANNELS = 2

    @JvmStatic private external fun nativeCompute(left: FloatArray, right: FloatArray, rate: Double): FloatArray
    @JvmStatic private external fun nativeBins(): Int
    @JvmStatic private external fun nativeSampleRate(): Double
    @JvmStatic private external fun nativeHop(): Int
    @JvmStatic private external fun nativeFftSize(): Int
}

class VocalTracker(
    private val modelPath: () -> String,
    private val inferenceThreads: () -> Int,
) {

    @Volatile private var session: OrtSession? = null
    @Volatile private var sessionThreads = 0
    private val lock = Any()

    private fun session(): OrtSession? {
        val threads = inferenceThreads()
        session?.takeIf { sessionThreads == threads }?.let { return it }
        synchronized(lock) {
            session?.takeIf { sessionThreads == threads }?.let { return it }
            runCatching { session?.close() }
            session = null
            return runCatching {
                val file = File(modelPath())
                val options = OrtSession.SessionOptions().apply {
                    setIntraOpNumThreads(threads)
                    setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                    setCPUArenaAllocator(false)
                    setMemoryPatternOptimization(false)
                }
                OrtEnvironment.getEnvironment().createSession(file.absolutePath, options)
                    .also {
                        session = it
                        sessionThreads = threads
                    }
            }.onFailure { AnalysisLog.warn("Vocal model unavailable; no mask will be produced", it) }
                .getOrNull()
        }
    }

    fun track(left: FloatArray, right: FloatArray, rate: Double): FloatArray? {
        if (!VocalSpectrogram.available) return null
        val resampledLeft = MelSpectrogram.resample(left, rate, VocalSpectrogram.sampleRate) ?: return null
        val resampledRight = MelSpectrogram.resample(right, rate, VocalSpectrogram.sampleRate) ?: return null

        val spectrogram = VocalSpectrogram.compute(resampledLeft, resampledRight) ?: return null
        if (spectrogram.frames > FIXED_FRAMES) {
            AnalysisLog.warn("Window of ${spectrogram.frames} frames exceeds the model's $FIXED_FRAMES")
            return null
        }
        val active = session() ?: return null

        return runCatching {
            val bins = spectrogram.bins
            val backing = ByteBuffer
                .allocateDirect(VocalSpectrogram.CHANNELS * bins * FIXED_FRAMES * Float.SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
            fillFixedFrames(backing.asFloatBuffer(), spectrogram.values, bins, spectrogram.frames)
            val environment = OrtEnvironment.getEnvironment()
            val shape = longArrayOf(1, VocalSpectrogram.CHANNELS.toLong(), bins.toLong(), FIXED_FRAMES.toLong())

            OnnxTensor.createTensor(environment, backing.asFloatBuffer(), shape).use { tensor ->
                active.run(mapOf(active.inputNames.first() to tensor)).use { outputs ->
                    val target = (outputs.get(0) as OnnxTensor).floatBuffer
                    reduceToBandCurve(backing.asFloatBuffer(), target, bins, spectrogram.frames)
                }
            }
        }.onFailure { AnalysisLog.warn("Vocal inference failed", it) }.getOrNull()
    }

    private fun fillFixedFrames(into: FloatBuffer, values: FloatArray, bins: Int, frames: Int) {
        if (frames == FIXED_FRAMES) {
            into.put(values)
            return
        }
        val pad = FloatArray(FIXED_FRAMES - frames)
        for (channel in 0 until VocalSpectrogram.CHANNELS) {
            for (bin in 0 until bins) {
                into.put(values, (channel * bins + bin) * frames, frames)
                into.put(pad)
            }
        }
    }

    private fun reduceToBandCurve(
        mix: FloatBuffer,
        target: FloatBuffer,
        bins: Int,
        usableFrames: Int,
    ): FloatArray {
        val lowBin = floor(LOW_HZ * VocalSpectrogram.fftSize / VocalSpectrogram.sampleRate)
            .toInt().coerceAtLeast(0)
        val highBin = ceil(HIGH_HZ * VocalSpectrogram.fftSize / VocalSpectrogram.sampleRate)
            .toInt().coerceAtMost(bins - 1)
        if (highBin <= lowBin || usableFrames <= 0) return FloatArray(0)

        val curve = FloatArray(usableFrames)
        for (frame in 0 until usableFrames) {
            var sum = 0.0
            var count = 0
            for (channel in 0 until VocalSpectrogram.CHANNELS) {
                for (bin in lowBin..highBin) {
                    val index = (channel * bins + bin) * FIXED_FRAMES + frame
                    val mixValue = mix.get(index)
                    if (mixValue <= 1e-6f) continue
                    val ratio = target.get(index) / mixValue
                    sum += ratio.coerceIn(0f, 1f)
                    count += 1
                }
            }
            curve[frame] = if (count > 0) (sum / count).toFloat() else 0f
        }
        return curve
    }

    fun release() {
        synchronized(lock) {
            runCatching { session?.close() }
            session = null
            sessionThreads = 0
        }
    }

    companion object {
        const val MODEL_ASSET = "vocals_umxhq_int8.onnx"
        const val FIXED_FRAMES = 960
        private const val LOW_HZ = 200.0
        private const val HIGH_HZ = 4000.0
    }
}
