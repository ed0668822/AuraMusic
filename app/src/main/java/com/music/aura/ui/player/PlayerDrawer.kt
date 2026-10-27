package com.music.aura.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.aura.data.SettingsRepository
import com.music.aura.ui.components.optimizedHazeEffect
import com.music.aura.ui.utils.containSheetGestures
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlin.math.roundToInt

internal val DRAWER_SHAPE = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
internal val DRAWER_MAX_WIDTH = 640.dp
internal val ROW_SHAPE = RoundedCornerShape(16.dp)
internal val SCRIM_COLOR = Color.Black.copy(alpha = 0.5f)

private const val DISMISS_DRAG_FRACTION = 0.25f

/**
 * Whether the drawer's nested scroll connection follows list scroll leftovers.
 * Set to false for drawers whose content does not scroll internally.
 */
internal var drawerFollowsListScroll = true

/**
 * The bottom-edge drawer shell every player sheet in this package puts up.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun PlayerDrawer(
    hazeState: HazeState,
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    titleGap: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val reduceDynamicBlur by SettingsRepository.reduceDynamicBlur.collectAsStateWithLifecycle()

    var drag by remember { mutableFloatStateOf(0f) }
    var height by remember { mutableIntStateOf(0) }
    val offset by animateFloatAsState(
        targetValue = drag,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "playerDrawerOffset",
    )
    val scrimAlpha = if (height > 0) (1f - offset / height).coerceIn(0f, 1f) else 1f

    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    val dismissOnRelease: () -> Unit = {
        if (height > 0 && drag > height * DISMISS_DRAG_FRACTION) onDismiss() else drag = 0f
    }
    val drawerScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (drag <= 0f || available.y >= 0f) return Offset.Zero
                val used = available.y.coerceAtLeast(-drag)
                drag += used
                return Offset(0f, used)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) drag += available.y
                if (drag > 0f && available.y < 0f) {
                    val used = available.y.coerceAtLeast(-drag)
                    drag += used
                    return Offset(0f, used)
                }
                return available
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (drag <= 0f) return Velocity.Zero
                dismissOnRelease()
                return available
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity) = available
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .containSheetGestures()
            .background(SCRIM_COLOR.copy(alpha = SCRIM_COLOR.alpha * scrimAlpha))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = shown,
            enter = slideInVertically(tween(260, easing = FastOutSlowInEasing)) { it },
            exit = slideOutVertically(tween(180)) { it },
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .widthIn(max = DRAWER_MAX_WIDTH)
                    .fillMaxWidth()
                    .onSizeChanged { height = it.height }
                    .offset { IntOffset(0, offset.roundToInt()) }
                    .clip(DRAWER_SHAPE)
                    .then(
                        if (reduceDynamicBlur) {
                            Modifier.background(Color(0xFF121212))
                        } else {
                            Modifier
                                .optimizedHazeEffect(
                                    state = hazeState,
                                    style = HazeMaterials.regular(Color(0xFF141414)),
                                )
                                .background(Color(0xFF121212).copy(alpha = 0.9f))
                        }
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {},
                    )
                    .then(if (drawerFollowsListScroll) Modifier.nestedScroll(drawerScroll) else Modifier)
                    .pointerInput(height) {
                        detectVerticalDragGestures(
                            onDragEnd = dismissOnRelease,
                            onDragCancel = { drag = 0f },
                        ) { _, delta ->
                            drag = (drag + delta).coerceAtLeast(0f)
                        }
                    }
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(top = 10.dp, bottom = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .padding(bottom = 12.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, bottom = titleGap),
                )
                content()
            }
        }
    }
}
