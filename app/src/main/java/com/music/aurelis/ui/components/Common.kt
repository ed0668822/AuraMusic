// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.aurelis.R
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.haptics.Haptic
import com.music.aurelis.ui.haptics.rememberHaptics
import kotlinx.coroutines.delay
import kotlin.math.abs

/* ------------------------------------------------------------------ */
/*  Constantes de layout — un solo punto de verdad                     */
/* ------------------------------------------------------------------ */

val PAGE_GUTTER = 16.dp
val BAR_GUTTER = PAGE_GUTTER
val ROW_DIVIDER_INSET = PAGE_GUTTER + 68.dp
val TRACK_NUMBER_WIDTH = 24.dp
val NUMBERED_ROW_DIVIDER_INSET = PAGE_GUTTER + TRACK_NUMBER_WIDTH + PAGE_GUTTER
val FLOATING_BAR_MAX_WIDTH = 440.dp
val SHELF_CARD_WIDTH = 150.dp

private const val HERO_CARD_FRACTION = 0.70f
private val HERO_CARD_MAX_WIDTH = 320.dp
const val HERO_CARD_RATIO = 0.92f

fun heroCardWidth(available: Dp): Dp = minOf(available * HERO_CARD_FRACTION, HERO_CARD_MAX_WIDTH)

private const val TRACK_COLUMN_FRACTION = 0.88f
private val TRACK_COLUMN_MAX_WIDTH = 400.dp
fun trackColumnWidth(available: Dp): Dp =
    minOf(available * TRACK_COLUMN_FRACTION, TRACK_COLUMN_MAX_WIDTH)

data class LibraryGridSpec(val columns: Int, val cardWidth: Dp)
private val LIBRARY_GRID_MIN_CARD_WIDTH = 140.dp
val LIBRARY_GRID_SPACING = 12.dp
private const val LIBRARY_GRID_MIN_COLUMNS = 2
private const val LIBRARY_GRID_MAX_COLUMNS = 5

fun libraryGrid(available: Dp): LibraryGridSpec {
    val raw = ((available + LIBRARY_GRID_SPACING) / (LIBRARY_GRID_MIN_CARD_WIDTH + LIBRARY_GRID_SPACING))
        .toInt()
    val columns = raw.coerceIn(LIBRARY_GRID_MIN_COLUMNS, LIBRARY_GRID_MAX_COLUMNS)
    val cardWidth = (available - LIBRARY_GRID_SPACING * (columns - 1)) / columns
    return LibraryGridSpec(columns, cardWidth)
}

val PlayingAccent = Color(0xFFFB4A62)

/** Espacio entre frames de las barras animadas: 25ms = 40fps, sin quemar batería. */
const val MOTION_FRAME_GAP_MS = 25L

/** Si las barras animadas deben moverse — el host puede pausarlas. */
val LocalPlayingBarsMotion = compositionLocalOf { true }

