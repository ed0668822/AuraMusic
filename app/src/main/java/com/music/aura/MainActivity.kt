package com.music.aura

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.music.aura.data.AppUpdateChecker
import com.music.aura.data.AudioPermission
import com.music.aura.data.LibraryState
import com.music.aura.data.model.LocalSong
import com.music.aura.playback.PlaybackViewModel
import com.music.aura.ui.components.BottomTab
import com.music.aura.ui.components.GlassNavBar
import com.music.aura.ui.components.MiniPlayer
import com.music.aura.ui.components.UpdateAvailableDialog
import com.music.aura.ui.icons.AuraIcons
import com.music.aura.ui.screens.HomeScreen
import com.music.aura.ui.screens.LibraryLink
import com.music.aura.ui.screens.LibraryScreen
import com.music.aura.ui.screens.LocalMusicScreen
import com.music.aura.ui.screens.NowPlayingScreen
import com.music.aura.ui.screens.QueueScreen
import com.music.aura.ui.screens.ReplayScreen
import com.music.aura.ui.screens.SearchScreen
import com.music.aura.ui.screens.SettingsScreen
import com.music.aura.ui.theme.AuraTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playbackViewModel: PlaybackViewModel = viewModel()
    val libraryViewModel: LibraryViewModel = viewModel()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showNowPlaying by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showLocalMusic by remember { mutableStateOf(false) }
    var showReplay by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        playbackViewModel.connect()
        libraryViewModel.loadLibrary()
        AppUpdateChecker.check()
    }

    val playbackState by playbackViewModel.state.collectAsState()
    val libraryUiState by libraryViewModel.uiState.collectAsState()
    val hazeState = remember { HazeState() }

    val updateInfo by AppUpdateChecker.available.collectAsState()
    val downloadState by AppUpdateChecker.download.collectAsState()

    LaunchedEffect(updateInfo) {
        if (updateInfo != null) showUpdateDialog = true
    }

    val tabs = listOf(
        BottomTab("Home", Icons.Filled.Home),
        BottomTab("Explore", Icons.Filled.Explore),
        BottomTab("Library", Icons.Filled.LibraryMusic),
        BottomTab("Search", Icons.Filled.Search),
    )

    val librarySuccess = libraryUiState.libraryState as? LibraryState.Success
    val allSongs = librarySuccess?.songs ?: emptyList()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .hazeSource(hazeState),
    ) {
        when (selectedTab) {
            0 -> {
                val listState = rememberLazyListState()
                HomeScreen(
                    recentlyPlayed = emptyList(),
                    mostPlayed = emptyList(),
                    recentlyAdded = librarySuccess?.recentlyAdded ?: emptyList(),
                    listState = listState,
                    onSongClick = { song: LocalSong ->
                        val idx = allSongs.indexOfFirst { it.id == song.id }
                        if (idx >= 0) playbackViewModel.playSongs(allSongs, idx)
                    },
                    onSongLongPress = { },
                    currentSong = playbackState.currentSong,
                    isPlaying = playbackState.isPlaying,
                    contentPadding = PaddingValues(0.dp),
                )
            }

            1 -> Text("Explore (Próximamente)", modifier = Modifier.align(Alignment.Center))

            2 -> {
                when {
                    showReplay -> ReplayScreen(onBackClick = { showReplay = false })
                    showLocalMusic -> LocalMusicScreen(
                        playbackViewModel = playbackViewModel,
                        onBackClick = { showLocalMusic = false },
                    )
                    else -> {
                        val listState = rememberLazyListState()
                        val links = listOf(
                            LibraryLink("Canciones", AuraIcons.MusicNote) { showLocalMusic = true },
                            LibraryLink("Replay", AuraIcons.Clock) { showReplay = true },
                            LibraryLink("Ajustes", AuraIcons.Home) { showSettings = true },
                        )
                        LibraryScreen(
                            listState = listState,
                            links = links,
                            onShowAll = { },
                            contentPadding = PaddingValues(0.dp),
                        )
                    }
                }
            }

            3 -> {
                var searchQuery by remember { mutableStateOf("") }
                val searchListState = rememberLazyListState()
                val results: List<LocalSong>? = if (searchQuery.isBlank()) null else allSongs.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                        it.artist.contains(searchQuery, ignoreCase = true) ||
                        it.album?.contains(searchQuery, ignoreCase = true) == true
                }
                SearchScreen(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    results = results,
                    listState = searchListState,
                    focusRequested = false,
                    onFocusHandled = { },
                    onSongClick = { songs, index -> playbackViewModel.playSongs(songs, index) },
                    onSongLongPress = { },
                    onSongSwipe = { },
                    history = emptyList(),
                    onSubmit = { },
                    onSuggestionClick = { },
                    onHistoryClick = { },
                    onHistoryRemove = { },
                    onHistoryClear = { },
                    currentSong = playbackState.currentSong,
                    isPlaying = playbackState.isPlaying,
                    contentPadding = PaddingValues(0.dp),
                    topPadding = 0.dp,
                )
            }
        }

        if (!showNowPlaying && !showQueue && !showSettings && !showLocalMusic && !showReplay) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp, start = 12.dp, end = 12.dp),
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

        NowPlayingScreen(
            playbackViewModel = playbackViewModel,
            isVisible = showNowPlaying,
            onDismiss = { showNowPlaying = false },
            onOpenQueue = { showNowPlaying = false; showQueue = true },
        )
        QueueScreen(
            playbackViewModel = playbackViewModel,
            isVisible = showQueue,
            onDismiss = { showQueue = false },
        )
        SettingsScreen(isVisible = showSettings, onDismiss = { showSettings = false })

        if (showUpdateDialog && updateInfo != null) {
            UpdateAvailableDialog(
                version = updateInfo!!.version,
                hazeState = hazeState,
                onDismiss = {
                    showUpdateDialog = false
                    AppUpdateChecker.resetDownload()
                },
                onDownload = { scope.launch { AppUpdateChecker.downloadApk(context) } },
                onCancelDownload = { AppUpdateChecker.cancelDownload() },
                onInstall = {
                    val state = downloadState
                    if (state is AppUpdateChecker.DownloadState.Ready) {
                        AppUpdateChecker.installApk(context, state.file)
                    }
                },
                onOpenReleasePage = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo!!.releaseUrl))
                    context.startActivity(intent)
                },
            )
        }
    }
}
