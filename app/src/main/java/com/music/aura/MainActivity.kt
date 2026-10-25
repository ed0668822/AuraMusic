package com.music.aura

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.music.aura.data.AudioPermission
import com.music.aura.data.SettingsRepository
import com.music.aura.playback.PlaybackViewModel
import com.music.aura.ui.components.BottomTab
import com.music.aura.ui.components.GlassNavBar
import com.music.aura.ui.components.MiniPlayer
import com.music.aura.ui.screens.HomeScreen
import com.music.aura.ui.screens.LibraryScreen
import com.music.aura.ui.screens.NowPlayingScreen
import com.music.aura.ui.screens.QueueScreen
import com.music.aura.ui.screens.SettingsScreen
import com.music.aura.ui.theme.AuraTheme
import dev.chrisbanes.haze.HazeState

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        SettingsRepository.init(this)

        if (!AudioPermission.isGranted(this)) {
            permissionLauncher.launch(AudioPermission.requiredPermission())
        }

        setContent {
            AuraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AuraApp()
                }
            }
        }
    }
}

@Composable
private fun AuraApp() {
    val playbackViewModel: PlaybackViewModel = viewModel()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showNowPlaying by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        playbackViewModel.connect()
    }

    val playbackState by playbackViewModel.state.collectAsState()
    val hazeState = remember { HazeState() }

    val tabs = listOf(
        BottomTab("Home", Icons.Filled.Home),
        BottomTab("Explore", Icons.Filled.Explore),
        BottomTab("Library", Icons.Filled.LibraryMusic),
        BottomTab("Search", Icons.Filled.Search),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {

        when (selectedTab) {
            0 -> HomeScreen(playbackViewModel = playbackViewModel)
            1 -> Text("Explore (Módulo 2)", modifier = Modifier.align(Alignment.Center))
            2 -> LibraryScreen(
                playbackViewModel = playbackViewModel,
                onSettingsClick = { showSettings = true },
            )
            3 -> Text("Search (Módulo 2)", modifier = Modifier.align(Alignment.Center))
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp, start = 12.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (playbackState.currentSong != null && !showNowPlaying && !showQueue && !showSettings) {
                MiniPlayer(
                    state = playbackState,
                    hazeState = hazeState,
                    onPlayPause = { playbackViewModel.togglePlayPause() },
                    onNext = { playbackViewModel.next() },
                    onPrevious = { playbackViewModel.previous() },
                    onExpand = { showNowPlaying = true },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
            }

            if (!showNowPlaying && !showQueue && !showSettings) {
                GlassNavBar(
                    tabs = tabs,
                    selectedIndex = selectedTab,
                    onTabSelected = { selectedTab = it },
                    hazeState = hazeState,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        NowPlayingScreen(
            playbackViewModel = playbackViewModel,
            isVisible = showNowPlaying,
            onDismiss = { showNowPlaying = false },
            onOpenQueue = {
                showNowPlaying = false
                showQueue = true
            },
        )

        QueueScreen(
            playbackViewModel = playbackViewModel,
            isVisible = showQueue,
            onDismiss = { showQueue = false },
        )

        SettingsScreen(
            isVisible = showSettings,
            onDismiss = { showSettings = false },
        )
    }
}