/* ------------------------------------------------------------------ */
/*  SongRow — fila de canción unificada                                */
/* ------------------------------------------------------------------ */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: LocalSong,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
    onMore: (() -> Unit)? = null,
    onSwipeToQueue: (() -> Unit)? = null,
    rowBackground: Color = MaterialTheme.colorScheme.background,
    trackNumber: Int? = null,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    activeTint: Color = MaterialTheme.colorScheme.primary,
    searchPlayingStyle: Boolean = false,
    selected: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    SwipeToQueueBox(
        onSwipeToQueue = onSwipeToQueue,
        modifier = modifier,
    ) {
        SongRowContent(
            song = song,
            onClick = onClick,
            onLongPress = onLongPress,
            onMore = onMore ?: onLongPress,
            modifier = if (onSwipeToQueue != null) Modifier.background(rowBackground) else modifier,
            trackNumber = trackNumber,
            subtitleColor = subtitleColor,
            isCurrent = isCurrent,
            isPlaying = isPlaying,
            activeTint = activeTint,
            searchPlayingStyle = searchPlayingStyle,
            selected = selected,
            trailing = trailing,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToQueueBox(
    onSwipeToQueue: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (onSwipeToQueue == null) {
        content()
        return
    }

    val haptics = rememberHaptics()
    val swipeStateHolder = remember { mutableStateOf<SwipeToDismissBoxState?>(null) }
    var boxWidth by remember { mutableFloatStateOf(0f) }
    val currentOnSwipeToQueue by rememberUpdatedState(onSwipeToQueue)

    val swipeState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled && currentOnSwipeToQueue != null) {
                val offset = try { swipeStateHolder.value?.requireOffset() ?: 0f } catch (_: Exception) { 0f }
                if (abs(offset) >= boxWidth * 0.45f) {
                    haptics.play(Haptic.Select)
                    currentOnSwipeToQueue.invoke()
                }
            }
            false
        },
        positionalThreshold = { distance -> distance * 0.5f },
    )
    swipeStateHolder.value = swipeState

    LaunchedEffect(swipeState, boxWidth) {
        if (boxWidth <= 0f) return@LaunchedEffect
        val armAt = boxWidth * 0.45f
        var armed = false
        snapshotFlow { try { swipeState.requireOffset() } catch (_: Exception) { 0f } }
            .collect { offset ->
                val travelled = abs(offset)
                when {
                    !armed && travelled >= armAt -> { armed = true; haptics.play(Haptic.Tick) }
                    armed && travelled < armAt * 0.8f -> armed = false
                }
            }
    }

    SwipeToDismissBox(
        state = swipeState,
        modifier = modifier.onSizeChanged { boxWidth = it.width.toFloat() },
        backgroundContent = { QueueSwipeBackground(swipeState) },
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSwipeBackground(swipeState: SwipeToDismissBoxState) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                val offset = try { swipeState.requireOffset() } catch (_: Exception) { 0f }
                if (offset > 0f) {
                    clipRect(left = 0f, top = 0f, right = offset, bottom = size.height) {
                        this@drawWithContent.drawContent()
                    }
                } else if (offset < 0f) {
                    clipRect(left = size.width + offset, top = 0f, right = size.width, bottom = size.height) {
                        this@drawWithContent.drawContent()
                    }
                }
            }
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
            .padding(horizontal = PAGE_GUTTER),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        QueueSwipeLabel()
        QueueSwipeLabel()
    }
}

