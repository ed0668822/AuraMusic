// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

enum class Haptic {
    Tick,
    Tap,
    Select,
    ToggleOn,
    ToggleOff,
    SkipNext,
    SkipPrevious,
    Resume,
    Pause,
    Expand,
}

class Haptics(private val vibrator: Vibrator?) {

    fun play(haptic: Haptic) {
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = when (haptic) {
                Haptic.Tick -> VibrationEffect.createOneShot(8, 40)
                Haptic.Tap -> VibrationEffect.createOneShot(12, 80)
                Haptic.Select -> VibrationEffect.createOneShot(15, 120)
                Haptic.ToggleOn -> VibrationEffect.createWaveform(longArrayOf(0, 10, 30, 18), intArrayOf(0, 60, 0, 140), -1)
                Haptic.ToggleOff -> VibrationEffect.createWaveform(longArrayOf(0, 18, 30, 10), intArrayOf(0, 140, 0, 60), -1)
                Haptic.SkipNext -> VibrationEffect.createWaveform(longArrayOf(0, 8, 20, 8, 20, 12), intArrayOf(0, 60, 0, 90, 0, 130), -1)
                Haptic.SkipPrevious -> VibrationEffect.createWaveform(longArrayOf(0, 12, 20, 8, 20, 8), intArrayOf(0, 130, 0, 90, 0, 60), -1)
                Haptic.Resume -> VibrationEffect.createWaveform(longArrayOf(0, 25, 15), intArrayOf(0, 80, 160), -1)
                Haptic.Pause -> VibrationEffect.createWaveform(longArrayOf(0, 15, 25), intArrayOf(0, 160, 80), -1)
                Haptic.Expand -> VibrationEffect.createOneShot(20, 100)
            }
            v.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(15)
        }
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val context = LocalContext.current
    return remember(context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        Haptics(vibrator)
    }
}
