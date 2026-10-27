package com.music.aura.ui.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.music.aura.ui.replay.Fonts
import com.music.aura.ui.replay.drawArtwork
import com.music.aura.ui.replay.drawBackdrop
import com.music.aura.ui.replay.drawLogo
import com.music.aura.ui.replay.ellipsised
import com.music.aura.ui.replay.loadBitmap
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One line of the share card.
 */
data class LyricsShareLine(
    val text: String,
    val subText: String? = null,
    val isGap: Boolean = false,
)

/**
 * The request to render a lyrics share card.
 */
data class LyricsShareRequest(
    val song: LyricsShareSong,
    val lines: List<LyricsShareLine>,
    val artworkUri: String?,
)

/**
 * Minimal song info for the share card.
 */
data class LyricsShareSong(
    val title: String,
    val artist: String,
)

/**
 * The picked lines as one picture, drawn to a bitmap you can send.
 */
internal suspend fun renderLyricsShareCard(
    context: Context,
    card: LyricsShareRequest,
    story: Boolean = false,
): Bitmap = withContext(Dispatchers.Default) {
    val type = Fonts(context)
    val cover = card.artworkUri?.let { loadBitmap(context, it) }

    val headTop = HEADER_BOTTOM + GAP_ABOVE_BODY
    val frameRoom = MAX_CARD_H - FOOTER_THUMB - FOOTER_PAD - GAP_BELOW_BODY - headTop
    val plan = fittestPlan(type, card.lines, frameRoom)
    val spare = if (story) (frameRoom - plan.content).coerceAtLeast(0f) else 0f
    val bodyTop = headTop + spare / 2
    val bodyBottom = bodyTop + plan.content
    val cardH = bodyBottom + spare / 2 + GAP_BELOW_BODY + FOOTER_THUMB + FOOTER_PAD

    val bitmap = Bitmap.createBitmap(CARD_W, cardH.roundToInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawBackdrop(canvas, cover, 0f, cardH)

    val brand = type.heading(46f, 0xE6FFFFFF.toInt()).apply {
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText(BRAND, CARD_W - CARD_MARGIN, HEADER_BASELINE, brand)
    drawLogo(
        canvas,
        context,
        CARD_W - CARD_MARGIN - brand.measureText(BRAND) - LOGO_GAP,
        HEADER_BASELINE,
    )

    var y = bodyTop
    for (row in plan.rows) {
        val baseline = y + row.gapBefore - row.paint.ascent() +
            if (row.isGap) row.paint.textSize * GAP_MARKER_OFFSET else 0f
        val x = if (row.centered) CARD_W / 2f else CARD_MARGIN
        canvas.drawText(row.text, x, baseline, row.paint)
        y += row.gapBefore + row.height
    }

    val thumbY = cardH - FOOTER_PAD - FOOTER_THUMB
    drawArtwork(canvas, cover, card.song.title, CARD_MARGIN, thumbY, FOOTER_THUMB, false)
    val textX = CARD_MARGIN + FOOTER_THUMB + 34f
    val room = CARD_W - CARD_MARGIN - textX
    val title = type.body(50f, Color.WHITE, bold = true)
    canvas.drawText(ellipsised(card.song.title, title, room), textX, thumbY + 74f, title)
    val artist = type.body(42f, 0x99FFFFFF.toInt())
    canvas.drawText(ellipsised(card.song.artist, artist, room), textX, thumbY + 132f, artist)

    bitmap
}

private class Row(
    val text: String,
    val paint: Paint,
    val height: Float,
    val gapBefore: Float,
    val centered: Boolean = false,
    val isGap: Boolean = false,
)

private class Plan(val rows: List<Row>, val ascent: Float) {
    val content: Float
        get() = ascent + rows.fold(0f) { total, row -> total + row.gapBefore + row.height }
}

private fun fittestPlan(
    type: Fonts,
    lines: List<LyricsShareLine>,
    room: Float,
): Plan {
    for (size in FIT_LADDER) {
        val plan = planAt(type, lines, size)
        if (plan.content <= room) return plan
    }
    return planAt(type, lines, FIT_LADDER.last())
}

private fun planAt(type: Fonts, lines: List<LyricsShareLine>, size: Float): Plan {
    val primary = type.heading(size, Color.WHITE).apply { letterSpacing = TRACKING }
    val subSize = (size * SUB_RATIO).coerceAtLeast(SUB_FLOOR)
    val secondary = type.heading(subSize, SUB_COLOR).apply { letterSpacing = SUB_TRACKING }

    val rows = mutableListOf<Row>()
    lines.forEachIndexed { index, line ->
        if (line.isGap) {
            val marker = type.body(size * 0.65f, 0x66FFFFFF.toInt())
            val markerHeight = marker.descent() - marker.ascent()
            rows += Row("(...)", marker, markerHeight, 0f, isGap = true)
            return@forEachIndexed
        }
        var firstOfLine = true
        fun add(text: String, paint: Paint, height: Float) {
            val gap = when {
                rows.isEmpty() -> 0f
                firstOfLine && index > 0 -> size * 0.55f
                else -> 0f
            }
            rows += Row(text, paint, height, gap)
            firstOfLine = false
        }
        wrap(line.text, primary, CARD_CONTENT_W).forEach { add(it, primary, size * LEAD) }
        val sub = line.subText
        if (!sub.isNullOrBlank() && sub != line.text) {
            wrap(sub, secondary, CARD_CONTENT_W).forEach { add(it, secondary, subSize * SUB_LEAD) }
        }
    }
    return Plan(rows, -primary.ascent())
}

private fun wrap(text: String, paint: Paint, width: Float): List<String> {
    val source = text.trim()
    if (source.isEmpty()) return listOf("")

    val out = mutableListOf<String>()
    var rest = source
    while (rest.isNotEmpty()) {
        val fit = paint.breakText(rest, true, width, null)
        if (fit <= 0) {
            out += rest
            break
        }
        val space = rest.lastIndexOf(' ', fit - 1)
        val take = if (space > 0 && fit < rest.length) space else fit
        if (take <= 0) {
            out += rest
            break
        }
        out += rest.substring(0, take).trimEnd()
        rest = rest.substring(take).trimStart()
    }
    return out.filter { it.isNotEmpty() }.ifEmpty { listOf("") }
}

private const val CARD_W = 1080
private const val MAX_CARD_H = 1920f
private const val CARD_MARGIN = 72f
private const val CARD_CONTENT_W = CARD_W - CARD_MARGIN * 2
private const val LOGO_GAP = 20f
private const val BRAND = "Aura"
private const val HEADER_BASELINE = 132f
private const val HEADER_BOTTOM = 170f
private const val GAP_ABOVE_BODY = 20f
private const val GAP_BELOW_BODY = 20f
private const val FOOTER_THUMB = 160f
private const val FOOTER_PAD = 96f
private const val SUB_FLOOR = 24f
private const val GAP_MARKER_OFFSET = 0.18f
private val FIT_LADDER = floatArrayOf(96f, 88f, 80f, 72f, 64f, 56f, 50f, 44f, 38f, 32f)
private const val TRACKING = -0.7f / 34f
private const val SUB_TRACKING = -0.7f / 20f
private const val LEAD = 41f / 34f
private const val SUB_LEAD = 25f / 20f
private const val SUB_RATIO = 20f / 34f
private val SUB_COLOR = 0xD9FFFFFF.toInt()
