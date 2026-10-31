// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.data.model

enum class QueueTier {
    USER_QUEUE,
    CONTEXT,
    AUTOPLAY,
}

data class ArtistRef(
    val name: String,
    val browseId: String? = null,
)

enum class LikeStatus { LIKE, DISLIKE, INDIFFERENT }

enum class BrowseType { ALBUM, ARTIST, PLAYLIST, FOLDER, OTHER }

enum class PlaybackSourceType {
    HOME,
    SEARCH,
    HISTORY,
    REPLAY,
    EXPLORE,
    BROWSE,
    QUEUE,
    LOCAL_FOLDER,
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class PlaylistPrivacy(val label: String) {
    PRIVATE("Private"),
    UNLISTED("Unlisted"),
    PUBLIC("Public"),
}

data class UserPlaylist(
    val playlistId: String,
    val title: String,
    val subtitle: String,
    val thumbnailUri: String?,
)

enum class SearchFilter(val label: String) {
    ALL("All"),
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    PLAYLISTS("Playlists"),
    FOLDERS("Folders"),
}

data class LibraryPage(
    val likedSongs: List<LocalSong>,
    val librarySongs: List<LocalSong>,
    val shelves: List<LocalShelf>,
) {
    val isEmpty: Boolean
        get() = likedSongs.isEmpty() && librarySongs.isEmpty() && shelves.isEmpty()
}

data class LocalShelf(
    val title: String,
    val items: List<LocalShelfItem>,
    val subtitle: String = "",
)

data class LocalShelfItem(
    val title: String,
    val subtitle: String,
    val artworkUri: String?,
    val songId: String?,
    val albumId: String?,
)

fun LocalSong.isSameTrackAs(other: LocalSong?): Boolean {
    other ?: return false
    return title == other.title && artist == other.artist
}

fun playingRowMatcher(rows: List<LocalSong>, current: LocalSong?): (LocalSong) -> Boolean {
    current ?: return { false }
    val sameId = rows.filter { it.id == current.id }
    if (sameId.isNotEmpty()) {
        return { it.id == current.id }
    }
    val byName = rows.firstOrNull { it.isSameTrackAs(current) } ?: return { false }
    return { it == byName }
}

fun LocalSong.durationMillis(): Long = durationMs

fun String?.durationMillis(): Long {
    val parts = this?.trim()?.takeIf { it.isNotEmpty() }?.split(":") ?: return 0L
    val numbers = parts.map { it.trim().toLongOrNull() ?: return 0L }
    val seconds = when (numbers.size) {
        2 -> numbers[0] * 60 + numbers[1]
        3 -> numbers[0] * 3_600 + numbers[1] * 60 + numbers[2]
        else -> return 0L
    }
    return (seconds * 1_000).coerceAtLeast(0L)
}

const val ROW_ART_PX = 160
const val CARD_ART_PX = 480
const val HEADER_ART_PX = 720
const val NOTIFICATION_ART_PX = 544
const val PLAYER_ART_PX = 1200
