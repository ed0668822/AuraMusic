// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.aurelis.R
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.data.lyrics.LyricsSource
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlin.math.abs

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun LyricsSourcesDialog(
    hazeState: HazeState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceDynamicBlur by SettingsRepository.reduceDynamicBlur.collectAsStateWithLifecycle()
    val selected by SettingsRepository.lyricsSources.collectAsStateWithLifecycle()
    val savedOrder by SettingsRepository.lyricsSourceOrder.collectAsStateWithLifecycle()
    val prioritizeSyllableSync by SettingsRepository.prioritizeSyllableSync.collectAsStateWithLifecycle()
    val paxSenixApiKey by SettingsRepository.paxSenixApiKey.collectAsStateWithLifecycle()
    var showPaxSenixKeyDialog by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(ALERT_CORNER)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SCRIM_COLOR)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(ALERT_WIDTH)
                .clip(shape)
                .then(
                    if (reduceDynamicBlur) {
                        Modifier.background(MaterialTheme.colorScheme.surface)
                    } else {
                        Modifier.optimizedHazeEffect(
                            state = hazeState,
                            style = HazeMaterials.regular(MaterialTheme.colorScheme.surface),
                        )
                    },
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 19.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.lyrics_sources),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.W600,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.lyrics_sources_order),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }

            ReorderableSourceList(
                order = savedOrder,
                selected = selected,
                onReorder = SettingsRepository::setLyricsSourceOrder,
                onToggle = { source ->
                    val checked = source in selected
                    if (checked && selected.size <= 1) return@ReorderableSourceList
                    SettingsRepository.setLyricsSources(
                        if (checked) selected - source else selected + source,
                    )
                },
            )

            AlertRule()
            AlertAction(
                label = stringResource(
                    if (paxSenixApiKey.isBlank()) R.string.paxsenix_api_key_missing
                    else R.string.paxsenix_api_key_configured,
                ),
                emphasised = false,
                onClick = { showPaxSenixKeyDialog = true },
            )
            AlertRule()
            SyllableSyncToggle(
                checked = prioritizeSyllableSync,
                onToggle = { SettingsRepository.setPrioritizeSyllableSync(!prioritizeSyllableSync) },
            )
            AlertRule()
            AlertAction(
                label = stringResource(R.string.reset_to_default),
                emphasised = false,
                onClick = SettingsRepository::resetLyricsSourceSettings,
            )
            AlertRule()
            AlertAction(label = stringResource(R.string.done), emphasised = true, onClick = onDismiss)
        }
    }

    if (showPaxSenixKeyDialog) {
        var input by remember(paxSenixApiKey) { mutableStateOf(paxSenixApiKey) }
        AlertDialog(
            onDismissRequest = { showPaxSenixKeyDialog = false },
            title = { Text(stringResource(R.string.paxsenix_api_key)) },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    SettingsRepository.setPaxSenixApiKey(input)
                    showPaxSenixKeyDialog = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showPaxSenixKeyDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun SyllableSyncToggle(checked: Boolean, onToggle: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ACTION_HEIGHT)
            .background(
                if (pressed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f) else Color.Transparent,
            )
            .clickable(
                indication = null,
                interactionSource = interactionSource,
                onClick = onToggle,
            )
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.prioritize_syllable_lyrics),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.prioritize_syllable_lyrics_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, lineHeight = 15.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
        }
        Spacer(Modifier.width(10.dp))
        if (checked) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = stringResource(R.string.enabled),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Composable
