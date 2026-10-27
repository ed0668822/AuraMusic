package com.music.aura.ui.player

import android.content.Context
import android.media.AudioManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.StateFlow

enum class AudioOutputKind { PHONE, WIRED, BLUETOOTH, USB, HDMI, OTHER }
enum class BluetoothForm { UNKNOWN, BUDS, HEADPHONES, SPEAKER, CAR }

data class AudioOutputDevice(
    val id: Int,
    val name: String,
    val kind: AudioOutputKind,
    val isActive: Boolean,
    val form: BluetoothForm = BluetoothForm.UNKNOWN,
)

interface SystemVolume {
    val level: StateFlow<Float>
    fun set(level: Float)
    fun refresh()
}

@Composable
internal fun rememberAndroidAudioOutputs(): List<AudioOutputDevice> {
    val context = LocalContext.current
    val manager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    var outputs by remember(manager) { mutableStateOf(readOutputs(manager)) }

    DisposableEffect(manager) {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val callback = object : android.media.AudioDeviceCallback() {
            override fun onAudioDevicesAdded(added: Array<out android.media.AudioDeviceInfo>?) {
                outputs = readOutputs(manager)
            }
            override fun onAudioDevicesRemoved(removed: Array<out android.media.AudioDeviceInfo>?) {
                outputs = readOutputs(manager)
            }
        }
        manager.registerAudioDeviceCallback(callback, handler)
        onDispose { manager.unregisterAudioDeviceCallback(callback) }
    }

    return outputs
}

private fun readOutputs(manager: AudioManager): List<AudioOutputDevice> {
    val devices = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
    val result = mutableListOf(
        AudioOutputDevice(0, "", AudioOutputKind.PHONE, isActive = true)
    )
    devices.forEach { device ->
        val kind = when (device.type) {
            android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET,
            android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> AudioOutputKind.WIRED
            android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> AudioOutputKind.BLUETOOTH
            android.media.AudioDeviceInfo.TYPE_USB_DEVICE,
            android.media.AudioDeviceInfo.TYPE_USB_HEADSET -> AudioOutputKind.USB
            android.media.AudioDeviceInfo.TYPE_HDMI -> AudioOutputKind.HDMI
            else -> return@forEach
        }
        result.add(
            AudioOutputDevice(
                id = device.id,
                name = device.productName?.toString() ?: "",
                kind = kind,
                isActive = false,
                form = if (kind == AudioOutputKind.BLUETOOTH) BluetoothForm.HEADPHONES else BluetoothForm.UNKNOWN
            )
        )
    }
    return result
}
