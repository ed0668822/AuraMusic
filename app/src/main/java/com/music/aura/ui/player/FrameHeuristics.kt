package com.music.aura.ui.player

import androidx.compose.ui.graphics.ImageBitmap

internal fun isLikelyBlackFrame(bitmap: ImageBitmap): Boolean {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= 0 || h <= 0) return true
    val stride = maxOf(1, minOf(w, h) / 32)
    var sumLum = 0L
    var count = 0
    val row = IntArray(w)
    for (y in 0 until h step stride) {
        bitmap.readPixels(row, startX = 0, startY = y, width = w, height = 1)
        for (x in 0 until w step stride) {
            val p = row[x]
            sumLum += ((p shr 16 and 0xFF) * 30 +
                (p shr 8 and 0xFF) * 59 +
                (p and 0xFF) * 11) / 100
            count++
        }
    }
    val mean = sumLum / count
    return mean < 12
}
