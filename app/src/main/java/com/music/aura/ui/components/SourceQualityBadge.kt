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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.music.aura.R
import com.music.aura.data.model.LocalSong

/**
 * What a source row says it can be streamed at, in the player badge's terms.
 *
 * Read off [LocalSong.sourceQuality] — the tier the source *offered* in its
 * search answer, not anything measured — so it is a label for picking between
 * rows, and the player's own badge is still the one that confirms what played.
 * `LOW` and unlabelled rows get none.
 *
 * Until Fase 8 (NerdStats real), [LocalSong.sourceQuality] is always null, so
 * this function returns null and no badge is drawn. The badge is wired up now
 * so the UI surface (row integration) is already in place when the scanner
 * starts populating the fields.
 */
enum class SourceQuality { DOLBY_ATMOS, HI_RES_LOSSLESS, LOSSLESS, HIGH_QUALITY }

fun LocalSong.sourceQualityBadge(): SourceQuality? = when (sourceQuality?.uppercase()) {
    "DOLBY" -> SourceQuality.DOLBY_ATMOS
    "HI-RES", "HIRES" -> SourceQuality.HI_RES_LOSSLESS
    // Local files carry their container here — FLAC, WAV.
    "LOSSLESS", "FLAC", "ALAC", "WAV" ->
        if (sourceHiRes) SourceQuality.HI_RES_LOSSLESS else SourceQuality.LOSSLESS
    "HIGH", "320" -> SourceQuality.HIGH_QUALITY
    else -> null
}

/**
 * The player's quality glyph and label, at row size: the same lossless wave,
 * Dolby double-D and headphones the badge under the seek bar draws.
 */
@Composable
fun SourceQualityBadge(
    quality: SourceQuality,
    color: Color,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        when (quality) {
            SourceQuality.DOLBY_ATMOS -> Icon(
                painterResource(R.drawable.ic_dolby_atmos),
                contentDescription = null,
                tint = color,
            )
            SourceQuality.HI_RES_LOSSLESS, SourceQuality.LOSSLESS -> Icon(
                painterResource(R.drawable.ic_lossless),
                contentDescription = null,
                tint = color,
            )
            SourceQuality.HIGH_QUALITY -> Icon(
                rememberVectorPainter(Icons.Rounded.Headphones),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp),
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = when (quality) {
                SourceQuality.DOLBY_ATMOS -> "Dolby Atmos"
                SourceQuality.HI_RES_LOSSLESS -> stringResource(R.string.hi_res_lossless)
                SourceQuality.LOSSLESS -> stringResource(R.string.lossless)
                SourceQuality.HIGH_QUALITY -> stringResource(R.string.high_quality)
            },
            style = style,
            color = color,
            maxLines = 1,
            softWrap = false,
        )
    }
}