@Composable
private fun QueueSwipeLabel() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Rounded.PlaylistAdd,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.queue),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongRowContent(
    song: LocalSong,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)?,
    onMore: (() -> Unit)?,
    modifier: Modifier = Modifier,
    trackNumber: Int? = null,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    activeTint: Color = MaterialTheme.colorScheme.primary,
    searchPlayingStyle: Boolean = false,
    selected: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val titleColor by animateColorAsState(
        targetValue = if (isCurrent) activeTint else MaterialTheme.colorScheme.onBackground,
        label = "song row title",
    )
    val activeBackground by animateColorAsState(
        targetValue = if (isCurrent || selected) activeTint.copy(alpha = 0.14f) else Color.Transparent,
        label = "song row background",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(activeBackground)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(horizontal = PAGE_GUTTER, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (trackNumber != null) {
            Box(Modifier.width(TRACK_NUMBER_WIDTH).height(52.dp), contentAlignment = Alignment.Center) {
                if (searchPlayingStyle && isCurrent && isPlaying) {
                    SearchPlayingBars()
                } else if (isCurrent) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(R.string.now_playing),
                        tint = activeTint,
                        modifier = Modifier.size(22.dp),
                    )
                } else {
                    Text(
                        text = "$trackNumber",
                        style = MaterialTheme.typography.bodyLarge,
                        color = subtitleColor,
                        maxLines = 1,
                    )
                }
            }
        } else {
            Box(Modifier.size(52.dp)) {
                AsyncImage(
                    model = song.artworkUri,
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .thumbnailBorder(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                if (searchPlayingStyle && isCurrent && isPlaying) {
                    SearchPlayingBars(Modifier.align(Alignment.Center))
                }
            }
        }
        Spacer(Modifier.width(if (trackNumber == null) 14.dp else PAGE_GUTTER))
        Column(Modifier.weight(1f)) {
            ExplicitSongTitle(
                song = song,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = listOfNotNull(
                    song.artist.takeIf { it.isNotBlank() },
                    song.album?.takeIf { it.isNotBlank() },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isCurrent && trackNumber == null && !(searchPlayingStyle && isPlaying)) {
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(R.string.now_playing),
                tint = activeTint,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = song.durationText,
            style = MaterialTheme.typography.labelMedium,
            color = subtitleColor,
        )
        if (trailing != null) {
            Spacer(Modifier.width(4.dp))
            trailing()
        }
        if (onMore != null) {
            RowMoreButton(onClick = onMore, tint = subtitleColor)
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Botón ⋮                                                             */
/* ------------------------------------------------------------------ */

val ROW_MORE_OUTSET = 14.dp

@Composable
fun RowMoreButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    iconSize: Dp = 20.dp,
) {
    Box(
        modifier = modifier
            .hangIntoGutter(ROW_MORE_OUTSET)
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.MoreVert,
            contentDescription = stringResource(R.string.more),
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

fun Modifier.hangIntoGutter(outset: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val width = (placeable.width - outset.roundToPx()).coerceAtLeast(0)
    layout(width, placeable.height) { placeable.place(0, 0) }
}

/* ------------------------------------------------------------------ */
/*  Barras animadas eficientes                                         */
/* ------------------------------------------------------------------ */

private val PLAYING_BAR_MINIMUMS = floatArrayOf(0.38f, 0.78f, 0.52f)

private fun playingBarHeight(index: Int, minimum: Float, ms: Long): Float {
    val duration = 520 + index * 130
    val peak = 260 + index * 50
    var t = (ms % (2L * duration)).toInt()
    if (t > duration) t = 2 * duration - t
    return if (t <= peak) {
        minimum + (1f - minimum) * t / peak
    } else {
        1f - (1f - minimum) * (t - peak) / (duration - peak)
    }
}

@Composable
fun SearchPlayingBars(modifier: Modifier = Modifier) {
    val elapsedNanos = remember { mutableLongStateOf(0L) }
    val running = LocalPlayingBarsMotion.current

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var last = Long.MIN_VALUE
        while (true) {
            withFrameNanos { now ->
                if (last != Long.MIN_VALUE) elapsedNanos.longValue += now - last
                last = now
            }
            delay(MOTION_FRAME_GAP_MS)
        }
    }

    Box(
        modifier = modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.52f)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(width = 13.dp, height = 14.dp)) {
            val barWidth = 3.dp.toPx()
            val step = barWidth + 2.dp.toPx()
            val corner = CornerRadius(minOf(2.dp.toPx(), barWidth / 2f))
            val ms = elapsedNanos.longValue / 1_000_000L
            PLAYING_BAR_MINIMUMS.forEachIndexed { index, minimum ->
                val height = size.height * playingBarHeight(index, minimum, ms)
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(index * step, size.height - height),
                    size = Size(barWidth, height),
                    cornerRadius = corner,
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/*  Pull-to-refresh + estados vacíos                                   */
/* ------------------------------------------------------------------ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToRefresh(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    state: PullToRefreshState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        state = state,
        modifier = modifier.fillMaxSize(),
        indicator = {},
    ) {
        content()
    }
}

@Composable
fun MessageState(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER + 6.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun PillTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPassword: Boolean = false,
    container: Color = MaterialTheme.colorScheme.surfaceVariant,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(container, RoundedCornerShape(11.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
