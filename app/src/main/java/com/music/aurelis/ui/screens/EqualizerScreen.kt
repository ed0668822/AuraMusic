package com.music.aurelis.ui.screens

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.aurelis.R
import com.music.aurelis.data.EqualizerMode
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.playback.EqLayout
import com.music.aurelis.playback.EqualizerPreset
import com.music.aurelis.ui.components.*
import com.music.aurelis.ui.haptics.Haptic
import com.music.aurelis.ui.haptics.rememberHaptics
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val enabled by SettingsRepository.equalizerEnabled.collectAsStateWithLifecycle()
    val mode by SettingsRepository.equalizerMode.collectAsStateWithLifecycle()
    val toneX by SettingsRepository.equalizerToneX.collectAsStateWithLifecycle()
    val toneY by SettingsRepository.equalizerToneY.collectAsStateWithLifecycle()
    val focused by SettingsRepository.equalizerFocused.collectAsStateWithLifecycle()
    val balance by SettingsRepository.equalizerBalance.collectAsStateWithLifecycle()
    val bands by SettingsRepository.equalizerBands.collectAsStateWithLifecycle()
    val preset by SettingsRepository.equalizerPreset.collectAsStateWithLifecycle()

    val haptics = rememberHaptics()
    var pickingPreset by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 8.dp, end = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
            }
            Text(
                text = stringResource(R.string.equalizer),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        SettingsGroup(
            footer = stringResource(R.string.equalizer_footer),
            topSpacing = 8.dp,
        ) {
            SettingsRow(
                icon = Icons.Rounded.Tune,
                title = stringResource(R.string.equalizer),
                trailing = {
                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            haptics.play(if (it) Haptic.ToggleOn else Haptic.ToggleOff)
                            SettingsRepository.setEqualizerEnabled(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = {
                    haptics.play(if (!enabled) Haptic.ToggleOn else Haptic.ToggleOff)
                    SettingsRepository.setEqualizerEnabled(!enabled)
                },
            )
        }

        val settingsAlpha by animateFloatAsState(
            targetValue = if (enabled) 1f else 0.38f,
            animationSpec = tween(220),
            label = "equalizerEnabled",
        )
        Column(modifier = Modifier.graphicsLayer { alpha = settingsAlpha }) {
            Spacer(Modifier.height(18.dp))
            SegmentedControl(
                options = EqualizerMode.entries.map { it.localizedLabel() },
                selectedIndex = EqualizerMode.entries.indexOf(mode),
                onSelect = { SettingsRepository.setEqualizerMode(EqualizerMode.entries[it]) },
                modifier = Modifier.padding(horizontal = GROUP_INSET),
            )

            when (mode) {
                EqualizerMode.DYNAMIC -> {
                    SettingsGroup(
                        header = stringResource(R.string.equalizer_tone),
                        footer = stringResource(
                            if (focused) R.string.equalizer_focused_footer else R.string.equalizer_broad_footer,
                        ),
                    ) {
                        TonePad(
                            x = toneX,
                            y = toneY,
                            onChange = { newX, newY -> SettingsRepository.setEqualizerTone(newX, newY) },
                            enabled = enabled,
                        )
                        ToneReadout(x = toneX, y = toneY)
                        RowDivider()
                        ChoiceRow(
                            label = stringResource(R.string.equalizer_broad),
                            selected = !focused,
                            onClick = { SettingsRepository.setEqualizerFocused(false) },
                            enabled = enabled,
                        )
                        RowDivider()
                        ChoiceRow(
                            label = stringResource(R.string.equalizer_focused),
                            selected = focused,
                            onClick = { SettingsRepository.setEqualizerFocused(true) },
                            enabled = enabled,
                        )
                    }
                }

                EqualizerMode.MANUAL -> {
                    SettingsGroup {
                        SettingsRow(
                            icon = Icons.Rounded.GraphicEq,
                            title = stringResource(R.string.equalizer_preset),
                            value = preset.localizedLabel(),
                            onClick = { pickingPreset = true },
                            enabled = enabled,
                        )
                    }
                    SettingsGroup(
                        header = stringResource(R.string.equalizer_bands),
                        footer = stringResource(R.string.equalizer_bands_footer),
                    ) {
                        BandSliders(
                            bands = bands,
                            onChange = { SettingsRepository.setEqualizerBands(it) },
                            enabled = enabled,
                        )
                        RowDivider()
                        DestructiveRow(
                            label = stringResource(R.string.equalizer_reset),
                            enabled = enabled,
                        ) {
                            haptics.play(Haptic.Select)
                            SettingsRepository.setEqualizerPreset(EqualizerPreset.FLAT)
                        }
                    }
                }
            }

            SettingsGroup(
                header = stringResource(R.string.equalizer_balance),
                footer = stringResource(R.string.equalizer_balance_footer),
            ) {
                BalanceControl(
                    balance = balance,
                    onChange = { SettingsRepository.setEqualizerBalance(it) },
                    enabled = enabled,
                )
            }
        }

        SettingsGroup(footer = stringResource(R.string.system_equalizer_footer)) {
            SettingsRow(
                icon = Icons.Rounded.GraphicEq,
                title = stringResource(R.string.system_equalizer),
                subtitle = stringResource(R.string.system_equalizer_subtitle),
                onClick = { openEqualizer(context) },
            )
        }
    }

    if (pickingPreset) {
        ModalBottomSheet(
            onDismissRequest = { pickingPreset = false },
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            PresetSheet(
                selected = preset,
                onSelect = {
                    SettingsRepository.setEqualizerPreset(it)
                    pickingPreset = false
                },
            )
        }
    }
}

@Composable
fun DestructiveRow(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = ROW_INSET, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun openEqualizer(context: Context) {
    val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
        putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        putExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0)
        putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    }
}

