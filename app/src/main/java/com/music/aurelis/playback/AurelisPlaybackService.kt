// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.music.aurelis.MainActivity
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.playback.audio.DspChain
import com.music.aurelis.playback.dsd.DsdExtractorsFactory

@OptIn(UnstableApi::class)
class AurelisPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var dspChain: DspChain? = null

    override fun onCreate() {
        super.onCreate()

        val chain = DspChain()
        dspChain = chain

        val renderersFactory = AurelisRenderersFactory(this, chain)

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

        applyEqSettings()
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

    private fun applyEqSettings() {
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
