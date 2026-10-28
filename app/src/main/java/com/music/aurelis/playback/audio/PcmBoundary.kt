package com.music.aurelis.playback.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Raw PCM sample formats that [PcmBoundary] can decode/encode.
 */
enum class PcmEncoding(val bytesPerSample: Int) {
    PCM_16BIT(2),
    PCM_24BIT_PACKED(3),
    PCM_32BIT(4),
    PCM_FLOAT(4),
    ;

    fun bytesPerFrame(channelCount: Int): Int =
        if (channelCount <= 0) 0 else bytesPerSample * channelCount
}

object PcmBoundary {

    fun decode(
        inputBuffer: ByteBuffer,
        encoding: PcmEncoding,
        destinationBlock: AudioBlock,
        maxFrames: Int = destinationBlock.capacityFrames,
    ): Int {
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val channelCount = destinationBlock.channelCount
        val bytesPerFrame = encoding.bytesPerFrame(channelCount)
        if (bytesPerFrame <= 0) {
            destinationBlock.reset(0)
            return 0
        }

        val availableFrames = inputBuffer.remaining() / bytesPerFrame
        val framesToRead = minOf(availableFrames, maxFrames, destinationBlock.capacityFrames)
        if (framesToRead <= 0) {
            destinationBlock.reset(0)
            return 0
        }

        val totalSamples = framesToRead * channelCount
        var dstIdx = 0
        when (encoding) {
            PcmEncoding.PCM_16BIT -> {
                val inv = 1.0f / 32768.0f
                for (i in 0 until totalSamples) {
                    destinationBlock.samples[dstIdx++] = inputBuffer.getShort().toFloat() * inv
                }
            }
            PcmEncoding.PCM_24BIT_PACKED -> {
                val inv = 1.0f / 8388608.0f
                for (i in 0 until totalSamples) {
                    val b0 = inputBuffer.get().toInt() and 0xFF
                    val b1 = inputBuffer.get().toInt() and 0xFF
                    val b2 = inputBuffer.get().toInt()
                    val sample24 = (b2 shl 16) or (b1 shl 8) or b0
                    destinationBlock.samples[dstIdx++] = sample24.toFloat() * inv
                }
            }
            PcmEncoding.PCM_32BIT -> {
                val inv = 1.0 / 2147483648.0
                for (i in 0 until totalSamples) {
                    destinationBlock.samples[dstIdx++] = (inputBuffer.getInt().toDouble() * inv).toFloat()
                }
            }
            PcmEncoding.PCM_FLOAT -> {
                for (i in 0 until totalSamples) {
                    destinationBlock.samples[dstIdx++] = sanitizeFloat(inputBuffer.getFloat())
                }
            }
        }

        destinationBlock.reset(framesToRead)
        return framesToRead
    }

    fun encode(
        sourceBlock: AudioBlock,
        encoding: PcmEncoding,
        outputBuffer: ByteBuffer,
        startFrame: Int = 0,
        framesToEncode: Int = sourceBlock.frameCount - startFrame,
    ): Int {
        outputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val channelCount = sourceBlock.channelCount
        val bytesPerFrame = encoding.bytesPerFrame(channelCount)
        if (bytesPerFrame <= 0) return 0

        require(startFrame >= 0 && startFrame <= sourceBlock.frameCount) {
            "startFrame ($startFrame) out of bounds for frameCount ${sourceBlock.frameCount}"
        }
        val availableSourceFrames = sourceBlock.frameCount - startFrame
        val requestedFrames = minOf(framesToEncode, availableSourceFrames)
        val availableOutputFrames = outputBuffer.remaining() / bytesPerFrame
        val framesToWrite = minOf(requestedFrames, availableOutputFrames)
        if (framesToWrite <= 0) return 0

        var srcIdx = startFrame * channelCount
        val totalSamples = framesToWrite * channelCount
        when (encoding) {
            PcmEncoding.PCM_16BIT -> {
                for (i in 0 until totalSamples) {
                    outputBuffer.putShort(clamp16FromFloat(sourceBlock.samples[srcIdx++]))
                }
            }
            PcmEncoding.PCM_24BIT_PACKED -> {
                for (i in 0 until totalSamples) {
                    val sample24 = clamp24FromFloat(sourceBlock.samples[srcIdx++])
                    outputBuffer.put((sample24 and 0xFF).toByte())
                    outputBuffer.put(((sample24 ushr 8) and 0xFF).toByte())
                    outputBuffer.put(((sample24 ushr 16) and 0xFF).toByte())
                }
            }
            PcmEncoding.PCM_32BIT -> {
                for (i in 0 until totalSamples) {
                    outputBuffer.putInt(clamp32FromFloat(sourceBlock.samples[srcIdx++]))
                }
            }
            PcmEncoding.PCM_FLOAT -> {
                for (i in 0 until totalSamples) {
                    outputBuffer.putFloat(sanitizeFloat(sourceBlock.samples[srcIdx++]))
                }
            }
        }

        return framesToWrite
    }

    fun clamp16FromFloat(f: Float): Short {
        if (f.isNaN()) return 0
        val scaled = f * 32768.0f
        if (scaled <= -32768.0f) return Short.MIN_VALUE
        if (scaled >= 32767.0f) return Short.MAX_VALUE
        return Math.round(scaled).toShort()
    }

    fun clamp24FromFloat(f: Float): Int {
        if (f.isNaN()) return 0
        val scaled = f * 8388608.0f
        if (scaled <= -8388608.0f) return -8388608
        if (scaled >= 8388607.0f) return 8388607
        return Math.round(scaled)
    }

    fun clamp32FromFloat(f: Float): Int {
        if (f.isNaN()) return 0
        val scaled = f.toDouble() * 2147483648.0
        if (scaled <= -2147483648.0) return Int.MIN_VALUE
        if (scaled >= 2147483647.0) return Int.MAX_VALUE
        return Math.round(scaled).toInt()
    }

    fun sanitizeFloat(f: Float): Float {
        if (f.isNaN()) return 0.0f
        if (f == Float.POSITIVE_INFINITY) return 1.0f
        if (f == Float.NEGATIVE_INFINITY) return -1.0f
        return f
    }
}
