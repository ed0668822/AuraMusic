package com.music.aura.ui.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.content.pm.PackageManager
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.aura.playback.AudioRouting
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The outputs this phone can play music through, kept current while on screen.
 *
 * Simplified for Aura: no Cast, no Watch. Only local device outputs.
 */
@Composable
internal fun rememberAndroidAudioOutputs(): List<AudioOutputDevice> {
    val context = LocalContext.current
    val manager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val selectedId by AudioRouting.selectedId.collectAsStateWithLifecycle()
    var outputs by remember(manager) { mutableStateOf(AudioRouting.outputs(manager)) }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(manager, selectedId, lifecycle) {
        fun refresh() {
            outputs = AudioRouting.outputs(manager)
        }
        refresh()

        val resumeObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycle.addObserver(resumeObserver)

        val handler = android.os.Handler(android.os.Looper.getMainLooper())
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

    return remember(outputs) {
        outputs.map { it.toOutputDevice() }
    }
}

private fun AudioRouting.Device.toOutputDevice() = AudioOutputDevice(
    id = id,
    name = name,
    kind = when (kind) {
        AudioRouting.Kind.PHONE -> AudioOutputKind.PHONE
        AudioRouting.Kind.WIRED -> AudioOutputKind.WIRED
        AudioRouting.Kind.USB -> AudioOutputKind.USB
        AudioRouting.Kind.BLUETOOTH -> AudioOutputKind.BLUETOOTH
        AudioRouting.Kind.HDMI -> AudioOutputKind.HDMI
        AudioRouting.Kind.OTHER -> AudioOutputKind.OTHER
    },
    isActive = isActive,
    form = when (form) {
        AudioRouting.Form.UNKNOWN -> BluetoothForm.UNKNOWN
        AudioRouting.Form.BUDS -> BluetoothForm.BUDS
        AudioRouting.Form.HEADPHONES -> BluetoothForm.HEADPHONES
        AudioRouting.Form.SPEAKER -> BluetoothForm.SPEAKER
        AudioRouting.Form.CAR -> BluetoothForm.CAR
    },
)

private val SETTLE_MS = longArrayOf(350L, 1_200L, 2_500L)

/**
 * Opens the output picker, asking for the Bluetooth permission the first time.
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

private const val BLUETOOTH_CONNECT = "android.permission.BLUETOOTH_CONNECT"
