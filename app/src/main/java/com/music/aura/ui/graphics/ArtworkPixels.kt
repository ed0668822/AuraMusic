package com.music.aura.ui.graphics

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.palette.graphics.Palette
import coil3.Image
import coil3.request.ImageRequest
import coil3.request.allowHardware

fun ImageBitmap.argbPixels(): IntArray {
    val bitmap = asAndroidBitmap()
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    return pixels
}

fun ImageRequest.Builder.forPixelAccess(): ImageRequest.Builder =
    this.allowHardware(false)

fun imageBitmapOf(pixels: IntArray, width: Int, height: Int): ImageBitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap.asImageBitmap()
}

fun Image.toImageBitmap(): ImageBitmap {
    return (this as coil3.BitmapImage).bitmap.asImageBitmap()
}

fun paletteSwatches(bitmap: ImageBitmap, maxSwatches: Int, clearFilters: Boolean): List<Palette.Swatch> {
    val androidBitmap = try {
        bitmap.asAndroidBitmap()
    } catch (e: Exception) {
        return emptyList()
    }
    val builder = Palette.from(androidBitmap).maximumColorCount(maxSwatches * 4)
    if (clearFilters) {
        builder.clearFilters()
    }
    return builder.generate().swatches.take(maxSwatches)
}
