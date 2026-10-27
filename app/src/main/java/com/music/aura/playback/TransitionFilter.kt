package com.music.aura.playback

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.tan

class TransitionFilter {

    @Volatile
    private var targetLowPassHz: Float = OPEN_HZ

    @Volatile
    private var targetHighPassHz: Float = OFF_HZ

    private var channelCount = 0
    private var sampleRate = 0

    private var currentLowPassHz = OPEN_HZ
    private var currentHighPassHz = OFF_HZ

    private var lowState = FloatArray(0)
    private var highState = FloatArray(0)

    private val lowA1 = FloatArray(STAGES)
    private val lowA2 = FloatArray(STAGES)
    private val lowA3 = FloatArray(STAGES)
    private val highA1 = FloatArray(STAGES)
    private val highA2 = FloatArray(STAGES)
    private val highA3 = FloatArray(STAGES)
    private val highK = FloatArray(STAGES)

    private var lowOn = false
    private var highOn = false

    val active: Boolean get() = lowOn || highOn

    fun configure(channelCount: Int, sampleRate: Int) {
        this.channelCount = channelCount
        this.sampleRate = sampleRate
        lowState = FloatArray(channelCount * STAGES * 2)
        highState = FloatArray(channelCount * STAGES * 2)
        currentLowPassHz = targetLowPassHz
        currentHighPassHz = targetHighPassHz
    }

    fun setCutoffs(lowPassHz: Float, highPassHz: Float) {
        targetLowPassHz = lowPassHz.coerceIn(MIN_HZ, OPEN_HZ)
        targetHighPassHz = highPassHz.coerceIn(OFF_HZ, MAX_HIGH_PASS_HZ)
    }

    fun open() = setCutoffs(OPEN_HZ, OFF_HZ)

    val parked: Boolean
        get() = targetLowPassHz >= OPEN_HZ && targetHighPassHz <= OFF_HZ &&
            currentLowPassHz >= OPEN_HZ - SETTLED_HZ && currentHighPassHz <= OFF_HZ + SETTLED_HZ

    fun advance() {
        currentLowPassHz = glide(currentLowPassHz, targetLowPassHz)
        currentHighPassHz = glide(currentHighPassHz, targetHighPassHz)
        lowOn = currentLowPassHz < OPEN_HZ - SETTLED_HZ
        highOn = currentHighPassHz > OFF_HZ + SETTLED_HZ
        if (lowOn) updateLowCoefficients()
        if (highOn) updateHighCoefficients()
    }

    fun filter(channel: Int, input: Float): Float {
        var value = input
        if (lowOn) value = lowPass(channel, value)
        if (highOn) value = highPass(channel, value)
        return value
    }

    fun flush() {
        lowState.fill(0f)
        highState.fill(0f)
        currentLowPassHz = targetLowPassHz
        currentHighPassHz = targetHighPassHz
    }

    fun reset() {
        targetLowPassHz = OPEN_HZ
        targetHighPassHz = OFF_HZ
        lowState = FloatArray(0)
        highState = FloatArray(0)
        lowOn = false
        highOn = false
    }

    private fun glide(current: Float, target: Float): Float {
        val from = ln(current.coerceAtLeast(MIN_HZ))
        val to = ln(target.coerceAtLeast(MIN_HZ))
        return exp(from + (to - from) * GLIDE_RATE)
    }

    private fun usableCutoff(hz: Float): Float =
        hz.coerceIn(MIN_HZ, sampleRate * MAX_CUTOFF_FRACTION)

    private fun updateLowCoefficients() {
        val g = tan(Math.PI * usableCutoff(currentLowPassHz) / sampleRate).toFloat()
        for (stage in 0 until STAGES) {
            val k = 1f / BUTTERWORTH_Q[stage]
            val a1 = 1f / (1f + g * (g + k))
            lowA1[stage] = a1
            lowA2[stage] = g * a1
            lowA3[stage] = g * (g * a1)
        }
    }

    private fun updateHighCoefficients() {
        val g = tan(Math.PI * usableCutoff(currentHighPassHz) / sampleRate).toFloat()
        for (stage in 0 until STAGES) {
            val k = 1f / BUTTERWORTH_Q[stage]
            val a1 = 1f / (1f + g * (g + k))
            highA1[stage] = a1
            highA2[stage] = g * a1
            highA3[stage] = g * (g * a1)
            highK[stage] = k
        }
    }

    private fun lowPass(channel: Int, input: Float): Float {
        var value = input
        for (stage in 0 until STAGES) {
            val i = (channel * STAGES + stage) * 2
            val ic1 = lowState[i]
            val ic2 = lowState[i + 1]
            val v3 = value - ic2
            val v1 = lowA1[stage] * ic1 + lowA2[stage] * v3
            val v2 = ic2 + lowA2[stage] * ic1 + lowA3[stage] * v3
            lowState[i] = 2f * v1 - ic1
            lowState[i + 1] = 2f * v2 - ic2
            value = v2
        }
        return value
    }

    private fun highPass(channel: Int, input: Float): Float {
        var value = input
        for (stage in 0 until STAGES) {
            val i = (channel * STAGES + stage) * 2
            val ic1 = highState[i]
            val ic2 = highState[i + 1]
            val v3 = value - ic2
            val v1 = highA1[stage] * ic1 + highA2[stage] * v3
            val v2 = ic2 + highA2[stage] * ic1 + highA3[stage] * v3
            highState[i] = 2f * v1 - ic1
            highState[i + 1] = 2f * v2 - ic2
            value -= highK[stage] * v1 + v2
        }
        return value
    }

    companion object {
        const val OPEN_HZ = 20_000f
        const val OFF_HZ = 20f
        const val MAX_HIGH_PASS_HZ = 2_000f
        const val GLIDE_FRAMES = 64

        private const val MIN_HZ = 10f
        private const val STAGES = 2
        private val BUTTERWORTH_Q = floatArrayOf(0.54120f, 1.30656f)
        private const val GLIDE_RATE = 0.05f
        private const val SETTLED_HZ = 1f
        private const val MAX_CUTOFF_FRACTION = 0.45f
    }
}
