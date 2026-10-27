package com.music.aura.ui.player

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.aura.data.model.LocalSong
import kotlinx.coroutines.delay

/**
 * A single-line text that scrolls horizontally when it overflows.
 * Scrolls twice, then rests — matching Apple Music's player credits.
 */
@Composable
internal fun MarqueeText(
    text: String,
    style: TextStyle,
    color: Color,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    onOverflowChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(6.dp))
        }
        if (enabled) {
            Text(
                text = text,
                style = style,
                color = color,
                maxLines = 1,
                modifier = Modifier.basicMarquee(
                    iterations = 2,
                    initialDelayMillis = 800,
                    repeatDelayMillis = 1_500,
                ),
            )
        } else {
            Text(
                text = text,
                style = style,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Artist credits with per-artist tappable segments, scrolling as a marquee.
 */
@Composable
internal fun ArtistCreditsMarquee(
    text: String,
    credits: List<ArtistCredit>,
    style: TextStyle,
    color: Color,
    enabled: Boolean = true,
    startDelayMillis: Long = 0L,
    onOpenArtist: (name: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // For local music, artist credits are plain text segments.
    // No browse IDs to navigate to, so we render as a simple marquee text.
    if (enabled) {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = 1,
            modifier = modifier.basicMarquee(
                iterations = 2,
                initialDelayMillis = 800 + startDelayMillis,
                repeatDelayMillis = 1_500,
            ),
        )
    } else {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
    }
}
