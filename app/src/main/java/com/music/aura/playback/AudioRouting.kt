package com.music.aura.playback

import android.media.AudioManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AudioRouting {
    enum class Kind { PHONE, WIRED, USB, BLUETOOTH, HDMI, OTHER }
    enum class Form { UNKNOWN, BUDS, HEADPHONES, SPEAKER, CAR }

    data class Device(
        val id: Int,
        val name: String,
        val kind: Kind,
        val isActive: Boolean = false,
        val form: Form = Form.UNKNOWN,
    )

    private val _selectedId = MutableStateFlow<Int?>(null)
    val selectedId: StateFlow<Int?> = _selectedId.asStateFlow()

    fun outputs(manager: AudioManager): List<Device> {
        val devices = mutableListOf<Device>()
        val selectedId = _selectedId.value
        devices += Device(
            id = 0,
            name = "Phone",
            kind = Kind.PHONE,
            isActive = selectedId == null || selectedId == 0,
            form = Form.SPEAKER,
        )
        if (manager.isWiredHeadsetOn) {
            devices += Device(
                id = 1,
                name = "Wired headphones",
                kind = Kind.WIRED,
                isActive = selectedId == 1,
                form = Form.HEADPHONES,
            )
        }
        return devices
    }

    fun select(id: Int?) {
        _selectedId.value = id
    }
}
