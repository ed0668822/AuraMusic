package com.music.aura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.aura.data.stats.ListeningStats
import com.music.aura.data.stats.ReplayPeriod
import com.music.aura.data.stats.ReplaySummary

@Composable
fun ReplayScreen(onBackClick: () -> Unit) {
    var summary by remember { mutableStateOf<ReplaySummary?>(null) }
    var selectedPeriod by remember { mutableStateOf(ReplayPeriod.ALL_TIME) }

    LaunchedEffect(selectedPeriod) {
        summary = ListeningStats.summary(selectedPeriod)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
            }
            Text("Replay", style = MaterialTheme.typography.titleLarge)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReplayPeriod.values().forEach { period ->
                FilterChip(
                    selected = selectedPeriod == period,
                    onClick = { selectedPeriod = period },
                    label = { Text(period.chip) }
                )
            }
        }

        summary?.let { s ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))))
                            .padding(20.dp)
                    ) {
                        Column(modifier = Modifier.align(Alignment.BottomStart)) {
                            Text("MINUTOS ESCUCHADOS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                            Text("${s.minutes}", fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text("En ${s.label}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }

                if (s.songs.isNotEmpty()) {
                    item { Text("Top Canciones", style = MaterialTheme.typography.titleMedium) }
                    items(s.songs.take(5)) { ranked ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                if (ranked.song.artworkUri != null) {
                                    AsyncImage(model = ranked.song.artworkUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(ranked.song.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                                Text(ranked.song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${ranked.plays} plays", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                
                if (s.artists.isNotEmpty()) {
                    item { Text("Top Artistas", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp)) }
                    items(s.artists.take(5)) { ranked ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(ranked.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            Text("${ranked.ms / 60000} min", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
