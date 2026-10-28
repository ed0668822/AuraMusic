package com.music.aurelis.ui.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.aurelis.R
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.QueueTier
import com.music.aurelis.playback.autoplaySectionStart
import com.music.aurelis.ui.components.ExplicitSongTitle
import com.music.aurelis.ui.components.thumbnailBorder
import kotlin.math.abs
import kotlin.math.roundToInt

private const val PLAYER_GUTTER = 16

internal fun keepScrollInList(listState: LazyListState) = object : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = available
    override suspend fun onPreFling(available: Velocity): Velocity = if (available.y > 0f && !listState.canScrollBackward) available else Velocity.Zero
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
}

private val QUEUE_ROW_MOTION = tween<IntOffset>(durationMillis = 200, easing = FastOutSlowInEasing)

internal fun Modifier.fadingEdges(): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val fade = 28.dp.toPx()
        drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black), 0f, fade), blendMode = BlendMode.DstIn)
        drawRect(brush = Brush.verticalGradient(listOf(Color.Black, Color.Transparent), size.height - fade, size.height), blendMode = BlendMode.DstIn)
    }

internal data class QueueTrack(val timelineIndex: Int, val song: LocalSong, val key: String)
internal enum class QueueSection { USER, CONTEXT, AUTOPLAY }

internal class QueueTracks(
    val nowPlaying: QueueTrack?,
    val user: List<QueueTrack>,
    val context: List<QueueTrack>,
    val autoplay: List<QueueTrack>,
    val history: List<QueueTrack> = emptyList(),
) {
    operator fun get(section: QueueSection): List<QueueTrack> = when (section) {
        QueueSection.USER -> user
        QueueSection.CONTEXT -> context
        QueueSection.AUTOPLAY -> autoplay
    }
    companion object { val EMPTY = QueueTracks(null, emptyList(), emptyList(), emptyList()) }
}

internal fun splitQueue(queue: List<LocalSong>, currentIndex: Int, showHistory: Boolean = false): QueueTracks {
    if (currentIndex !in queue.indices) return QueueTracks.EMPTY
    val seen = HashMap<String, Int>()
    val occurrence = IntArray(queue.size) { i -> val id = queue[i].videoId; seen.getOrDefault(id, 0).also { seen[id] = it + 1 } }
    val usedKeys = HashSet<String>()
    fun track(index: Int, prefix: String): QueueTrack {
        val song = queue[index]
        val base = prefix + "_" + (song.queueEntryId ?: "${song.videoId}#${occurrence[index]}")
        var key = base
        var repeat = 1
        while (!usedKeys.add(key)) key = base + "~" + repeat++
        return QueueTrack(index, song, key)
    }
    val user = ArrayList<QueueTrack>()
    val context = ArrayList<QueueTrack>()
    val autoplay = ArrayList<QueueTrack>()
    for (index in currentIndex + 1 until queue.size) {
        when (queue[index].queueTier) {
            QueueTier.USER_QUEUE -> user += track(index, "user")
            QueueTier.CONTEXT -> context += track(index, "context")
            QueueTier.AUTOPLAY -> autoplay += track(index, "autoplay")
        }
    }
    val history = if (showHistory) (0 until currentIndex).map { track(it, "history") } else emptyList()
    return QueueTracks(track(currentIndex, "np"), user, context, autoplay, history)
}

private fun LazyItemScope.queueMotion(): Modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = QUEUE_ROW_MOTION)

