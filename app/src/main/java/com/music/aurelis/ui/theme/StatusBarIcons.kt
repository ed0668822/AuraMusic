// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun StatusBarIcons(dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = (LocalContext.current as? Activity)?.window ?: return
    SideEffect {
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = dark
        }
    }
}

@Composable
fun HideStatusBar(hidden: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = (LocalContext.current as? Activity)?.window ?: return
    DisposableEffect(hidden) {
        val controller = WindowCompat.getInsetsController(window, view)
        if (hidden) {
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.statusBars())
        }
        onDispose {
            controller.show(WindowInsetsCompat.Type.statusBars())
        }
    }
}

@Composable
fun HideSystemBars(hidden: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = (LocalContext.current as? Activity)?.window ?: return
    DisposableEffect(hidden) {
        val controller = WindowCompat.getInsetsController(window, view)
        if (hidden) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@Composable
fun stableStatusBarInsets(): WindowInsets {
    return WindowInsets.statusBars
}
