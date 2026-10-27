package com.music.aura.ui.replay

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
