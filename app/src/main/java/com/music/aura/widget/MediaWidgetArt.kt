package com.music.aura.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.os.SystemClock
import android.util.LruCache
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal object MediaWidgetArt {

    suspend fun render(
        context: Context,
        artworkUri: String?,
        widthPx: Int,
        heightPx: Int,
        bandPx: Int,
        key: String?,
        cornerRadiusPx: Float,
    ): Bitmap {
        peek(key, widthPx, heightPx, bandPx)?.let { return it }
        val cacheKey = key?.let { cacheKey(it, widthPx, heightPx, bandPx) }

        val cover = if (isCoolingOff(key)) null else loadArtwork(context, artworkUri, maxOf(widthPx, heightPx))
        val failed = cover == null && !artworkUri.isNullOrBlank()
        if (failed && key != null) failures[key] = SystemClock.elapsedRealtime()
        val composed = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(composed)
        if (cover != null) canvas.fillCentreCropped(cover) else canvas.fillPlaceholder()

        val blurRegion = (bandPx * BLUR_REGION_SCALE).coerceIn(1, heightPx)
        canvas.blurBottom(composed, blurRegion, bandPx)
        canvas.scrimBottom(bandPx)

        val rounded = composed.withRoundedCorners(cornerRadiusPx)
        composed.recycle()
        if (!failed) cacheKey?.let { composites.put(it, rounded) }
        return rounded
    }

    fun peek(key: String?, widthPx: Int, heightPx: Int, bandPx: Int): Bitmap? =
        key?.let { composites[cacheKey(it, widthPx, heightPx, bandPx)] }?.takeIf { !it.isRecycled }

    suspend fun circle(context: Context, artworkUri: String?, sizePx: Int, key: String): Bitmap? {
        peekCircle(key, sizePx)?.let { return it }
        if (isCoolingOff(key)) return null
        val cover = loadArtwork(context, artworkUri, sizePx)
        if (cover == null) {
            if (!artworkUri.isNullOrBlank()) failures[key] = SystemClock.elapsedRealtime()
            return null
        }
        val square = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        Canvas(square).fillCentreCropped(cover)
        val round = square.withRoundedCorners(sizePx / 2f)
        square.recycle()
        composites.put(circleKey(key, sizePx), round)
        return round
    }

    fun peekCircle(key: String, sizePx: Int): Bitmap? =
        composites[circleKey(key, sizePx)]?.takeIf { !it.isRecycled }

    private fun circleKey(key: String, sizePx: Int) = "circle|$key|$sizePx"

    suspend fun pill(context: Context, artworkUri: String?, widthPx: Int, heightPx: Int, key: String?): Bitmap {
        key?.let { peekPill(it, widthPx, heightPx) }?.let { return it }
        val cover = if (key == null || isCoolingOff(key)) null else loadArtwork(context, artworkUri, 128)
        val failed = cover == null && !artworkUri.isNullOrBlank()
        if (failed && key != null) failures[key] = SystemClock.elapsedRealtime()

        val composed = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(composed)
        if (cover == null) {
            canvas.fillPlaceholder()
        } else {
            val workW = PILL_WORKING_WIDTH_PX
            val workH = (workW * heightPx / widthPx.coerceAtLeast(1)).coerceAtLeast(4)
            val crop = Bitmap.createBitmap(workW * 4, workH * 4, Bitmap.Config.ARGB_8888)
            Canvas(crop).fillCentreCropped(cover)
            val small = crop.halvedTo(workH.toFloat())
            if (small !== crop) crop.recycle()
            val w = small.width
            val h = small.height
            val pixels = IntArray(w * h)
            small.getPixels(pixels, 0, w, 0, 0, w, h)
            blurInPlace(pixels, IntArray(pixels.size), w, h, (w * PILL_BLUR_FRACTION).roundToInt().coerceAtLeast(1))
            small.setPixels(pixels, 0, w, 0, 0, w, h)
            canvas.drawBitmap(
                small,
                Rect(0, 0, w, h),
                Rect(0, 0, widthPx, heightPx),
                Paint().apply { isFilterBitmap = true },
            )
            small.recycle()
        }
        canvas.drawRect(
            0f, 0f, widthPx.toFloat(), heightPx.toFloat(),
            Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, heightPx.toFloat(),
                    0x40000000, 0x70000000, Shader.TileMode.CLAMP,
                )
            },
        )

        val rounded = composed.withRoundedCorners(heightPx / 2f, PILL_ALPHA)
        composed.recycle()
        if (!failed && key != null) composites.put(pillKey(key, widthPx, heightPx), rounded)
        return rounded
    }

    fun peekPill(key: String, widthPx: Int, heightPx: Int): Bitmap? =
        composites[pillKey(key, widthPx, heightPx)]?.takeIf { !it.isRecycled }

    private fun pillKey(key: String, widthPx: Int, heightPx: Int) = "pill|$key|$widthPx|$heightPx"

    fun clear() {
        composites.evictAll()
        failures.clear()
    }

    private fun isCoolingOff(key: String?): Boolean {
        val failedAt = key?.let { failures[it] } ?: return false
        val since = SystemClock.elapsedRealtime() - failedAt
        if (since in 0 until FAILURE_COOLDOWN_MS) return true
        failures.remove(key)
        return false
    }

    private fun cacheKey(key: String, widthPx: Int, heightPx: Int, bandPx: Int) =
        "$key|$widthPx|$heightPx|$bandPx"

    private suspend fun loadArtwork(context: Context, uriString: String?, sizePx: Int): Bitmap? {
        if (uriString.isNullOrBlank()) return null
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return null
        val request = ImageRequest.Builder(context)
            .data(uri)
            .size(sizePx)
            .allowHardware(false)
            .build()
        val result = runCatching { SingletonImageLoader.get(context).execute(request) }.getOrNull()
        return (result as? SuccessResult)?.image?.toBitmap()
    }

    private fun Canvas.fillCentreCropped(src: Bitmap) {
        val scale = maxOf(width.toFloat() / src.width, height.toFloat() / src.height)
        val sampleW = (width / scale).coerceAtMost(src.width.toFloat())
        val sampleH = (height / scale).coerceAtMost(src.height.toFloat())
        val left = (src.width - sampleW) / 2f
        val top = (src.height - sampleH) / 2f
        drawBitmap(
            src,
            Rect(left.toInt(), top.toInt(), (left + sampleW).toInt(), (top + sampleH).toInt()),
            Rect(0, 0, width, height),
            Paint().apply { isFilterBitmap = true },
        )
    }

    private fun Canvas.fillPlaceholder() {
        drawRect(
            0f, 0f, width.toFloat(), height.toFloat(),
            Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(0xFF2E3446.toInt(), 0xFF1B2130.toInt(), 0xFF07090E.toInt()),
                    floatArrayOf(0f, 0.45f, 1f),
                    Shader.TileMode.CLAMP,
                )
            },
        )
    }

    private fun Canvas.blurBottom(source: Bitmap, regionPx: Int, bandPx: Int) {
        val top = source.height - regionPx
        val region = runCatching {
            Bitmap.createBitmap(source, 0, top, source.width, regionPx)
        }.getOrNull() ?: return

        val floorPx = regionPx * MIN_WORKING_SIGMA / (BLUR_SIGMAS.first() * bandPx)
        val small = region.halvedTo(floorPx)
        val w = small.width
        val h = small.height
        val pixels = IntArray(w * h)
        if (w >= 2 && h >= 2) small.getPixels(pixels, 0, w, 0, 0, w, h)
        if (small !== region) small.recycle()
        if (region !== source) region.recycle()
        if (w < 2 || h < 2) return

        val toWorking = h.toFloat() / regionPx
        val scratch = IntArray(pixels.size)
        val level = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val paint = Paint().apply { isFilterBitmap = true }

        var applied = 0f
        for (index in BLUR_SIGMAS.indices) {
            val target = BLUR_SIGMAS[index] * bandPx * toWorking
            val radius = boxRadiusFor(sqrt((target * target - applied * applied).coerceAtLeast(0f)))
            if (radius >= 1) {
                blurInPlace(pixels, scratch, w, h, radius)
                val step = sigmaOf(radius)
                applied = sqrt(applied * applied + step * step)
            }
            level.setPixels(pixels, 0, w, 0, 0, w, h)

            val soft = BitmapShader(level, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                setLocalMatrix(
                    Matrix().apply {
                        setScale(source.width.toFloat() / w, regionPx.toFloat() / h)
                        postTranslate(0f, top.toFloat())
                    },
                )
            }
            val start = top + STOPS[index] * regionPx
            val end = top + (STOPS[index] + STOP_FEATHER).coerceAtMost(1f) * regionPx
            val mask = LinearGradient(0f, start, 0f, maxOf(end, start + 1f), Color.TRANSPARENT, Color.WHITE, Shader.TileMode.CLAMP)
            paint.shader = ComposeShader(soft, mask, PorterDuff.Mode.DST_IN)
            drawRect(0f, top.toFloat(), source.width.toFloat(), source.height.toFloat(), paint)
        }
        level.recycle()
    }

    private fun Bitmap.halvedTo(target: Float): Bitmap {
        var current = this
        while (current.height / 2 >= target && current.width / 2 >= 2) {
            val next = Bitmap.createScaledBitmap(current, current.width / 2, current.height / 2, true)
            if (current !== this) current.recycle()
            current = next
        }
        return current
    }

    private fun blurInPlace(pixels: IntArray, scratch: IntArray, w: Int, h: Int, radius: Int) {
        val r = radius.coerceAtMost(maxOf(1, minOf(w, h) - 1))
        repeat(BLUR_PASSES) {
            boxPass(pixels, scratch, lines = h, lineStride = w, span = w, step = 1, radius = r)
            boxPass(scratch, pixels, lines = w, lineStride = 1, span = h, step = w, radius = r)
        }
    }

    private fun boxPass(src: IntArray, dst: IntArray, lines: Int, lineStride: Int, span: Int, step: Int, radius: Int) {
        val window = radius * 2 + 1
        for (line in 0 until lines) {
            val base = line * lineStride
            var r = 0; var g = 0; var b = 0
            for (i in -radius..radius) {
                val c = src[base + i.coerceIn(0, span - 1) * step]
                r += (c shr 16) and 0xFF; g += (c shr 8) and 0xFF; b += c and 0xFF
            }
            for (i in 0 until span) {
                dst[base + i * step] = OPAQUE or ((r / window) shl 16) or ((g / window) shl 8) or (b / window)
                val gone = src[base + (i - radius).coerceIn(0, span - 1) * step]
                val come = src[base + (i + radius + 1).coerceIn(0, span - 1) * step]
                r += ((come shr 16) and 0xFF) - ((gone shr 16) and 0xFF)
                g += ((come shr 8) and 0xFF) - ((gone shr 8) and 0xFF)
                b += (come and 0xFF) - (gone and 0xFF)
            }
        }
    }

    private fun sigmaOf(radius: Int): Float = sqrt(BLUR_PASSES * (radius.toFloat() * radius + radius) / 3f)

    private fun boxRadiusFor(sigma: Float): Int =
        ((-1f + sqrt(1f + 12f * sigma * sigma / BLUR_PASSES)) / 2f).roundToInt()

    private fun Canvas.scrimBottom(bandPx: Int) {
        val top = (height - bandPx * SCRIM_SCALE).coerceAtLeast(0f)
        drawRect(0f, top, width.toFloat(), height.toFloat(), Paint().apply {
            shader = LinearGradient(0f, top, 0f, height.toFloat(),
                intArrayOf(Color.TRANSPARENT, 0x40000000, 0xB8000000.toInt()),
                floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
        })
    }

    private fun Bitmap.withRoundedCorners(radiusPx: Float, alpha: Int = 255): Bitmap {
        if (radiusPx <= 0f && alpha == 255) return this
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(out).drawRoundRect(
            RectF(0f, 0f, width.toFloat(), height.toFloat()), radiusPx, radiusPx,
            Paint().apply {
                isAntiAlias = true
                this.alpha = alpha
                shader = BitmapShader(this@withRoundedCorners, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            },
        )
        return out
    }

    private const val BLUR_REGION_SCALE = 2
    private val BLUR_SIGMAS = floatArrayOf(0.035f, 0.070f, 0.110f, 0.155f)
    private const val MIN_WORKING_SIGMA = 1.6f
    private const val BLUR_PASSES = 3
    private const val OPAQUE = 0xFF shl 24
    private val STOPS = floatArrayOf(0.28f, 0.50f, 0.70f, 0.86f)
    private const val STOP_FEATHER = 0.26f
    private const val PILL_ALPHA = 210
    private const val PILL_WORKING_WIDTH_PX = 64
    private const val PILL_BLUR_FRACTION = 0.1f
    private const val SCRIM_SCALE = 1.2f
    private val composites = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }
    private val failures = ConcurrentHashMap<String, Long>()
    private const val FAILURE_COOLDOWN_MS = 30_000L
}
