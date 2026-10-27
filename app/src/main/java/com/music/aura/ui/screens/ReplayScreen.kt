package com.music.aura.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.aura.R
import com.music.aura.data.stats.ListeningStats
import com.music.aura.data.stats.ReplayPeriod
import com.music.aura.data.stats.ReplaySummary
import com.music.aura.ui.components.PAGE_GUTTER
import com.music.aura.ui.theme.MeshGradient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ReplayScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var summary by remember { mutableStateOf<ReplaySummary?>(null) }
    var loading by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        loading = true
        summary = withContext(Dispatchers.IO) {
            runCatching { ListeningStats.summary(ReplayPeriod.THIS_YEAR) }.getOrNull()
        }
        loading = false
    }

    Box(modifier.fillMaxSize()) {
        MeshGradient(modifier = Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.30f),
                            Color.Black.copy(alpha = 0.72f),
                            Color.Black.copy(alpha = 0.88f),
                        ),
                    ),
                ),
        )

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color.White,
                    )
                }
                Text(
                    text = stringResource(R.string.replay),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.W800,
                    color = Color.White,
                )
            }

            when {
                loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Color.White.copy(alpha = 0.7f))
                }

                summary == null || summary!!.isEmpty -> Box(
                    Modifier.fillMaxSize().padding(horizontal = 32.dp),
                    Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.BarChart,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.not_enough_listening),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                else -> {
                    val data = summary!!
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(bottom = 32.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item {
                            Column(Modifier.padding(horizontal = PAGE_GUTTER)) {
                                Text(
                                    text = stringResource(R.string.minutes_listened),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White.copy(alpha = 0.7f),
                                )
                                Text(
                                    text = formatListeningTime(data.totalMs),
                                    style = MaterialTheme.typography.displayLarge,
                                    fontWeight = FontWeight.W800,
                                    color = Color.White,
                                )
                                Spacer(Modifier.height(24.dp))
                            }
                        }

                        item {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = PAGE_GUTTER),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                StatTile(
                                    stringResource(R.string.songs),
                                    data.distinctSongs.toString(),
                                    Modifier.weight(1f),
                                )
                                StatTile(
                                    stringResource(R.string.artists),
                                    data.distinctArtists.toString(),
                                    Modifier.weight(1f),
                                )
                                StatTile(
                                    stringResource(R.string.albums),
                                    data.distinctAlbums.toString(),
                                    Modifier.weight(1f),
                                )
                            }
                        }

                        if (data.songs.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.top_songs),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.W800,
                                    color = Color.White,
                                    modifier = Modifier.padding(
                                        start = PAGE_GUTTER,
                                        end = PAGE_GUTTER,
                                        top = 24.dp,
                                        bottom = 10.dp,
                                    ),
                                )
                            }
                            items(data.songs.take(5)) { row ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = PAGE_GUTTER, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    AsyncImage(
                                        model = row.song.artworkUri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = row.song.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.W600,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = row.song.artist,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.55f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.09f))
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.W800,
            color = Color.White,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.55f),
        )
    }
}

private fun formatListeningTime(ms: Long): String {
    val minutes = ms / 60_000
    return when {
        minutes < 60 -> "${minutes}m"
        minutes < 1_440 -> "${minutes / 60}h ${minutes % 60}m"
        else -> "${minutes / 60}h"
    }
}
