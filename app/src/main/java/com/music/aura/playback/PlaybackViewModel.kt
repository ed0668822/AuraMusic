package com.music.aura.playback

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import com.music.aura.data.model.LocalSong
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Estado completo de la reproducción para la UI.
 */
data class PlaybackState(
    val currentSong: LocalSong? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<LocalSong> = emptyList(),
    val currentIndex: Int = -1,
    val isConnected: Boolean = false,
)

/**
 * ViewModel que gestiona la conexión con AuraPlaybackService
 * a través de MediaController y expone el estado de reproducción.
 */
class PlaybackViewModel(application: Application) : AndroidViewModel(application) {

    private var controller: MediaController? = null
    private var positionJob: Job? = null

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    /** Listener que observa cambios del reproductor. */
    private val playerListener = object : Player.Listener {

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            syncState()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) startPositionPolling() else stopPositionPolling()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val loading = playbackState == Player.STATE_BUFFERING
            _state.update { it.copy(isLoading = loading) }
            if (playbackState == Player.STATE_READY) {
                syncDuration()
            }
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            syncState()
        }
    }

    /**
     * Conecta al servicio de reproducción.
     * Debe llamarse una vez al iniciar la app.
     */
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

    /**
     * Reproduce una lista de canciones desde [startIndex].
     */
    fun playSongs(songs: List<LocalSong>, startIndex: Int = 0) {
        viewModelScope.launch {
            val ctrl = controller ?: return@launch
            _state.update {
                it.copy(
                    queue = songs,
                    currentIndex = startIndex,
                    currentSong = songs.getOrNull(startIndex),
                    isLoading = true,
                )
            }
            PlayerConnection.playSongs(ctrl, songs, startIndex)
        }
    }

    /** Alterna entre reproducir y pausar. */
    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) {
            ctrl.pause()
        } else {
            ctrl.play()
        }
    }

    /** Salta a la siguiente canción. */
    fun next() {
        controller?.seekToNextMediaItem()
    }

    /** Vuelve a la canción anterior. */
    fun previous() {
        controller?.seekToPreviousMediaItem()
    }

    /** Salta a una posición en milisegundos. */
    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _state.update { it.copy(positionMs = positionMs) }
    }

    /**
     * Salta directamente a un índice de la cola.
     */
    fun seekToIndex(index: Int) {
        val ctrl = controller ?: return
        if (index < 0 || index >= ctrl.mediaItemCount) return
        ctrl.seekToDefaultPosition(index)
        ctrl.play()
        _state.update {
            it.copy(
                currentIndex = index,
                currentSong = it.queue.getOrNull(index),
            )
        }
    }

    /**
     * Elimina una canción de la cola por índice.
     * No permite eliminar la canción actualmente en reproducción.
     */
    fun removeFromQueue(index: Int) {
        val ctrl = controller ?: return
        val currentIdx = ctrl.currentMediaItemIndex

        // No eliminar la canción que está sonando
        if (index == currentIdx) return
        // No eliminar si solo queda una canción
        if (ctrl.mediaItemCount <= 1) return

        ctrl.removeMediaItem(index)

        // Actualizar estado local
        _state.update { currentState ->
            val newQueue = currentState.queue.toMutableList()
            if (index < newQueue.size) {
                newQueue.removeAt(index)
            }
            val newCurrentIndex = if (index < currentIdx) currentIdx - 1 else currentIdx
            currentState.copy(
                queue = newQueue,
                currentIndex = newCurrentIndex,
            )
        }
    }

    /**
     * Sincroniza el estado completo desde el MediaController.
     */
    private fun syncState() {
        val ctrl = controller ?: return
        val currentIndex = ctrl.currentMediaItemIndex
        val mediaItem = ctrl.currentMediaItem

        val song = if (mediaItem != null) {
            mediaItemToLocalSong(mediaItem)
        } else null

        // Reconstruir la cola completa desde el timeline
        val queueSongs = mutableListOf<LocalSong>()
        for (i in 0 until ctrl.mediaItemCount) {
            val item = ctrl.getMediaItemAt(i)
            queueSongs.add(mediaItemToLocalSong(item))
        }

        _state.update {
            it.copy(
                currentSong = song,
                currentIndex = currentIndex,
                isPlaying = ctrl.isPlaying,
                isLoading = ctrl.playbackState == Player.STATE_BUFFERING,
                positionMs = ctrl.currentPosition.coerceAtLeast(0L),
                durationMs = ctrl.duration.coerceAtLeast(0L),
                queue = queueSongs,
            )
        }
    }

    /** Actualiza solo la duración (cuando el estado pasa a READY). */
    private fun syncDuration() {
        val ctrl = controller ?: return
        _state.update { it.copy(durationMs = ctrl.duration.coerceAtLeast(0L)) }
    }

    /**
     * Convierte un MediaItem de vuelta a LocalSong.
     * Los datos viajan en los extras del MediaMetadata.
     */
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
        )
    }

    /** Inicia el sondeo periódico de posición mientras reproduce. */
    private fun startPositionPolling() {
        positionJob?.cancel()
        positionJob = viewModelScope.launch {
            while (isActive) {
                val ctrl = controller ?: break
                _state.update {
                    it.copy(
                        positionMs = ctrl.currentPosition.coerceAtLeast(0L),
                        durationMs = ctrl.duration.coerceAtLeast(0L),
                    )
                }
                delay(500L)
            }
        }
    }

    /** Detiene el sondeo de posición. */
    private fun stopPositionPolling() {
        positionJob?.cancel()
        positionJob = null
    }

    override fun onCleared() {
        positionJob?.cancel()
        controller?.let { ctrl ->
            ctrl.removeListener(playerListener)
            MediaController.releaseFuture(
                com.google.common.util.concurrent.Futures.immediateFuture(ctrl)
            )
        }
        controller = null
        super.onCleared()
    }
}
