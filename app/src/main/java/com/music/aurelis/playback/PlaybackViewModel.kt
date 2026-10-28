package com.music.aurelis.playback

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import com.music.aurelis.data.lyrics.Lyrics
import com.music.aurelis.data.lyrics.LyricsRepository
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.stats.ListeningStats
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackState(
    val currentSong: LocalSong? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<LocalSong> = emptyList(),
    val currentIndex: Int = -1,
    val isConnected: Boolean = false,
    val lyrics: Lyrics? = null,
)

class PlaybackViewModel(application: Application) : AndroidViewModel(application) {

    private var controller: MediaController? = null
    private var positionJob: Job? = null
    private var lyricsJob: Job? = null
    private var lastLyricsSongId: String? = null
    private var lastTickPositionMs = 0L // Para calcular el delta de escucha

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            lastTickPositionMs = 0L
            syncState()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) {
                startPositionPolling()
            } else {
                stopPositionPolling()
                ListeningStats.flush()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.update { it.copy(isLoading = playbackState == Player.STATE_BUFFERING) }
            if (playbackState == Player.STATE_READY) syncDuration()
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            syncState()
        }
    }

    fun connect() {
        viewModelScope.launch {
            try {
                val ctx = getApplication<Application>()
                val mediaController = PlayerConnection.connect(ctx)
                controller = mediaController
                mediaController.addListener(playerListener)
                _state.update { it.copy(isConnected = true) }
                syncState()
            } catch (e: Exception) {
                _state.update { it.copy(isConnected = false) }
            }
        }
    }

    fun playSongs(songs: List<LocalSong>, startIndex: Int = 0) {
        viewModelScope.launch {
            val ctrl = controller ?: return@launch
            _state.update {
                it.copy(queue = songs, currentIndex = startIndex, currentSong = songs.getOrNull(startIndex), isLoading = true)
            }
            PlayerConnection.playSongs(ctrl, songs, startIndex)
        }
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    fun next() = controller?.seekToNextMediaItem()
    fun previous() = controller?.seekToPreviousMediaItem()

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _state.update { it.copy(positionMs = positionMs) }
        lastTickPositionMs = positionMs // Evitar que el salto cuente como tiempo escuchado
    }

    fun seekToIndex(index: Int) {
        val ctrl = controller ?: return
        if (index < 0 || index >= ctrl.mediaItemCount) return
        ctrl.seekToDefaultPosition(index)
        ctrl.play()
        _state.update { it.copy(currentIndex = index, currentSong = it.queue.getOrNull(index)) }
    }

    fun removeFromQueue(index: Int) {
        val ctrl = controller ?: return
        val currentIdx = ctrl.currentMediaItemIndex
        if (index == currentIdx || ctrl.mediaItemCount <= 1) return
        ctrl.removeMediaItem(index)
        _state.update { currentState ->
            val newQueue = currentState.queue.toMutableList()
            if (index < newQueue.size) newQueue.removeAt(index)
            val newCurrentIndex = if (index < currentIdx) currentIdx - 1 else currentIdx
            currentState.copy(queue = newQueue, currentIndex = newCurrentIndex)
        }
    }

    private fun syncState() {
        val ctrl = controller ?: return
        val currentIndex = ctrl.currentMediaItemIndex
        val mediaItem = ctrl.currentMediaItem
        val song = mediaItem?.let { mediaItemToLocalSong(it) }

        val queueSongs = mutableListOf<LocalSong>()
        for (i in 0 until ctrl.mediaItemCount) {
            queueSongs.add(mediaItemToLocalSong(ctrl.getMediaItemAt(i)))
        }

        _state.update {
            it.copy(
                currentSong = song, currentIndex = currentIndex, isPlaying = ctrl.isPlaying,
                isLoading = ctrl.playbackState == Player.STATE_BUFFERING,
                positionMs = ctrl.currentPosition.coerceAtLeast(0L),
                durationMs = ctrl.duration.coerceAtLeast(0L),
                queue = queueSongs,
            )
        }

        if (song != null && song.id != lastLyricsSongId) loadLyrics(song)
        else if (song == null) {
            lastLyricsSongId = null
            _state.update { it.copy(lyrics = null) }
        }
    }

    private fun syncDuration() {
        val ctrl = controller ?: return
        _state.update { it.copy(durationMs = ctrl.duration.coerceAtLeast(0L)) }
    }

    private fun loadLyrics(song: LocalSong) {
        lastLyricsSongId = song.id
        lyricsJob?.cancel()
        _state.update { it.copy(lyrics = null) }
        lyricsJob = viewModelScope.launch {
            val context = getApplication<Application>()
            val parsed = LyricsRepository.load(context, song)
            if (lastLyricsSongId == song.id) _state.update { it.copy(lyrics = parsed) }
        }
    }

    private fun mediaItemToLocalSong(item: MediaItem): LocalSong {
        val metadata = item.mediaMetadata
        val extras = metadata.extras
        return LocalSong(
            id = item.mediaId, title = metadata.title?.toString() ?: "Unknown",
            artist = metadata.artist?.toString() ?: "Unknown Artist",
            album = metadata.albumTitle?.toString(), contentUri = item.localConfiguration?.uri?.toString() ?: "",
            artworkUri = metadata.artworkUri?.toString(), durationMs = extras?.getLong("durationMs", 0L) ?: 0L,
            filePath = extras?.getString("filePath"), trackNumber = extras?.getInt("trackNumber", -1)?.takeIf { it > 0 },
            year = extras?.getInt("year", -1)?.takeIf { it > 0 }, dateAddedMs = extras?.getLong("dateAddedMs", 0L) ?: 0L,
        )
    }

    private fun startPositionPolling() {
        positionJob?.cancel()
        positionJob = viewModelScope.launch {
            while (isActive) {
                val ctrl = controller ?: break
                val currentPos = ctrl.currentPosition.coerceAtLeast(0L)
                
                // --- LÓGICA DE ESTADÍSTICAS ---
                val delta = currentPos - lastTickPositionMs
                // Si avanzó entre 0 y 5 segundos (ignorar saltos), y hay canción actual
                if (delta in 1..5000 && _state.value.currentSong != null && ctrl.isPlaying) {
                    ListeningStats.record(_state.value.currentSong!!, delta, false)
                }
                lastTickPositionMs = currentPos
                // ------------------------------

                _state.update { it.copy(positionMs = currentPos, durationMs = ctrl.duration.coerceAtLeast(0L)) }
                delay(1000L) // Cada segundo para mayor precisión
            }
        }
    }

    private fun stopPositionPolling() {
        positionJob?.cancel()
        positionJob = null
    }

    override fun onCleared() {
        positionJob?.cancel()
        lyricsJob?.cancel()
        ListeningStats.flush() // Guardar al salir
        controller?.let { ctrl ->
            ctrl.removeListener(playerListener)
            MediaController.releaseFuture(com.google.common.util.concurrent.Futures.immediateFuture(ctrl))
        }
        controller = null
        super.onCleared()
    }
}
