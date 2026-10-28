package com.music.aurelis.ui.player

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize

/**
 * The mini player the expanded player docks into, for a host that has one.
 *
 * Opening and closing the player is one movement on screen: the sheet's
 * artwork travels between its own place and the mini player's cover while the
 * rest of the player fades — see the sleeve in NowPlayingScreen. The two are
 * in different windows on a phone (the player is a modal sheet over the page),
 * so nothing here is shared layout; it is the two ends of the movement, each
 * reported by the side that owns it, and met in screen coordinates.
 *
 * Read in layout and draw only. Nothing in it is worth a recomposition.
 */
@Stable
class PlayerDock {
    private var miniArt: LayoutCoordinates? = null
    private var miniArtCorner: Dp = 8.dp

    var sheetOffset: (() -> Float)? by mutableStateOf(null)

    var sheetTravel by mutableFloatStateOf(0f)

    var attached by mutableStateOf(false)
        internal set

    fun reportMiniArt(coordinates: LayoutCoordinates, corner: Dp) {
        miniArt = coordinates
        miniArtCorner = corner
    }

    fun releaseMiniArt(coordinates: LayoutCoordinates) {
        if (miniArt === coordinates) miniArt = null
    }

    internal fun hasMiniArt(): Boolean = miniArt?.isAttached == true

    internal fun miniArtOnScreen(): Rect? {
        val coordinates = miniArt?.takeIf { it.isAttached } ?: return null
        val size = coordinates.size.toSize()
        return Rect(
            coordinates.localToScreen(Offset.Zero),
            coordinates.localToScreen(Offset(size.width, size.height)),
        )
    }

    internal fun miniArtCorner(): Dp = miniArtCorner

    internal fun offset(): Float? = sheetOffset?.invoke()?.takeUnless { it.isNaN() }

    fun openFraction(): Float {
        val travel = sheetTravel
        val offset = offset() ?: return 0f
        if (travel <= 0f) return 0f
        return (1f - offset / travel).coerceIn(0f, 1f)
    }

    val coversMiniArt: Boolean
        get() = coversMiniArtState.value

    private val coversMiniArtState = derivedStateOf { attached && openFraction() > 0f }
}

/** The [PlayerDock] around the player, if its host has a mini player to dock into. */
val LocalPlayerDock = staticCompositionLocalOf<PlayerDock?> { null }