@Composable
private fun TonePad(
    x: Int,
    y: Int,
    enabled: Boolean,
    onChange: (Int, Int) -> Unit,
) {
    val haptics = rememberHaptics()
    val dotColor = MaterialTheme.colorScheme.onSurfaceVariant
    val puckColor = MaterialTheme.colorScheme.background
    val shadowColor = Color.Black.copy(alpha = 0.16f)
    val steps = EqLayout.TONE_STEPS

    val latestX by rememberUpdatedState(x)
    val latestY by rememberUpdatedState(y)
    val currentOnChange by rememberUpdatedState(onChange)

    val animatedX by animateFloatAsState(
        targetValue = x.toFloat(),
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessHigh),
        label = "tonePadX",
    )
    val animatedY by animateFloatAsState(
        targetValue = y.toFloat(),
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessHigh),
        label = "tonePadY",
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ROW_INSET, vertical = 18.dp)
            .height(PAD_HEIGHT)
            .pointerInput(steps, enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    var lastX = latestX
                    var lastY = latestY
                    fun report(offset: Offset) {
                        val (newX, newY) = stepAt(offset, size.toSize(), PUCK_RADIUS.toPx(), steps)
                        if (newX != lastX || newY != lastY) {
                            lastX = newX
                            lastY = newY
                            haptics.play(Haptic.Tick)
                            currentOnChange(newX, newY)
                        }
                    }
                    report(down.position)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            change.consume()
                            break
                        }
                        if (change.positionChanged()) {
                            change.consume()
                            report(change.position)
                        }
                    }
                }
            },
    ) {
        val inset = PUCK_RADIUS.toPx()
        val usableWidth = size.width - inset * 2
        val usableHeight = size.height - inset * 2
        val columns = steps * 2
        val dotRadius = DOT_RADIUS.toPx()

        for (column in 0..columns) {
            for (row in 0..columns) {
                val onAxis = column == steps || row == steps
                drawCircle(
                    color = dotColor.copy(alpha = if (onAxis) 0.55f else 0.3f),
                    radius = dotRadius,
                    center = Offset(
                        inset + usableWidth * column / columns,
                        inset + usableHeight * row / columns,
                    ),
                )
            }
        }

        val puckCentre = Offset(
            inset + usableWidth * (animatedX + steps) / columns,
            inset + usableHeight * (steps - animatedY) / columns,
        )
        for (ring in 3 downTo 1) {
            drawCircle(
                color = shadowColor.copy(alpha = shadowColor.alpha / ring),
                radius = inset + ring * 2f,
                center = puckCentre.copy(y = puckCentre.y + ring),
            )
        }
        drawCircle(color = puckColor, radius = inset, center = puckCentre)
    }
}

private fun stepAt(offset: Offset, size: Size, inset: Float, steps: Int): Pair<Int, Int> {
    val usableWidth = (size.width - inset * 2).coerceAtLeast(1f)
    val usableHeight = (size.height - inset * 2).coerceAtLeast(1f)
    val fractionX = ((offset.x - inset) / usableWidth).coerceIn(0f, 1f)
    val fractionY = ((offset.y - inset) / usableHeight).coerceIn(0f, 1f)
    val x = (fractionX * steps * 2).roundToInt() - steps
    val y = steps - (fractionY * steps * 2).roundToInt()
    return x to y
}

