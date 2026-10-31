// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.music.aurelis.data.lyrics.LyricLine
import com.music.aurelis.data.lyrics.LyricsSource
import com.music.aurelis.data.model.LocalSong
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface PlayerHost {
    val settings: PlayerSettingsSource

    @Composable
    fun rememberLocalArtworkUri(song: LocalSong?): String?

    val volume: SystemVolume

    @Composable
    fun rememberAudioOutputs(): List<AudioOutputDevice>

    @Composable
    fun rememberOutputPicker(onOpen: () -> Unit): () -> Unit

    fun selectAudioOutput(id: Int?)

    val outputFormat: StateFlow<OutputFormatUi>

    suspend fun translateLyrics(
        trackId: String,
        lines: List<LyricLine>,
        targetLanguageTag: String,
    ): LyricsTranslationResult

    suspend fun romanizeLyrics(
        trackId: String,
        lines: List<LyricLine>,
        targetLanguageTag: String,
    ): LyricsRomanizationResult

    fun showMessage(message: String)

    @Composable
    fun AudioPipelineDialog(hazeState: HazeState, isPlaying: Boolean, onDismiss: () -> Unit)

    @Composable
    fun rememberPlayerFrost(): PlayerFrost? = null

    val lyricsShareAvailable: Boolean get() = false

    @Composable
    fun LyricsShareSheet(hazeState: HazeState, request: LyricsShareRequest, onDismiss: () -> Unit) = Unit
}

object PlayerPlatform {
    @Volatile
    private var installed: PlayerHost? = null

    val host: PlayerHost
        get() = installed ?: error("PlayerPlatform.install() was never called")

    fun install(host: PlayerHost) {
        installed = host
    }
}

interface PlayerSettingsSource {
    val fullBleedArtwork: StateFlow<Boolean>
    val hideSongStatus: StateFlow<Boolean>
    val showQueueHistory: StateFlow<Boolean>
    val hideVolumeBar: StateFlow<Boolean>
    val hideStatusBar: StateFlow<Boolean>
    val lastPlayerScreen: StateFlow<LastPlayerScreen>
    val legacyMeshGradient: StateFlow<Boolean>
    val lyricsBlur: StateFlow<Boolean>
    val lyricsOffsetMs: StateFlow<Int>
    val lyricsSourceOrder: StateFlow<List<LyricsSource>>
    val preferUsbDac: StateFlow<Boolean>
    val reduceAnimation: StateFlow<Boolean>
    val reduceDynamicBlur: StateFlow<Boolean>
    val showNerdStats: StateFlow<Boolean>
    val smartAnalysis: StateFlow<SmartAnalysis>
    val automixDetail: StateFlow<String?>
    val smartFadeEnabled: StateFlow<Boolean>
    val smartMixInProgress: StateFlow<Boolean>
    val smartMixBlend: StateFlow<MixBlend?>
    val smartMixView: StateFlow<MixTransitionView?>
    val smartMixProgress: StateFlow<Float>
    val smartTransitionWindow: StateFlow<TransitionWindow?>
    val syncedLyrics: StateFlow<Boolean>
    val translationLanguage: StateFlow<String>
    val versionAlignmentInProgress: StateFlow<Boolean>

    fun setLastPlayerScreen(value: LastPlayerScreen)
    fun setLyricsOffsetMs(value: Int)
}

internal val PlayerSettings: PlayerSettingsSource
    get() = PlayerPlatform.host.settings

const val MIN_LYRICS_OFFSET_MS = -5_000
const val MAX_LYRICS_OFFSET_MS = 5_000

interface SystemVolume {
    val level: StateFlow<Float>
    fun set(level: Float)
    fun refresh()
}

enum class AudioOutputKind { PHONE, WIRED, USB, BLUETOOTH, HDMI, OTHER }

data class AudioOutputDevice(
    val id: Int,
    val name: String,
    val kind: AudioOutputKind,
    val isActive: Boolean = false,
    val form: BluetoothForm = BluetoothForm.UNKNOWN,
)

enum class BluetoothForm { UNKNOWN, BUDS, HEADPHONES, SPEAKER, CAR }

data class OutputFormatUi(
    val summary: String = "",
    val bitPerfect: Boolean = false,
    val dacLimited: Boolean = false,
)

sealed interface LyricsTranslationResult {
    data class Translated(val lines: List<LyricLine>) : LyricsTranslationResult
    data class SameLanguage(val language: String) : LyricsTranslationResult
    data object Unavailable : LyricsTranslationResult
}

sealed interface LyricsRomanizationResult {
    data class Romanized(val lines: List<LyricLine>) : LyricsRomanizationResult
    data object AlreadyRomanized : LyricsRomanizationResult
    data object Unavailable : LyricsRomanizationResult
}

enum class LastPlayerScreen { LYRICS, QUEUE, PLAYER }
enum class SmartAnalysis { OFF, BASIC, FULL }
data class MixBlend(val playing: Boolean, val beatMs: Float, val beatAnchorNanos: Long)
data class MixTransitionView(val label: String)
data class TransitionWindow(val start: Float, val end: Float)

object RepeatModes {
    const val OFF = 0
    const val ONE = 1
    const val ALL = 2
}

@Composable
internal fun rememberAudioOutputs(): List<AudioOutputDevice> = PlayerPlatform.host.rememberAudioOutputs()

@Composable
internal fun rememberOutputPicker(onOpen: () -> Unit): () -> Unit =
    PlayerPlatform.host.rememberOutputPicker(onOpen)

@Composable
internal fun rememberLocalArtworkUri(song: LocalSong?): String? =
    PlayerPlatform.host.rememberLocalArtworkUri(song)

@Composable
internal fun AudioPipelineDialog(hazeState: HazeState, isPlaying: Boolean, onDismiss: () -> Unit) =
    PlayerPlatform.host.AudioPipelineDialog(hazeState, isPlaying, onDismiss)
