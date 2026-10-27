package com.music.aura.ui.player

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.Composable
import com.music.aura.data.SettingsRepository
import com.music.aura.data.lyrics.LyricLine
import com.music.aura.data.lyrics.LyricsTranslation
import com.music.aura.data.model.LocalSong
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

/**
 * Everything the player takes from the Aura application it is drawn in.
 *
 * Simplified from BitChord's PlayerHost: no Canvas, no Cast, no Watch,
 * no Party, no streaming. Local music only.
 */
interface AuraPlayerHost {
    val settings: AuraPlayerSettingsSource

    /** The system output level the player's volume bar drives. */
    val volume: SystemVolume

    /** The outputs the output drawer lists, the active one marked. */
    @Composable
    fun rememberAudioOutputs(): List<AudioOutputDevice>

    /**
     * Wraps opening the output drawer in whatever it needs first.
     */
    @Composable
    fun rememberOutputPicker(onOpen: () -> Unit): () -> Unit

    fun selectAudioOutput(id: Int?)

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

    /** A short, non-blocking notice — a Toast on the phone. */
    fun showMessage(message: String)

    /** The full signal-chain readout. */
    @Composable
    fun AudioPipelineDialog(hazeState: HazeState, isPlaying: Boolean, onDismiss: () -> Unit)

    /**
     * Whether lines can be put on a share card here.
     */
    val lyricsShareAvailable: Boolean get() = false

    /**
     * The picked lines, drawn as a picture.
     */
    @Composable
    fun LyricsShareSheet(hazeState: HazeState, request: LyricsShareRequest, onDismiss: () -> Unit) = Unit
}

/** Where the installed [AuraPlayerHost] lives. */
object AuraPlayerPlatform {
    @Volatile
    private var installed: AuraPlayerHost? = null

    val host: AuraPlayerHost
        get() = installed ?: error("AuraPlayerPlatform.install() was never called")

    fun install(host: AuraPlayerHost) {
        installed = host
    }
}

/** The settings the player reads. */
interface AuraPlayerSettingsSource {
    val syncedLyrics: StateFlow<Boolean>
    val lyricsOffsetMs: StateFlow<Int>
    val hideVolumeBar: StateFlow<Boolean>
    val reduceDynamicBlur: StateFlow<Boolean>
    val reduceAnimation: StateFlow<Boolean>
    val spatialAudio: StateFlow<Boolean>
    val loudnessNormalization: StateFlow<Boolean>
    val equalizerEnabled: StateFlow<Boolean>

    fun setLyricsOffsetMs(value: Int)
}

/** The lyric offset sheet's range, in milliseconds either way. */
const val MIN_LYRICS_OFFSET_MS = -5_000
const val MAX_LYRICS_OFFSET_MS = 5_000

/** A system output level, 0..1, and the means to set it. */
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

/** Media3's repeat modes, by value. */
object RepeatModes {
    const val OFF = 0
    const val ONE = 1
    const val ALL = 2
}

/**
 * The phone's implementation of [AuraPlayerHost].
 */
class AndroidAuraPlayerHost(context: Context) : AuraPlayerHost {
    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val main = Handler(Looper.getMainLooper())

    override val settings: AuraPlayerSettingsSource = object : AuraPlayerSettingsSource {
        override val syncedLyrics get() = SettingsRepository.syncedLyrics
        override val lyricsOffsetMs get() = SettingsRepository.lyricsOffsetMs
        override val hideVolumeBar get() = SettingsRepository.hideVolumeBar
        override val reduceDynamicBlur get() = SettingsRepository.reduceDynamicBlur
        override val reduceAnimation get() = SettingsRepository.reduceAnimation
        override val spatialAudio get() = SettingsRepository.spatialAudio
        override val loudnessNormalization get() = SettingsRepository.loudnessNormalization
        override val equalizerEnabled get() = SettingsRepository.equalizerEnabled

        override fun setLyricsOffsetMs(value: Int) = SettingsRepository.setLyricsOffsetMs(value)
    }

    override val volume: SystemVolume by lazy { MusicStreamVolume(app) }

    @Composable
    override fun rememberAudioOutputs(): List<AudioOutputDevice> =
        rememberAndroidAudioOutputs()

    @Composable
    override fun rememberOutputPicker(onOpen: () -> Unit): () -> Unit =
        rememberAndroidOutputPicker(onOpen)

    override fun selectAudioOutput(id: Int?) {
        AudioRouting.select(id)
    }

    override suspend fun translateLyrics(
        trackId: String,
        lines: List<LyricLine>,
        targetLanguageTag: String,
    ): LyricsTranslationResult =
        when (val result = LyricsTranslation.translate(trackId, lines, targetLanguageTag)) {
            is LyricsTranslation.Result.Translated -> LyricsTranslationResult.Translated(result.lines)
            is LyricsTranslation.Result.SameLanguage -> LyricsTranslationResult.SameLanguage(result.language)
            LyricsTranslation.Result.Unavailable -> LyricsTranslationResult.Unavailable
        }

    override suspend fun romanizeLyrics(
        trackId: String,
        lines: List<LyricLine>,
        targetLanguageTag: String,
    ): LyricsRomanizationResult =
        when (val result = LyricsTranslation.romanize(trackId, lines, targetLanguageTag)) {
            is LyricsTranslation.RomanizationResult.Romanized -> LyricsRomanizationResult.Romanized(result.lines)
            LyricsTranslation.RomanizationResult.AlreadyRomanized -> LyricsRomanizationResult.AlreadyRomanized
            LyricsTranslation.RomanizationResult.Unavailable -> LyricsRomanizationResult.Unavailable
        }

    override fun showMessage(message: String) {
        main.post { Toast.makeText(app, message, Toast.LENGTH_SHORT).show() }
    }

    @Composable
    override fun AudioPipelineDialog(hazeState: HazeState, isPlaying: Boolean, onDismiss: () -> Unit) {
        // TODO: Simplified AudioPipelineDialog for Aura (deliver in next batch)
    }

    override val lyricsShareAvailable: Boolean get() = true

    @Composable
    override fun LyricsShareSheet(
        hazeState: HazeState,
        request: LyricsShareRequest,
        onDismiss: () -> Unit,
    ) = AndroidLyricsShareSheet(hazeState = hazeState, request = request, onDismiss = onDismiss)
}

/**
 * The music stream's volume. Hardware keys and the system panel change it
 * behind the app's back, so Settings is watched and the level follows.
 */
private class MusicStreamVolume(private val context: Context) : SystemVolume {
    private val manager = context.getSystemService(AudioManager::class.java)
    private val _level = MutableStateFlow(read())
    override val level: StateFlow<Float> = _level.asStateFlow()

    init {
        context.contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) = refresh()
            },
        )
    }

    private fun max(): Int = manager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 15

    private fun read(): Float =
        (manager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0).toFloat() / max()

    override fun set(level: Float) {
        manager?.setStreamVolume(AudioManager.STREAM_MUSIC, (level * max()).roundToInt(), 0)
    }

    override fun refresh() {
        _level.value = read()
    }
}

@Composable
internal fun rememberAudioOutputs(): List<AudioOutputDevice> =
    AuraPlayerPlatform.host.rememberAudioOutputs()

@Composable
internal fun rememberOutputPicker(onOpen: () -> Unit): () -> Unit =
    AuraPlayerPlatform.host.rememberOutputPicker(onOpen)

@Composable
internal fun AudioPipelineDialog(hazeState: HazeState, isPlaying: Boolean, onDismiss: () -> Unit) =
    AuraPlayerPlatform.host.AudioPipelineDialog(hazeState, isPlaying, onDismiss)