@Composable
private fun ToneReadout(x: Int, y: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Waves,
            contentDescription = stringResource(R.string.equalizer_tilt),
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = signed(x),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.width(22.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ShowChart,
            contentDescription = stringResource(R.string.equalizer_contour),
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = signed(y),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

private fun signed(value: Int): String = if (value > 0) "+$value" else value.toString()

@Composable
private fun ChoiceRow(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val haptics = rememberHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                if (!selected) {
                    haptics.play(Haptic.Select)
                    onClick()
                }
            }
            .padding(horizontal = ROW_INSET, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = stringResource(R.string.selected),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun BandSliders(
    bands: List<Float>,
    enabled: Boolean,
    onChange: (List<Float>) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ROW_INSET - 4.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        EqLayout.MANUAL_BANDS_HZ.forEachIndexed { index, hz ->
            val value = bands.getOrElse(index) { 0f }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = formatGain(value),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (abs(value) < 0.05f) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    maxLines = 1,
                )
                Spacer(Modifier.height(8.dp))
                BandFader(
                    enabled = enabled,
                    value = value,
                    onChange = { updated ->
                        onChange(bands.toMutableList().also { it[index] = updated })
                    },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = formatFrequency(hz),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun BandFader(
    value: Float,
    enabled: Boolean,
    onChange: (Float) -> Unit,
) {
    val haptics = rememberHaptics()
    val trackColor = MaterialTheme.colorScheme.outline
    val fillColor = MaterialTheme.colorScheme.primary
    val knobColor = MaterialTheme.colorScheme.background
    val range = EqLayout.MANUAL_RANGE_DB
    val latest by rememberUpdatedState(value)
    val currentOnChange by rememberUpdatedState(onChange)

    Canvas(
        modifier = Modifier
            .width(FADER_WIDTH)
            .height(FADER_HEIGHT)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    var last = latest
                    fun report(offset: Offset) {
                        val inset = KNOB_RADIUS.toPx()
                        val usable = (size.height - inset * 2).coerceAtLeast(1f)
                        val fraction = ((offset.y - inset) / usable).coerceIn(0f, 1f)
                        val raw = (1f - fraction * 2f) * range
                        val snapped = snapGain(raw, range)
                        if (abs(snapped - last) > 0.001f) {
                            haptics.play(Haptic.Tick)
                            last = snapped
                            currentOnChange(snapped)
                        }
                    }
                    report(down.position)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            change.consume()
                            break
                        }
                        if (change.positionChanged()) {
                            change.consume()
                            report(change.position)
                        }
                    }
                }
            },
    ) {
        val inset = KNOB_RADIUS.toPx()
        val usable = size.height - inset * 2
        val centreY = inset + usable / 2
        val trackWidth = TRACK_WIDTH.toPx()
        val centreX = size.width / 2
        val knobY = inset + usable * (1f - (value / range + 1f) / 2f)

        drawRoundRect(
            color = trackColor,
            topLeft = Offset(centreX - trackWidth / 2, inset),
            size = Size(trackWidth, usable),
            cornerRadius = CornerRadius(trackWidth / 2),
        )
        if (abs(value) > 0.05f) {
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(centreX - trackWidth / 2, minOf(centreY, knobY)),
                size = Size(trackWidth, abs(centreY - knobY)),
                cornerRadius = CornerRadius(trackWidth / 2),
            )
        }
        drawLine(
            color = trackColor.copy(alpha = 0.9f),
            start = Offset(centreX - trackWidth, centreY),
            end = Offset(centreX + trackWidth, centreY),
            strokeWidth = 1.dp.toPx(),
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.18f),
            radius = inset + 1.5f,
            center = Offset(centreX, knobY + 1.5f),
        )
        drawCircle(color = knobColor, radius = inset, center = Offset(centreX, knobY))
    }
}

private fun snapGain(raw: Float, range: Float): Float {
    if (abs(raw) < ZERO_DETENT_DB) return 0f
    return ((raw * 2f).roundToInt() / 2f).coerceIn(-range, range)
}

private fun formatGain(value: Float): String = when {
    abs(value) < 0.05f -> "0"
    value > 0 -> "+" + trimGain(value)
    else -> trimGain(value)
}

private fun trimGain(value: Float): String =
    if (abs(value - value.roundToInt()) < 0.05f) {
        value.roundToInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", value)
    }

private fun formatFrequency(hz: Float): String = when {
    hz < 1_000f -> hz.roundToInt().toString()
    hz % 1_000f == 0f -> "${(hz / 1_000f).roundToInt()}k"
    else -> String.format(Locale.getDefault(), "%.1fk", hz / 1_000f)
}

