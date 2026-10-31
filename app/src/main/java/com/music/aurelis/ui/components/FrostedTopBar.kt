// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material.icons.Icons
import com.music.aurelis.ui.icons.AurelisIcons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import com.music.aurelis.R
import com.music.aurelis.data.AppSettings
import com.music.aurelis.data.ProfileRepository
import com.music.aurelis.ui.theme.stableStatusBarInsets
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlin.math.roundToInt

val TopBarContentHeight = 52.dp
val TopBarContentGap = 12.dp

private val BackInset = 54.dp
private val WordmarkInset = 96.dp
private val ActionsInset = 56.dp
private val ACCESSORY_GAP = 8.dp

private fun leadingInset(hasBack: Boolean): Dp = if (hasBack) BackInset else WordmarkInset

@Composable
fun topBarHeight(): Dp =
    stableStatusBarInsets().asPaddingValues().calculateTopPadding() + TopBarContentHeight

@Composable
fun topBarContentPadding(): Dp = topBarHeight() + TopBarContentGap

@Composable
fun FrostedTopBar(
    title: String,
    scrolled: () -> Boolean,
    modifier: Modifier = Modifier,
    transparentBackdrop: Boolean = false,
    artworkPageChrome: Boolean = false,
    backButtonHazeState: HazeState? = null,
    trailingTitle: String? = null,
    onBack: (() -> Unit)? = null,
    refreshing: Boolean = false,
    pullFraction: () -> Float = { 0f },
    accessory: (@Composable () -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val useFloatingChrome = artworkPageChrome ||
        (LocalLiquidGlassEnabled.current && isGlassSupported())
    val titleAlpha by animateFloatAsState(
        targetValue = if (scrolled() || trailingTitle != null) 1f else 0f,
        animationSpec = tween(220),
        label = "topBarTitleAlpha",
    )
    val dividerColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.outline.copy(
            alpha = when {
                transparentBackdrop -> 0f
                else -> 0.6f
            },
        ),
        animationSpec = tween(220),
        label = "topBarDivider",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (reduceDynamicBlur && !transparentBackdrop) {
                    Modifier.background(MaterialTheme.colorScheme.surface)
                } else Modifier,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(stableStatusBarInsets())
                .height(TopBarContentHeight),
        ) {
            if (onBack != null) {
                if (useFloatingChrome) {
                    ArtworkPageBackButton(
                        onClick = onBack,
                        hazeState = backButtonHazeState,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = BAR_GUTTER),
                    )
                } else {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp),
                    ) {
                        Icon(
                            AurelisIcons.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            val edge = if (useFloatingChrome) BAR_GUTTER else 4.dp
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .then(if (accessory != null) Modifier.fillMaxWidth().padding(start = edge) else Modifier)
                    .padding(end = edge),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (accessory != null) {
                    Box(Modifier.weight(1f)) {
                        AccessoryPill(
                            floating = useFloatingChrome,
                            hazeState = backButtonHazeState,
                            content = accessory,
                        )
                    }
                    Spacer(Modifier.width(ACCESSORY_GAP))
                }
                if (useFloatingChrome) {
                    ArtworkPageActions(hazeState = backButtonHazeState, content = actions)
                } else {
                    actions()
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
        RefreshPuck(refreshing = refreshing, pullFraction = pullFraction)
    }
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
private fun ArtworkPageBackButton(
    onClick: () -> Unit,
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
) {
    val shape = CircleShape
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val useLiquidGlass = LocalLiquidGlassEnabled.current && isGlassSupported()
    val contentColor = if (useLiquidGlass && !reduceDynamicBlur) {
        glassContentColor()
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(TOP_CONTROL_SIZE)
            .then(artworkPageSurface(shape = shape, hazeState = hazeState)),
    ) {
        Icon(
            AurelisIcons.ArrowBack,
            contentDescription = stringResource(R.string.back),
            tint = contentColor,
        )
    }
}

@Composable
private fun ArtworkPageActions(
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    ActionsRow(
        modifier = modifier
            .then(artworkPageSurface(shape = CircleShape, hazeState = hazeState))
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            .artworkActionEdgePadding(),
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides TOP_CONTROL_SIZE) {
            content()
        }
    }
}

@Composable
private fun ActionsRow(modifier: Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val target = TOP_CONTROL_SIZE.roundToPx()
        val gap = ACTION_GAP.toPx()
        val gaps = placeables.mapIndexed { index, placeable ->
            if (index == 0) 0 else (gap * (placeable.width.toFloat() / target).coerceIn(0f, 1f)).roundToInt()
        }
        val width = placeables.sumOf { it.width } + gaps.sum()
        val height = placeables.maxOfOrNull { it.height } ?: 0
        layout(width, height) {
            var x = 0
            placeables.forEachIndexed { index, placeable ->
                x += gaps[index]
                placeable.placeRelative(x, (height - placeable.height) / 2)
                x += placeable.width
            }
        }
    }
}

@Composable
private fun AccessoryPill(
    floating: Boolean,
    hazeState: HazeState?,
    content: @Composable () -> Unit,
) {
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val glass = floating && LocalLiquidGlassEnabled.current && isGlassSupported() && !reduceDynamicBlur
    val shape = remember { SquirclePillShape() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(TOP_CONTROL_SIZE)
            .then(
                if (floating) {
                    artworkPageSurface(shape = shape, hazeState = hazeState)
                } else {
                    Modifier
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                },
            ),
    ) {
        CompositionLocalProvider(LocalSearchFieldOnSurface provides true) {
            if (glass) {
                val glassContent = glassContentColor()
                MaterialTheme(
                    colorScheme = MaterialTheme.colorScheme.copy(
                        onBackground = glassContent,
                        onSurfaceVariant = glassContent.copy(alpha = ACCESSORY_HINT_ALPHA),
                        primary = glassContent,
                    ),
                    content = content,
                )
            } else {
                content()
            }
        }
    }
}

private const val ACCESSORY_HINT_ALPHA = 0.6f

private fun Modifier.artworkActionEdgePadding(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0))
    val oneActionWidth = TOP_CONTROL_SIZE.roundToPx()
    val extraActionFraction = ((placeable.width - oneActionWidth).toFloat() / oneActionWidth)
        .coerceIn(0f, 1f)
    val edgePadding = (ACTION_EDGE_INSET.roundToPx() * extraActionFraction).roundToInt()
    val width = (placeable.width + edgePadding * 2).coerceIn(constraints.minWidth, constraints.maxWidth)

    layout(width, placeable.height) {
        placeable.placeRelative(edgePadding, 0)
    }
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
private fun artworkPageSurface(
    shape: CornerBasedShape,
    hazeState: HazeState?,
): Modifier {
    val container = MaterialTheme.colorScheme.surface
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val useLiquidGlass = LocalLiquidGlassEnabled.current && isGlassSupported()

    return Modifier
        .clip(shape)
        .then(
            when {
                useLiquidGlass -> Modifier.liquidGlass(shape)
                reduceDynamicBlur || hazeState == null -> Modifier.background(container)
                else -> Modifier.optimizedHazeEffect(
                    state = hazeState,
                    style = HazeMaterials.regular(container),
                )
            },
        )
        .border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, shape)
}

