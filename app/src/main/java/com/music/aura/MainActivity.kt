package com.music.aura

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.music.aura.data.AudioPermission
import com.music.aura.data.SettingsRepository
import com.music.aura.playback.PlaybackViewModel
import com.music.aura.ui.components.BottomFadeScrim
import com.music.aura.ui.components.MiniPlayer
import com.music.aura.ui.screens.LibraryScreen
import com.music.aura.ui.screens.NowPlayingScreen
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

        // Inicializar preferencias
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
    var showNowPlaying by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        playbackViewModel.connect()
    }

    val playbackState by playbackViewModel.state.collectAsState()
    val miniPlayerHazeState = remember { HazeState() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Biblioteca
        LibraryScreen(
            playbackViewModel = playbackViewModel,
            onSettingsClick = { showSettings = true },
        )

        // Degradado inferior
        if (playbackState.currentSong != null && !showNowPlaying && !showQueue && !showSettings) {
            BottomFadeScrim(
                modifier = Modifier.align(Alignment.BottomCenter),
                height = 100,
            )
        }

        // Mini player
        if (!showNowPlaying && !showQueue && !showSettings) {
            MiniPlayer(
                state = playbackState,
                hazeState = miniPlayerHazeState,
                onPlayPause = { playbackViewModel.togglePlayPause() },
                onNext = { playbackViewModel.next() },
                onPrevious = { playbackViewModel.previous() },
                onExpand = { showNowPlaying = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
            )
        }

        // Now Playing
        NowPlayingScreen(
            playbackViewModel = playbackViewModel,
            isVisible = showNowPlaying,
            onDismiss = { showNowPlaying = false },
            onOpenQueue = {
                showNowPlaying = false
                showQueue = true
            },
        )

        // Cola
        QueueScreen(
            playbackViewModel = playbackViewModel,
            isVisible = showQueue,
            onDismiss = { showQueue = false },
        )

        // Configuración
        SettingsScreen(
            isVisible = showSettings,
            onDismiss = { showSettings = false },
        )
    }
}
