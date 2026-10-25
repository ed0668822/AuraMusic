package com.music.aura.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import com.music.aura.MainActivity
import com.music.aura.R

/**
 * Servicio de reproducción de Aura Music.
 *
 * Usa Media3 MediaSessionService para gestionar ExoPlayer y exponer
 * controles a la notificación, auriculares, Android Auto (si aplica).
 *
 * La notificación se personaliza con:
 * - Canal dedicado con nombre descriptivo.
 * - Icono propio (nota musical).
 * - Estilo MediaStyle con controles compactos.
 * - PendingIntent que abre la app al tocar la notificación.
 *
 * 100% local: solo reproduce archivos del dispositivo vía MediaStore.
 */
@OptIn(UnstableApi::class)
class AuraPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        // Intent que abre la app al tocar la notificación
        val sessionActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    /**
     * Personaliza la notificación que Media3 genera automáticamente.
     *
     * Se llama cada vez que el estado del reproductor cambia.
     * Aquí añadimos icono, canal y estilo propios.
     */
    override fun updateNotification(session: MediaSession) {
        val player = session.player
        if (player.mediaItemCount == 0) {
            // Sin contenido: no mostrar notificación.
            stopForeground(STOP_FOREGROUND_REMOVE)
            return
        }

        val notification = buildNotification(session, player)
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(session: MediaSession, player: Player): android.app.Notification {
        val metadata = player.currentMediaItem?.mediaMetadata
        val title = metadata?.title?.toString() ?: "Aura Music"
        val artist = metadata?.artist?.toString() ?: ""

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(artist)
            .setOngoing(player.isPlaying)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setStyle(
                MediaStyleNotificationHelper.MediaStyle(session)
                    .setShowActionsInCompactView(0, 1, 2)
            )

        // Acción: Anterior
        val prevIntent = Intent(this, AuraPlaybackService::class.java).apply {
            action = ACTION_PREVIOUS
        }
        builder.addAction(
            NotificationCompat.Action.Builder(
                R.drawable.ic_skip_previous,
                "Anterior",
                PendingIntent.getService(
                    this, 1, prevIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            ).build(),
        )

        // Acción: Play/Pausa
        val playPauseIcon = if (player.isPlaying) {
            R.drawable.ic_pause
        } else {
            R.drawable.ic_play_arrow
        }
        val playPauseLabel = if (player.isPlaying) "Pausar" else "Reproducir"
        val playPauseIntent = Intent(this, AuraPlaybackService::class.java).apply {
            action = ACTION_PLAY_PAUSE
        }
        builder.addAction(
            NotificationCompat.Action.Builder(
                playPauseIcon,
                playPauseLabel,
                PendingIntent.getService(
                    this, 2, playPauseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            ).build(),
        )

        // Acción: Siguiente
        val nextIntent = Intent(this, AuraPlaybackService::class.java).apply {
            action = ACTION_NEXT
        }
        builder.addAction(
            NotificationCompat.Action.Builder(
                R.drawable.ic_skip_next,
                "Siguiente",
                PendingIntent.getService(
                    this, 3, nextIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            ).build(),
        )

        return builder.build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Manejar acciones de los botones de la notificación
        when (intent?.action) {
            ACTION_PREVIOUS -> mediaSession?.player?.seekToPreviousMediaItem()
            ACTION_PLAY_PAUSE -> {
                val player = mediaSession?.player
                if (player != null) {
                    if (player.isPlaying) player.pause() else player.play()
                }
            }
            ACTION_NEXT -> mediaSession?.player?.seekToNextMediaItem()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = getString(R.string.notification_channel_description)
                    setShowBadge(false)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "aura_playback"
        private const val NOTIFICATION_ID = 1001

        private const val ACTION_PREVIOUS = "com.music.aura.PREVIOUS"
        private const val ACTION_PLAY_PAUSE = "com.music.aura.PLAY_PAUSE"
        private const val ACTION_NEXT = "com.music.aura.NEXT"
    }
}
