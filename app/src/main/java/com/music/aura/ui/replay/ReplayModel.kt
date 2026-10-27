package com.music.aura.ui.replay

import android.content.Context
import com.music.aura.R
import java.util.Locale

enum class ReplayPeriod { THIS_WEEK, THIS_MONTH, THIS_YEAR, ALL_TIME }

enum class ReplayStoryPage {
    INTRO, MINUTES, ARTISTS, SONGS, ALBUMS, GENRES, HABITS, SUMMARY;
    companion object { val ordered: List<ReplayStoryPage> = entries }
}

data class HeadlineRun(val text: String, val bold: Boolean)

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

    fun songRows(count: Int): List<ReplayRow> = songs.take(count)
    fun artistRows(count: Int): List<ReplayRow> = artists.take(count)
    fun albumRows(count: Int): List<ReplayRow> = albums.take(count)
    fun genreRows(count: Int): List<ReplayRow> = genres.take(count)

    /** The artwork URI for a story page's backdrop. */
    fun storyArtwork(page: ReplayStoryPage): String? = when (page) {
        ReplayStoryPage.INTRO, ReplayStoryPage.MINUTES -> songs.firstOrNull()?.artworkUri
        ReplayStoryPage.SONGS -> songs.firstOrNull()?.artworkUri
        ReplayStoryPage.ARTISTS -> artists.firstOrNull()?.artworkUri
        ReplayStoryPage.ALBUMS -> albums.firstOrNull()?.artworkUri
        ReplayStoryPage.GENRES -> songs.firstOrNull()?.artworkUri
        ReplayStoryPage.HABITS -> songs.firstOrNull()?.artworkUri
        ReplayStoryPage.SUMMARY -> songs.firstOrNull()?.artworkUri
    }

    /** The headline runs for a story page. */
    fun storyHeadline(context: Context, page: ReplayStoryPage): List<HeadlineRun> = when (page) {
        ReplayStoryPage.INTRO -> listOf(
            HeadlineRun(context.getString(R.string.replay_intro_headline), false),
        )
        ReplayStoryPage.MINUTES -> listOf(
            HeadlineRun(context.getString(R.string.replay_minutes_headline_prefix) + " ", false),
            HeadlineRun(formatMinutes(totalMs), true),
            HeadlineRun(" " + context.getString(R.string.replay_minutes_headline_suffix), false),
        )
        ReplayStoryPage.SONGS -> {
            val top = songs.firstOrNull()?.title ?: "—"
            listOf(
                HeadlineRun(context.getString(R.string.replay_songs_headline_prefix) + " ", false),
                HeadlineRun(top, true),
                HeadlineRun(" " + context.getString(R.string.replay_songs_headline_suffix), false),
            )
        }
        ReplayStoryPage.ARTISTS -> {
            val top = artists.firstOrNull()?.title ?: "—"
            listOf(
                HeadlineRun(context.getString(R.string.replay_artists_headline_prefix) + " ", false),
                HeadlineRun(top, true),
                HeadlineRun(" " + context.getString(R.string.replay_artists_headline_suffix), false),
            )
        }
        ReplayStoryPage.ALBUMS -> {
            val top = albums.firstOrNull()?.title ?: "—"
            listOf(
                HeadlineRun(context.getString(R.string.replay_albums_headline_prefix) + " ", false),
                HeadlineRun(top, true),
                HeadlineRun(" " + context.getString(R.string.replay_albums_headline_suffix), false),
            )
        }
        ReplayStoryPage.GENRES -> {
            val top = genres.firstOrNull()?.title ?: "—"
            listOf(
                HeadlineRun(context.getString(R.string.replay_genres_headline_prefix) + " ", false),
                HeadlineRun(top, true),
                HeadlineRun(" " + context.getString(R.string.replay_genres_headline_suffix), false),
            )
        }
        ReplayStoryPage.HABITS -> listOf(
            HeadlineRun(context.getString(R.string.replay_habits_headline), false),
        )
        ReplayStoryPage.SUMMARY -> listOf(
            HeadlineRun(context.getString(R.string.replay_summary_headline), false),
        )
    }

    fun localizedLabel(context: Context): String = when (period) {
        ReplayPeriod.THIS_WEEK -> context.getString(R.string.this_week)
        ReplayPeriod.THIS_MONTH -> context.getString(R.string.this_month)
        ReplayPeriod.THIS_YEAR -> {
            val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            year.toString()
        }
        ReplayPeriod.ALL_TIME -> context.getString(R.string.all_time)
    }

    val label: String get() = when (period) {
        ReplayPeriod.THIS_WEEK -> "This Week"
        ReplayPeriod.THIS_MONTH -> "This Month"
        ReplayPeriod.THIS_YEAR -> java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()
        ReplayPeriod.ALL_TIME -> "All Time"
    }
}

class ReplayState(
    val period: ReplayPeriod,
    val summary: ReplaySummary?,
    val loading: Boolean,
    val memberSince: String?,
)

fun grouped(value: Long): String = String.format(Locale.US, "%,d", value)

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

fun formatMinutes(ms: Long): String {
    val totalMinutes = ms / 60_000
    return grouped(totalMinutes)
}

fun formatListening(context: Context, ms: Long): String {
    val minutes = ms / 60_000
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return when {
        hours > 0 -> "${hours}h ${remainingMinutes}m"
        else -> "${minutes}m"
    }
}

fun formatHour(context: Context, hour: Int): String {
    val calendar = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, hour) }
    val format = java.text.SimpleDateFormat("h a", Locale.getDefault())
    return format.format(calendar.time)
}

fun formatDay(context: Context, day: String): String = day
