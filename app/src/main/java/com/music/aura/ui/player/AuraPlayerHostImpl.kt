package com.music.aura.ui.player

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

/**
 * Implementación Android del host del player para Aura Music.
 * Adaptado de BitChord: sin Canvas, Cast, Watch, Party, streaming.
 */
class AuraPlayerHostImpl(context: Context) {
    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val main = Handler(Looper.getMainLooper())

    val volume: SystemVolume by lazy { MusicStreamVolume(app) }

    fun showMessage(message: String) {
        main.post { Toast.makeText(app, message, Toast.LENGTH_SHORT).show() }
    }
}

/**
 * Volumen del stream de música. Las teclas de hardware y el panel
 * del sistema lo cambian detrás de la app.
 */
internal class MusicStreamVolume(private val context: Context) : SystemVolume {
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
