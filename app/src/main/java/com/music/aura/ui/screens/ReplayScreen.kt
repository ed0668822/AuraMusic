package com.music.aura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.aura.R
import com.music.aura.data.model.LocalSong
import com.music.aura.data.stats.ListeningStats
import com.music.aura.ui.components.PAGE_GUTTER
import com.music.aura.ui.icons.AuraIcons
import com.music.aura.ui.theme.MeshGradient

@Composable
fun ReplayScreen(
    stats: ListeningStats?,
    loading: Boolean,
    onPlaySong: (LocalSong) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val textColor = if (dark) Color.White else Color.Black
    val textVariant = if (dark) Color.White.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.55f)

    Box(modifier.fillMaxSize()) {
        MeshGradient(modifier = Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        if (dark) listOf(
                            Color.Black.copy(alpha = 0.30f),
                            Color.Black.copy(alpha = 0.72f),
                            Color.Black.copy(alpha = 0.88f),
                        ) else listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.65f),
                            Color.White.copy(alpha = 0.82f),
                        ),
                    ),
                ),
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            item("heading") {
                Column(Modifier.padding(horizontal = PAGE_GUTTER + 4.dp)) {
                    Spacer(Modifier.height(48.dp))
                    Text(
                        text = stringResource(R.string.replay),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.W800,
                        color = textColor,
                    )
                    Spacer(Modifier.height(18.dp))
                }
            }

            when {
                loading -> item("loading") {
                    Box(Modifier.fillMaxWidth().padding(64.dp), Alignment.Center) {
                        CircularProgressIndicator(color = textColor.copy(alpha = 0.6f))
                    }
                }
                stats == null || stats.isEmpty -> item("empty") {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = AuraIcons.Clock,
                            contentDescription = null,
                            tint = textVariant,
                            modifier = Modifier.size(44.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.not_enough_listening),
                            style = MaterialTheme.typography.titleLarge,
                            color = textColor,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                else -> {
                    chart(
                        key = "songs",
                        title = stringResource(R.string.top_songs),
                        songs = stats.topSongs,
                        dark = dark,
                        onClick = { index -> stats.topSongs.getOrNull(index)?.let(onPlaySong) },
                    )
                    chart(
                        key = "artists",
                        title = stringResource(R.string.top_artists),
                        songs = stats.topArtists.map { it },
                        dark = dark,
                        onClick = {},
                    )

                    item("habits") {
                        Column(Modifier.padding(horizontal = PAGE_GUTTER + 4.dp)) {
                            Text(
                                text = stringResource(R.string.listening_shape),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.W800,
                                color = textColor,
                                modifier = Modifier.padding(top = 8.dp, bottom = 10.dp),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatTile(stringResource(R.string.songs), stats.distinctSongs.toString(), textColor, Modifier.weight(1f))
                                StatTile(stringResource(R.string.artists), stats.distinctArtists.toString(), textColor, Modifier.weight(1f))
                                StatTile(stringResource(R.string.albums), stats.distinctAlbums.toString(), textColor, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.chart(
    key: String,
    title: String,
    songs: List<LocalSong>,
    dark: Boolean,
    onClick: (Int) -> Unit,
) {
    if (songs.isEmpty()) return
    val textColor = if (dark) Color.White else Color.Black
    item("$key-title") {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.W800,
            color = textColor,
            modifier = Modifier.padding(
                start = PAGE_GUTTER + 4.dp,
                end = PAGE_GUTTER + 4.dp,
                top = 8.dp,
                bottom = 10.dp,
            ),
        )
    }
    items(songs.take(5), key = { "$key-${it.id}" }) { song ->
        val index = songs.indexOf(song)
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onClick(index) }
                .padding(horizontal = PAGE_GUTTER + 4.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.W800,
                color = textColor.copy(alpha = 0.4f),
                modifier = Modifier.width(24.dp),
            )
            AsyncImage(
                model = song.artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.W600,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
    item("$key-gap") { Spacer(Modifier.height(20.dp)) }
}

@Composable
private fun StatTile(label: String, value: String, textColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(textColor.copy(alpha = 0.09f))
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.W800,
            color = textColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor.copy(alpha = 0.55f),
            letterSpacing = 1.sp,
        )
    }
}
