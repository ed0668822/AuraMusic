package com.music.aurelis.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.music.aurelis.MainActivity
import com.music.aurelis.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

abstract class MediaWidget : AppWidgetProvider() {

    protected abstract val fallbackWidthDp: Int

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        renderAsync(context, ids)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        id: Int,
        newOptions: Bundle?,
    ) {
        renderAsync(context, intArrayOf(id))
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        for (id in ids) lastArt.remove(id)
    }

    override fun onDisabled(context: Context) {
        MediaWidgetArt.clear()
        lastArt.clear()
    }

    private fun renderAsync(context: Context, ids: IntArray) {
        val pending = goAsync()
        val app = context.applicationContext
        val fallback = fallbackWidthDp
        scope.launch {
            try {
                render(app, ids, fallback, SystemClock.elapsedRealtime() + BROADCAST_BUDGET_MS)
            } finally {
                runCatching { pending.finish() }
            }
        }
    }

    companion object {
        const val WIDE_LAYOUT_MIN_DP = 215

        fun refresh(context: Context) {
            val app = context.applicationContext
            scope.launch {
                val manager = runCatching { AppWidgetManager.getInstance(app) }.getOrNull() ?: return@launch
                for ((provider, fallbackWidthDp) in PROVIDERS) {
                    val ids = runCatching {
                        manager.getAppWidgetIds(ComponentName(app, provider))
                    }.getOrNull()
                    if (ids == null || ids.isEmpty()) continue
                    render(app, ids, fallbackWidthDp, deadline = null)
                }
            }
        }

        private suspend fun render(
            context: Context,
            ids: IntArray,
            fallbackWidthDp: Int,
            deadline: Long?,
        ) {
            val manager = runCatching { AppWidgetManager.getInstance(context) }.getOrNull() ?: return
            val snapshot = MediaWidgetSnapshot.load(context)
            val key = snapshot.artworkKey ?: KEY_NO_ARTWORK
            for (id in ids) {
                val budget = when (deadline) {
                    null -> RENDER_TIMEOUT_MS
                    else -> minOf(RENDER_TIMEOUT_MS, deadline - SystemClock.elapsedRealtime())
                }
                if (budget <= 0L) return
                withTimeoutOrNull(budget) { render(context, manager, id, snapshot, key, fallbackWidthDp) }
            }
        }

        private suspend fun render(
            context: Context,
            manager: AppWidgetManager,
            id: Int,
            snapshot: MediaWidgetSnapshot,
            key: String,
            fallbackWidthDp: Int,
        ) {
            val size = measure(context, manager, id, fallbackWidthDp)
            val cached = MediaWidgetArt.peek(key, size.widthPx, size.heightPx, size.bandPx)
            if (cached == null) {
                manager.push(id, views(context, snapshot, size, lastArt[id]))
            }
            val art = cached ?: MediaWidgetArt.render(
                context = context,
                artworkUri = snapshot.artworkUri,
                widthPx = size.widthPx,
                heightPx = size.heightPx,
                bandPx = size.bandPx,
                key = key,
                cornerRadiusPx = size.cornerRadiusPx,
            )
            lastArt[id] = art
            manager.push(id, views(context, snapshot, size, art))
        }

        private fun views(
            context: Context,
            snapshot: MediaWidgetSnapshot,
            size: WidgetSize,
            art: Bitmap?,
        ): RemoteViews {
            val layout =
                if (size.wide) R.layout.widget_media_wide else R.layout.widget_media_compact
            val views = RemoteViews(context.packageName, layout)
            art?.let { views.setImageViewBitmap(R.id.widget_art, it) }

            views.setTextViewText(
                R.id.widget_title,
                if (snapshot.hasTrack) {
                    snapshot.title
                } else {
                    context.getString(R.string.widget_nothing_played)
                },
            )
            if (size.wide) {
                views.setTextViewText(R.id.widget_artist, snapshot.artist)
                views.setViewVisibility(
                    R.id.widget_artist,
                    if (snapshot.artist.isBlank()) View.GONE else View.VISIBLE,
                )
            }

            views.setImageViewResource(
                R.id.widget_toggle,
                if (snapshot.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow,
            )

            val open = openPlayer(context)
            views.setOnClickPendingIntent(R.id.widget_root, open)

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
            return PendingIntent.getActivity(
                context,
                REQUEST_OPEN_PLAYER,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun AppWidgetManager.push(id: Int, views: RemoteViews) {
            runCatching { updateAppWidget(id, views) }
        }

        private class WidgetSize(
            val wide: Boolean,
            val widthPx: Int,
            val heightPx: Int,
            val bandPx: Int,
            val cornerRadiusPx: Float,
        )

        private fun measure(
            context: Context,
            manager: AppWidgetManager,
            id: Int,
            fallbackWidthDp: Int,
        ): WidgetSize {
            val options = runCatching { manager.getAppWidgetOptions(id) }.getOrNull()
            val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val widthDp = options?.getInt(
                if (landscape) AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
            )?.takeIf { it > 0 } ?: fallbackWidthDp
            val heightDp = options?.getInt(
                if (landscape) AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
            )?.takeIf { it > 0 } ?: FALLBACK_HEIGHT_DP

            val wide = widthDp >= WIDE_LAYOUT_MIN_DP
            val resources = context.resources
            val density = resources.displayMetrics.density
            val rawWidth = widthDp * density
            val rawHeight = heightDp * density
            val longest = maxOf(rawWidth, rawHeight)
            val scale = if (longest > MAX_BITMAP_PX) MAX_BITMAP_PX / longest else 1f
            val band = resources.getDimensionPixelSize(
                if (wide) R.dimen.widget_band_wide else R.dimen.widget_band_compact,
            )
            return WidgetSize(
                wide = wide,
                widthPx = (rawWidth * scale).roundToInt().coerceAtLeast(1),
                heightPx = (rawHeight * scale).roundToInt().coerceAtLeast(1),
                bandPx = (band * scale).roundToInt().coerceAtLeast(1),
                cornerRadiusPx = resources.getDimension(R.dimen.widget_corner_radius) * scale,
            )
        }

        private val PROVIDERS = listOf(
            MediaWidgetSquare::class.java to SQUARE_WIDTH_DP,
            MediaWidgetWide::class.java to WIDE_WIDTH_DP,
        )

        private val TRANSPORT = intArrayOf(R.id.widget_previous, R.id.widget_toggle, R.id.widget_next)

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        private const val RENDER_TIMEOUT_MS = 8_000L
        private const val BROADCAST_BUDGET_MS = 8_000L

        private val lastArt = ConcurrentHashMap<Int, Bitmap>()

        private const val MAX_BITMAP_PX = 1_200f
        private const val ALPHA_ENABLED = 255
        private const val ALPHA_DISABLED = 90
        private const val KEY_NO_ARTWORK = "no-artwork"
        private const val REQUEST_OPEN_PLAYER = 0
    }
}

private const val SQUARE_WIDTH_DP = 110
private const val WIDE_WIDTH_DP = 250
private const val FALLBACK_HEIGHT_DP = 110

class MediaWidgetSquare : MediaWidget() {
    override val fallbackWidthDp = SQUARE_WIDTH_DP
}

class MediaWidgetWide : MediaWidget() {
    override val fallbackWidthDp = WIDE_WIDTH_DP
}
