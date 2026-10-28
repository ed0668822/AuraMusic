package com.music.aurelis.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.music.aurelis.R

@Composable
internal fun outputLabel(device: AudioOutputDevice): String {
    return when {
        device.name.isNotBlank() -> device.name
        device.kind == AudioOutputKind.WIRED -> stringResource(R.string.wired_headphones)
        device.kind == AudioOutputKind.USB -> stringResource(R.string.usb_audio)
        device.kind == AudioOutputKind.HDMI -> stringResource(R.string.hdmi_output)
        else -> stringResource(R.string.this_phone)
    }
}
