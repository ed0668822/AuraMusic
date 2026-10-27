package com.music.aura.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.music.aura.R
import com.music.aura.data.model.LocalSong

/**
 * Audio quality badge for local files, based on file format.
 */
enum class LocalAudioQuality { LOSSLESS, HI_RES_LOSSLESS, HIGH_QUALITY, STANDARD }

fun LocalSong.audioQualityBadge(): LocalAudioQuality? {
    val ext = filePath.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "flac", "alac", "wav", "aiff", "aif" -> {
            if (durationMs > 0) LocalAudioQuality.LOSSLESS else null
        }
        "dsf", "dff" -> LocalAudioQuality.HI_RES_LOSSLESS
        "mp3", "aac", "m4a", "ogg", "opus", "wma" -> LocalAudioQuality.HIGH_QUALITY
        else -> null
    }
}

@Composable
fun SourceQualityBadge(
    quality: LocalAudioQuality,
    color: Color,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        when (quality) {
            LocalAudioQuality.HI_RES_LOSSLESS, LocalAudioQuality.LOSSLESS ->
                Icon(painterResource(R.drawable.ic_lossless), null, tint = color)
            LocalAudioQuality.HIGH_QUALITY -> Icon(
                rememberVectorPainter(Icons.Rounded.Headphones),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp),
            )
            LocalAudioQuality.STANDARD -> {}
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = when (quality) {
                LocalAudioQuality.HI_RES_LOSSLESS -> "Hi-Res"
                LocalAudioQuality.LOSSLESS -> "Lossless"
                LocalAudioQuality.HIGH_QUALITY -> "Compressed"
                LocalAudioQuality.STANDARD -> ""
            },
            style = style,
            color = color,
            maxLines = 1,
            softWrap = false,
        )
    }
}
