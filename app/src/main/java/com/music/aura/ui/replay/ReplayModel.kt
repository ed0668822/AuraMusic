package com.music.aura.ui.replay

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.music.aura.R
import com.music.aura.data.model.LocalSong
import com.music.aura.data.stats.ListeningStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

enum class ReplayPeriod { THIS_WEEK, THIS_MONTH, THIS_YEAR, ALL_TIME }

enum class ReplayStoryPage {
    INTRO, MINUTES, ARTISTS, SONGS, ALBUMS, GENRES, HABITS, SUMMARY;
    companion object { val ordered: List<ReplayStoryPage> = entries }
}

data class ReplayRow(
    val key: String,
    val rank: Int,
    val title: String,
    val subtitle: String?,
    val artworkUri: String?,
    val ms: Long,
    val plays: Int,
)

data class ReplayHeroCard(
    val label: String,
    val value: String,
    val detail: String?,
    val artworkUri: String?,
    val page: ReplayStoryPage,
)

data class ReplaySummary(
    val period: ReplayPeriod,
    val totalMs: Long,
    val totalPlays: Int,
    val distinctSongs: Int,
    val distinctArtists: Int,
    val distinctAlbums: Int,
    val songs: List<ReplayRow>,
    val artists: List<ReplayRow>,
    val albums: List<ReplayRow>,
    val genres: List<ReplayRow>,
    val peakHour: Int?,
    val busiestDay: String?,
    val busiestDayMs: Long,
) {
    val isEmpty: Boolean get() = totalMs == 0L && songs.isEmpty()
    val hours: Long get() = totalMs / 3_600_000
}

class ReplayState(
    val period: ReplayPeriod,
    val summary: ReplaySummary?,
    val loading: Boolean,
    val memberSince: String?,
)

@Composable
fun rememberReplayState(active: Boolean): Pair<ReplayState, (ReplayPeriod) -> Unit> {
    var period by rememberSaveable { mutableStateOf(ReplayPeriod.THIS_YEAR) }
    var summary by remember { mutableStateOf<ReplaySummary?>(null) }
    var loading by remember { mutableStateOf(true) }
    var memberSince by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        memberSince = withContext(Dispatchers.IO) {
            ListeningStats.firstMonth()?.let {
                "%02d/%02d".format(Locale.ROOT, it.monthValue, it.year % 100)
            }
        }
    }
    LaunchedEffect(period, active) {
        if (!active) return@LaunchedEffect
        loading = summary == null
        summary = withContext(Dispatchers.IO) { ListeningStats.summary(period) }
        loading = false
    }
    return ReplayState(period, summary, loading, memberSince) to { next: ReplayPeriod -> period = next }
}

fun formatListening(context: Context, ms: Long): String {
    val minutes = ms / 60_000
    return when {
        minutes < 60 -> "${minutes}m"
        minutes < 1_440 -> "${minutes / 60}h ${minutes % 60}m"
        else -> "${grouped(minutes)}m"
    }
}

fun formatMinutes(ms: Long): String = grouped(ms / 60_000)

fun grouped(value: Long): String = String.format(Locale.US, "%,d", value)

fun formatHour(context: Context, hour: Int): String = when (hour) {
    0 -> context.getString(R.string.midnight)
    12 -> context.getString(R.string.midday)
    in 1..11 -> "${hour} AM"
    else -> "${hour - 12} PM"
}

fun ReplayPeriod.localizedChip(context: Context): String = when (this) {
    ReplayPeriod.THIS_WEEK -> context.getString(R.string.replay_this_week)
    ReplayPeriod.THIS_MONTH -> context.getString(R.string.this_month)
    ReplayPeriod.THIS_YEAR -> context.getString(R.string.this_year)
    ReplayPeriod.ALL_TIME -> context.getString(R.string.all_time)
}

fun ReplayPeriod.localizedTab(context: Context): String = when (this) {
    ReplayPeriod.THIS_WEEK -> context.getString(R.string.replay_period_weekly)
    ReplayPeriod.THIS_MONTH -> context.getString(R.string.replay_period_monthly)
    ReplayPeriod.THIS_YEAR -> context.getString(R.string.replay_period_yearly)
    ReplayPeriod.ALL_TIME -> context.getString(R.string.replay_period_lifetime)
}

fun ReplaySummary.songRows(limit: Int): List<ReplayRow> = songs.take(limit)
fun ReplaySummary.artistRows(limit: Int): List<ReplayRow> = artists.take(limit)
fun ReplaySummary.albumRows(limit: Int): List<ReplayRow> = albums.take(limit)
fun ReplaySummary.genreRows(limit: Int): List<ReplayRow> = genres.take(limit)

fun ReplaySummary.cards(context: Context): List<ReplayHeroCard> = buildList {
    add(ReplayHeroCard(
        label = context.getString(R.string.minutes_listened),
        value = formatMinutes(totalMs),
        detail = "${grouped(totalPlays.toLong())} plays",
        artworkUri = songs.firstOrNull()?.artworkUri,
        page = ReplayStoryPage.MINUTES,
    ))
    songs.firstOrNull()?.let {
        add(ReplayHeroCard(
            label = context.getString(R.string.top_song),
            value = it.title,
            detail = "${it.subtitle ?: ""} · ${grouped(it.plays.toLong())} plays",
            artworkUri = it.artworkUri,
            page = ReplayStoryPage.SONGS,
        ))
    }
    artists.firstOrNull()?.let {
        add(ReplayHeroCard(
            label = context.getString(R.string.top_artist),
            value = it.title,
            detail = formatListening(context, it.ms),
            artworkUri = it.artworkUri,
            page = ReplayStoryPage.ARTISTS,
        ))
    }
    albums.firstOrNull()?.let {
        add(ReplayHeroCard(
            label = context.getString(R.string.top_album),
            value = it.title,
            detail = it.subtitle ?: "",
            artworkUri = it.artworkUri,
            page = ReplayStoryPage.ALBUMS,
        ))
    }
}

fun ReplaySummary.storyArtwork(page: ReplayStoryPage): String? {
    val pool = (songs.mapNotNull { it.artworkUri } + artists.mapNotNull { it.artworkUri } + albums.mapNotNull { it.artworkUri }).distinct()
    val pinned = when (page) {
        ReplayStoryPage.SONGS -> songs.firstOrNull()?.artworkUri
        ReplayStoryPage.ARTISTS -> artists.firstOrNull()?.artworkUri
        ReplayStoryPage.ALBUMS -> albums.firstOrNull()?.artworkUri
        else -> null
    }
    if (pinned != null) return pinned
    if (pool.isEmpty()) return null
    return pool[page.ordinal % pool.size]
}

fun storyHue(page: ReplayStoryPage): Float = when (page) {
    ReplayStoryPage.INTRO -> 0f
    ReplayStoryPage.MINUTES -> 40f
    ReplayStoryPage.SONGS -> 95f
    ReplayStoryPage.ARTISTS -> 145f
    ReplayStoryPage.ALBUMS -> 195f
    ReplayStoryPage.GENRES -> 240f
    ReplayStoryPage.HABITS -> 285f
    ReplayStoryPage.SUMMARY -> 325f
}
