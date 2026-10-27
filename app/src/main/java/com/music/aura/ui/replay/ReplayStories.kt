package com.music.aura.ui.replay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import coil3.compose.AsyncImage
import com.music.aura.R
import com.music.aura.data.stats.ReplaySummary
import com.music.aura.ui.player.MeshGradientBackground
import com.music.aura.ui.player.MeshPalette
import com.music.aura.ui.player.rememberArtworkColors
import com.music.aura.ui.theme.AccentRed
import kotlinx.coroutines.launch

/**
 * The Replay as a run of full-screen cards you tap through.
 */
@Composable
fun ReplayStories(
    summary: ReplaySummary,
    start: ReplayStoryPage,
    onClose: () -> Unit,
    onShare: (ReplayStoryPage) -> Unit,
    paused: Boolean = false,
) {
    val pages = remember(summary) {
        ReplayStoryPage.ordered.filter { page ->
            when (page) {
                ReplayStoryPage.ALBUMS -> summary.albums.isNotEmpty()
                ReplayStoryPage.GENRES -> summary.genres.isNotEmpty()
                else -> true
            }
        }
    }
    val pagerState = rememberPagerState(
        initialPage = pages.indexOf(start).coerceAtLeast(0),
        pageCount = { pages.size },
    )
    val scope = rememberCoroutineScope()
    var held by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val current by remember { derivedStateOf { pagerState.settledPage } }

    fun goTo(target: Int, animate: Boolean) {
        val next = target.coerceIn(0, pages.lastIndex)
        scope.launch {
            progress.snapTo(0f)
            if (animate) pagerState.animateScrollToPage(next) else pagerState.scrollToPage(next)
        }
    }

    fun step(forward: Boolean) {
        val currentPage = pagerState.settledPage
        if (forward && currentPage == pages.lastIndex) {
            scope.launch { onClose() }
            return
        }
        goTo(currentPage + if (forward) 1 else -1, animate = false)
    }

    LaunchedEffect(current) { progress.snapTo(0f) }
    LaunchedEffect(current, held, paused) {
        if (held || paused) return@LaunchedEffect
        if (current == pages.lastIndex) {
            progress.animateTo(1f, tween(PAGE_MILLIS.toInt(), easing = LinearEasing))
            scope.launch { onClose() }
            return@LaunchedEffect
        }
        val remaining = ((1f - progress.value) * PAGE_MILLIS).toInt().coerceAtLeast(0)
        progress.animateTo(1f, tween(remaining, easing = LinearEasing))
        goTo(current + 1, animate = true)
    }

    val page = pages.getOrElse(current) { ReplayStoryPage.INTRO }
    val artwork = summary.storyArtwork(page)
    val palette = rememberArtworkColors(artwork).rotated(storyHue(page))

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        StoryFrame {
            Stage(
                pages = pages,
                pagerState = pagerState,
                summary = summary,
                current = current,
                progress = progress,
                onHold = { held = it },
                onStep = ::step,
                onClose = onClose,
                onShare = onShare,
                palette = palette,
                artwork = artwork,
                page = page,
            )
        }
    }
}

@Composable
private fun StoryFrame(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val fitsByWidth = maxWidth / maxHeight < STORY_ASPECT
        Box(
            Modifier
                .then(if (fitsByWidth) Modifier.fillMaxWidth() else Modifier.fillMaxHeight())
                .aspectRatio(STORY_ASPECT),
        ) {
            content()
        }
    }
}

@Composable
private fun Stage(
    pages: List<ReplayStoryPage>,
    pagerState: PagerState,
    summary: ReplaySummary,
    current: Int,
    progress: Animatable<Float, *>,
    onHold: (Boolean) -> Unit,
    onStep: (Boolean) -> Unit,
    onClose: () -> Unit,
    onShare: (ReplayStoryPage) -> Unit,
    palette: MeshPalette,
    artwork: String?,
    page: ReplayStoryPage,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF17171A))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onHold(true)
                        tryAwaitRelease()
                        onHold(false)
                    },
                    onLongPress = {},
                    onTap = { offset -> onStep(offset.x >= size.width * BACK_ZONE) },
                )
            },
    ) {
        MeshGradientBackground(palette = palette, trackKey = page.name, animated = false)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.45f),
                        0.35f to Color.Black.copy(alpha = 0.10f),
                        1.0f to Color.Black.copy(alpha = 0.34f),
                    ),
                ),
        )

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
            StoryPage(page = pages[index], summary = summary, onShare = onShare)
        }

        StoryChrome(
            label = summary.localizedLabel(LocalContext.current),
            count = pages.size,
            current = current,
            progress = progress.value,
            onClose = onClose,
        )
    }
}

