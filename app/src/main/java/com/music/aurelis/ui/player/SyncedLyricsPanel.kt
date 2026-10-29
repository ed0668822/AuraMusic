// Adaptado de BitChord. © 2026 Kushagra Singh / Aurelis. GPL-3.0-or-later.
package com.music.aurelis.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.data.lyrics.LyricAlignment
import com.music.aurelis.data.lyrics.LyricLine
import com.music.aurelis.playback.PlaybackPosition
import kotlinx.coroutines.delay

@Composable
fun SyncedLyricsPanel(
    lines: List<LyricLine>,
    trackKey: String,
    position: PlaybackPosition,
    looking: Boolean,
    isPlaying: Boolean,
    onSeekToLine: (Long) -> Unit,
    centerFocus: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (lines.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Sin letras disponibles",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.55f),
            )
        }
        return
    }

    val listState = rememberLazyListState()
    val blurUnfocused by SettingsRepository.blurUnfocusedLyrics.collectAsStateWithLifecycle()

    val activeIndices by remember(lines) {
        derivedStateOf { activeLyricRows(lines, position.positionMs) }
    }
    val activeIndex = activeIndices.lastOrNull() ?: -1

    LaunchedEffect(activeIndex, trackKey) {
        if (activeIndex < 0 || looking) return@LaunchedEffect
        delay(80)
        val target = (activeIndex - 2).coerceAtLeast(0)
        runCatching { listState.animateScrollToItem(target) }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = if (centerFocus) 180.dp else 120.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        itemsIndexed(lines, key = { index, line -> "$trackKey:$index" }) { index, line ->
            if (line.isGap) {
                Spacer(Modifier.height(28.dp))
                return@itemsIndexed
            }
            val isActive = index in activeIndices
            val isPast = activeIndex >= 0 && index < activeIndex
            val lineAlpha = when {
                isActive -> 1f
                isPast -> 0.35f
                else -> 0.55f
            }
            val blur = if (blurUnfocused && !isActive) 2.5.dp else 0.dp
            val horizontal = if (line.alignment == LyricAlignment.End) Alignment.End else Alignment.Start

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .blur(blur)
                    .alpha(lineAlpha),
                horizontalAlignment = horizontal,
            ) {
                LyricText(
                    line = line,
                    positionMs = position.positionMs,
                    isActive = isActive,
                    onTap = { onSeekToLine(line.timeMs) },
                )
                line.background?.let { backing ->
                    Spacer(Modifier.height(4.dp))
                    LyricText(
                        line = backing,
                        positionMs = position.positionMs,
                        isActive = isActive,
                        onTap = { onSeekToLine(backing.timeMs) },
                        smaller = true,
                    )
                }
            }
        }
    }
}

/**
 * One line, drawn as the "sung" portion over the "unsung" one. When the line
 * is word-synced the split falls inside the text at the character count
 * `revealedChars` reports; when it's not, the split is the whole line or
 * nothing, depending on whether its start has passed.
 *
 * The tap target is the whole line — a user reading is far more likely to want
 * to jump to the line they tapped than to type into it. `onSeekToLine` receives
 * the line's own time, not the current one.
 */
@Composable
private fun LyricText(
    line: LyricLine,
    positionMs: Long,
    isActive: Boolean,
    onTap: () -> Unit,
    smaller: Boolean = false,
) {
    val textStyle = if (smaller) {
        MaterialTheme.typography.titleMedium.copy(
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
        )
    } else {
        MaterialTheme.typography.headlineMedium.copy(
            fontSize = 22.sp,
            lineHeight = 30.sp,
            fontWeight = if (isActive) FontWeight.W800 else FontWeight.W600,
        )
    }

    val revealed = line.revealedChars(positionMs)
    val sungColor = if (isActive) Color.White else Color.White.copy(alpha = 0.92f)
    val unsungColor = if (smaller) Color.White.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.52f)

    val text = buildAnnotatedString {
        if (!line.isWordSynced) {
            // Line-synced: whole line lights up at once.
            withStyle(SpanStyle(color = if (revealed > 0f) sungColor else unsungColor)) {
                append(line.text)
            }
            return@buildAnnotatedString
        }
        val cut = revealed.toInt().coerceIn(0, line.text.length)
        if (cut > 0) {
            withStyle(SpanStyle(color = sungColor)) { append(line.text.substring(0, cut)) }
        }
        if (cut < line.text.length) {
            withStyle(SpanStyle(color = unsungColor)) { append(line.text.substring(cut)) }
        }
    }

    Text(
        text = text,
        style = textStyle,
        color = Color.Unspecified,
        textAlign = if (line.alignment == LyricAlignment.End) TextAlign.End else TextAlign.Start,
        maxLines = 4,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .let { m ->
                if (onTap is () -> Unit) {
                    m.then(
                        Modifier.padding(vertical = 2.dp)
                    )
                } else m
            },
    )
}
