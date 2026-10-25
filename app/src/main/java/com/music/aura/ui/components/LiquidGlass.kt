package com.music.aura.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * Placeholder de "liquid glass" para Aura Music.
 *
 * La implementación real (backdrop vendored + lens + highlight) se portará
 * en el Módulo 5. Mientras tanto, todas las APIs expuestas son seguras de
 * llamar y caen a MaterialTheme.colorScheme.surface.
 *
 * Nunca hace captura de backdrop ni refracción — cero coste de GPU.
 */

/** Switch maestro. Falso hasta que llegue el Módulo 5. */
val LocalLiquidGlassEnabled = staticCompositionLocalOf { false }

/** ¿La GPU puede hacer el blur real? (RenderEffect: API 31+). */
fun isGlassSupported(sdkInt: Int = Build.VERSION.SDK_INT): Boolean =
    sdkInt >= Build.VERSION_CODES.S

/** Color de contenido sobre una superficie de cristal. */
@Composable
fun glassContentColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) Color.Black else Color.White

/** Color del indicador de selección. */
@Composable
fun glassIndicatorColor(): Color =
    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)

/** Tinte del fondo de la barra. */
@Composable
fun glassBarTint(): Color? = null

/** Edge de cristal reutilizable (hairline de 0.5dp). */
val GLASS_EDGE_WIDTH = 0.5.dp
val GLASS_EDGE_COLOR: Color = Color.White.copy(alpha = 0.10f)

/**
 * Fallback: rellena con surface + border. No hace blur.
 * Cuando Módulo 5 llegue, esta función se reemplaza por la versión con backdrop.
 */
@Composable
fun Modifier.liquidGlass(
    shape: CornerBasedShape,
    surfaceTint: Color? = null,
): Modifier = this
    .clip(shape)
    .background(surfaceTint ?: MaterialTheme.colorScheme.surface, shape)
    .border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, shape)

/** Solo el borde, sin relleno. */
@Composable
fun Modifier.liquidGlassRim(shape: CornerBasedShape): Modifier =
    border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, shape)

/** Variante "ligera": solo tint + borde. */
@Composable
fun Modifier.lightweightLiquidGlass(
    shape: CornerBasedShape,
    fallbackColor: Color,
): Modifier = this
    .clip(shape)
    .background(fallbackColor, shape)
    .border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, shape)
