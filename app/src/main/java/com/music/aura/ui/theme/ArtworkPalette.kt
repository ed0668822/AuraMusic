package com.music.aura.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Colores extraídos del artwork de una canción.
 * Se usan para generar el tema dinámico del reproductor.
 */
data class ArtworkColors(
    val primary: Color = Color(0xFFB4A7F5),
    val onPrimary: Color = Color(0xFF1F1147),
    val primaryContainer: Color = Color(0xFF3B2D6B),
    val onPrimaryContainer: Color = Color(0xFFE8DEFF),
    val secondary: Color = Color(0xFF8ECFF5),
    val onSecondary: Color = Color(0xFF00344B),
    val secondaryContainer: Color = Color(0xFF004C6B),
    val onSecondaryContainer: Color = Color(0xFFC8E9FF),
    val background: Color = Color(0xFF0E0E14),
    val onBackground: Color = Color(0xFFE4E1EC),
    val surface: Color = Color(0xFF0E0E14),
    val onSurface: Color = Color(0xFFE4E1EC),
    val surfaceVariant: Color = Color(0xFF2A2833),
    val onSurfaceVariant: Color = Color(0xFFC8C4D4),
)

/**
 * Extrae colores de un artwork usando la librería Palette de AndroidX.
 *
 * Proceso:
 * 1. Carga el bitmap desde la URI content:// (local, sin red).
 * 2. Lo escala a un tamaño manejable para Palette.
 * 3. Genera el Palette y extrae los swatches.
 * 4. Construye ArtworkColors con fallbacks seguros.
 */
object ArtworkPalette {

    /** Tamaño máximo para el bitmap antes de pasarlo a Palette. */
    private const val MAX_DIMENSION = 128

