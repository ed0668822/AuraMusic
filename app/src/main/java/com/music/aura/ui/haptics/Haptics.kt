package com.music.aura.ui.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
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
                Haptic.Tick -> VibrationEffect.createOneShot(10, 30)
                Haptic.Tap -> VibrationEffect.createOneShot(20, 60)
                Haptic.Select -> VibrationEffect.createOneShot(25, 80)
                Haptic.ToggleOn -> VibrationEffect.createWaveform(longArrayOf(0, 15, 30, 35), -1)
                Haptic.ToggleOff -> VibrationEffect.createWaveform(longArrayOf(0, 35, 30, 15), -1)
                Haptic.SkipNext -> VibrationEffect.createWaveform(longArrayOf(0, 12, 20, 12, 20, 30), -1)
                Haptic.SkipPrevious -> VibrationEffect.createWaveform(longArrayOf(0, 30, 20, 12, 20, 12), -1)
                Haptic.Resume -> VibrationEffect.createWaveform(longArrayOf(0, 20, 30, 40), -1)
                Haptic.Pause -> VibrationEffect.createWaveform(longArrayOf(0, 40, 30, 20), -1)
                Haptic.Expand -> VibrationEffect.createOneShot(35, 100)
            }
            v.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(20)
        }
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val context = LocalContext.current
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    return Haptics(vibrator)
}