@Composable
internal fun InlineQueue(
    queue: List<LocalSong>,
    currentIndex: Int,
    autoplayEnabled: Boolean,
    controlsLocked: Boolean,
    onJumpTo: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onClear: () -> Unit,
    onScrollingChange: (Boolean) -> Unit = {},
    onDragActiveChange: (Boolean) -> Unit = {},
    collapsePlayerOnScroll: Boolean = false,
    onRevealPlayer: () -> Unit = {},
    onHidePlayer: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState) { snapshotFlow { listState.isScrollInProgress }.collect(onScrollingChange) }
    val keepScroll = remember(listState) { keepScrollInList(listState) }
    
    val showHistory by SettingsRepository.showQueueHistory.collectAsStateWithLifecycle()
    val tracks = remember(queue, currentIndex, showHistory) { splitQueue(queue, currentIndex, showHistory) }
    val drag = rememberQueueDrag(listState, tracks, onMove, onDragActiveChange)
    val nowPlaying = tracks.nowPlaying
    
    val contextSong = tracks.context.firstOrNull()?.song
    val contextTitle = contextSong?.playbackSource?.takeIf { it.isNotBlank() }
        ?: contextSong?.albumName?.takeIf { it.isNotBlank() }
        ?: nowPlaying?.song?.playbackSource?.takeIf { it.isNotBlank() }
        ?: nowPlaying?.song?.albumName?.takeIf { it.isNotBlank() }

    var hasScrolledOnce by remember { mutableStateOf(false) }
    LaunchedEffect(currentIndex, nowPlaying?.key, showHistory) {
        val nowPlayingRow = if (tracks.history.isEmpty()) 0 else tracks.history.size + 1
        val scrolledAway = listState.firstVisibleItemIndex != nowPlayingRow || listState.firstVisibleItemScrollOffset != 0
        if (drag.held == null && nowPlaying != null && scrolledAway) {
            if (hasScrolledOnce) listState.animateScrollToItem(nowPlayingRow) else { listState.scrollToItem(nowPlayingRow); hasScrolledOnce = true }
        }
    }

    Column(modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(R.string.queue), style = MaterialTheme.typography.titleLarge, color = Color.White, modifier = Modifier.weight(1f))
            if (tracks.user.isNotEmpty()) QueueClearButton(MaterialTheme.typography.titleMedium, controlsLocked, onClear)
        }
        Spacer(Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth().fadingEdges()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().nestedScroll(keepScroll),
                contentPadding = PaddingValues(horizontal = PLAYER_GUTTER.dp),
            ) {
                if (tracks.history.isNotEmpty()) {
                    item(key = "header-history") { QueueHeading(stringResource(R.string.previously_played), Modifier.padding(top = 12.dp, bottom = 6.dp)) }
                    items(tracks.history, key = { it.key }) { track ->
                        InlineQueueRow(track.song, false, { onJumpTo(track.timelineIndex) }, { onRemove(track.timelineIndex) }, locked = controlsLocked, modifier = queueMotion())
                    }
                }
                if (nowPlaying != null) {
                    item(key = "header-now-playing") { QueueHeading(stringResource(R.string.now_playing), Modifier.padding(top = 12.dp, bottom = 6.dp)) }
                    item(key = nowPlaying.key) {
                        InlineQueueRow(nowPlaying.song, true, { onJumpTo(nowPlaying.timelineIndex) }, { onRemove(nowPlaying.timelineIndex) }, locked = controlsLocked, modifier = queueMotion())
                    }
                }
                if (tracks.user.isNotEmpty()) {
                    item(key = "header-user-queue") {
                        QueueHeading(stringResource(R.string.next_in_queue), Modifier.padding(top = 16.dp, bottom = 6.dp).then(queueMotion())) {
                            QueueClearButton(MaterialTheme.typography.labelLarge, controlsLocked, onClear)
                        }
                    }
                    queueSection(tracks.user, QueueSection.USER, drag, controlsLocked, onJumpTo, onRemove)
                }
                if (tracks.context.isNotEmpty()) {
                    item(key = "header-context") {
                        QueueHeading(if (contextTitle != null) stringResource(R.string.next_from, contextTitle) else stringResource(R.string.queue), Modifier.padding(top = 16.dp, bottom = 6.dp).then(queueMotion()))
                    }
                    queueSection(tracks.context, QueueSection.CONTEXT, drag, controlsLocked, onJumpTo, onRemove)
                }
                if (autoplayEnabled || tracks.autoplay.isNotEmpty()) {
                    item(key = "autoplay-heading") { AutoplayHeading(hasTracks = tracks.autoplay.isNotEmpty(), modifier = queueMotion()) }
                    queueSection(tracks.autoplay, QueueSection.AUTOPLAY, drag, controlsLocked, onJumpTo, onRemove)
                }
            }
            drag.held?.let { held ->
                Box(Modifier.matchParentSize().padding(horizontal = PLAYER_GUTTER.dp)) {
                    InlineQueueRow(held.song, false, {}, {}, draggable = true, lifted = true, modifier = Modifier.offset { IntOffset(0, drag.heldTop.roundToInt()) })
                }
            }
        }
    }
}

