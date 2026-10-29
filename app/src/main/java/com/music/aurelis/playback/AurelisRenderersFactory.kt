// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.playback

import android.content.Context
import android.os.Handler
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import com.music.aurelis.playback.audio.DspChain
import com.music.aurelis.playback.audio.DspChainAudioProcessor
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.FfmpegAudioRenderer
import java.util.ArrayList

@UnstableApi
class AurelisRenderersFactory(
    private val appContext: Context,
    private val dspChain: DspChain
) : DefaultRenderersFactory(appContext) {

    override fun buildAudioRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        audioSink: AudioSink?,
        eventHandler: Handler,
        eventListener: AudioRendererEventListener,
        out: ArrayList<Renderer>
    ) {
        val dspProcessor = DspChainAudioProcessor(dspChain)
        val customAudioSink = DefaultAudioSink.Builder(context)
            .setAudioProcessors(arrayOf(dspProcessor))
            .setEnableFloatOutput(true)
            .build()

        // FFmpeg para formatos exóticos (DSD, AC3, etc.)
        out.add(FfmpegAudioRenderer(eventHandler, eventListener, customAudioSink))

        // Decodificador estándar para AAC, MP3, FLAC, etc.
        out.add(MediaCodecAudioRenderer(context, mediaCodecSelector, enableDecoderFallback, eventHandler, eventListener, customAudioSink))
    }
}