/**
 * Perfil local: foto elegida por el usuario + nombre, persistidos en DataStore.
 * Sin login, sin OAuth, sin servidores.
 */
@Composable
fun TopBarProfileButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profilePhoto by ProfileRepository.photoUri.collectAsStateWithLifecycle()
    IconButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        if (profilePhoto != null) {
            AsyncImage(
                model = profilePhoto,
                contentDescription = stringResource(R.string.profile),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(AVATAR_SIZE)
                    .clip(CircleShape)
                    .thumbnailBorder(CircleShape),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(AVATAR_SIZE)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .thumbnailBorder(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = stringResource(R.string.profile),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun RefreshPuck(refreshing: Boolean, pullFraction: () -> Float, modifier: Modifier = Modifier) {
    val currentFraction by rememberUpdatedState(pullFraction)
    val pulling by remember { derivedStateOf { currentFraction() > 0f } }
    if (!refreshing && !pulling) return

    val travel = with(LocalDensity.current) { (PUCK_REST + PUCK_SIZE).toPx() }
    Box(
        modifier = modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minHeight = 0))
                layout(placeable.width, 0) { placeable.place(0, 0) }
            }
            .fillMaxWidth()
            .height(PUCK_REST + PUCK_SIZE + PUCK_OVERSHOOT + 12.dp)
            .clipToBounds(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    val f = currentFraction().coerceIn(0f, 2f)
                    val eased = if (f <= 1f) f else 1f + (f - 1f) * 0.35f
                    translationY = travel * eased - PUCK_SIZE.toPx()
                    alpha = (f * 3f).coerceAtMost(1f)
                }
                .shadow(6.dp, CircleShape)
                .size(PUCK_SIZE)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            val indicatorModifier = Modifier.size(22.dp)
            if (refreshing) {
                CircularProgressIndicator(
                    modifier = indicatorModifier,
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.5.dp,
                    strokeCap = StrokeCap.Round,
                )
            } else {
                CircularProgressIndicator(
                    progress = { (currentFraction() * 0.8f).coerceIn(0f, 0.8f) },
                    modifier = indicatorModifier.graphicsLayer {
                        rotationZ = currentFraction().coerceIn(0f, 2f) * 180f
                    },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent,
                    strokeWidth = 2.5.dp,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                )
            }
        }
    }
}

private val PUCK_SIZE = 40.dp
private val TOP_CONTROL_SIZE = 44.dp
private val ACTION_GAP = 10.dp
private val ACTION_EDGE_INSET = 1.5.dp
private val PUCK_REST = 16.dp
private val PUCK_OVERSHOOT = 20.dp
private val AVATAR_SIZE = 28.dp
