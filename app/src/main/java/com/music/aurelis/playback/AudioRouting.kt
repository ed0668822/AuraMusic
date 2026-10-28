package com.music.aurelis.playback

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AudioRouting {

    enum class Kind { PHONE, WIRED, USB, BLUETOOTH, HDMI, OTHER }

    data class Device(
        val id: Int,
        val type: Int,
        val name: String,
        val kind: Kind,
        val isActive: Boolean = false,
        val form: Form = Form.UNKNOWN,
    )

    enum class Form { UNKNOWN, BUDS, HEADPHONES, SPEAKER, CAR }

    private val _selectedId = MutableStateFlow<Int?>(null)
    val selectedId: StateFlow<Int?> = _selectedId.asStateFlow()

    fun select(id: Int?) {
        _selectedId.value = id
    }

    fun forget() {
        _selectedId.value = null
    }

    fun infoFor(manager: AudioManager, id: Int?): AudioDeviceInfo? {
        if (id == null) return null
        return runCatching { manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) }
            .getOrNull()
            ?.firstOrNull { it.id == id }
    }

    fun outputs(manager: AudioManager): List<Device> {
        val infos = runCatching { manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) }
            .getOrNull()
            ?: return emptyList()
        val active = activeOf(infos, _selectedId.value)
        return infos
            .mapNotNull { info -> kindOf(info.type)?.let { info to it } }
            .map { (info, kind) ->
                val name = nameOf(info, kind)
                Device(
                    id = info.id,
                    type = info.type,
                    name = name,
                    kind = kind,
                    isActive = info.id == active?.id,
                    form = if (kind == Kind.BLUETOOTH) formOf(info, name) else Form.UNKNOWN,
                )
            }
            .groupBy { it.name to it.kind }
            .map { (_, sameDevice) -> sameDevice.minByOrNull { preferenceWithin(it.type) }!! }
            .sortedBy { it.kind.ordinal }
    }

    fun activeOf(infos: Array<AudioDeviceInfo>, chosenId: Int?): AudioDeviceInfo? {
        chosenId?.let { id -> infos.firstOrNull { it.id == id } }?.let { return it }
        return systemDefault(infos)
    }

    private fun preferenceWithin(type: Int): Int = when (type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> 0
        AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER -> 1
        else -> 2
    }

    private fun systemDefault(infos: Array<AudioDeviceInfo>): AudioDeviceInfo? =
        infos.firstOrNull { kindOf(it.type) == Kind.BLUETOOTH }
            ?: infos.firstOrNull { kindOf(it.type) == Kind.WIRED }
            ?: infos.firstOrNull { kindOf(it.type) == Kind.USB }
            ?: infos.firstOrNull { kindOf(it.type) == Kind.HDMI }
            ?: infos.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }

    internal fun kindOf(type: Int): Kind? = when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> Kind.PHONE
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_LINE_ANALOG,
        AudioDeviceInfo.TYPE_AUX_LINE,
        -> Kind.WIRED
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_USB_ACCESSORY,
        -> Kind.USB
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_BLE_SPEAKER,
        AudioDeviceInfo.TYPE_BLE_BROADCAST,
        AudioDeviceInfo.TYPE_HEARING_AID,
        -> Kind.BLUETOOTH
        AudioDeviceInfo.TYPE_HDMI,
        AudioDeviceInfo.TYPE_HDMI_ARC,
        -> Kind.HDMI
        else -> null
    }

    private fun formOf(info: AudioDeviceInfo, name: String): Form {
        when (info.type) {
            AudioDeviceInfo.TYPE_BLE_SPEAKER -> return Form.SPEAKER
            AudioDeviceInfo.TYPE_HEARING_AID -> return Form.BUDS
        }
        return formFromName(name) ?: formFromClass(info) ?: Form.UNKNOWN
    }

    @Suppress("DEPRECATION", "MissingPermission")
    private fun formFromClass(info: AudioDeviceInfo): Form? {
        if (Build.VERSION.SDK_INT < 28) return null
        val address = info.address.takeIf { BluetoothAdapter.checkBluetoothAddress(it) } ?: return null
        val deviceClass = runCatching {
            BluetoothAdapter.getDefaultAdapter()?.getRemoteDevice(address)?.bluetoothClass?.deviceClass
        }.getOrNull() ?: return null
        return when (deviceClass) {
            BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET,
            BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE,
            -> Form.BUDS
            BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES -> Form.HEADPHONES
            BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER,
            BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO,
            BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO,
            BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER,
            -> Form.SPEAKER
            BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO -> Form.CAR
            else -> null
        }
    }

    internal fun formFromName(name: String): Form? {
        val lower = name.lowercase()
        if (lower.isBlank()) return null
        fun matches(words: List<String>) = words.any { it in lower }
        return when {
            matches(HEADPHONE_NAMES) -> Form.HEADPHONES
            matches(BUDS_NAMES) -> Form.BUDS
            matches(SPEAKER_NAMES) -> Form.SPEAKER
            else -> null
        }
    }

    private val HEADPHONE_NAMES = listOf(
        "headphone", "airpods max", "wh-", "qc35", "qc45",
        "quietcomfort ultra headphones", "momentum 4", "studio pro", "solo", "major iv",
        "major v", "monitor ii",
    )

    private val BUDS_NAMES = listOf(
        "buds", "airpods", "pods", "earbud", "earphone", "wf-", "tws", "liberty",
        "freebuds", "nothing ear", "jabra elite", "motif", "minor iv", "earfun",
    )

    private val SPEAKER_NAMES = listOf(
        "speaker", "soundbar", "sound bar", "emberton", "kilburn", "stanmore", "acton",
        "woburn", "middleton", "willen", "tufton", "stockwell", "uxbridge",
        "jbl flip", "jbl charge", "jbl go", "jbl clip", "jbl xtreme", "jbl boombox",
        "jbl pulse", "partybox", "wonderboom", "megaboom", "boom 3", "everboom", "hyperboom",
        "soundlink", "srs-", "ult field", "soundcore motion", "soundcore flare",
        "soundcore boom", "stormbox", "beats pill", "sonos", "homepod", "echo", "nest audio",
        "bose home", "beosound", "beoplay a", "boombox",
    )

    private fun nameOf(info: AudioDeviceInfo, kind: Kind): String = when (kind) {
        Kind.PHONE, Kind.WIRED -> ""
        else -> info.productName?.toString()?.trim().orEmpty()
    }
}
