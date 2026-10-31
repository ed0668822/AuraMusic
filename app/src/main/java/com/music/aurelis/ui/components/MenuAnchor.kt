// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced

object MenuAnchor {
    private var bounds: Rect? = null
    private var recordedAt = 0L

    internal fun record(bounds: Rect) {
        this.bounds = bounds
        recordedAt = System.nanoTime()
    }

    fun consume(): Rect? {
        val anchor = bounds?.takeIf { System.nanoTime() - recordedAt < FRESH_NANOS }
        bounds = null
        return anchor
    }

    private const val FRESH_NANOS = 3_000_000_000L
}

@Composable
fun Modifier.menuAnchor(): Modifier {
    val anchor = remember { AnchorCoordinates() }
    return this
        .onPlaced { anchor.coordinates = it }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                anchor.coordinates?.takeIf { it.isAttached }?.let { MenuAnchor.record(it.boundsInWindow()) }
            }
        }
}

private class AnchorCoordinates {
    var coordinates: LayoutCoordinates? = null
}

@Composable
fun Modifier.leftOutOfHeldPicture(): Modifier {
    val position = remember { HeldPosition() }
    return this
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            val capturing = LongPressOrigin.capturing
            if (capturing != null && position.center?.let(capturing::contains) == true) {
                layout(0, placeable.height) {}
            } else {
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
        }
        .onPlaced { position.center = it.boundsInWindow().center }
}

private class HeldPosition {
    var center: Offset? = null
}