@Composable
private fun StoryChrome(
    label: String,
    count: Int,
    current: Int,
    progress: Float,
    onClose: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.End) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.close_replay),
                tint = Color.White,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClose)
                    .padding(4.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(count) { index ->
                Segment(
                    fraction = when {
                        index < current -> 1f
                        index > current -> 0f
                        else -> progress
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (label.length == 4 && label.all { it.isDigit() }) {
                    "Replay ${label.takeLast(2)}"
                } else {
                    "Replay · $label"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W700,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Aura",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W700,
                color = Color.White.copy(alpha = 0.9f),
            )
        }
    }
}

@Composable
private fun Segment(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(2.5.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = 0.28f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(2.5.dp)
                .background(Color.White),
        )
    }
}

@Composable
private fun StoryPage(
    page: ReplayStoryPage,
    summary: ReplaySummary,
    onShare: (ReplayStoryPage) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = CHROME_HEIGHT, bottom = 18.dp),
    ) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.fillMaxSize()) {
                val headline = summary.storyHeadline(LocalContext.current, page)
                when (page) {
                    ReplayStoryPage.INTRO -> Intro(summary, headline)
                    ReplayStoryPage.MINUTES -> Minutes(summary, headline)
                    ReplayStoryPage.SONGS ->
                        Leaderboard(headline, summary.songRows(STORY_ROWS), circular = false)
                    ReplayStoryPage.ARTISTS ->
                        Leaderboard(headline, summary.artistRows(STORY_ROWS), circular = true)
                    ReplayStoryPage.ALBUMS ->
                        Leaderboard(headline, summary.albumRows(STORY_ROWS), circular = false)
                    ReplayStoryPage.GENRES -> Genres(summary, headline)
                    ReplayStoryPage.HABITS -> Habits(summary, headline)
                    ReplayStoryPage.SUMMARY -> Recap(summary, headline)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable { onShare(page) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.IosShare,
                    contentDescription = stringResource(R.string.share_my_replay),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun Headline(parts: List<HeadlineRun>) {
    Text(
        text = buildAnnotatedString {
            parts.forEach { run ->
                withStyle(
                    SpanStyle(
                        fontWeight = if (run.bold) FontWeight.W800 else FontWeight.W600,
                        color = if (run.bold) Color.White else Color.White.copy(alpha = 0.62f),
                    ),
                ) { append(run.text) }
            }
        },
        style = MaterialTheme.typography.displayLarge,
        fontSize = 30.sp,
        lineHeight = 37.sp,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ColumnScope.Intro(summary: ReplaySummary, headline: List<HeadlineRun>) {
    Headline(headline)
    Spacer(Modifier.weight(1f))
    ArtworkCollage(summary)
    Spacer(Modifier.weight(1f))
    Text(
        text = stringResource(R.string.replay_private_counting),
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.5f),
    )
}

@Composable
private fun ColumnScope.Minutes(summary: ReplaySummary, headline: List<HeadlineRun>) {
    val context = LocalContext.current
    Headline(headline)
    Spacer(Modifier.weight(1f))
    ArtworkCollage(summary)
    Spacer(Modifier.weight(1f))
    Text(
        text = buildString {
            if (summary.hours >= 1) {
                append(context.getString(R.string.replay_hours_across, grouped(summary.hours)))
            } else {
                append(context.getString(R.string.replay_across))
                append(" ")
            }
            if (summary.hours >= 1) append(", ")
            append(context.resources.getQuantityString(
                R.plurals.replay_play_count,
                summary.totalPlays,
                grouped(summary.totalPlays.toLong()),
            ))
            if (summary.peakHour != null) {
                append(", mostly around ")
                append(formatHour(context, summary.peakHour!!))
                append(".")
            } else {
                append(".")
            }
        },
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White.copy(alpha = 0.62f),
    )
}

@Composable
private fun ColumnScope.Leaderboard(
    headline: List<HeadlineRun>,
    rows: List<ReplayRow>,
    circular: Boolean,
) {
    val context = LocalContext.current
    val lead = rows.firstOrNull() ?: return
    val shape = if (circular) CircleShape else RoundedCornerShape(10.dp)
    Headline(headline)
    Spacer(Modifier.height(18.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Cover(lead.artworkUri, lead.title, 116.dp, shape, elevated = true)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = lead.title,
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 26.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.W800,
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            lead.subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${formatListening(context, lead.ms)} · " + context.resources.getQuantityString(
                    R.plurals.replay_play_count,
                    lead.plays,
                    grouped(lead.plays.toLong()),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.55f),
            )
        }
    }
    Spacer(Modifier.weight(1f))
    rows.drop(1).forEach { row ->
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RankBadge(row.rank, AccentRed)
            Cover(row.artworkUri, row.title, 36.dp, shape)
            Spacer(Modifier.width(12.dp))
            Text(
                text = row.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.W600,
                color = Color.White.copy(alpha = 0.92f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatListening(context, row.ms),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun ColumnScope.Genres(summary: ReplaySummary, headline: List<HeadlineRun>) {
    val context = LocalContext.current
    val rows = summary.genreRows(STORY_ROWS)
    val lead = rows.firstOrNull() ?: return
    Headline(headline)
    Spacer(Modifier.weight(1f))
    Text(
        text = lead.title,
        style = MaterialTheme.typography.displayLarge,
        fontSize = 60.sp,
        lineHeight = 62.sp,
        fontWeight = FontWeight.W800,
        color = Color.White,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    Text(
        text = formatListening(context, lead.ms),
        style = MaterialTheme.typography.titleMedium,
        color = Color.White.copy(alpha = 0.6f),
    )
    Spacer(Modifier.height(22.dp))
    rows.drop(1).forEach { row ->
        Row(
            Modifier.fillMaxWidth().padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RankBadge(row.rank, AccentRed)
            Text(
                text = row.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W600,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatListening(context, row.ms),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.45f),
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ColumnScope.Habits(summary: ReplaySummary, headline: List<HeadlineRun>) {
    val context = LocalContext.current
    Headline(headline)
    Spacer(Modifier.weight(1f))
    if (summary.distinctAlbums > 0) {
        BigStat(grouped(summary.distinctAlbums.toLong()), stringResource(R.string.different_albums))
    }
    summary.busiestDay?.let {
        BigStat(
            formatDay(context, it),
            stringResource(R.string.your_biggest_day, formatListening(context, summary.busiestDayMs)),
        )
    }
    summary.peakHour?.let {
        BigStat(formatHour(context, it), stringResource(R.string.when_you_listen_the_most))
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun BigStat(value: String, label: String) {
    Column(Modifier.padding(bottom = 22.dp)) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.W800,
            color = Color.White,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.58f),
        )
    }
}

@Composable
private fun ColumnScope.Recap(summary: ReplaySummary, headline: List<HeadlineRun>) {
    Headline(headline)
    Spacer(Modifier.height(20.dp))
    RecapLine(stringResource(R.string.minutes), formatMinutes(summary.totalMs))
    summary.songs.firstOrNull()?.let { RecapLine(stringResource(R.string.top_song), it.title) }
    summary.artists.firstOrNull()?.let { RecapLine(stringResource(R.string.top_artist), it.title) }
    summary.albums.firstOrNull()?.let { RecapLine(stringResource(R.string.top_album), it.title) }
    summary.genres.firstOrNull()?.let { RecapLine(stringResource(R.string.top_genre), it.title) }
    Spacer(Modifier.weight(1f))
    Text(
        text = stringResource(R.string.tap_share_for_picture),
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White.copy(alpha = 0.55f),
    )
}

@Composable
private fun RecapLine(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.width(96.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.W700,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Cover(
    uri: android.net.Uri?,
    fallbackText: String,
    size: Dp,
    shape: Shape,
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
) {
    val base = modifier
        .size(size)
        .let { if (elevated) it.shadow(18.dp, shape, clip = false) else it }
        .clip(shape)
    when {
        uri != null -> AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = base,
        )
        fallbackText.isNotBlank() -> Box(base) { InitialTile(fallbackText, size, shape) }
        else -> Box(base.background(Color.White.copy(alpha = 0.10f)))
    }
}

@Composable
private fun ArtworkCollage(summary: ReplaySummary, modifier: Modifier = Modifier) {
    val covers = remember(summary) {
        summary.songs.mapNotNull { it.artworkUri }.distinct().take(3)
    }
    val faces = remember(summary) {
        summary.artists.mapNotNull { it.artworkUri }.distinct()
            .filterNot { it in covers }
            .take(3)
    }
    if (covers.isEmpty() && faces.isEmpty()) return

    BoxWithConstraints(modifier.fillMaxWidth().height(300.dp)) {
        val w = maxWidth
        val h = maxHeight
        val squares = listOf(
            Triple(0.26f to 0.34f, 168.dp, -3f),
            Triple(0.05f to 0.10f, 88.dp, -9f),
            Triple(0.62f to 0.04f, 72.dp, 7f),
        )
        val circles = listOf(
            Triple(0.02f to 0.62f, 62.dp, 0f),
            Triple(0.70f to 0.34f, 76.dp, 0f),
            Triple(0.44f to 0.78f, 66.dp, 0f),
        )
        covers.forEachIndexed { index, uri ->
            val (position, size, angle) = squares[index]
            Cover(
                uri = uri,
                fallbackText = "",
                size = size,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .offset(x = w * position.first, y = h * position.second)
                    .rotate(angle),
                elevated = true,
            )
        }
        faces.forEachIndexed { index, uri ->
            val (position, size, _) = circles[index]
            Cover(
                uri = uri,
                fallbackText = "",
                size = size,
                shape = CircleShape,
                modifier = Modifier.offset(x = w * position.first, y = h * position.second),
                elevated = true,
            )
        }
    }
}

private fun MeshPalette.rotated(degrees: Float): MeshPalette {
    if (degrees == 0f) return this
    return MeshPalette(
        colors.map { color ->
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(color.toArgb(), hsl)
            hsl[0] = (hsl[0] + degrees) % 360f
            Color(ColorUtils.HSLToColor(hsl))
        },
    )
}

private val CHROME_HEIGHT = 96.dp
private const val STORY_ROWS = 5
private const val STORY_ASPECT = 9f / 16f
private const val PAGE_MILLIS = 14_000f
private const val BACK_ZONE = 0.32f
