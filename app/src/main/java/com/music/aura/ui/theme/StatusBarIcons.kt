package com.music.aura.ui.theme

import android.app.Activity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun StatusBarIcons(dark: Boolean) {
    val view = LocalView.current
    val window = (view.context as? Activity)?.window ?: return
    val controller = remember(window) { WindowInsetsControllerCompat(window, view) }
    controller.isAppearanceLightStatusBars = dark
}

@Composable
fun HideStatusBar(hidden: Boolean) {
    val view = LocalView.current
    val window = (view.context as? Activity)?.window ?: return
    val controller = remember(window) { WindowInsetsControllerCompat(window, view) }

    DisposableEffect(hidden) {
        if (hidden) {
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.statusBars())
        }
        onDispose {
            controller.show(WindowInsetsCompat.Type.statusBars())
        }
    }
}

@Composable
fun stableStatusBarInsets(): WindowInsets {
    return WindowInsets.statusBars
}