private fun ReorderableSourceList(
    order: List<LyricsSource>,
    selected: Set<LyricsSource>,
    onReorder: (List<LyricsSource>) -> Unit,
    onToggle: (LyricsSource) -> Unit,
) {
    var liveOrder by remember(order) { mutableStateOf(order) }
    var draggedSource by remember { mutableStateOf<LyricsSource?>(null) }
    var totalDrag by remember { mutableStateOf(0f) }
    var startIndex by remember { mutableStateOf(0) }
    var pitchPx by remember { mutableStateOf(0f) }
    var lockedPitchPx by remember { mutableStateOf(0f) }

    val scrollState = rememberScrollState()
    var viewportHeightPx by remember { mutableStateOf(0f) }
    val autoScroll = remember { SourceDragAutoScroll() }
    val density = LocalDensity.current
    val edgeZonePx = with(density) { 28.dp.toPx() }
    val edgeSpeedPx = with(density) { 220.dp.toPx() }

    fun advanceDrag(delta: Float) {
        val source = draggedSource ?: return
        val pitch = lockedPitchPx
        if (pitch <= 0f) return
        var index = liveOrder.indexOf(source)
        if (index < 0) return

        totalDrag = (totalDrag + delta).coerceIn(
            -startIndex * pitch,
            (liveOrder.lastIndex - startIndex) * pitch,
        )

        while (true) {
            val travelled = totalDrag / pitch
            val moved = (index - startIndex).toFloat()
            if (travelled > moved + 0.6f && index < liveOrder.lastIndex) {
                liveOrder = liveOrder.toMutableList().apply { add(index + 1, removeAt(index)) }
                index++
            } else if (travelled < moved - 0.6f && index > 0) {
                liveOrder = liveOrder.toMutableList().apply { add(index - 1, removeAt(index)) }
                index--
            } else break
        }

        val top = startIndex * pitch + totalDrag - scrollState.value
        val speed = edgeScrollSpeed(
            top = top,
            bottom = top + pitch,
            viewportStart = 0,
            viewportEnd = viewportHeightPx.toInt(),
            zone = edgeZonePx,
            speed = edgeSpeedPx,
        )
        val blocked = when {
            speed < 0f -> index <= 0 || !scrollState.canScrollBackward
            speed > 0f -> index >= liveOrder.lastIndex || !scrollState.canScrollForward
            else -> true
        }
        autoScroll.setSpeed(if (blocked) 0f else speed)
    }

    LaunchedEffect(autoScroll.dir) {
        if (autoScroll.dir == 0) return@LaunchedEffect
        scrollState.scroll {
            var previous = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                val seconds = ((now - previous) / 1_000_000_000f).coerceAtMost(1f / 30f)
                previous = now
                val scrolled = scrollBy(autoScroll.speed * seconds)
                if (scrolled == 0f) break
                advanceDrag(scrolled)
            }
        }
    }

    Column(
        modifier = Modifier
            .heightIn(max = 340.dp)
            .onSizeChanged { viewportHeightPx = it.height.toFloat() }
            .verticalScroll(scrollState),
    ) {
        liveOrder.forEach { source ->
            key(source) {
                val checked = source in selected
                val toggleable = !checked || selected.size > 1
                val dragging = source == draggedSource
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (dragging) 1f else 0f)
                        .onSizeChanged { pitchPx = it.height.toFloat() }
                        .graphicsLayer {
                            translationY = if (dragging) {
                                totalDrag - (liveOrder.indexOf(source) - startIndex) * lockedPitchPx
                            } else 0f
                        },
                ) {
                    AlertRule()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = ACTION_HEIGHT)
                            .clickable(
                                enabled = toggleable,
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = { onToggle(source) },
                            )
                            .padding(start = 4.dp, end = 16.dp, top = 9.dp, bottom = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DragHandle,
                            contentDescription = stringResource(R.string.drag_to_reorder),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .size(18.dp)
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = {
                                            draggedSource = source
                                            totalDrag = 0f
                                            startIndex = liveOrder.indexOf(source)
                                            lockedPitchPx = pitchPx
                                            autoScroll.setSpeed(0f)
                                        },
                                        onDrag = { change, delta ->
                                            change.consume()
                                            advanceDrag(delta.y)
                                        },
                                        onDragEnd = {
                                            draggedSource = null
                                            totalDrag = 0f
                                            autoScroll.setSpeed(0f)
                                            onReorder(liveOrder)
                                        },
                                        onDragCancel = {
                                            draggedSource = null
                                            totalDrag = 0f
                                            autoScroll.setSpeed(0f)
                                            liveOrder = order
                                        },
                                    )
                                },
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = source.label,
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                color = MaterialTheme.colorScheme.onSurface
                                    .copy(alpha = if (toggleable) 1f else 0.5f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = source.detail,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        if (checked) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = stringResource(R.string.enabled),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private class SourceDragAutoScroll {
    var dir by mutableIntStateOf(0)
        private set
    var speed: Float = 0f
        private set

    fun setSpeed(value: Float) {
        speed = value
        val next = when {
            value > 0f -> 1
            value < 0f -> -1
            else -> 0
        }
        if (dir != next) dir = next
    }
}

private fun edgeScrollSpeed(
    top: Float, bottom: Float,
    viewportStart: Int, viewportEnd: Int,
    zone: Float, speed: Float
): Float {
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
