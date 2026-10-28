package com.music.aurelis.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * Barra superior con efecto frosted-glass (blur translúcido).
 *
 * Portada de BitChord con dos mejoras premium:
 * - Haze "regular" (más denso que "ultraThin") que da el look iOS de verdad.
 * - Hairline glass edge de 0.5dp blanco translúcido, igual que la navbar.
 *
 * [scrolled] permite que el subtítulo se desvanezca sutilmente al hacer scroll,
 * dando paso al título solo. Opcional (default false), así las llamadas
 * existentes siguen compilando.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun FrostedTopBar(
    title: String,
    subtitle: String? = null,
    hazeState: HazeState,
    scrolled: Boolean = false,
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val subtitleAlpha by animateFloatAsState(
        targetValue = if (scrolled) 0f else 1f,
        animationSpec = tween(durationMillis = 220),
        label = "topBarSubtitleAlpha",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .hazeChild(
                state = hazeState,
                style = HazeMaterials.regular(MaterialTheme.colorScheme.surface),
            )
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.10f),
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .alpha(subtitleAlpha),
                )
            }
        }

        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Configuración",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
