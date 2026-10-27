package com.music.aura.playback.audio

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import com.music.aura.BuildConfig
import java.nio.ByteBuffer
import java.nio.ByteOrder

@androidx.annotation.OptIn(UnstableApi::class)
class PrecisionAudioSink(
    private val delegate: AudioSink,
    val dspChain: DspChain,
    private val enableFloatOutput: Boolean,
    private val preferredOutputEncodingProvider: ((Format) -> PcmEncoding?)? = null,
    private val isAudible: () -> Boolean = { true },
) : ForwardingAudioSink(delegate) {

    var isPrecisionActive: Boolean = false
        private set

    var inputPcmEncoding: PcmEncoding? = null
        private set

    var targetOutputEncoding: PcmEncoding? = null
        private set

    var activeFormat: Format? = null
        private set

    private var configuredSampleRate: Int = 0

    override fun configure(audioSinkConfig: AudioSink.AudioSinkConfig) {
        val format = audioSinkConfig.format
        activeFormat = format
        val resolved = resolveTargetEncoding(preferredOutputEncodingProvider?.invoke(format), format)
        try {
            delegate.configure(audioSinkConfig)
            isPrecisionActive = false
            inputPcmEncoding = null
            targetOutputEncoding = resolved
            logConfig(
                precisionActive = false,
                inputEncoding = mapToPcmEncoding(format.pcmEncoding)?.name ?: "unknown",
                sampleRate = format.sampleRate,
                channelCount = format.channelCount,
                targetOutputEncoding = resolved.name,
                enableFloatOutput = enableFloatOutput,
                delegateEncoding = format.pcmEncoding,
            )
        } catch (error: Exception) {
            isPrecisionActive = false
            inputPcmEncoding = null
            targetOutputEncoding = null
            Log.w("PrecisionAudioSink", "configure failed", error)
        }
    }

    private fun resolveTargetEncoding(preferred: PcmEncoding?, format: Format): PcmEncoding {
        if (preferred != null) return preferred
        return if (enableFloatOutput) PcmEncoding.PCM_FLOAT else PcmEncoding.PCM_16BIT
    }

    private fun logConfig(
        precisionActive: Boolean,
        inputEncoding: String,
        sampleRate: Int,
        channelCount: Int,
        targetOutputEncoding: String,
        enableFloatOutput: Boolean,
        delegateEncoding: Int,
    ) {
        if (BuildConfig.DEBUG) {
            Log.d(
                "PrecisionAudioSink",
                "precisionActive=$precisionActive input=$inputEncoding sampleRate=$sampleRate channels=$channelCount target=$targetOutputEncoding enableFloatOutput=$enableFloatOutput delegateEncoding=$delegateEncoding",
            )
        }
    }

    private fun mapToPcmEncoding(value: Int): PcmEncoding? = when (value) {
        C.ENCODING_PCM_16BIT -> PcmEncoding.PCM_16BIT
        C.ENCODING_PCM_24BIT -> PcmEncoding.PCM_24BIT_PACKED
        C.ENCODING_PCM_32BIT -> PcmEncoding.PCM_32BIT
        C.ENCODING_PCM_FLOAT -> PcmEncoding.PCM_FLOAT
        else -> null
    }

    private fun ensureBuffers(channelCount: Int) {
        configuredSampleRate = if (channelCount > 0) configuredSampleRate else 0
    }

    private fun shouldActivatePrecision(format: Format): Boolean =
        format.sampleRate > 0 && format.channelCount > 0

    private fun PcmEncoding.toMedia3PcmEncoding(): Int = when (this) {
        PcmEncoding.PCM_16BIT -> C.ENCODING_PCM_16BIT
        PcmEncoding.PCM_24BIT_PACKED -> C.ENCODING_PCM_24BIT
        PcmEncoding.PCM_32BIT -> C.ENCODING_PCM_32BIT
        PcmEncoding.PCM_FLOAT -> C.ENCODING_PCM_FLOAT
    }
}
