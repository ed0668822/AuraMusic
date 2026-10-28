package com.music.aurelis.widget

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.music.aurelis.playback.AurelisPlaybackService
import java.util.concurrent.atomic.AtomicBoolean

class MediaWidgetActions : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action?.takeIf { it in ACTIONS } ?: return
        val app = context.applicationContext

        val pending = goAsync()
        val handler = Handler(Looper.getMainLooper())
        val token = SessionToken(app, ComponentName(app, AurelisPlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()

        val done = AtomicBoolean(false)
        fun release() {
            if (!done.compareAndSet(false, true)) return
            MediaController.releaseFuture(future)
            runCatching { pending.finish() }
        }

        future.addListener(
            {
                runCatching { future.get() }.getOrNull()?.let { controller ->
                    runCatching { controller.execute(action) }
                }
                handler.postDelayed(::release, SETTLE_MS)
            },
            ContextCompat.getMainExecutor(app),
        )
        handler.postDelayed(::release, GIVE_UP_MS)
    }

    private fun MediaController.execute(action: String) {
        if (mediaItemCount == 0) return
        when (action) {
            ACTION_TOGGLE -> if (playWhenReady) {
                pause()
            } else {
                if (playbackState == Player.STATE_ENDED) seekTo(0L)
                prepareIfIdle()
                play()
            }
            ACTION_NEXT -> {
                seekToNextMediaItem()
                prepareIfIdle()
            }
            ACTION_PREVIOUS -> {
                seekToPreviousMediaItem()
                prepareIfIdle()
            }
        }
    }

    private fun MediaController.prepareIfIdle() {
        if (playbackState == Player.STATE_IDLE) prepare()
    }

    companion object {
        const val ACTION_TOGGLE = "com.music.aurelis.widget.TOGGLE"
        const val ACTION_NEXT = "com.music.aurelis.widget.NEXT"
        const val ACTION_PREVIOUS = "com.music.aurelis.widget.PREVIOUS"

        fun pendingIntent(context: Context, action: String): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                REQUEST_BASE + ACTIONS.indexOf(action),
                Intent(context, MediaWidgetActions::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        private val ACTIONS = listOf(ACTION_TOGGLE, ACTION_NEXT, ACTION_PREVIOUS)

        private const val REQUEST_BASE = 100
        private const val SETTLE_MS = 2_000L
        private const val GIVE_UP_MS = 7_000L
    }
}
