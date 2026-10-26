package com.music.aura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.music.aura.LibraryViewModel
import com.music.aura.data.AudioPermission
import com.music.aura.data.LibraryState
import com.music.aura.data.model.LocalAlbum
import com.music.aura.data.model.LocalArtist
import com.music.aura.data.model.LocalSong
import com.music.aura.playback.PlaybackViewModel
import com.music.aura.ui.components.ShelfRow

@Composable
fun HomeScreen(
    playbackViewModel: PlaybackViewModel,
    libraryViewModel: LibraryViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by libraryViewModel.uiState.collectAsState()
    val playbackState by playbackViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        if (AudioPermission.isGranted(libraryViewModel.getApplication())) {
            libraryViewModel.loadLibrary()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState.libraryState) {
            is LibraryState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is LibraryState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is LibraryState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 60.dp, bottom = 260.dp),
                ) {
                    // Título "Listen Now"
                    item {
                        Text(
                            text = "Listen Now",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                        )
                    }

                    // Hero Card
                    if (state.recentlyAdded.isNotEmpty()) {
                        val latest = state.recentlyAdded.first()
                        item {
                            HomeHero(
                                song = latest,
                                onPlayClick = {
                                    playbackViewModel.playSongs(state.songs, state.songs.indexOf(latest))
                                },
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Recientes
                    if (state.recentlyAdded.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(16.dp))
                            SongShelf(
                                title = "Recents",
                                songs = state.recentlyAdded.take(10),
                                currentSongId = playbackState.currentSong?.id,
                                isPlaying = playbackState.isPlaying,
                                onSongClick = { index -> playbackViewModel.playSongs(state.recentlyAdded, index) }
                            )
                        }
                    }

                    // Álbumes
                    if (state.albums.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(16.dp))
                            AlbumShelf(
                                title = "Álbumes",
                                albums = state.albums.take(10),
                                onAlbumClick = { album -> playbackViewModel.playSongs(album.songs, 0) }
                            )
                        }
                    }

                    // Artistas
                    if (state.artists.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(16.dp))
                            ArtistShelf(
                                title = "Artistas",
                                artists = state.artists.take(10),
                                onArtistClick = { artist -> playbackViewModel.playSongs(artist.songs, 0) }
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
    currentSongId: String?,
    isPlaying: Boolean,
    onSongClick: (Int) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        ShelfRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(songs.size) { index ->
                val song = songs[index]
                Column(
                    modifier = Modifier
                        .width(140.dp)
                        .clickable { onSongClick(index) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (song.artworkUri != null) {
                            AsyncImage(
                                model = song.artworkUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (song.id == currentSongId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumShelf(
    title: String,
    albums: List<LocalAlbum>,
    onAlbumClick: (LocalAlbum) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        ShelfRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(albums.size) { index ->
                val album = albums[index]
                Column(
                    modifier = Modifier
                        .width(150.dp)
                        .clickable { onAlbumClick(album) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (album.artworkUri != null) {
                            AsyncImage(
                                model = album.artworkUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Text(
                        text = album.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = album.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistShelf(
    title: String,
    artists: List<LocalArtist>,
    onArtistClick: (LocalArtist) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        ShelfRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(artists.size) { index ->
                val artist = artists[index]
                Column(
                    modifier = Modifier
                        .width(100.dp)
                        .clickable { onArtistClick(artist) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (artist.artworkUri != null) {
                            AsyncImage(
                                model = artist.artworkUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHero(song: LocalSong, onPlayClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (song.artworkUri != null) {
            AsyncImage(
                model = song.artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(48.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(0.2f), Color.Black.copy(0.85f))))
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(20.dp).fillMaxWidth(0.75f)
        ) {
            Text("ESCUCHA AHORA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White.copy(0.75f))
            Text(song.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onPlayClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.Black, modifier = Modifier.size(28.dp))
        }
    }
}
