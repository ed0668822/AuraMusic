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
import com.music.aura.playback.PlaybackViewModel
import com.music.aura.ui.components.BottomFadeScrim
import com.music.aura.ui.components.MiniPlayer
import com.music.aura.ui.screens.LibraryScreen
import com.music.aura.ui.screens.NowPlayingScreen
import com.music.aura.ui.theme.AuraTheme
import dev.chrisbanes.haze.HazeState

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!AudioPermission.isGranted(this)) {
            permissionLauncher.launch(AudioPermission.requiredPermission())
        }

        setContent {
            AuraTheme(darkTheme = true) {
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

    // Conectar al servicio de reproducción una sola vez
    androidx.compose.runtime.LaunchedEffect(Unit) {
        playbackViewModel.connect()
    }

    val playbackState by playbackViewModel.state.collectAsState()
    val miniPlayerHazeState = remember { HazeState() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Contenido principal: la biblioteca
        LibraryScreen(
            playbackViewModel = playbackViewModel,
        )

        // Degradado inferior detrás del mini player
        if (playbackState.currentSong != null && !showNowPlaying) {
            BottomFadeScrim(
                modifier = Modifier.align(Alignment.BottomCenter),
                height = 100,
            )
        }

        // Mini player en la parte inferior (oculto cuando NowPlaying está abierto)
        if (!showNowPlaying) {
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

        // Pantalla Now Playing a pantalla completa con tema dinámico
        NowPlayingScreen(
            playbackViewModel = playbackViewModel,
            isVisible = showNowPlaying,
            onDismiss = { showNowPlaying = false },
        )
    }
}