@Composable
private fun BalanceControl(
    balance: Float,
    enabled: Boolean,
    onChange: (Float) -> Unit,
) {
    val haptics = rememberHaptics()
    val trackColor = MaterialTheme.colorScheme.outline
    val fillColor = MaterialTheme.colorScheme.primary
    val knobColor = MaterialTheme.colorScheme.background
    val latest by rememberUpdatedState(balance)
    val currentOnChange by rememberUpdatedState(onChange)

    Column(Modifier.padding(horizontal = ROW_INSET, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.equalizer_balance_left),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = String.format(Locale.getDefault(), "%.2f", balance),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.equalizer_balance_right),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(10.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(KNOB_RADIUS * 2 + 8.dp)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        var last = latest
                        fun report(offset: Offset) {
                            val inset = KNOB_RADIUS.toPx()
                            val usable = (size.width - inset * 2).coerceAtLeast(1f)
                            val fraction = ((offset.x - inset) / usable).coerceIn(0f, 1f)
                            val raw = fraction * 2f - 1f
                            val snapped = if (abs(raw) < BALANCE_DETENT) 0f else raw
                            if (abs(snapped - last) > 0.004f) {
                                if (snapped == 0f && last != 0f) haptics.play(Haptic.Tick)
                                last = snapped
                                currentOnChange(snapped)
                            }
                        }
                        report(down.position)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            if (change.positionChanged()) {
                                change.consume()
                                report(change.position)
                            }
                        }
                    }
                },
        ) {
            val inset = KNOB_RADIUS.toPx()
            val usable = size.width - inset * 2
            val centreY = size.height / 2
            val centreX = inset + usable / 2
            val trackHeight = TRACK_WIDTH.toPx()
            val knobX = inset + usable * (balance + 1f) / 2f

            drawRoundRect(
                color = trackColor,
                topLeft = Offset(inset, centreY - trackHeight / 2),
                size = Size(usable, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2),
            )
            if (abs(balance) > 0.004f) {
                drawRoundRect(
                    color = fillColor,
                    topLeft = Offset(minOf(centreX, knobX), centreY - trackHeight / 2),
                    size = Size(abs(centreX - knobX), trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2),
                )
            }
            drawCircle(
                color = Color.Black.copy(alpha = 0.18f),
                radius = inset + 1.5f,
                center = Offset(knobX, centreY + 1.5f),
            )
            drawCircle(color = knobColor, radius = inset, center = Offset(knobX, centreY))
        }
    }
}

@Composable
private fun PresetSheet(selected: EqualizerPreset, onSelect: (EqualizerPreset) -> Unit) {
    val haptics = rememberHaptics()
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Text(
            text = stringResource(R.string.equalizer_preset),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 14.dp),
        )
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline,
        )
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
        ) {
            EqualizerPreset.entries.forEach { preset ->
                if (preset == EqualizerPreset.CUSTOM && selected != EqualizerPreset.CUSTOM) {
                    return@forEach
                }
                val chosen = preset == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = preset != EqualizerPreset.CUSTOM) {
                            haptics.play(Haptic.Select)
                            onSelect(preset)
                        }
                        .padding(horizontal = 22.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = preset.localizedLabel(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    if (chosen) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = stringResource(R.string.selected),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EqualizerMode.localizedLabel(): String = stringResource(
    when (this) {
        EqualizerMode.DYNAMIC -> R.string.equalizer_dynamic
        EqualizerMode.MANUAL -> R.string.equalizer_manual
    },
)

@Composable
private fun EqualizerPreset.localizedLabel(): String = stringResource(
    when (this) {
        EqualizerPreset.FLAT -> R.string.eq_preset_flat
        EqualizerPreset.ACOUSTIC -> R.string.eq_preset_acoustic
        EqualizerPreset.BASS_BOOST -> R.string.eq_preset_bass_boost
        EqualizerPreset.BASS_CUT -> R.string.eq_preset_bass_cut
        EqualizerPreset.VOCAL -> R.string.eq_preset_vocal
        EqualizerPreset.TREBLE_BOOST -> R.string.eq_preset_treble_boost
        EqualizerPreset.TREBLE_CUT -> R.string.eq_preset_treble_cut
        EqualizerPreset.LOUDNESS -> R.string.eq_preset_loudness
        EqualizerPreset.SPOKEN_WORD -> R.string.eq_preset_spoken_word
        EqualizerPreset.ELECTRONIC -> R.string.eq_preset_electronic
        EqualizerPreset.ROCK -> R.string.eq_preset_rock
        EqualizerPreset.HIP_HOP -> R.string.eq_preset_hip_hop
        EqualizerPreset.JAZZ -> R.string.eq_preset_jazz
        EqualizerPreset.CLASSICAL -> R.string.eq_preset_classical
        EqualizerPreset.SMALL_SPEAKERS -> R.string.eq_preset_small_speakers
        EqualizerPreset.LATE_NIGHT -> R.string.eq_preset_late_night
        EqualizerPreset.CUSTOM -> R.string.eq_preset_custom
    },
)

private val PAD_HEIGHT = 230.dp
private val PUCK_RADIUS = 19.dp
private val DOT_RADIUS = 2.5.dp
private val FADER_HEIGHT = 150.dp
private val FADER_WIDTH = 34.dp
private val TRACK_WIDTH = 5.dp
private val KNOB_RADIUS = 10.dp

private const val ZERO_DETENT_DB = 0.6f
private const val BALANCE_DETENT = 0.04f
