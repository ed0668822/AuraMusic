package com.music.aura.ui.components

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlin.math.roundToInt

/**
 * Menú contextual estilo Apple Music: el item se levanta de la lista,
 * la página se difumina detrás, y las acciones cuelgan debajo.
 *
 * [reduceMotion] y [reduceDynamicBlur] se pasan desde el caller
 * (leídos de SettingsRepository).
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun <T : Any> HeldContextMenu(
    item: T?,
    held: HeldItem?,
    hazeState: HazeState,
    onDismiss: () -> Unit,
    onPreviewClick: ((T) -> Unit)?,
    reduceMotion: Boolean = false,
    reduceDynamicBlur: Boolean = false,
    actions: @Composable (T) -> Unit,
) {
    val requested = if (item != null && held != null) item to held else null
    var last by remember { mutableStateOf<Pair<T, HeldItem>?>(null) }
    if (requested != null && requested != last) {
        SideEffect { last = requested }
    }
    val shown = requested ?: last ?: return
    val (menuItem, heldItem) = shown
    val open = requested != null

    val progress = remember { Animatable(0f) }
    LaunchedEffect(open) {
        if (open) {
            progress.animateTo(
                1f,
                if (reduceMotion) {
                    tween(durationMillis = 140, easing = LinearEasing)
                } else {
                    spring(dampingRatio = 0.72f, stiffness = 420f)
                },
            )
        } else {
            progress.animateTo(
                0f,
                tween(durationMillis = if (reduceMotion) 110 else 230, easing = FastOutSlowInEasing),
            )
            last = null
        }
    }

    BackHandler(enabled = open, onBack = onDismiss)

    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val plate = (if (dark) Color.Black else Color.White).copy(alpha = if (reduceDynamicBlur) 0.85f else 0.5f)
    val hairline = if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f)
    val scrimColor = when {
        reduceDynamicBlur && dark -> Color.Black.copy(alpha = 0.62f)
        reduceDynamicBlur -> Color.Black.copy(alpha = 0.36f)
        dark -> Color.Black.copy(alpha = 0.30f)
        else -> Color.Black.copy(alpha = 0.10f)
    }
    val cardShape = RoundedCornerShape(20.dp)
    val menuShape = RoundedCornerShape(16.dp)
    val platePadding = 8.dp

    val insets = WindowInsets.safeDrawing
    var overlayOffset by remember { mutableStateOf(Offset.Zero) }

    Layout(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { overlayOffset = it.positionInWindow() }
            .then(if (open) Modifier else Modifier.pointerInput(Unit) { swallowAll() }),
        content = {
            val backdropAlpha = { progress.value.coerceIn(0f, 1f) }
            Spacer(
                Modifier
                    .fillMaxSize()
                    .then(
                        if (reduceDynamicBlur) {
                            Modifier
                        } else {
                            Modifier.optimizedHazeEffect(
                                state = hazeState,
                                style = HazeMaterials.ultraThin(MaterialTheme.colorScheme.background),
                            ) {
                                alpha = backdropAlpha()
                            }
                        },
                    )
                    .graphicsLayer { alpha = backdropAlpha() }
                    .background(scrimColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = open,
                        onClick = onDismiss,
                    ),
            )
            Box(
                Modifier
                    .then(if (reduceMotion) Modifier else Modifier.shadow(20.dp, cardShape, clip = false))
                    .pointerInput(Unit) {}
                    .clip(cardShape)
                    .background(plate)
                    .border(0.5.dp, hairline, cardShape)
                    .then(
                        if (onPreviewClick != null) {
                            Modifier.clickable(enabled = open) { onPreviewClick(menuItem) }
                        } else {
                            Modifier
                        },
                    )
                    .padding(platePadding),
            ) {
                heldItem.image?.let { image ->
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Column(
                Modifier
                    .pointerInput(Unit) {}
                    .clip(menuShape)
                    .background(plate)
                    .border(0.5.dp, hairline, menuShape),
            ) {
                actions(menuItem)
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val margin = 12.dp.roundToPx()
        val gap = 10.dp.roundToPx()
        val pad = platePadding.roundToPx()
        val top = insets.getTop(this) + 8.dp.roundToPx()
        val bottom = height - insets.getBottom(this) - 8.dp.roundToPx()

        val backdrop = measurables[0].measure(Constraints.fixed(width, height))

        val itemBounds = heldItem.bounds.translate(-overlayOffset)
        val plateW = (itemBounds.width.roundToInt() + pad * 2).coerceAtLeast(1)
        val plateH = (itemBounds.height.roundToInt() + pad * 2).coerceAtLeast(1)
        val card = measurables[1].measure(Constraints.fixed(plateW, plateH))
        val liftScale = ((width - margin * 2f) / plateW).coerceAtMost(1f)
        val visualW = plateW * liftScale
        val visualH = plateH * liftScale

        val menuWidth = (width - margin * 2).coerceAtMost(280.dp.roundToPx()).coerceAtLeast(0)
        val menuMaxHeight = (bottom - top - visualH.roundToInt() - gap).coerceAtLeast(0)
        val menu = measurables[2].measure(
            Constraints(minWidth = menuWidth, maxWidth = menuWidth, maxHeight = menuMaxHeight),
        )

        val visualLeft = (itemBounds.center.x - visualW / 2f)
            .coerceIn(margin.toFloat(), (width - margin - visualW).coerceAtLeast(margin.toFloat()))
        var visualTop = (itemBounds.center.y - visualH / 2f)
            .coerceIn(top.toFloat(), (bottom - visualH).coerceAtLeast(top.toFloat()))
        val menuBelow: Boolean
        when {
            visualTop + visualH + gap + menu.height <= bottom -> menuBelow = true
            visualTop - gap - menu.height >= top -> menuBelow = false
            else -> {
                menuBelow = true
                visualTop = (bottom - menu.height - gap - visualH).coerceAtLeast(top.toFloat())
            }
        }
        val visualCenterX = visualLeft + visualW / 2f
        val visualCenterY = visualTop + visualH / 2f

        val alignEnd = visualCenterX > width / 2f
        val menuX = (if (alignEnd) visualLeft + visualW - menuWidth else visualLeft).roundToInt()
            .coerceIn(margin, (width - margin - menuWidth).coerceAtLeast(margin))
        val menuY = if (menuBelow) {
            (visualTop + visualH + gap).roundToInt()
        } else {
            (visualTop - gap - menu.height).roundToInt()
        }

        val cardX = (visualCenterX - plateW / 2f).roundToInt()
        val cardY = (visualCenterY - plateH / 2f).roundToInt()
        val fromDx = itemBounds.center.x - visualCenterX
        val fromDy = itemBounds.center.y - visualCenterY

        layout(width, height) {
            backdrop.place(0, 0)
            card.placeWithLayer(cardX, cardY) {
                val p = progress.value
                if (reduceMotion) {
                    scaleX = liftScale
                    scaleY = liftScale
                    alpha = p.coerceIn(0f, 1f)
                } else {
                    val scale = lerp(1f, liftScale, p)
                    scaleX = scale
                    scaleY = scale
                    translationX = lerp(fromDx, 0f, p)
                    translationY = lerp(fromDy, 0f, p)
                    alpha = (p * 3f).coerceIn(0f, 1f)
                }
            }
            menu.placeWithLayer(menuX, menuY) {
                val p = progress.value
                alpha = ((p - 0.1f) / 0.9f).coerceIn(0f, 1f)
                if (!reduceMotion) {
                    transformOrigin = TransformOrigin(
                        pivotFractionX = if (alignEnd) 1f else 0f,
                        pivotFractionY = if (menuBelow) 0f else 1f,
                    )
                    val scale = lerp(0.55f, 1f, p)
                    scaleX = scale
                    scaleY = scale
                    translationX = lerp(fromDx, 0f, p)
                    translationY = lerp(fromDy, 0f, p)
                }
            }
        }
    }
}

private suspend fun PointerInputScope.swallowAll() {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
        }
    }
}
