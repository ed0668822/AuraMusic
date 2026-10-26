package com.music.aura

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.music.aura.data.AppUpdateChecker
import com.music.aura.data.AudioPermission
import com.music.aura.playback.PlaybackViewModel
import com.music.aura.ui.components.BottomTab
import com.music.aura.ui.components.GlassNavBar
import com.music.aura.ui.components.MiniPlayer
import com.music.aura.ui.components.UpdateAvailableDialog
import com.music.aura.ui.screens.*
import com.music.aura.ui.theme.AuraTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (!AudioPermission.isGranted(this)) permissionLauncher.launch(AudioPermission.requiredPermission())

        setContent {
            AuraTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AuraApp()
                }
            }
        }
    }
}

@Composable
private fun AuraApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val playbackViewModel: PlaybackViewModel = viewModel()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showNowPlaying by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showLocalMusic by remember { mutableStateOf(false) }
    var showReplay by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { 
        playbackViewModel.connect() 
        AppUpdateChecker.check() // <-- Buscar actualización al arrancar
    }

    val playbackState by playbackViewModel.state.collectAsState()
    val hazeState = remember { HazeState() }
    
    val updateInfo by AppUpdateChecker.available.collectAsState()
    val downloadState by AppUpdateChecker.download.collectAsState()

    val tabs = listOf(
        BottomTab("Home", Icons.Filled.Home),
        BottomTab("Explore", Icons.Filled.Explore),
        BottomTab("Library", Icons.Filled.LibraryMusic),
        BottomTab("Search", Icons.Filled.Search),
    )

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).hazeSource(hazeState)) {
        when (selectedTab) {
            0 -> HomeScreen(playbackViewModel = playbackViewModel)
            1 -> Text("Explore (Próximamente)", modifier = Modifier.align(Alignment.Center))
            2 -> {
                when {
                    showReplay -> ReplayScreen(onBackClick = { showReplay = false })
                    showLocalMusic -> LocalMusicScreen(playbackViewModel = playbackViewModel, onBackClick = { showLocalMusic = false })
                    else -> LibraryScreen(
                        playbackViewModel = playbackViewModel,
                        onSettingsClick = { showSettings = true },
                        onOpenLocalMusic = { showLocalMusic = true },
                        onOpenReplay = { showReplay = true }
                    )
                }
            }
            3 -> Text("Search (Próximamente)", modifier = Modifier.align(Alignment.Center))
        }

        if (!showNowPlaying && !showQueue && !showSettings && !showLocalMusic && !showReplay) {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp, start = 12.dp, end = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (playbackState.currentSong != null) {
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
                GlassNavBar(
                    tabs = tabs,
                    selectedIndex = selectedTab,
                    onTabSelected = { selectedTab = it },
                    hazeState = hazeState,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        NowPlayingScreen(playbackViewModel = playbackViewModel, isVisible = showNowPlaying, onDismiss = { showNowPlaying = false }, onOpenQueue = { showNowPlaying = false; showQueue = true })
        QueueScreen(playbackViewModel = playbackViewModel, isVisible = showQueue, onDismiss = { showQueue = false })
        SettingsScreen(isVisible = showSettings, onDismiss = { showSettings = false })

        // Diálogo de actualización
        updateInfo?.let { info ->
            if (downloadState !is AppUpdateChecker.DownloadState.Idle || !isCheckingUpdate) {
                 UpdateAvailableDialog(
                    version = info.version,
                    hazeState = hazeState,
                    onDismiss = { AppUpdateChecker.resetDownload() }, // Ocultar temporalmente
                    onDownload = { kotlinx.coroutines.GlobalScope.launch { AppUpdateChecker.downloadApk(context) } },
                    onCancelDownload = { AppUpdateChecker.cancelDownload() },
                    onInstall = {
                        val state = downloadState
                        if (state is AppUpdateChecker.DownloadState.Ready) {
                            AppUpdateChecker.installApk(context, state.file)
                        }
                    },
                    onOpenReleasePage = { 
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(info.releaseUrl))
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

// Función auxiliar para no dejar en blanco la variable en el if de arriba
val isCheckingUpdate = false 
