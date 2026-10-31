// Aurelis © 2026. GPL-3.0-or-later
@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.music.aurelis.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.aurelis.R
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.components.floatingtabbar.FloatingTabBar
import com.music.aurelis.ui.components.floatingtabbar.FloatingTabBarDefaults
import com.music.aurelis.ui.components.floatingtabbar.FloatingTabBarScrollConnection
import com.music.aurelis.ui.haptics.Haptic
import com.music.aurelis.ui.haptics.rememberHaptics
import com.music.aurelis.ui.player.PlayerDock
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.fillMaxSize

@Composable
fun GlassNavBar(
    tabs: List<BottomTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    scrollConnection: FloatingTabBarScrollConnection,
    song: LocalSong?,
    isPlaying: Boolean,
    isLoading: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onExpand: () -> Unit,
    controlsLocked: Boolean = false,
    onBlockedControl: () -> Unit = {},
    dock: PlayerDock? = null,
    pull: MiniPlayerPull? = null,
    modifier: Modifier = Modifier,
) {
    val pillShape = remember { RoundedCornerShape(percent = 50) }
    val contentColor = glassContentColor()
    val selectedColor = contentColor
    val unselectedColor = contentColor.copy(alpha = 0.65f)
    val haptics = rememberHaptics()

    val currentOnTabSelected by rememberUpdatedState(onTabSelected)
    val standaloneIndex = tabs.lastIndex

    val glassSurface: @Composable () -> Modifier = { Modifier.liquidGlass(shape = pillShape) }

    FloatingTabBar(
        selectedTabKey = selectedIndex,
        scrollConnection = scrollConnection,
        modifier = modifier
            .windowInsetsPadding(floatingBarInsets)
            .padding(horizontal = NAV_BAR_GUTTER)
            .padding(bottom = NAV_BAR_LIFT)
            .fillMaxWidth(),
        tabBarContentModifier = glassSurface,
        inlineAccessory = song?.let { current ->
            { accessoryModifier, _ ->
                GlassNowPlaying(
                    song = current,
                    isInline = true,
                    isPlaying = isPlaying,
                    isLoading = isLoading,
                    contentColor = contentColor,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onExpand = onExpand,
                    controlsLocked = controlsLocked,
                    onBlockedControl = onBlockedControl,
                    dock = dock,
                    pull = pull,
                    modifier = accessoryModifier.then(glassSurface()),
                )
            }
        },
        expandedAccessory = song?.let { current ->
            { accessoryModifier, _ ->
                GlassNowPlaying(
                    song = current,
                    isInline = false,
                    isPlaying = isPlaying,
                    isLoading = isLoading,
                    contentColor = contentColor,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onExpand = onExpand,
                    controlsLocked = controlsLocked,
                    onBlockedControl = onBlockedControl,
                    dock = dock,
                    pull = pull,
                    modifier = accessoryModifier.fillMaxWidth().then(glassSurface()),
                )
            }
        },
        colors = FloatingTabBarDefaults.colors(
            backgroundColor = Color.Transparent,
            accessoryBackgroundColor = Color.Transparent,
            indicatorColor = glassIndicatorColor(),
        ),
        elevations = FloatingTabBarDefaults.elevations(
            inlineElevation = 0.dp,
            expandedElevation = 0.dp,
        ),
        sizes = FloatingTabBarDefaults.sizes(
            tabBarContentPadding = PaddingValues(PILL_INSET),
            tabExpandedContentPadding = PaddingValues(vertical = TAB_VERTICAL_PADDING),
            inlineComponentSpacing = INLINE_SPACING,
            inlineBarPadding = PaddingValues(
                start = INLINE_GUTTER - NAV_BAR_GUTTER,
                end = INLINE_GUTTER - NAV_BAR_GUTTER,
                bottom = INLINE_LIFT,
            ),
        ),
        contentKey = remember(selectedIndex, tabs, contentColor) {
            listOf(selectedIndex, tabs, contentColor)
        },
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = index == selectedIndex
            val tint = if (isSelected) selectedColor else unselectedColor
            val onClick = {
                if (!isSelected) haptics.play(Haptic.Select)
                currentOnTabSelected(index)
            }
            if (index == standaloneIndex) {
                standaloneTab(
                    key = index,
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = tint,
                            modifier = Modifier.size(TAB_ICON_SIZE),
                        )
                    },
                    onClick = onClick,
                )
            } else {
                tab(
                    key = index,
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = tint,
                            modifier = Modifier.size(TAB_ICON_SIZE),
                        )
                    },
                    title = {
                        Text(
                            text = tab.label,
                            style = tabLabelStyle(),
                            color = tint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = TAB_ICON_LABEL_GAP),
                        )
                    },
                    onClick = onClick,
                )
            }
        }
    }
}

