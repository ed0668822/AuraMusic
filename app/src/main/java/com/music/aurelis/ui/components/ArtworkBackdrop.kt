package com.music.aurelis.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.ui.theme.ArtworkPalette
import android.net.Uri

/**
 * Fondo de superficie tintado con el artwork: un tinte plano en toda la
 * superficie, con el artwork difuminado en la parte superior y disuelto
 * hacia ese tinte.
 *
 * Adaptado para Aurelis Music: carga artwork local (content:// URI) en vez
 * de URLs remotas. El blur es idéntico al de BitChord.
 *
 * Dimensionado completamente por [modifier] — dentro de un padre
 * wrap-content, pasar `Modifier.matchParentSize()` para que el wash
 * siga al contenido.
 */
@Composable
fun ArtworkBackdrop(
    palette: ArtworkPalette,
    artworkUri: Uri?,
    modifier: Modifier = Modifier,
    /** Hasta dónde llega el artwork difuminado en la superficie. */
    washFraction: Float = 0.72f,
    reduceDynamicBlur: Boolean = false,
) {
    val canBlur = !reduceDynamicBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(modifier.background(palette.background)) {
        if (canBlur && artworkUri != null) {
            AsyncImage(
                model = artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(washFraction)
                    .graphicsLayer {
                        scaleX = 1.4f
                        scaleY = 1.4f
                    }
                    .blur(72.dp, BlurredEdgeTreatment.Unbounded),
            )
        }
        val isDark = androidx.compose.foundation.isSystemInDarkTheme()
        val alpha0 = if (isDark) 0.40f else 0.08f
        val alpha1 = if (isDark) 0.55f else 0.25f
        val alpha2 = if (isDark) 0.85f else 0.72f

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to palette.background.copy(alpha = alpha0),
                        0.20f to palette.background.copy(alpha = alpha1),
                        0.55f to palette.background.copy(alpha = alpha2),
                        washFraction to palette.background,
                    ),
                ),
        )
    }
}