private fun LazyListScope.queueSection(
    rows: List<QueueTrack>, section: QueueSection, drag: QueueDrag, locked: Boolean, onJumpTo: (Int) -> Unit, onRemove: (Int) -> Unit
) {
    items(rows, key = { it.key }) { track ->
        val held = drag.held?.key == track.key
        if (held) DisposableEffect(Unit) { onDispose { drag.end(track.key) } }
        InlineQueueRow(
            track.song, false, { onJumpTo(track.timelineIndex) }, { onRemove(track.timelineIndex) }, locked = locked,
            draggable = !locked, onDragStart = { drag.start(track.key, section) }, onDrag = drag::drag, onDragEnd = { drag.end(track.key) },
            modifier = if (held) Modifier.alpha(0f) else queueMotion()
        )
    }
}

@Composable
private fun QueueHeading(title: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.75f), modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun QueueClearButton(style: TextStyle, locked: Boolean, onClear: () -> Unit) {
    Text(
        text = stringResource(R.string.clear), style = style, color = Color.White.copy(alpha = if (locked) 0.25f else 0.75f),
        modifier = Modifier.clip(RoundedCornerShape(percent = 50)).clickable(enabled = !locked, onClick = onClear).padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun AutoplayHeading(hasTracks: Boolean, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.AllInclusive, null, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(stringResource(R.string.autoplay), style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(stringResource(if (hasTracks) R.string.autoplay_queue_description else R.string.autoplay_empty_description), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.55f))
        }
    }
}

private val QUEUE_EDGE_SCROLL_ZONE = 40.dp
private val QUEUE_EDGE_SCROLL_SPEED = 340.dp

fun edgeScrollSpeed(top: Float, bottom: Float, viewportStart: Int, viewportEnd: Int, zone: Float, speed: Float): Float {
    if (zone <= 0f) return 0f
    val intoStart = (viewportStart + zone) - top
    val intoEnd = bottom - (viewportEnd - zone)
    val reach = when {
        intoStart > 0f && intoEnd <= 0f -> -intoStart
        intoEnd > 0f && intoStart <= 0f -> intoEnd
        else -> return 0f
    }
    val ramp = speed * (0.2f + 0.8f * (abs(reach) / zone).coerceAtMost(1f))
    return if (reach < 0f) -ramp else ramp
}

@Composable
private fun rememberQueueDrag(listState: LazyListState, tracks: QueueTracks, onMove: (Int, Int) -> Unit, onActiveChange: (Boolean) -> Unit): QueueDrag {
    val drag = remember(listState) { QueueDrag(listState) }
    drag.tracks = tracks
    drag.onMove = onMove
    drag.onActiveChange = onActiveChange
    with(LocalDensity.current) { drag.edgeZone = QUEUE_EDGE_SCROLL_ZONE.toPx(); drag.edgeSpeed = QUEUE_EDGE_SCROLL_SPEED.toPx() }
    val direction = drag.autoScrollDir
    LaunchedEffect(drag, direction) {
        if (direction == 0) return@LaunchedEffect
        listState.scroll {
            var previous = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                val seconds = ((now - previous) / 1_000_000_000f).coerceAtMost(1f / 30f)
                previous = now
                if (scrollBy(drag.autoScrollSpeed * seconds) == 0f) break
                drag.scrolled()
            }
        }
    }
    return drag
}

private class QueueDrag(private val listState: LazyListState) {
    var tracks = QueueTracks.EMPTY
    var onMove: (Int, Int) -> Unit = { _, _ -> }
    var onActiveChange: (Boolean) -> Unit = {}
    var edgeZone = 0f
    var edgeSpeed = 0f
    var held by mutableStateOf<QueueTrack?>(null); private set
    var heldTop by mutableFloatStateOf(0f); private set
    var autoScrollDir by mutableIntStateOf(0); private set
    var autoScrollSpeed = 0f; private set
    private var section = QueueSection.USER
    private var heldCenter = 0f
    private var heldSize = 0
    private var awaiting: Int? = null

    fun start(key: String, section: QueueSection) {
        val track = tracks[section].firstOrNull { it.key == key } ?: return
        val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return
        this.section = section
        heldSize = item.size
        heldCenter = item.offset + item.size / 2f
        heldTop = item.offset.toFloat()
        awaiting = null
        held = track
        onActiveChange(true)
    }

