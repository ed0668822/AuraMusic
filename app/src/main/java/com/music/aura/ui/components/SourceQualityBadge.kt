package com.music.aura.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.music.aura.R
import com.music.aura.data.model.LocalSong

/**
 * What a local file's own container suggests it can play at, for the small
 * quality badge next to titles and in the player.
 *
 * Inferred from the file extension, not measured. A FLAC that was transcoded
 * up from a 128kbps MP3 will still be called "Lossless" here, because the
 * container is all we can see without decoding the whole file. The player's
 * own badge — when it lands in Phase 7 — will confirm what actually played.
 */
enum class AudioQuality { DOLBY_ATMOS, HI_RES_LOSSLESS, LOSSLESS, HIGH_QUALITY }

fun LocalSong.audioQualityBadge(): AudioQuality? =
    when (filePath?.substringAfterLast('.', "")?.lowercase()) {
        "flac", "alac", "wav", "aiff", "aif", "ape" -> AudioQuality.LOSSLESS
        "dsf", "dff" -> AudioQuality.HI_RES_LOSSLESS
        "mp3", "aac", "m4a", "ogg", "opus", "wma" -> AudioQuality.HIGH_QUALITY
        else -> null
    }

/**
 * The player's quality glyph and label, at row size: the same lossless wave
 * and Dolby double-D the player draws.
 */
@Composable
fun SourceQualityBadge(
    quality: AudioQuality,
    color: Color,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        when (quality) {
            AudioQuality.DOLBY_ATMOS -> Icon(
                painter = painterResource(R.drawable.ic_dolby_atmos),
                contentDescription = null,
                tint = color,
            )
            AudioQuality.HI_RES_LOSSLESS, AudioQuality.LOSSLESS -> Icon(
                painter = painterResource(R.drawable.ic_lossless),
                contentDescription = null,
                tint = color,
            )
            AudioQuality.HIGH_QUALITY -> Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp),
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = when (quality) {
                AudioQuality.DOLBY_ATMOS -> "Dolby Atmos"
                AudioQuality.HI_RES_LOSSLESS -> "Hi-Res Lossless"
                AudioQuality.LOSSLESS -> "Lossless"
                AudioQuality.HIGH_QUALITY -> "High Quality"
            },
            style = style,
            color = color,
            maxLines = 1,
            softWrap = false,
        )
    }
}
