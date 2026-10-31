// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Stable
interface PlayerFrost {
    @Composable
    fun Modifier.frostSource(): Modifier

    fun capture()

    fun release()

    fun Modifier.frostPanel(shape: Shape, fill: Color): Modifier
}

val LocalPlayerFrost = staticCompositionLocalOf<PlayerFrost?> { null }

@Composable
internal fun FrostedPlayerFrame(
    modifier: Modifier,
    tint: Color,
    frost: PlayerFrost?,
    live: Boolean,
    overlayOpen: () -> Boolean,
    overlays: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    if (frost != null && !live) {
        val open by rememberUpdatedState(overlayOpen)
        LaunchedEffect(frost) {
            snapshotFlow { open() }.collectLatest { up ->
                if (up) {
                    frost.capture()
                } else {
                    delay(FROST_RELEASE_DELAY_MS)
                    frost.release()
                }
            }
        }
    }
    CompositionLocalProvider(LocalPlayerTint provides tint) {
        Box(modifier.fillMaxSize()) {
            val source = if (frost != null) with(frost) { Modifier.frostSource() } else Modifier
            Box(Modifier.fillMaxSize().then(source)) {
                content()
            }
            CompositionLocalProvider(LocalPlayerFrost provides frost?.takeIf { !live }) {
                overlays()
            }
        }
    }
}

private const val FROST_RELEASE_DELAY_MS = 400L