    fun drag(deltaY: Float) = settle(deltaY)
    fun scrolled() = settle(0f)

    fun end(key: String) {
        if (held?.key != key) return
        held = null
        awaiting = null
        setAutoScroll(0f)
        onActiveChange(false)
    }

    private fun settle(deltaY: Float) {
        val key = held?.key ?: return
        val rows = tracks[section]
        val info = listState.layoutInfo
        val items = info.visibleItemsInfo
        val half = heldSize / 2f

        var center = heldCenter + deltaY
        rows.firstOrNull()?.let { first -> items.firstOrNull { it.key == first.key }?.let { center = center.coerceAtLeast(it.offset + half) } }
        rows.lastOrNull()?.let { last -> items.firstOrNull { it.key == last.key }?.let { center = center.coerceAtMost(it.offset + it.size - half) } }
        heldCenter = center
        val top = center - half
        val minTop = info.viewportStartOffset.toFloat()
        heldTop = top.coerceIn(minTop, (info.viewportEndOffset - heldSize).toFloat().coerceAtLeast(minTop))

        val dragged = items.firstOrNull { it.key == key } ?: return setAutoScroll(0f)
        aimAutoScroll(top, key, rows)

        awaiting?.let { if (dragged.index != it) return; awaiting = null }
        val target = items.filter { item -> item.key != key && rows.any { it.key == item.key } }.minByOrNull { abs(it.offset + it.size / 2f - center) } ?: return
        if (abs(center - (target.offset + target.size / 2f)) > target.size / 2f) return
        if (target.index == listState.firstVisibleItemIndex && listState.canScrollBackward) return
        val from = rows.firstOrNull { it.key == key }?.timelineIndex ?: return
        val to = rows.firstOrNull { it.key == target.key }?.timelineIndex ?: return
        onMove(from, to)
        awaiting = target.index
    }

    private fun aimAutoScroll(top: Float, key: String, rows: List<QueueTrack>) {
        val info = listState.layoutInfo
        val speed = edgeScrollSpeed(top, top + heldSize, info.viewportStartOffset, info.viewportEndOffset, edgeZone, edgeSpeed)
        val blocked = when {
            speed < 0f -> rows.firstOrNull()?.key == key || !listState.canScrollBackward
            speed > 0f -> rows.lastOrNull()?.key == key || !listState.canScrollForward
            else -> true
        }
        setAutoScroll(if (blocked) 0f else speed)
    }

    private fun setAutoScroll(speed: Float) {
        autoScrollSpeed = speed
        val direction = when { speed > 0f -> 1; speed < 0f -> -1; else -> 0 }
        if (autoScrollDir != direction) autoScrollDir = direction
    }
}

@Composable
private fun InlineQueueRow(
    song: LocalSong, isCurrent: Boolean, onClick: () -> Unit, onRemove: () -> Unit, modifier: Modifier = Modifier, locked: Boolean = false,
    draggable: Boolean = false, lifted: Boolean = false, onDragStart: () -> Unit = {}, onDrag: (Float) -> Unit = {}, onDragEnd: () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (lifted) Color.White.copy(alpha = 0.06f) else Color.Transparent)
            .clickable(enabled = !locked, onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (draggable) {
            Icon(
                Icons.Rounded.DragHandle, contentDescription = stringResource(R.string.drag_to_reorder), tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp).offset(x = (-4).dp).pointerInput(Unit) {
                    detectDragGestures(onDragStart = { onDragStart() }, onDragEnd = { onDragEnd() }, onDragCancel = { onDragEnd() }, onDrag = { change, amount -> change.consume(); onDrag(amount.y) })
                },
            )
            Spacer(Modifier.width(4.dp))
        }
        AsyncImage(
            model = song.artworkUri, contentDescription = null,
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)).thumbnailBorder(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.08f)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            ExplicitSongTitle(song = song, style = MaterialTheme.typography.titleMedium, color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.92f))
            Text(song.artist, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (isCurrent) {
            Icon(Icons.Rounded.GraphicEq, contentDescription = stringResource(R.string.now_playing), tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        }
        if (!locked) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onRemove), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.remove_from_queue), tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.size(18.dp))
            }
        }
    }
}
