// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The bridge between ExoPlayer's ByteBuffer world and Aurelis's Float32 DSP.
 *
 * ExoPlayer feeds AudioProcessor.queueInput with interleaved 16-bit PCM in a
 * ByteBuffer. Aurelis's DspChain (equalizer, spatial, transition, loudness)
 * works on AudioBlock — Float32 in-place, with reverb tails and filter state
 * that have to survive between blocks. So this decodes, runs, and re-encodes:
 * one AudioBlock reused across calls, one DspChain instance for the lifetime
 * of the renderer.
 *
 * Not an AudioSink: the sink stays DefaultAudioSink, so the whole existing
 * audio pipeline (AudioTrack, float output, offload rejection) is unchanged.
 * This just sits inside setAudioProcessors.
 */
@UnstableApi
class DspChainAudioProcessor(
    private val chain: DspChain,
) : BaseAudioProcessor() {

    // AudioBlock is not thread-safe and doesn't grow; it's allocated once here
    // sized for the largest block ExoPlayer can hand us.
    private var block: AudioBlock = AudioBlock()
    private var inputEncoding: PcmEncoding = PcmEncoding.PCM_16BIT
    private var outputEncoding: PcmEncoding = PcmEncoding.PCM_16BIT
    private var configured = false

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val encoding = when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT -> PcmEncoding.PCM_16BIT
            C.ENCODING_PCM_24BIT -> PcmEncoding.PCM_24BIT_PACKED
            C.ENCODING_PCM_32BIT -> PcmEncoding.PCM_32BIT
            C.ENCODING_PCM_FLOAT -> PcmEncoding.PCM_FLOAT
            else -> return AudioProcessor.AudioFormat.NOT_SET
        }
        if (inputAudioFormat.channelCount <= 0 || inputAudioFormat.sampleRate <= 0) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        inputEncoding = encoding
        outputEncoding = encoding
        if (!configured || block.channelCount != inputAudioFormat.channelCount) {
            block = AudioBlock(channelCount = inputAudioFormat.channelCount)
        }
        chain.configure(inputAudioFormat.sampleRate, inputAudioFormat.channelCount)
        configured = true
        // Same format out as in: the DSP chain changes sample values, not their
        // container, and forcing Float32 out would fight the sink's own
        // negotiation (which already prefers float where the device supports it).
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val bytesPerFrame = inputEncoding.bytesPerFrame(block.channelCount)
        if (bytesPerFrame <= 0) {
            replaceOutputBuffer(inputBuffer.remaining()).put(inputBuffer).flip()
            return
        }
        val available = inputBuffer.remaining()
        if (available < bytesPerFrame) {
            // Tail too small to form a frame; forward untouched so the sink's
            // own frame accounting stays in step.
            replaceOutputBuffer(available).put(inputBuffer).flip()
            return
        }

        val frameCount = available / bytesPerFrame
        val dst = replaceOutputBuffer(available)
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        dst.order(ByteOrder.LITTLE_ENDIAN)

        var remainingFrames = frameCount
        while (remainingFrames > 0) {
            val chunkFrames = minOf(remainingFrames, block.capacityFrames)
            val chunkBytes = chunkFrames * bytesPerFrame
            val slice = inputBuffer.slice().order(ByteOrder.LITTLE_ENDIAN)
            slice.limit(chunkBytes)
            PcmBoundary.decode(slice, inputEncoding, block, chunkFrames)
            chain.process(block)
            PcmBoundary.encode(block, outputEncoding, dst, startFrame = 0, framesToEncode = block.frameCount)
            inputBuffer.position(inputBuffer.position() + chunkBytes)
            remainingFrames -= chunkFrames
        }
        dst.flip()
    }

    override fun onFlush() {
        // A seek, a track change, or a format change: the filters' tails would
        // otherwise carry across the boundary and bleed one track into the next.
        chain.flush()
        chain.onStreamBoundary()
    }

    override fun onReset() {
        chain.reset()
        configured = false
        block = AudioBlock()
    }
}
