package com.music.aura.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.music.aura.MainActivity
import com.music.aura.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

class MediaWidgetPill : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        renderAsync(context, ids)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: Bundle?) {
        renderAsync(context, intArrayOf(id))
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        for (id in ids) lastBody.remove(id)
    }

    private fun renderAsync(context: Context, ids: IntArray) {
        val pending = goAsync()
        val app = context.applicationContext
        scope.launch {
            try { render(app, ids) } finally { runCatching { pending.finish() } }
        }
    }

    companion object {
        internal fun refresh(context: Context) {
            val app = context.applicationContext
            scope.launch {
                val manager = runCatching { AppWidgetManager.getInstance(app) }.getOrNull() ?: return@launch
                val ids = runCatching { manager.getAppWidgetIds(ComponentName(app, MediaWidgetPill::class.java)) }.getOrNull()
                if (ids == null || ids.isEmpty()) return@launch
                render(app, ids)
            }
        }

        private suspend fun render(context: Context, ids: IntArray) {
            val manager = runCatching { AppWidgetManager.getInstance(context) }.getOrNull() ?: return
            val snapshot = MediaWidgetSnapshot.load(context)
            val key = snapshot.artworkKey
            val resources = context.resources
            val discPx = resources.getDimensionPixelSize(R.dimen.widget_pill_art)
            val heightPx = resources.getDimensionPixelSize(R.dimen.widget_pill_height)
            for (id in ids) {
                val widthPx = widthPx(context, manager, id)
                val cachedDisc = key?.let { MediaWidgetArt.peekCircle(it, discPx) }
                val cachedBody = key?.let { MediaWidgetArt.peekPill(it, widthPx, heightPx) }
                if (cachedDisc == null || cachedBody == null) {
                    manager.push(id, views(context, snapshot, cachedDisc ?: lastDisc, cachedBody ?: lastBody[id]))
                }
                val drawn = withTimeoutOrNull(RENDER_TIMEOUT_MS) {
                    val disc = cachedDisc ?: key?.let { MediaWidgetArt.circle(context, snapshot.artworkUri, discPx, it) }
                    disc to (cachedBody ?: MediaWidgetArt.pill(context, snapshot.artworkUri, widthPx, heightPx, key))
                } ?: continue
                val (disc, body) = drawn
                lastDisc = disc
                lastBody[id] = body
                manager.push(id, views(context, snapshot, disc, body))
            }
        }

        private fun widthPx(context: Context, manager: AppWidgetManager, id: Int): Int {
            val options = runCatching { manager.getAppWidgetOptions(id) }.getOrNull()
            val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val widthDp = options?.getInt(
                if (landscape) AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
            )?.takeIf { it > 0 } ?: FALLBACK_WIDTH_DP
            return (widthDp * context.resources.displayMetrics.density).roundToInt().coerceIn(1, MAX_WIDTH_PX)
        }

        private fun views(context: Context, snapshot: MediaWidgetSnapshot, disc: Bitmap?, body: Bitmap?): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_media_pill)
            disc?.let { views.setImageViewBitmap(R.id.widget_art, it) }
            body?.let {
                views.setImageViewBitmap(R.id.widget_pill_bg, it)
                views.setInt(R.id.widget_pill_bg, "setBackgroundResource", 0)
            }
            views.setTextViewText(R.id.widget_title, when {
                !snapshot.hasTrack -> context.getString(R.string.widget_nothing_played)
                snapshot.artist.isBlank() -> snapshot.title
                else -> "${snapshot.title} - ${snapshot.artist}"
            })
            views.setImageViewResource(R.id.widget_toggle, if (snapshot.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow)

            val open = openPlayer(context)
            views.setOnClickPendingIntent(R.id.widget_pill, open)

            if (snapshot.hasTrack) {
                views.setInt(R.id.widget_toggle, "setImageAlpha", ALPHA_ENABLED)
                views.setInt(R.id.widget_previous, "setImageAlpha", if (snapshot.hasPrevious) ALPHA_ENABLED else ALPHA_DISABLED)
                views.setInt(R.id.widget_next, "setImageAlpha", if (snapshot.hasNext) ALPHA_ENABLED else ALPHA_DISABLED)
                views.setOnClickPendingIntent(R.id.widget_toggle, MediaWidgetActions.pendingIntent(context, MediaWidgetActions.ACTION_TOGGLE))
                views.setOnClickPendingIntent(R.id.widget_previous, MediaWidgetActions.pendingIntent(context, MediaWidgetActions.ACTION_PREVIOUS))
                views.setOnClickPendingIntent(R.id.widget_next, MediaWidgetActions.pendingIntent(context, MediaWidgetActions.ACTION_NEXT))
            } else {
                for (button in TRANSPORT) {
                    views.setInt(button, "setImageAlpha", ALPHA_DISABLED)
                    views.setOnClickPendingIntent(button, open)
                }
            }
            return views
        }

        private fun openPlayer(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            return PendingIntent.getActivity(context, REQUEST_OPEN_PLAYER, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        private fun AppWidgetManager.push(id: Int, views: RemoteViews) { runCatching { updateAppWidget(id, views) } }

        private val TRANSPORT = intArrayOf(R.id.widget_previous, R.id.widget_toggle, R.id.widget_next)
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        @Volatile private var lastDisc: Bitmap? = null
        private val lastBody = ConcurrentHashMap<Int, Bitmap>()
        private const val FALLBACK_WIDTH_DP = 250
        private const val MAX_WIDTH_PX = 1_600
        private const val RENDER_TIMEOUT_MS = 8_000L
        private const val ALPHA_ENABLED = 255
        private const val ALPHA_DISABLED = 90
        private const val REQUEST_OPEN_PLAYER = 0
    }
}
