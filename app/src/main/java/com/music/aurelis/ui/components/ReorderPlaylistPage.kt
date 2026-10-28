package com.music.aurelis.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.music.aurelis.R
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.theme.ArtworkPalette

@Composable
fun ReorderPlaylistPage(
    playlistTitle: String,
    entries: List<LocalSong>,
    saving: Boolean,
    palette: ArtworkPalette,
    onSave: (List<LocalSong>) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    BackHandler(onBack = onClose)
    val order = remember(entries) { mutableStateListOf<LocalSong>().apply { addAll(entries) } }
    val changed = order.map { it.id } != entries.map { it.id }

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 22.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.back),
                    tint = palette.onBackground,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.reorder_songs),
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.onBackground,
                )
                Text(
                    text = playlistTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.onBackgroundVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(
                onClick = { onSave(order.toList()) },
                enabled = changed && !saving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.White.copy(alpha = 0.16f),
                    disabledContentColor = palette.onBackgroundVariant,
                ),
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = palette.onBackground,
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Text(stringResource(R.string.save_order))
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = palette.divider)

        ReorderableSongList(order, enabled = !saving, palette, contentPadding)
    }
}

@Composable
private fun ReorderableSongList(
    order: MutableList<LocalSong>,
    enabled: Boolean,
    palette: ArtworkPalette,
    contentPadding: PaddingValues,
) {
    val listState = rememberLazyListState()
    var held by remember { mutableStateOf<String?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    var autoScroll by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val edgeZone = with(density) { 56.dp.toPx() }
    val edgeSpeed = with(density) { 900.dp.toPx() }

    fun settle() {
        val key = held ?: return
        val items = listState.layoutInfo.visibleItemsInfo
        val dragged = items.firstOrNull { it.key == key } ?: return
        val top = dragged.offset + offset
        val center = top + dragged.size / 2f
        val target = items.firstOrNull { item ->
            item.key != key && item.index < order.size &&
                center >= item.offset && center <= item.offset + item.size
        }
        if (target != null) {
            val from = dragged.index
            val to = target.index
            if (from !in order.indices) return
            val first = listState.firstVisibleItemIndex
            val firstOffset = listState.firstVisibleItemScrollOffset
            order.add(to, order.removeAt(from))
            if (from == first || to == first) listState.requestScrollToItem(first, firstOffset)
            offset -= (target.offset - dragged.offset).toFloat()
        }
        val viewport = listState.layoutInfo
        autoScroll = when {
            top < viewport.viewportStartOffset + edgeZone && listState.canScrollBackward -> -edgeSpeed
            top + dragged.size > viewport.viewportEndOffset - edgeZone && listState.canScrollForward -> edgeSpeed
            else -> 0f
        }
    }

    fun end() {
        held = null
        offset = 0f
        autoScroll = 0f
    }

    LaunchedEffect(autoScroll != 0f) {
        if (autoScroll == 0f) return@LaunchedEffect
        var previous = withFrameNanos { it }
        while (held != null && autoScroll != 0f) {
            val now = withFrameNanos { it }
            val seconds = ((now - previous) / 1_000_000_000f).coerceAtMost(1f / 30f)
            previous = now
            val scrolled = listState.scrollBy(autoScroll * seconds)
            if (scrolled == 0f) break
            offset += scrolled
            settle()
        }
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
        itemsIndexed(order, key = { _, song -> song.id }) { _, song ->
            val key = song.id
            val dragging = held == key
            val start: () -> Unit = {
                held = key
                offset = 0f
            }
            val drag: (Float) -> Unit = { delta ->
                offset += delta
                settle()
            }
            ReorderRow(
                song = song,
                palette = palette,
                lifted = dragging,
                enabled = enabled,
                onDragStart = start,
                onDrag = drag,
                onDragEnd = ::end,
                modifier = Modifier
                    .zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer { translationY = if (dragging) offset else 0f }
                    .then(if (dragging) Modifier else Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)),
            )
        }
        item(key = "reorder-footer") { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ReorderRow(
    song: LocalSong,
    palette: ArtworkPalette,
    lifted: Boolean,
    enabled: Boolean,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gestures = Modifier.pointerInput(enabled) {
        if (!enabled) return@pointerInput
        detectDragGesturesAfterLongPress(
            onDragStart = { onDragStart() },
            onDragEnd = onDragEnd,
            onDragCancel = onDragEnd,
            onDrag = { change, amount ->
                change.consume()
                onDrag(amount.y)
            },
        )
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (lifted) palette.elevated.compositeOver(palette.background) else Color.Transparent)
            .then(gestures)
            .padding(start = 22.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.artworkUri,
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(7.dp))
                .thumbnailBorder(RoundedCornerShape(7.dp))
                .background(palette.elevated),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = palette.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.onBackgroundVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.Rounded.DragHandle,
            contentDescription = stringResource(R.string.drag_to_reorder),
            tint = palette.onBackgroundVariant,
            modifier = Modifier
                .size(48.dp)
                .padding(12.dp)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { onDragStart() },
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragEnd,
                        onDrag = { change, amount ->
                            change.consume()
                            onDrag(amount.y)
                        },
                    )
                },
        )
    }
}
