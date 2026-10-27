package com.music.aura.playback

import com.music.aura.playback.audio.AudioBlock
import kotlin.math.abs
import kotlin.math.exp

class LoudnessProcessor {

    @Volatile
    var gainFor: (String) -> Float = { 1f }

    @Volatile
    var mediaId: String? = null

    @Volatile
    var nextMediaId: String? = null

    @Volatile
    private var trimTarget = 1f

    var holdLimiter = false

    private var sampleRate = 0
    private var channelCount = 0
    private var currentGain = 1f
    private var currentTrim = 1f
    private var reduction = 1f
    private var glideCoef = 1f
    private var releaseCoef = 1f

    @Volatile
    private var snap = true

    fun track(mediaId: String?, nextMediaId: String?) {
        this.mediaId = mediaId
        this.nextMediaId = nextMediaId
    }

    fun setBlendTrim(value: Float) {
        trimTarget = value.coerceIn(MIN_TRIM, 1f)
    }

    fun onStreamBoundary() {
        nextMediaId?.let { mediaId = it }
        snap = true
    }

    fun configure(sampleRate: Int, channelCount: Int) {
        this.sampleRate = sampleRate
        this.channelCount = channelCount
        glideCoef = coefficient(GLIDE_SECONDS, sampleRate)
        releaseCoef = coefficient(RELEASE_SECONDS, sampleRate)
        snap = true
    }

    fun flush() {
        reduction = 1f
        snap = true
    }

    fun reset() {
        sampleRate = 0
        channelCount = 0
        reduction = 1f
        snap = true
    }

    fun process(block: AudioBlock) {
        val frames = block.frameCount
        val channels = channelCount
        if (frames == 0 || channels < 1 || sampleRate <= 0) return

        val targetGain = targetGain()
        val targetTrim = trimTarget
        if (snap) {
            currentGain = targetGain
            currentTrim = targetTrim
            snap = false
        }
        
        if (!holdLimiter && targetGain == 1f && targetTrim >= 1f &&
            abs(currentGain - 1f) < SETTLED && currentTrim >= 1f - SETTLED && reduction >= PARK_REDUCTION
        ) {
            currentGain = 1f
            currentTrim = 1f
            reduction = 1f
            return
        }

        val samples = block.samples
        var gain = currentGain
        var trim = currentTrim
        var gr = reduction
        var index = 0
        for (frame in 0 until frames) {
            gain += (targetGain - gain) * glideCoef
            trim += (targetTrim - trim) * glideCoef
            val scale = gain * trim
            var peak = 0f
            for (channel in 0 until channels) {
                val magnitude = abs(samples[index + channel])
                if (magnitude > peak) peak = magnitude
            }
            peak *= scale
            val wanted = if (peak > LIMIT) LIMIT / peak else 1f
            gr = if (wanted < gr) wanted else gr + (wanted - gr) * releaseCoef
            val applied = scale * gr
            for (channel in 0 until channels) {
                samples[index + channel] *= applied
            }
            index += channels
        }
        currentGain = gain
        currentTrim = trim
        reduction = gr
    }

    private fun targetGain(): Float = mediaId?.let { runCatching { gainFor(it) }.getOrNull() } ?: 1f

    private fun coefficient(seconds: Double, rate: Int): Float =
        if (rate <= 0) 1f else (1.0 - exp(-1.0 / (seconds * rate))).toFloat()

    companion object {
        private const val LIMIT = 0.985f
        private const val MIN_TRIM = 0.5f
        private const val GLIDE_SECONDS = 0.04
        private const val RELEASE_SECONDS = 0.08
        private const val SETTLED = 1e-4f
        private const val PARK_REDUCTION = 0.97f
    }
}
