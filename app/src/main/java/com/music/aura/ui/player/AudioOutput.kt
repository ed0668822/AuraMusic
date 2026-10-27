package com.music.aura.ui.player

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Detecta las salidas de audio disponibles.
 * Adaptado de BitChord: sin AudioRouting/AudioOutputStatus,
 * lectura directa de AudioManager.
 */
@Composable
internal fun rememberAndroidAudioOutputs(): List<AudioOutputDevice> {
    val context = LocalContext.current
    val manager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    var outputs by remember(manager) { mutableStateOf(readOutputs(manager)) }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(manager, lifecycle) {
        fun refresh() {
            outputs = readOutputs(manager)
        }
        refresh()

        val resumeObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycle.addObserver(resumeObserver)

        val handler = Handler(Looper.getMainLooper())
        fun refreshSoon() {
            refresh()
            SETTLE_MS.forEach { delay -> handler.postDelayed(::refresh, delay) }
        }

        val deviceCallback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>?) = refreshSoon()
            override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>?) = refreshSoon()
        }
        manager.registerAudioDeviceCallback(deviceCallback, handler)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = refreshSoon()
        }
        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(AudioManager.ACTION_HEADSET_PLUG)
            addAction(AudioManager.ACTION_HDMI_AUDIO_PLUG)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }

        onDispose {
            lifecycle.removeObserver(resumeObserver)
            manager.unregisterAudioDeviceCallback(deviceCallback)
            runCatching { context.unregisterReceiver(receiver) }
            handler.removeCallbacksAndMessages(null)
        }
    }

    return outputs
}

private fun readOutputs(manager: AudioManager): List<AudioOutputDevice> {
    val devices = if (Build.VERSION.SDK_INT >= 23) {
        manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
    } else {
        emptyList()
    }

    val result = mutableListOf<AudioOutputDevice>()

    // El altavoz del teléfono siempre está presente.
    result.add(
        AudioOutputDevice(
            id = 0,
            name = "",
            kind = AudioOutputKind.PHONE,
            isActive = true,
        )
    )

    devices.forEach { device ->
        val kind = when (device.type) {
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_USB_HEADSET -> AudioOutputKind.WIRED

            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> AudioOutputKind.BLUETOOTH

            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_ACCESSORY -> AudioOutputKind.USB

            AudioDeviceInfo.TYPE_HDMI,
            AudioDeviceInfo.TYPE_HDMI_ARC,
            AudioDeviceInfo.TYPE_HDMI_EARC -> AudioOutputKind.HDMI

            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> return@forEach

            else -> AudioOutputKind.OTHER
        }

        val form = if (kind == AudioOutputKind.BLUETOOTH) {
            bluetoothFormOf(device.productName?.toString() ?: "")
        } else {
            BluetoothForm.UNKNOWN
        }

        result.add(
            AudioOutputDevice(
                id = device.id,
                name = device.productName?.toString() ?: "",
                kind = kind,
                isActive = false,
                form = form,
            )
        )
    }

    return result
}

private fun bluetoothFormOf(name: String): BluetoothForm {
    val lower = name.lowercase()
    return when {
        lower.contains("buds") || lower.contains("earbuds") || lower.contains("airpods") -> BluetoothForm.BUDS
        lower.contains("headphone") || lower.contains("over-ear") || lower.contains("wh-") -> BluetoothForm.HEADPHONES
        lower.contains("car") || lower.contains("auto") -> BluetoothForm.CAR
        lower.contains("speaker") || lower.contains("soundbar") -> BluetoothForm.SPEAKER
        else -> BluetoothForm.UNKNOWN
    }
}

private val SETTLE_MS = longArrayOf(350L, 1_200L, 2_500L)

/**
 * Abre el selector de salidas, pidiendo permiso Bluetooth la primera vez.
 */
@Composable
internal fun rememberAndroidOutputPicker(onOpen: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val ask = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) onOpen() }
    return {
        if (bluetoothNamesAllowed(context)) onOpen() else ask.launch(BLUETOOTH_CONNECT)
    }
}

private fun bluetoothNamesAllowed(context: Context): Boolean =
    Build.VERSION.SDK_INT < 31 ||
        ContextCompat.checkSelfPermission(context, BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

private const val BLUETOOTH_CONNECT = Manifest.permission.BLUETOOTH_CONNECT
