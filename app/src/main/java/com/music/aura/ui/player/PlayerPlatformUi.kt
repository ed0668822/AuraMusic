package com.music.aura.ui.player

import android.app.Activity
import android.os.Build
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import java.util.Locale

internal fun uptimeMillis(): Long = SystemClock.uptimeMillis()

internal val renderEffectBlurSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
internal fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    val window = (view.context as? Activity)?.window
    DisposableEffect(enabled, window) {
        if (enabled) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

@Composable
internal fun appLanguageTag(): String =
    Locale.getDefault().toLanguageTag()

@Composable
internal fun PlayerBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}

internal val drawerFollowsListScroll: Boolean = true

internal fun DrawScope.clipShiftedDown(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    dy: Float,
    block: DrawScope.() -> Unit,
) {
    clipRect(left = left, top = top, right = right, bottom = bottom) {
        translate(top = dy) {
            block()
        }
    }
}
