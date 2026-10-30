// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.aurelis.R
import com.music.aurelis.data.LocalGroup
import com.music.aurelis.data.LocalGroups
import com.music.aurelis.data.LocalBrowseType
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.components.CoverPlaceholder
import com.music.aurelis.ui.components.ExplicitSongTitle
import com.music.aurelis.ui.components.LIBRARY_GRID_SPACING
import com.music.aurelis.ui.components.MessageState
import com.music.aurelis.ui.components.PAGE_GUTTER
import com.music.aurelis.ui.components.ROW_DIVIDER_INSET
import com.music.aurelis.ui.components.SearchPlayingBars
import com.music.aurelis.ui.components.SegmentedControl
import com.music.aurelis.ui.components.SongRow
import com.music.aurelis.ui.components.longPressMenuClickable
import com.music.aurelis.ui.components.thumbnailBorder
import com.music.aurelis.ui.icons.AurelisIcons
import com.music.aurelis.ui.theme.ArtworkPalette

class LocalFolder(
    val state: LocalFolderState,
    val onOpenGroup: (LocalGroup) -> Unit,
    val onGroupLongPress: (LocalGroup) -> Unit,
)

enum class LocalFolderTab { SONGS, ARTISTS, ALBUMS }

@Stable
class LocalFolderState {
    var tab by mutableStateOf(LocalFolderTab.SONGS)
}

@Composable
fun rememberLocalFolderGroups(
    browseId: String,
    folder: LocalFolder,
    tab: LocalFolderTab,
    songs: List<LocalSong>,
    query: String,
): List<LocalGroup> = remember(browseId, tab, songs, query) {
    fun LocalGroup.matches() = query.isBlank() ||
        title.contains(query, ignoreCase = true) ||
        subtitle.contains(query, ignoreCase = true)
    when (tab) {
        LocalFolderTab.SONGS -> emptyList()
        LocalFolderTab.ARTISTS -> LocalGroups.artistsOf(browseId, songs).filter { it.matches() }
        LocalFolderTab.ALBUMS -> LocalGroups.albumsOf(browseId, songs).filter { it.matches() }
    }
}

internal fun LazyListScope.localFolderItems(
    folder: LocalFolder,
    state: LocalFolderState,
    songs: List<LocalSong>,
    groups: List<LocalGroup>,
    query: String,
    columns: Int,
    palette: ArtworkPalette,
    currentSong: LocalSong?,
    isPlaying: Boolean,
    onSongClick: (List<LocalSong>, Int) -> Unit,
    onSongLongPress: (LocalSong) -> Unit,
) {
    item(key = "folder-tabs") {
        SegmentedControl(
            options = listOf(
                stringResource(R.string.songs),
                stringResource(R.string.artists),
                stringResource(R.string.albums),
            ),
            selectedIndex = state.tab.ordinal,
            onSelect = { state.tab = LocalFolderTab.entries[it] },
            modifier = Modifier.padding(start = PAGE_GUTTER, end = PAGE_GUTTER, bottom = 14.dp),
        )
    }

    val empty = when (state.tab) {
        LocalFolderTab.SONGS -> songs.isEmpty()
        else -> groups.isEmpty()
    }
    if (empty) {
        item(key = "folder-empty") {
            MessageState(
                if (query.isNotBlank()) {
                    stringResource(R.string.nothing_matches, query)
                } else if (state.tab == LocalFolderTab.ALBUMS) {
                    stringResource(R.string.no_local_albums)
                } else {
                    stringResource(R.string.no_tracks_here)
                },
            )
        }
        return
    }

    val songClick: (Int) -> Unit = { index ->
        onSongClick(songs, index)
    }
    val songHold: (LocalSong) -> Unit = { song ->
        onSongLongPress(song)
    }
    val groupClick: (LocalGroup) -> Unit = { group ->
        folder.onOpenGroup(group)
    }
    val groupHold: (LocalGroup) -> Unit = { group ->
        folder.onGroupLongPress(group)
    }

    when (state.tab) {
        LocalFolderTab.SONGS -> {
            itemsIndexed(songs, key = { index, song -> "song-${song.id}-$index" }) { index, song ->
                SongRow(
                    song = song,
                    isCurrent = currentSong?.id == song.id,
                    isPlaying = currentSong?.id == song.id && isPlaying,
                    searchPlayingStyle = true,
                    activeTint = palette.accent,
                    subtitleColor = palette.onBackgroundVariant,
                    rowBackground = Color.Transparent,
                    onClick = { songClick(index) },
                    onLongPress = { songHold(song) },
                )
                if (index < songs.lastIndex) FolderDivider(palette)
            }
        }
        LocalFolderTab.ARTISTS -> {
            items(groups, key = { it.browseId }) { group ->
                ArtistRow(group, palette, onClick = { groupClick(group) }, onLongPress = { groupHold(group) })
                FolderDivider(palette)
            }
        }
        LocalFolderTab.ALBUMS -> {
            items(groups, key = { it.browseId }) { group ->
                AlbumRow(
                    group = group,
                    palette = palette,
                    onClick = { groupClick(group) },
                    onLongPress = { groupHold(group) },
                )
                FolderDivider(palette)
            }
        }
    }
}

