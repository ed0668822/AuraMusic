// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.music.aurelis.MainActivity
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.playback.audio.DspChain
import com.music.aurelis.playback.audio.DspChainAudioProcessor
import com.music.aurelis.playback.dsd.DsdExtractorsFactory
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory

@OptIn(UnstableApi::class)
class AurelisPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var dspChain: DspChain? = null

    override fun onCreate() {
        super.onCreate()

        val chain = DspChain()
        dspChain = chain

        // The DspChain must see the *same* instances that SettingsRepository
        // pushes tuning into. DspChain's constructor already wires them via
        // its default arguments, so this is the single point that owns them.
        // EqualizerProcessor / SpatialAudioProcessor / TransitionFilterProcessor
        // all read their target from @Volatile fields set by the UI — the chain
        // just decides when to process.

        val dspProcessor = DspChainAudioProcessor(chain)

        val audioSink: AudioSink = DefaultAudioSink.Builder(this)
            .setAudioProcessors(arrayOf(dspProcessor))
            // Float output where the device supports it: the EQ's make-up gain
            // and the limiter were designed for Float32 throughout.
            .setEnableFloatOutput(true)
            .build()

        val renderersFactory = NextRenderersFactory(this)
            .setEnableDecoderFallback(true)
            .setAudioSink(audioSink)

        val mediaSourceFactory = DefaultMediaSourceFactory(
            DefaultDataSource.Factory(this),
            DsdExtractorsFactory(),
        )

        val player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        // Bridge the settings the user changes while playback runs. Both
        // are cheap StateFlows so no listener bookkeeping needed.
        applyEqSettings(player)
        applySpatialSettings()

        val sessionActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val sessionActivity = PendingIntent.getActivity(
            this, 0, sessionActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()
    }

    private fun applyEqSettings(player: ExoPlayer) {
        // Attach the EqualizerProcessor to whichever chain instance exists —
        // the chain owns it, and pushing here means the audio thread sees the
        // new tuning on its next block rather than on a recreate.
        val chain = dspChain ?: return
        chain.equalizer.setTuning(
            enabled = SettingsRepository.equalizerEnabled.value,
            curve = if (SettingsRepository.equalizerMode.value == com.music.aurelis.data.EqualizerMode.MANUAL) {
                manualCurve(SettingsRepository.equalizerBands.value)
            } else {
                toneCurve(
                    SettingsRepository.equalizerToneX.value,
                    SettingsRepository.equalizerToneY.value,
                    SettingsRepository.equalizerFocused.value,
                )
            },
            balance = SettingsRepository.equalizerBalance.value,
        )
        chain.spatial.enabled = SettingsRepository.spatialAudio.value
    }

    private fun applySpatialSettings() {
        dspChain?.spatial?.enabled = SettingsRepository.spatialAudio.value
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        dspChain = null
        super.onDestroy()
    }
}
