package com.music.aura.data

import com.music.aura.data.model.LocalSong
import java.util.Locale

enum class LocalBrowseType { ALBUM, ARTIST, PLAYLIST, OTHER }

enum class LocalMusicSort {
    TITLE_ASC,
    TITLE_DESC,
    DATE_ADDED,
    DATE_MODIFIED,
}

data class LocalGroup(
    val browseId: String,
    val title: String,
    val subtitle: String,
    val artworkUri: android.net.Uri?,
    val type: LocalBrowseType,
    val songs: List<LocalSong>,
)

object LocalGroups {
    const val PREFIX = "local:group:"
    private const val ALBUM = "album"
    private const val ARTIST = "artist"

    fun albumPageId(folderId: String, name: String): String = pageId(ALBUM, folderId, name)
    fun artistPageId(folderId: String, name: String): String = pageId(ARTIST, folderId, name)

    private fun pageId(kind: String, folderId: String, name: String) =
        "$PREFIX$kind:${folderId.removePrefix("local:")}:$name"

    private class Parsed(val kind: String, val folderId: String, val name: String)

    private fun parse(browseId: String?): Parsed? {
        if (browseId == null || !browseId.startsWith(PREFIX)) return null
        val parts = browseId.removePrefix(PREFIX).split(':', limit = 3)
        if (parts.size != 3 || (parts[0] != ALBUM && parts[0] != ARTIST) || parts[1].isEmpty()) return null
        return Parsed(parts[0], "local:" + parts[1], parts[2])
    }

    fun folderOf(browseId: String?): String? = parse(browseId)?.folderId

    fun typeOf(browseId: String): LocalBrowseType? =
        parse(browseId)?.let { if (it.kind == ALBUM) LocalBrowseType.ALBUM else LocalBrowseType.OTHER }

    fun isArtist(browseId: String?): Boolean = parse(browseId)?.kind == ARTIST

    fun songsOf(browseId: String, folderSongs: List<LocalSong>): List<LocalSong> {
        val group = parse(browseId) ?: return emptyList()
        return if (group.kind == ALBUM) {
            folderSongs.filter { it.album == group.name }.sortedForAlbum()
        } else {
            folderSongs.filter { it.artist == group.name }.sortedByTitle()
        }
    }

    fun albumsOf(folderId: String, songs: List<LocalSong>): List<LocalGroup> =
        songs
            .groupBy { it.album }
            .mapNotNull { (name, group) ->
                if (name.isNullOrBlank()) return@mapNotNull null
                LocalGroup(
                    browseId = albumPageId(folderId, name),
                    title = name,
                    subtitle = group.firstOrNull()?.artist.orEmpty(),
                    artworkUri = group.firstNotNullOfOrNull { it.artworkUri }?.let { android.net.Uri.parse(it) },
                    type = LocalBrowseType.ALBUM,
                    songs = group.sortedForAlbum(),
                )
            }
            .sortedBy { it.title.lowercase(Locale.ROOT) }

    fun artistsOf(folderId: String, songs: List<LocalSong>): List<LocalGroup> =
        songs.groupBy { it.artist }
            .map { (name, group) ->
                LocalGroup(
                    browseId = artistPageId(folderId, name),
                    title = name,
                    subtitle = "",
                    artworkUri = null,
                    type = LocalBrowseType.ARTIST,
                    songs = group.sortedByTitle(),
                )
            }
            .sortedBy { it.title.lowercase(Locale.ROOT) }
}

fun LocalSong.matchesSearch(query: String): Boolean =
    title.contains(query, ignoreCase = true) ||
        artist.contains(query, ignoreCase = true) ||
        album?.contains(query, ignoreCase = true) == true

fun List<LocalSong>.sortedForLibrary(order: LocalMusicSort): List<LocalSong> = when (order) {
    LocalMusicSort.TITLE_ASC -> sortedByTitle()
    LocalMusicSort.TITLE_DESC -> sortedWith(compareByDescending<LocalSong> { it.title.lowercase(Locale.ROOT) })
    LocalMusicSort.DATE_ADDED -> sortedWith(
        compareByDescending<LocalSong> { it.dateAddedMs }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title },
    )
    LocalMusicSort.DATE_MODIFIED -> sortedWith(
        compareByDescending<LocalSong> { it.dateAddedMs }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title },
    )
}

private fun List<LocalSong>.sortedByTitle(): List<LocalSong> =
    sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })

fun List<LocalSong>.sortedForAlbum(): List<LocalSong> {
    if (none { it.trackNumber != null }) return this
    return sortedWith(
        compareBy<LocalSong> { it.trackNumber ?: Int.MAX_VALUE }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title },
    )
}

fun List<LocalSong>.useTrackTags(): Boolean {
    if (isEmpty() || none { it.trackNumber != null }) return false
    val album = first().album
    return all { it.album == album }
}
