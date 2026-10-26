package com.music.aura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
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
import com.music.aura.LibraryViewModel
import com.music.aura.data.LibraryState
import com.music.aura.data.ProfileRepository
import com.music.aura.data.stats.ListeningStats
import com.music.aura.data.stats.ReplayPeriod
import com.music.aura.playback.PlaybackViewModel

@Composable
fun LibraryScreen(
    playbackViewModel: PlaybackViewModel,
    libraryViewModel: LibraryViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onSettingsClick: () -> Unit = {},
    onOpenLocalMusic: () -> Unit = {},
    onOpenReplay: () -> Unit = {},
) {
    val uiState by libraryViewModel.uiState.collectAsState()
    var minutesListened by remember { mutableStateOf(0L) }
    var userPhoto by remember { mutableStateOf(ProfileRepository.userPhotoUri) }

    LaunchedEffect(Unit) {
        val summary = ListeningStats.summary(ReplayPeriod.ALL_TIME)
        minutesListened = summary.minutes
        userPhoto = ProfileRepository.userPhotoUri
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 260.dp, top = 60.dp),
    ) {
        // Header: Avatar + Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Library",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).clickable { onSettingsClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (userPhoto != null) {
                        AsyncImage(model = userPhoto, contentDescription = "Perfil", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(Icons.Filled.Person, contentDescription = "Perfil", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Replay Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(colors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFFF59E0B))))
                    .clickable { onOpenReplay() }
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                    Text(
                        text = "YOUR LISTENING EXPERIENCE",
                        fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f), letterSpacing = 1.sp
                    )
                    Text(
                        text = "$minutesListened",
                        fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color.White,
                    )
                    Text(
                        text = "MINUTES LISTENED",
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }
        }

        // Local Music Link
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenLocalMusic() }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Local Music",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (uiState.libraryState is LibraryState.Success) {
                        val count = (uiState.libraryState as LibraryState.Success).songs.size
                        Text(
                            text = "$count Tracks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
