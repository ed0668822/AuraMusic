package com.music.aura.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.aura.playback.PlaybackState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

data class BottomTab(
    val label: String,
    val icon: ImageVector
)

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassNavBar(
    tabs: List<BottomTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    playbackState: PlaybackState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(percent = 50)
    val song = playbackState.currentSong

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Mini Reproductor (Se muestra arriba de los tabs si hay música)
        AnimatedVisibility(
            visible = song != null,
            enter = expandVertically() + androidx.compose.animation.fadeIn(),
            exit = shrinkVertically() + androidx.compose.animation.fadeOut()
        ) {
            if (song != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .height(56.dp)
                        .clip(shape)
                        .hazeChild(state = hazeState, style = HazeMaterials.thin(MaterialTheme.colorScheme.surface))
                        .border(0.5.dp, Color.White.copy(alpha = 0.10f), shape)
                        .clickable(onClick = onExpand)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (song.artworkUri != null) {
                                AsyncImage(model = song.artworkUri, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                            } else {
                                Text("♪")
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(song.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (playbackState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = onPlayPause) {
                                Icon(if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = "Play/Pause")
                            }
                        }
                        IconButton(onClick = onNext) {
                            Icon(Icons.Filled.SkipNext, contentDescription = "Next")
                        }
                    }
                }
            }
        }

        // 2. Barra de Navegación (Tabs)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(shape)
                .hazeChild(state = hazeState, style = HazeMaterials.thin(MaterialTheme.colorScheme.surface))
                .border(0.5.dp, Color.White.copy(alpha = 0.10f), shape)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = index == selectedIndex
                    val tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    
                    Box(
                        modifier = Modifier.weight(1f).height(48.dp).clickable { onTabSelected(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(tab.icon, contentDescription = tab.label, tint = tint, modifier = Modifier.size(24.dp))
                            if (isSelected) {
                                Text(tab.label, style = MaterialTheme.typography.labelSmall, color = tint)
                            }
                        }
                    }
                }
            }
        }
    }
}
