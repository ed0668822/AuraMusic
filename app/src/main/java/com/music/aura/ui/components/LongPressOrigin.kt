package com.music.aura.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalGraphicsContext
import kotlinx.coroutines.launch

@Stable
class HeldItem internal constructor(
    val bounds: Rect,
) {
    var image: ImageBitmap? by mutableStateOf(null)
        internal set
}

object LongPressOrigin {
    private var held: HeldItem? = null
    private var recordedAt = 0L

    internal fun record(item: HeldItem) {
        held = item
        recordedAt = System.nanoTime()
    }

    fun consume(): HeldItem? {
        val item = held?.takeIf { System.nanoTime() - recordedAt < FRESH_NANOS }
        held = null
        return item
    }

    private const val FRESH_NANOS = 500_000_000L
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.longPressMenuClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    enabled: Boolean = true,
): Modifier {
    val capture = remember { HoldCapture() }
    val graphicsContext = LocalGraphicsContext.current
    val scope = rememberCoroutineScope()
    val longClick = onLongClick?.let { held ->
        {
            capture.coordinates?.takeIf { it.isAttached }?.let { coordinates ->
                val item = HeldItem(coordinates.boundsInWindow())
                LongPressOrigin.record(item)
                capture.pending = item
                capture.requests++
            }
            held()
        }
    }
    return this
        .onPlaced { capture.coordinates = it }
        .combinedClickable(enabled = enabled, onClick = onClick, onLongClick = longClick)
        .drawWithContent {
            drawContent()
            val request = capture.requests
            if (request == capture.handled) return@drawWithContent
            capture.handled = request
            val item = capture.pending ?: return@drawWithContent
            capture.pending = null
            val layer = graphicsContext.createGraphicsLayer()
            layer.record { this@drawWithContent.drawContent() }
            scope.launch {
                try {
                    item.image = layer.toImageBitmap()
                } finally {
                    graphicsContext.releaseGraphicsLayer(layer)
                }
            }
        }
}

private class HoldCapture {
    var coordinates: LayoutCoordinates? = null
    var pending: HeldItem? = null
    var requests by mutableIntStateOf(0)
    var handled = 0
}