@Composable
private fun GlassNowPlaying(
    song: LocalSong,
    isInline: Boolean,
    isPlaying: Boolean,
    isLoading: Boolean,
    contentColor: Color,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onExpand: () -> Unit,
    controlsLocked: Boolean,
    onBlockedControl: () -> Unit,
    dock: PlayerDock?,
    pull: MiniPlayerPull?,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val pressSource = remember { MutableInteractionSource() }
    val isPressed by pressSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 1.04f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "accessoryPressScale",
    )

    val titleStyle = MaterialTheme.typography.titleMedium.copy(
        fontSize = ACCESSORY_TITLE_SIZE,
        fontWeight = ACCESSORY_TITLE_WEIGHT,
        letterSpacing = ACCESSORY_TITLE_TRACKING,
    )
    val artistStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = ACCESSORY_ARTIST_SIZE,
        letterSpacing = ACCESSORY_ARTIST_TRACKING,
    )
    val glyphSlot = if (isInline) 32.dp else 40.dp
    val glyphSize = if (isInline) 24.dp else EXPANDED_GLYPH_SIZE

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .miniPlayerGestures(
                pull = pull,
                onNext = {
                    haptics.play(Haptic.SkipNext)
                    onNext()
                },
                onPrevious = {
                    haptics.play(Haptic.SkipPrevious)
                    onPrevious()
                },
                locked = controlsLocked,
                onBlocked = onBlockedControl,
            )
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .then(modifier)
            .clickable(
                interactionSource = pressSource,
                indication = null,
                onClick = onExpand,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = if (isInline) 8.dp else 12.dp,
                    top = 4.dp,
                    bottom = 4.dp,
                ),
        ) {
            Box(
                modifier = Modifier
                    .size(ACCESSORY_ART_SIZE)
                    .playerDockArt(dock, ACCESSORY_ART_CORNER)
                    .clip(RoundedCornerShape(ACCESSORY_ART_CORNER))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                AsyncImage(
                    model = song.artworkUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.width(if (isInline) 8.dp else 9.dp))
            Column(Modifier.weight(1f)) {
                val secondary = contentColor.copy(alpha = 0.7f)
                ExplicitSongTitle(
                    song = song,
                    style = titleStyle,
                    color = contentColor,
                    badgeColor = secondary,
                    fadeOverflow = true,
                )
                FadingLine(
                    text = song.artist,
                    style = artistStyle,
                    color = secondary,
                )
            }
            if (isLoading) {
                Box(Modifier.size(glyphSlot), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = contentColor,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(if (isInline) 18.dp else 20.dp),
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        haptics.play(if (isPlaying) Haptic.Pause else Haptic.Resume)
                        onPlayPause()
                    },
                    modifier = Modifier.size(glyphSlot),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                        tint = contentColor,
                        modifier = Modifier.size(glyphSize),
                    )
                }
            }
            if (!isInline) {
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        haptics.play(Haptic.SkipNext)
                        onNext()
                    },
                    enabled = !controlsLocked,
                    modifier = Modifier.size(glyphSlot),
                ) {
                    MiniPlayerNextGlyph(
                        tint = contentColor.copy(alpha = if (controlsLocked) 0.3f else 1f),
                    )
                }
            }
        }
    }
}

private val INLINE_GUTTER = 28.dp
private val INLINE_SPACING = 14.dp
private val INLINE_LIFT = 7.dp
private val ACCESSORY_TITLE_SIZE = 13.sp
private val ACCESSORY_TITLE_WEIGHT = FontWeight.W700
private val ACCESSORY_TITLE_TRACKING = 0.15.sp
private val ACCESSORY_ARTIST_SIZE = 12.sp
private val ACCESSORY_ARTIST_TRACKING = 0.35.sp
private val ACCESSORY_ART_SIZE = 30.4.dp
private val ACCESSORY_ART_CORNER = 5.7.dp
private val EXPANDED_GLYPH_SIZE = 29.dp