@Composable
private fun FolderDivider(palette: ArtworkPalette) {
    HorizontalDivider(
        modifier = Modifier.padding(start = ROW_DIVIDER_INSET),
        thickness = 0.5.dp,
        color = palette.divider,
    )
}

@Composable
private fun ArtistAvatar(group: LocalGroup, modifier: Modifier) {
    val art = remember(group.songs) { group.songs.firstNotNullOfOrNull { it.artworkUri } }
    Box(modifier.clip(CircleShape).thumbnailBorder(CircleShape)) {
        CoverPlaceholder(Icons.Rounded.Person, Modifier.fillMaxSize(), iconFraction = 0.5f)
        if (art != null) {
            AsyncImage(
                model = art,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArtistRow(
    group: LocalGroup,
    palette: ArtworkPalette,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .longPressMenuClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtistAvatar(group, Modifier.size(52.dp))
        Spacer(Modifier.width(16.dp))
        GroupLabels(
            title = group.title,
            subtitle = pluralStringResource(R.plurals.song_count_plural, group.songs.size, group.songs.size),
            palette = palette,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = AurelisIcons.ChevronRight,
            contentDescription = null,
            tint = palette.onBackgroundVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun LocalGroup.albumSubtitle(): String {
    val count = pluralStringResource(R.plurals.song_count_plural, songs.size, songs.size)
    return when {
        type == LocalBrowseType.PLAYLIST -> "${stringResource(R.string.playlist)} · $count"
        subtitle.isNotBlank() && subtitle != title -> "$subtitle · $count"
        else -> count
    }
}

private fun LocalGroup.placeholderIcon(): ImageVector =
    if (type == LocalBrowseType.PLAYLIST) Icons.AutoMirrored.Rounded.QueueMusic else Icons.Rounded.Album

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumRow(
    group: LocalGroup,
    palette: ArtworkPalette,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val art = group.artworkUri?.toString()
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .longPressMenuClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(52.dp).clip(shape).thumbnailBorder(shape)) {
            CoverPlaceholder(group.placeholderIcon(), Modifier.fillMaxSize(), iconFraction = 0.46f)
            if (art != null) {
                AsyncImage(
                    model = art,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        GroupLabels(
            title = group.title,
            subtitle = group.albumSubtitle(),
            palette = palette,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = AurelisIcons.ChevronRight,
            contentDescription = null,
            tint = palette.onBackgroundVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun GroupLabels(title: String, subtitle: String, palette: ArtworkPalette, modifier: Modifier) {
    Column(modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = palette.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.onBackgroundVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
