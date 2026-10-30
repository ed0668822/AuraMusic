// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val SHIMMER_PERIOD_MS = 1400
private val BlockShape = RoundedCornerShape(6.dp)
private val LineShape = RoundedCornerShape(4.dp)
private val TitleWidths = listOf(0.68f, 0.46f, 0.58f, 0.74f, 0.52f)
private val SubtitleWidths = listOf(0.34f, 0.44f, 0.27f, 0.38f, 0.31f)

@Composable
fun ShimmerBox(modifier: Modifier = Modifier, shape: Shape = BlockShape) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f).compositeOver(base)
    val sweep = rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(SHIMMER_PERIOD_MS, easing = LinearEasing)),
        label = "sweep",
    )
    Box(
        modifier
            .clip(shape)
            .drawWithCache {
                val band = size.width * 0.5f
                val startX = -band + sweep.value * (size.width + band * 2)
                val brush = Brush.horizontalGradient(
                    colors = listOf(base, highlight, base),
                    startX = startX,
                    endX = startX + band,
                )
                onDrawBehind { drawRect(brush) }
            },
    )
}

@Composable
private fun SkeletonLine(fraction: Float, height: Dp, modifier: Modifier = Modifier) {
    ShimmerBox(modifier = modifier.fillMaxWidth(fraction).height(height), shape = LineShape)
}

@Composable
fun SectionHeaderSkeleton(index: Int = 0) {
    Column(Modifier.padding(horizontal = PAGE_GUTTER, vertical = 10.dp)) {
        SkeletonLine(fraction = TitleWidths[index % TitleWidths.size] * 0.7f, height = 18.dp)
    }
}

@Composable
fun SongRowSkeleton(index: Int = 0, circular: Boolean = false, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShimmerBox(Modifier.size(52.dp), if (circular) CircleShape else RoundedCornerShape(8.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            SkeletonLine(fraction = TitleWidths[index % TitleWidths.size], height = 14.dp)
            Spacer(Modifier.height(7.dp))
            SkeletonLine(fraction = SubtitleWidths[index % SubtitleWidths.size], height = 11.dp)
        }
    }
}

fun LazyListScope.songListSkeleton(
    count: Int = 8,
    keyPrefix: String = "skeleton:song",
    circular: Boolean = false,
    alpha: Float = 1f,
) {
    items(count, key = { "$keyPrefix:$it" }) { index ->
        Box(Modifier.graphicsLayer { this.alpha = alpha }) {
            SongRowSkeleton(index = index, circular = circular)
        }
    }
}

@Composable
private fun HeroShelfSkeleton() {
    Column(Modifier.padding(bottom = 26.dp)) {
        SectionHeaderSkeleton()
        BoxWithConstraints {
            val cardWidth = heroCardWidth(maxWidth)
            LazyRow(
                contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                userScrollEnabled = false,
            ) {
                items(2) {
                    ShimmerBox(
                        modifier = Modifier.width(cardWidth).aspectRatio(HERO_CARD_RATIO),
                        shape = RoundedCornerShape(18.dp),
                    )
                }
            }
        }
    }
}

fun LazyListScope.recentlyPlayedSkeleton() {
    item(key = "skeleton:recently-played") {
        Column(Modifier.padding(bottom = 26.dp)) {
            SectionHeaderSkeleton()
            BoxWithConstraints {
                val columnWidth = trackColumnWidth(maxWidth)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    userScrollEnabled = false,
                ) {
                    item {
                        Column(Modifier.width(columnWidth)) {
                            repeat(4) { CompactSongRowSkeleton(it) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShelfSkeleton(index: Int = 0, cardWidth: Dp = SHELF_CARD_WIDTH, cardCorner: Dp = 12.dp) {
    Column(Modifier.padding(bottom = 26.dp)) {
        SectionHeaderSkeleton(index = index)
        LazyRow(
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            userScrollEnabled = false,
        ) {
            items(3) { card ->
                Column(Modifier.width(cardWidth)) {
                    ShimmerBox(
                        modifier = Modifier.width(cardWidth).aspectRatio(1f),
                        shape = RoundedCornerShape(cardCorner),
                    )
                    Spacer(Modifier.height(12.dp))
                    SkeletonLine(fraction = TitleWidths[card % TitleWidths.size], height = 13.dp)
                    Spacer(Modifier.height(6.dp))
                    SkeletonLine(fraction = SubtitleWidths[card % SubtitleWidths.size], height = 11.dp)
                }
            }
        }
    }
}

fun LazyListScope.feedSkeleton(shelves: Int = 3, firstIsHero: Boolean = true) {
    if (firstIsHero) {
        item(key = "skeleton:hero") { HeroShelfSkeleton() }
        items(shelves - 1, key = { "skeleton:shelf:$it" }) { index ->
            ShelfSkeleton(index = index + 1)
        }
    } else {
        items(shelves, key = { "skeleton:shelf:$it" }) { index ->
            ShelfSkeleton(index = index)
        }
    }
}

fun LazyListScope.feedMoreSkeleton() {
    item(key = "skeleton:more") { ShelfSkeleton() }
}

fun LazyListScope.librarySkeleton() {
    item(key = "skeleton:library:shelf") { ShelfSkeleton() }
    item(key = "skeleton:library:header") { SectionHeaderSkeleton(index = 1) }
    songListSkeleton(count = 7, keyPrefix = "skeleton:library:song")
}

@Composable
private fun CompactSongRowSkeleton(index: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShimmerBox(Modifier.size(48.dp), RoundedCornerShape(7.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            SkeletonLine(fraction = TitleWidths[index % TitleWidths.size], height = 14.dp)
            Spacer(Modifier.height(6.dp))
            SkeletonLine(fraction = SubtitleWidths[index % SubtitleWidths.size], height = 11.dp)
        }
    }
}

fun LazyListScope.detailSkeleton(isArtist: Boolean) {
    if (!isArtist) {
        item(key = "skeleton:detail:songs") {
            songListSkeleton(count = 8, keyPrefix = "skeleton:detail:song", alpha = 0.4f)
        }
    } else {
        item(key = "skeleton:detail:top") {
            SectionHeaderSkeleton()
        }
    }
}