    /**
     * Extrae colores del artwork en [artworkUri].
     * Devuelve null si la URI es nula o no se puede decodificar.
     */
    suspend fun extractColors(
        context: Context,
        artworkUri: String?
    ): ArtworkColors? = withContext(Dispatchers.IO) {
        if (artworkUri.isNullOrBlank()) return@withContext null

        try {
            val uri = Uri.parse(artworkUri)
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext null

            // Decodificar con tamaño reducido para Palette
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            val width = options.outWidth
            val height = options.outHeight
            if (width <= 0 || height <= 0) return@withContext null

            // Calcular inSampleSize para no cargar el bitmap completo
            val sampleSize = calculateSampleSize(width, height, MAX_DIMENSION)

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val inputStream2 = context.contentResolver.openInputStream(uri)
                ?: return@withContext null
            val bitmap = BitmapFactory.decodeStream(inputStream2, null, decodeOptions)
            inputStream2.close()

            if (bitmap == null) return@withContext null

            // Generar Palette
            val palette = Palette.from(bitmap).generate()
            bitmap.recycle()

            buildColorsFromPalette(palette)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Calcula el inSampleSize para reducir el bitmap.
     */
    private fun calculateSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var sampleSize = 1
        if (width > maxDim || height > maxDim) {
            val halfWidth = width / 2
            val halfHeight = height / 2
            while ((halfWidth / sampleSize) >= maxDim &&
                   (halfHeight / sampleSize) >= maxDim) {
                sampleSize *= 2
            }
        }
        return sampleSize
    }

    /**
     * Construye ArtworkColors desde un Palette generado.
     * Usa los swatches más vibrantes y dominantes con fallbacks.
     */
    private fun buildColorsFromPalette(palette: Palette): ArtworkColors {
        // Swatch principal: el más vibrante o dominante
        val dominantSwatch = palette.dominantSwatch
            ?: palette.vibrantSwatch
            ?: palette.mutedSwatch

        val vibrantSwatch = palette.vibrantSwatch
            ?: palette.darkVibrantSwatch
            ?: dominantSwatch

        val mutedSwatch = palette.mutedSwatch
            ?: palette.darkMutedSwatch
            ?: dominantSwatch

        // Colores base extraídos
        val primaryRgb = vibrantSwatch?.rgb ?: 0xFFB4A7F5.toInt()
        val secondaryRgb = mutedSwatch?.rgb ?: 0xFF8ECFF5.toInt()
        val bgRgb = dominantSwatch?.rgb ?: 0xFF0E0E14.toInt()

        // Oscurecer el fondo para que sea usable como background
        val darkBg = darkenColor(bgRgb, 0.15f)
        val darkSurface = darkenColor(bgRgb, 0.20f)
        val surfaceVariant = lightenColor(darkBg, 0.08f)

        // Generar colores complementarios para texto
        val onPrimary = if (isColorDark(primaryRgb)) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        val onSecondary = if (isColorDark(secondaryRgb)) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        val onBackground = if (isColorDark(darkBg)) 0xFFE4E1EC.toInt() else 0xFF1A1B20.toInt()
        val onSurface = onBackground
        val onSurfaceVariant = if (isColorDark(surfaceVariant)) 0xFFC8C4D4.toInt() else 0xFF47454F.toInt()

        // Contenedores: versiones más suaves
        val primaryContainer = blendColors(primaryRgb, darkBg, 0.35f)
        val onPrimaryContainer = if (isColorDark(primaryContainer)) 0xFFE8DEFF.toInt() else 0xFF1F1147.toInt()
        val secondaryContainer = blendColors(secondaryRgb, darkBg, 0.35f)
        val onSecondaryContainer = if (isColorDark(secondaryContainer)) 0xFFC8E9FF.toInt() else 0xFF001E2C.toInt()

        return ArtworkColors(
            primary = Color(primaryRgb),
            onPrimary = Color(onPrimary),
            primaryContainer = Color(primaryContainer),
            onPrimaryContainer = Color(onPrimaryContainer),
            secondary = Color(secondaryRgb),
            onSecondary = Color(onSecondary),
            secondaryContainer = Color(secondaryContainer),
            onSecondaryContainer = Color(onSecondaryContainer),
            background = Color(darkBg),
            onBackground = Color(onBackground),
            surface = Color(darkSurface),
            onSurface = Color(onSurface),
            surfaceVariant = Color(surfaceVariant),
            onSurfaceVariant = Color(onSurfaceVariant),
        )
    }

    /**
     * Oscurece un color multiplicando sus canales RGB por [factor].
     */
    private fun darkenColor(color: Int, factor: Float): Int {
        val a = (color shr 24) and 0xFF
        val r = ((color shr 16) and 0xFF) * factor
        val g = ((color shr 8) and 0xFF) * factor
        val b = (color and 0xFF) * factor
        return (a shl 24) or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
    }

    /**
     * Aclara un color mezclándolo con blanco.
     */
    private fun lightenColor(color: Int, factor: Float): Int {
        val a = (color shr 24) and 0xFF
        val r = ((color shr 16) and 0xFF) + ((255 - ((color shr 16) and 0xFF)) * factor).toInt()
        val g = ((color shr 8) and 0xFF) + ((255 - ((color shr 8) and 0xFF)) * factor).toInt()
        val b = (color and 0xFF) + ((255 - (color and 0xFF)) * factor).toInt()
        return (a shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)
    }

    /**
     * Mezcla dos colores con un ratio.
     */
    private fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
        val invRatio = 1f - ratio
        val a = ((color1 shr 24) and 0xFF) * ratio + ((color2 shr 24) and 0xFF) * invRatio
        val r = ((color1 shr 16) and 0xFF) * ratio + ((color2 shr 16) and 0xFF) * invRatio
        val g = ((color1 shr 8) and 0xFF) * ratio + ((color2 shr 8) and 0xFF) * invRatio
        val b = (color1 and 0xFF) * ratio + (color2 and 0xFF) * invRatio
        return (a.toInt() shl 24) or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
    }

    /**
     * Determina si un color es oscuro (para elegir texto claro u oscuro encima).
     */
    private fun isColorDark(color: Int): Boolean {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        // Fórmula de luminancia percibida
        val luminance = 0.299 * r + 0.587 * g + 0.114 * b
        return luminance < 128
    }
}
