package com.music.aura.ui.player

import android.os.Build
import android.os.SystemClock
import android.view.View
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView

/**
 * Milliseconds on the clock `SystemClock.uptimeMillis` reads on the phone.
 */
internal fun uptimeMillis(): Long = SystemClock.uptimeMillis()

/** Whether `Modifier.blur` actually blurs here, rather than being a no-op. */
internal val renderEffectBlurSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** Holds off the display's idle timeout for as long as [enabled]. */
@Composable
internal fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

/**
 * The app's language, as a BCP 47 tag.
 *
 * Read from the current configuration rather than `AppCompatDelegate` so
 * Aura does not need `androidx.appcompat` yet. When the language picker
 * lands (Fase 12), this will move to `AppCompatDelegate.getApplicationLocales()`.
 */
@Composable
internal fun appLanguageTag(): String =
    LocalConfiguration.current.locales[0].toLanguageTag()

/**
 * Back goes to [onBack] while [enabled], ahead of the sheet the player is drawn
 * in.
 *
 * The BackHandler can't do that on its own. The player is a ModalBottomSheet,
 * and from API 33 the sheet puts its own dismiss straight onto the window's
 * OnBackInvokedDispatcher at PRIORITY_DEFAULT — which is also where the dialog
 * dispatcher that every BackHandler feeds ends up, and at equal priority the
 * platform picks whichever registered last: the sheet's, every time. So on 33+
 * this outranks it with an overlay-priority callback for as long as [enabled]
 * holds, and only that long — otherwise the sheet keeps its own back handling
 * and its predictive-back shrink. Below 33 there is no window dispatcher to
 * outrank and the BackHandler is already the newest callback on the dialog's.
 *
 * Call order is priority order among these: a later call is the one back
 * reaches first while both are enabled.
 */
@Composable
internal fun PlayerBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val view = LocalView.current
        DisposableEffect(view, enabled) {
            val callback = if (enabled) OverlayBack.register(view, onBack) else null
            onDispose { OverlayBack.unregister(view, callback) }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object OverlayBack {
    fun register(view: View, onBack: () -> Unit): Any? {
        val dispatcher = view.findOnBackInvokedDispatcher() ?: return null
        val callback = OnBackInvokedCallback { onBack() }
        dispatcher.registerOnBackInvokedCallback(
            OnBackInvokedDispatcher.PRIORITY_OVERLAY,
            callback,
        )
        return callback
    }

    fun unregister(view: View, callback: Any?) {
        if (callback !is OnBackInvokedCallback) return
        view.findOnBackInvokedDispatcher()?.unregisterOnBackInvokedCallback(callback)
    }
}

/**
 * A touch drag is the only thing that can move this platform's drawer, and a
 * drag is also how the list inside it scrolls — so a finger that runs out of
 * list has to take the drawer with it. See [PlayerDrawer].
 */
internal val drawerFollowsListScroll: Boolean get() = true

/**
 * Draws [block] clipped to the rectangle given, moved down by [dy] pixels.
 */
internal fun DrawScope.clipShiftedDown(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    dy: Float,
    block: DrawScope.() -> Unit,
) {
    clipRect(left = left, top = top, right = right, bottom = bottom) {
        translate(top = dy) { block() }
    }
}
