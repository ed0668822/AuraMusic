package com.music.aura.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.aura.R
import com.music.aura.data.model.LocalSong
import com.music.aura.ui.components.PAGE_GUTTER
import com.music.aura.ui.components.PlayingAccent
import com.music.aura.ui.components.RowMoreButton
import com.music.aura.ui.components.SearchPlayingBars
import com.music.aura.ui.components.SHELF_CARD_WIDTH
import com.music.aura.ui.components.ShelfRow
import com.music.aura.ui.components.contextClick
import com.music.aura.ui.components.longPressMenuClickable
import com.music.aura.ui.components.thumbnailBorder
import com.music.aura.ui.components.trackColumnWidth
import com.music.aura.ui.icons.AuraIcons

private val HOME_TITLE_LIFT = 28.dp
private const val RECENT_TRACKS_PER_COLUMN = 4

@Composable
fun HomeScreen(
    recentlyPlayed: List<LocalSong>,
    mostPlayed: List<LocalSong>,
    recentlyAdded: List<LocalSong>,
    listState: LazyListState,
    onSongClick: (LocalSong) -> Unit,
    onSongLongPress: (LocalSong) -> Unit,
    currentSong: LocalSong?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = (contentPadding.calculateTopPadding() - HOME_TITLE_LIFT).coerceAtLeast(0.dp),
            bottom = contentPadding.calculateBottomPadding(),
        ),
    ) {
        item { HomeTitle(stringResource(R.string.home)) }

        if (recentlyPlayed.isNotEmpty()) {
            item(key = "recents") {
                RecentShelf(
                    title = stringResource(R.string.recently_played),
                    songs = recentlyPlayed,
                    onSongClick = onSongClick,
                    onSongLongPress = onSongLongPress,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                )
            }
        }

        if (mostPlayed.isNotEmpty()) {
            item(key = "most-played") {
                SongShelf(
                    title = stringResource(R.string.most_played),
                    songs = mostPlayed,
                    onSongClick = onSongClick,
                    onSongLongPress = onSongLongPress,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                )
            }
        }

        if (recentlyAdded.isNotEmpty()) {
            item(key = "recently-added") {
                SongShelf(
                    title = stringResource(R.string.recently_added),
                    songs = recentlyAdded,
                    onSongClick = onSongClick,
                    onSongLongPress = onSongLongPress,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                )
            }
        }
    }
}

@Composable
private fun HomeTitle(title: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = PAGE_GUTTER, vertical = 8.dp)) {
        Icon(
            imageVector = AuraIcons.Home,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .padding(bottom = 14.dp)
                .size(30.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun RecentShelf(
    title: String,
    songs: List<LocalSong>,
    onSongClick: (LocalSong) -> Unit,
    onSongLongPress: (LocalSong) -> Unit,
    currentSong: LocalSong?,
    isPlaying: Boolean,
) {
    Column(Modifier.padding(bottom = 26.dp)) {
        SectionHeader(title)
        BoxWithConstraints {
            val columnWidth = trackColumnWidth(maxWidth)
            ShelfRow(
                contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(songs.chunked(RECENT_TRACKS_PER_COLUMN)) { column ->
                    Column(Modifier.width(columnWidth)) {
                        column.forEach { song ->
                            CompactTrackRow(
                                song = song,
                                onClick = { onSongClick(song) },
                                onLongPress = { onSongLongPress(song) },
                                isCurrent = currentSong?.id == song.id,
                                isPlaying = currentSong?.id == song.id && isPlaying,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SongShelf(
    title: String,
    songs: List<LocalSong>,
    onSongClick: (LocalSong) -> Unit,
    onSongLongPress: (LocalSong) -> Unit,
    currentSong: LocalSong?,
    isPlaying: Boolean,
) {
    Column(Modifier.padding(bottom = 26.dp)) {
        SectionHeader(title)
        ShelfRow(
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(songs) { song ->
                SongCard(
                    song = song,
                    onClick = { onSongClick(song) },
                    onLongPress = { onSongLongPress(song) },
                    isCurrent = currentSong?.id == song.id,
                    isPlaying = currentSong?.id == song.id && isPlaying,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactTrackRow(
    song: LocalSong,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)?,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .longPressMenuClickable(onClick = onClick, onLongClick = onLongPress)
            .contextClick(onLongPress)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp)) {
            AsyncImage(
                model = song.artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .thumbnailBorder(RoundedCornerShape(7.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            if (isCurrent && isPlaying) SearchPlayingBars(Modifier.align(Alignment.Center))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) PlayingAccent else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onLongPress != null) {
            RowMoreButton(onClick = onLongPress)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongCard(
    song: LocalSong,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier.width(SHELF_CARD_WIDTH),
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
) {
    Column(
        modifier = modifier
            .longPressMenuClickable(onClick = onClick, onLongClick = onLongPress)
            .contextClick(onLongPress),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .thumbnailBorder(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = song.artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (isCurrent && isPlaying) SearchPlayingBars(Modifier.align(Alignment.Center))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.titleMedium,
            color = if (isCurrent) PlayingAccent else MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun SectionHeader(title: String, subtitle: String = "", onShowAll: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .padding(horizontal = PAGE_GUTTER, vertical = 10.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onShowAll != null) {
            Text(
                text = stringResource(R.string.show_all),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onShowAll)
                    .padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
            )
        }
    }
}
