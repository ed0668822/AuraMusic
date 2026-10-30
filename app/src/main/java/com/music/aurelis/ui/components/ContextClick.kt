// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Hace que un Composable responda a toque corto ([onClick]) y pulsación
 * larga ([onLongClick]). Si la larga es `null` se degrada a un click normal.
 *
 * [liftPadding] existe para dejar aire alrededor del gesto en filas cuyo
 * contenido está desplazado por un `Modifier.padding` exterior; hoy no se
 * usa, pero mantenerlo en la firma evita reescribir llamadas existentes.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.longPressMenuClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    liftPadding: PaddingValues = PaddingValues(),
): Modifier = composed {
    if (onLongClick != null) {
        this.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        )
    } else {
        this.clickable(onClick = onClick)
    }
}

/**
 * Envuelve un segundo gesto de pulsación larga cuando el consumidor ya
 * escucha el `onLongPress` de otro modo (por ejemplo, un `SwipeToDismissBox`
 * que también reacciona al toque largo). No previene el `onClick` normal.
 */
fun Modifier.contextClick(onClick: (() -> Unit)?): Modifier = composed {
    if (onClick == null) return@composed this
    this.pointerInput(Unit) {
        detectTapGestures(
            onLongPress = { onClick() },
        )
    }
}
