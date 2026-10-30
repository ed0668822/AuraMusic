// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
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
import com.music.aurelis.R
import com.music.aurelis.data.model.LocalAlbum
import com.music.aurelis.data.model.LocalArtist
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.components.*
import com.music.aurelis.ui.components.home.*
import com.music.aurelis.ui.icons.AurelisIcons

private val HOME_TITLE_LIFT = 28.dp
private const val RECENT_TRACKS_PER_COLUMN = 4

@Composable
fun HomeScreen(
    recentlyPlayed: List<LocalSong>,
    mostPlayed: List<LocalSong>,
    recentlyAdded: List<LocalSong>,
    albums: List<LocalAlbum>,
    artists: List<LocalArtist>,
    listState: LazyListState,
    onSongClick: (LocalSong) -> Unit,
    onSongLongPress: (LocalSong) -> Unit,
    onAlbumClick: (LocalAlbum) -> Unit,
    onArtistClick: (LocalArtist) -> Unit,
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

        if (recentlyAdded.isNotEmpty()) {
            item(key = "hero") {
                HomeHero(
                    song = recentlyAdded.firstOrNull(),
                    onPlayClick = {
                        recentlyAdded.firstOrNull()?.let { onSongClick(it) }
                    },
                    modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
                )
            }
        }

        if (recentlyPlayed.isNotEmpty()) {
            item(key = "recents") {
                SongCarousel(
                    title = stringResource(R.string.recently_played),
                    songs = recentlyPlayed,
                    currentSongId = currentSong?.id,
                    isPlaying = isPlaying,
                    onSongClick = { index ->
                        recentlyPlayed.getOrNull(index)?.let { onSongClick(it) }
                    },
                )
            }
        }

        if (mostPlayed.isNotEmpty()) {
            item(key = "most-played") {
                SongCarousel(
                    title = stringResource(R.string.most_played),
                    songs = mostPlayed,
                    currentSongId = currentSong?.id,
                    isPlaying = isPlaying,
                    onSongClick = { index ->
                        mostPlayed.getOrNull(index)?.let { onSongClick(it) }
                    },
                )
            }
        }

        if (albums.isNotEmpty()) {
            item(key = "albums") {
                AlbumCarousel(
                    title = stringResource(R.string.albums),
                    albums = albums,
                    onAlbumClick = onAlbumClick,
                )
            }
        }

        if (artists.isNotEmpty()) {
            item(key = "artists") {
                ArtistCarousel(
                    title = stringResource(R.string.artists),
                    artists = artists,
                    onArtistClick = onArtistClick,
                )
            }
        }

        if (recentlyAdded.isNotEmpty()) {
            item(key = "recently-added") {
                SongCarousel(
                    title = stringResource(R.string.recently_added),
                    songs = recentlyAdded,
                    currentSongId = currentSong?.id,
                    isPlaying = isPlaying,
                    onSongClick = { index ->
                        recentlyAdded.getOrNull(index)?.let { onSongClick(it) }
                    },
                )
            }
        }

        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
private fun HomeTitle(title: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = PAGE_GUTTER, vertical = 8.dp)) {
        Icon(
            imageVector = AurelisIcons.Home,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 14.dp).size(30.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
