// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.graphics.fadingEdges

fun Modifier.thumbnailBorder(shape: Shape): Modifier = composed {
    this.border(
        width = 1.dp,
        color = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.15f),
        shape = shape
    )
}

@Composable
fun ExplicitSongTitle(
    song: LocalSong,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    badgeColor: Color = color,
    fadeOverflow: Boolean = false,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (song.isExplicit == true) {
            ExplicitBadge(badgeColor)
            Spacer(Modifier.width(6.dp))
        }
        if (fadeOverflow) {
            FadingLine(text = song.title, style = style, color = color, modifier = Modifier.weight(1f, fill = false))
        } else {
            Text(
                text = song.title,
                style = style,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

@Composable
fun FadingLine(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    var overflowing by remember { mutableStateOf(false) }
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        onTextLayout = { overflowing = it.didOverflowWidth },
        modifier = modifier.then(if (overflowing) Modifier.fadingEdges(LINE_FADE, end = { 1f }) else Modifier),
    )
}

@Composable
fun FadingText(text: String, style: TextStyle, color: Color, maxLines: Int, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier) {
        val maxWidth = constraints.maxWidth
        val layout = remember(text, style, maxWidth) {
            measurer.measure(text, style, constraints = Constraints(maxWidth = maxWidth))
        }
        if (layout.lineCount <= maxLines) {
            Text(text = text, style = style, color = color)
        } else {
            val lastStart = layout.getLineStart(maxLines - 1)
            Column {
                Text(text = text.substring(0, lastStart).trimEnd(), style = style, color = color, maxLines = maxLines - 1)
                FadingLine(text = text.substring(lastStart), style = style, color = color)
            }
        }
    }
}

val LINE_FADE = 36.dp

@Composable
fun ExplicitBadge(color: Color, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelSmall.copy(
        fontSize = BADGE_LETTER_SIZE,
        fontWeight = FontWeight.W800,
        letterSpacing = 0.sp,
    )
    val letter = remember(measurer, style) { measurer.measure("E", style) }
    val density = LocalDensity.current
    val capPx = with(density) { style.fontSize.toPx() } * CAP_HEIGHT
    val side = with(density) { (capPx / BADGE_CAP_SHARE).toDp() }
    Canvas(
        modifier
            .size(side)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
        drawRoundRect(color = color, cornerRadius = CornerRadius(2.dp.toPx()))
        drawText(
            textLayoutResult = letter,
            topLeft = Offset(
                (this.size.width - letter.size.width) / 2f,
                (this.size.height + capPx) / 2f - letter.firstBaseline,
            ),
            blendMode = BlendMode.DstOut,
        )
    }
}

private val BADGE_LETTER_SIZE = 9.5.sp
private const val BADGE_CAP_SHARE = 0.52f
private const val CAP_HEIGHT = 0.705f
