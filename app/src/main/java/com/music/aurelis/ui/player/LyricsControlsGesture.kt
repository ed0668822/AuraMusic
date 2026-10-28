package com.music.aurelis.ui.player

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput

private const val TAP_SLOP_FACTOR = 2.5f

@Composable
internal fun Modifier.revealLyricsControlsOnTap(
    enabled: Boolean,
    onReveal: () -> Unit,
): Modifier {
    val currentOnReveal = rememberUpdatedState(onReveal)
    return pointerInput(enabled) {
        if (!enabled) return@pointerInput
        val tapSlop = viewConfiguration.touchSlop * TAP_SLOP_FACTOR
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (down.position.y < size.height / 2f) return@awaitEachGesture
            if (down.type == PointerType.Mouse) {
                var claimed = down.isConsumed
                do {
                    val event = awaitPointerEvent(PointerEventPass.Final)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    claimed = claimed || change.isConsumed
                    if (!change.pressed) {
                        if (!claimed) currentOnReveal.value()
                        break
                    }
                } while (true)
                return@awaitEachGesture
            }
            var dragged = false
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if ((change.position - down.position).getDistance() > tapSlop ||
                    event.changes.size > 1
                ) {
                    dragged = true
                } else if (change.positionChange() != Offset.Zero) {
                    change.consume()
                }
                if (!change.pressed) {
                    if (!dragged) {
                        change.consume()
                        currentOnReveal.value()
                    }
                    break
                }
            } while (true)
        }
    }
}
