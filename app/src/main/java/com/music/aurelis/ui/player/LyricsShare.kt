package com.music.aurelis.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.aurelis.R
import com.music.aurelis.data.lyrics.LyricLine
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.haptics.Haptic
import com.music.aurelis.ui.haptics.Haptics
import com.music.aurelis.ui.haptics.rememberHaptics

data class LyricsShareLine(
    val text: String,
    val subText: String?,
    val isGap: Boolean = false,
)

data class LyricsShareRequest(
    val song: LocalSong,
    val artworkUri: String?,
    val lines: List<LyricsShareLine>,
)

const val SHARE_CARD_CHAR_BUDGET = 250

fun fitsOnCard(taken: Int, text: String?): Boolean =
    taken + (text?.length ?: 0) <= SHARE_CARD_CHAR_BUDGET

val lyricsShareAvailable: Boolean
    get() = AurelisPlayerPlatform.host.lyricsShareAvailable

internal class LyricsPicker(
    val picking: Boolean,
    val picks: Set<Int>,
    val overBudget: Boolean,
    val pick: (Int) -> Unit,
    val toggle: (Int) -> Unit,
    val cancel: () -> Unit,
    val share: () -> Unit,
    val beginPicking: () -> Unit,
)

@Composable
internal fun rememberLyricsPicker(
    song: LocalSong,
    lines: List<LyricLine>?,
    subLines: List<LyricLine>?,
    artworkUri: String?,
    haptics: Haptics,
    onCard: (LyricsShareRequest) -> Unit,
): LyricsPicker {
    val cardFullMessage = stringResource(R.string.lyrics_share_pick_limit)
    var picking by remember { mutableStateOf(false) }
    var picks by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var overBudget by remember { mutableStateOf(false) }

    LaunchedEffect(song.id, lines) {
        picking = false
        picks = emptySet()
        overBudget = false
    }

    val usedChars: () -> Int = {
        picks.sumOf { index -> lines.orEmpty().getOrNull(index)?.text?.length ?: 0 }
    }
    val toggle: (Int) -> Unit = { index ->
        val text = lines.orEmpty().getOrNull(index)?.text
        when {
            index in picks -> {
                picks = picks - index
                overBudget = false
            }
            fitsOnCard(usedChars(), text) -> {
                picks = picks + index
                overBudget = false
            }
            else -> {
                overBudget = true
                AurelisPlayerPlatform.host.showMessage(cardFullMessage)
            }
        }
    }
    val pick: (Int) -> Unit = { index ->
        if (picking) {
            toggle(index)
        } else {
            haptics.play(Haptic.Select)
            picking = true
            picks = emptySet()
            overBudget = false
            toggle(index)
        }
    }
    val cancel: () -> Unit = {
        picking = false
        picks = emptySet()
        overBudget = false
    }
    val beginPicking: () -> Unit = {
        if (!picking) {
            haptics.play(Haptic.Select)
            picking = true
            picks = emptySet()
            overBudget = false
        }
    }
    val share: () -> Unit = {
        val source = lines.orEmpty()
        val chosen = picks.sorted()
        val picked = buildList {
            chosen.forEachIndexed { position, index ->
                if (position > 0 && index > chosen[position - 1] + 1) {
                    add(LyricsShareLine(text = "", subText = null, isGap = true))
                }
                val line = source.getOrNull(index) ?: return@forEachIndexed
                val sub = subLines?.getOrNull(index)?.text
                add(
                    LyricsShareLine(
                        text = line.text,
                        subText = sub?.takeIf { it.isNotBlank() && it != line.text },
                    ),
                )
            }
        }.filter { it.isGap || it.text.isNotBlank() }
        if (picked.isNotEmpty()) {
            onCard(LyricsShareRequest(song = song, artworkUri = artworkUri, lines = picked))
        }
        cancel()
    }

    return LyricsPicker(
        picking = picking,
        picks = picks,
        overBudget = overBudget,
        pick = pick,
        toggle = toggle,
        cancel = cancel,
        share = share,
        beginPicking = beginPicking,
    )
}

@Composable
internal fun LyricsPickBar(
    shareEnabled: Boolean,
    onCancel: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF141414).copy(alpha = 0.92f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PickAction(
            label = stringResource(R.string.cancel),
            icon = Icons.Rounded.Close,
            accent = false,
            enabled = true,
            modifier = Modifier.weight(1f),
            onClick = {
                haptics.play(Haptic.Tap)
                onCancel()
            },
        )
        PickAction(
            label = stringResource(R.string.share),
            icon = Icons.Rounded.IosShare,
            accent = true,
            enabled = shareEnabled,
            modifier = Modifier.weight(1f),
            onClick = {
                haptics.play(Haptic.Select)
                onShare()
            },
        )
    }
}

@Composable
private fun RowScope.PickAction(
    label: String,
    icon: ImageVector,
    accent: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val background = if (accent) {
        Color.White.copy(alpha = if (enabled) 1f else 0.3f)
    } else {
        Color.White.copy(alpha = if (enabled) 0.10f else 0.05f)
    }
    val foreground = if (accent) {
        Color(0xFF121212).copy(alpha = if (enabled) 1f else 0.4f)
    } else {
        Color.White.copy(alpha = if (enabled) 0.85f else 0.4f)
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = foreground,
            modifier = Modifier.size(19.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.W700,
            color = foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
