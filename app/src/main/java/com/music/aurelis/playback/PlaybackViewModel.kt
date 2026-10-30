// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.playback

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import com.music.aurelis.data.SettingsRepository
import com.music.aurelis.data.lyrics.LyricLine
import com.music.aurelis.data.lyrics.LyricsRepository
import com.music.aurelis.data.lyrics.LyricsSource
import com.music.aurelis.data.lyrics.LyricsTranslation
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

sealed interface LyricsState {
    data object None : LyricsState
    data class Loading(val source: LyricsSource? = null) : LyricsState
    data class Loaded(
        val lines: List<LyricLine>,
        val source: LyricsSource,
        val translation: TranslationState = TranslationState.None,
    ) : LyricsState
    data object Missing : LyricsState
}

sealed interface TranslationState {
    data object None : TranslationState
    data object Loading : TranslationState
    data class Done(val lines: List<LyricLine>) : TranslationState
    data object Failed : TranslationState
}

data class PlaybackState(
    val currentSong: LocalSong? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<LocalSong> = emptyList(),
    val currentIndex: Int = -1,
    val isConnected: Boolean = false,
    val lyrics: LyricsState = LyricsState.None,
)

class PlaybackViewModel(application: Application) : AndroidViewModel(application) {

    private var controller: MediaController? = null
    private var positionJob: Job? = null
    private var lyricsJob: Job? = null
    private var translationJob: Job? = null
    private var lastLyricsSongId: String? = null
    private var lastTickPositionMs = 0L

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            lastTickPositionMs = 0L
            syncState()
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) startPositionPolling()
            else { stopPositionPolling(); ListeningStats.flush() }
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.update { it.copy(isLoading = playbackState == Player.STATE_BUFFERING) }
            if (playbackState == Player.STATE_READY) syncDuration()
        }
        override fun onTimelineChanged(timeline: Timeline, reason: Int) = syncState()
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
            } catch (_: Exception) {
                _state.update { it.copy(isConnected = false) }
            }
        }
    }

    fun playSongs(songs: List<LocalSong>, startIndex: Int = 0) {
        viewModelScope.launch {
            val ctrl = controller ?: return@launch
            _state.update {
                it.copy(
                    queue = songs, currentIndex = startIndex,
                    currentSong = songs.getOrNull(startIndex), isLoading = true,
                )
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
        lastTickPositionMs = positionMs
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

    fun translateLyrics(targetLanguageTag: String) {
        val loaded = _state.value.lyrics as? LyricsState.Loaded ?: return
        val song = _state.value.currentSong ?: return
        translationJob?.cancel()
        _state.update { it.copy(lyrics = loaded.copy(translation = TranslationState.Loading)) }
        translationJob = viewModelScope.launch {
            when (val res = LyricsTranslation.translate(song.id, loaded.lines, targetLanguageTag)) {
                is LyricsTranslation.Result.Translated -> {
                    _state.update { current ->
                        val now = current.lyrics as? LyricsState.Loaded ?: return@update current
                        current.copy(lyrics = now.copy(translation = TranslationState.Done(res.lines)))
                    }
                }
                is LyricsTranslation.Result.SameLanguage -> {
                    _state.update { current ->
                        val now = current.lyrics as? LyricsState.Loaded ?: return@update current
                        current.copy(lyrics = now.copy(translation = TranslationState.None))
                    }
                }
                LyricsTranslation.Result.Unavailable -> {
                    _state.update { current ->
                        val now = current.lyrics as? LyricsState.Loaded ?: return@update current
                        current.copy(lyrics = now.copy(translation = TranslationState.Failed))
                    }
                }
            }
        }
    }

    fun clearTranslation() {
        translationJob?.cancel()
        _state.update { current ->
            val now = current.lyrics as? LyricsState.Loaded ?: return@update current
            current.copy(lyrics = now.copy(translation = TranslationState.None))
        }
    }

    private fun syncState() {
        val ctrl = controller ?: return
        val currentIndex = ctrl.currentMediaItemIndex
        val mediaItem = ctrl.currentMediaItem
        val song = mediaItem?.let { mediaItemToLocalSong(it) }
        val queueSongs = (0 until ctrl.mediaItemCount).map { mediaItemToLocalSong(ctrl.getMediaItemAt(it)) }

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
            _state.update { it.copy(lyrics = LyricsState.None) }
        }
    }

    private fun syncDuration() {
        val ctrl = controller ?: return
        _state.update { it.copy(durationMs = ctrl.duration.coerceAtLeast(0L)) }
    }

    private fun loadLyrics(song: LocalSong) {
        lastLyricsSongId = song.id
        lyricsJob?.cancel()
        translationJob?.cancel()
        _state.update { it.copy(lyrics = LyricsState.Loading()) }
        lyricsJob = viewModelScope.launch {
            val result = runCatching {
                LyricsRepository.lyrics(
                    videoId = song.id,
                    title = song.title,
                    artist = song.artist,
                    durationMs = song.durationMs,
                    album = song.album,
                    sources = SettingsRepository.lyricsSources.value,
                    order = SettingsRepository.lyricsSourceOrder.value,
                    prioritizeSyllableSync = SettingsRepository.prioritizeSyllableSync.value,
                )
            }.getOrNull()
            if (lastLyricsSongId != song.id) return@launch
            _state.update {
                when {
                    result == null || result.lines.isEmpty() -> it.copy(lyrics = LyricsState.Missing)
                    else -> it.copy(lyrics = LyricsState.Loaded(result.lines, result.source))
                }
            }
        }
    }

    private fun mediaItemToLocalSong(item: MediaItem): LocalSong {
        val metadata = item.mediaMetadata
        val extras = metadata.extras
        return LocalSong(
            id = item.mediaId,
            title = metadata.title?.toString() ?: "Unknown",
            artist = metadata.artist?.toString() ?: "Unknown Artist",
            album = metadata.albumTitle?.toString(),
            contentUri = item.localConfiguration?.uri?.toString() ?: "",
            artworkUri = metadata.artworkUri?.toString(),
            durationMs = extras?.getLong("durationMs", 0L) ?: 0L,
            filePath = extras?.getString("filePath"),
            trackNumber = extras?.getInt("trackNumber", -1)?.takeIf { it > 0 },
            year = extras?.getInt("year", -1)?.takeIf { it > 0 },
            dateAddedMs = extras?.getLong("dateAddedMs", 0L) ?: 0L,
        )
    }

    private fun startPositionPolling() {
        positionJob?.cancel()
        positionJob = viewModelScope.launch {
            while (isActive) {
                val ctrl = controller ?: break
                val currentPos = ctrl.currentPosition.coerceAtLeast(0L)
                val delta = currentPos - lastTickPositionMs
                if (delta in 1..5000 && _state.value.currentSong != null && ctrl.isPlaying) {
                    ListeningStats.record(_state.value.currentSong!!, delta, false)
                }
                lastTickPositionMs = currentPos
                _state.update {
                    it.copy(
                        positionMs = currentPos,
                        durationMs = ctrl.duration.coerceAtLeast(0L),
                    )
                }
                delay(1000L)
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
        translationJob?.cancel()
        ListeningStats.flush()
        controller?.let { ctrl ->
            ctrl.removeListener(playerListener)
            ctrl.release()
        }
        controller = null
        super.onCleared()
    }
}
