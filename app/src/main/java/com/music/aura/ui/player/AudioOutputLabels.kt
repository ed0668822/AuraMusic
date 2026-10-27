package com.music.aura.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.music.aura.R

@Composable
internal fun outputLabel(device: AudioOutputDevice, accountName: String?): String {
    val firstName = accountName?.trim()?.split(Regex("\\s+"))?.firstOrNull()?.takeIf { it.isNotBlank() }
    return when {
        device.name.isNotBlank() -> device.name
        device.kind == AudioOutputKind.WIRED -> stringResource(R.string.wired_headphones)
        device.kind == AudioOutputKind.USB -> stringResource(R.string.usb_audio)
        device.kind == AudioOutputKind.HDMI -> stringResource(R.string.hdmi_output)
        firstName != null -> stringResource(R.string.personal_phone, firstName)
        else -> stringResource(R.string.this_phone)
    }
}

@Composable
internal fun rememberAudioOutputName(accountName: String?): String {
    val outputs = rememberAudioOutputs()
    val active = outputs.firstOrNull { it.isActive }
        ?: return if (accountName.isNullOrBlank()) {
            stringResource(R.string.this_phone)
        } else {
            stringResource(R.string.personal_phone, accountName.trim().split(Regex("\\s+")).first())
        }
    return outputLabel(active, accountName)
}
