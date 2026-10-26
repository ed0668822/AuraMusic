package com.music.aura.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object SettingsRepository {
    private lateinit var prefs: SharedPreferences

    // Flows para que la UI reaccione a los cambios
    val themeMode = MutableStateFlow(0) // 0=Dark, 1=Light, 2=System
    val dolbyAtmos = MutableStateFlow(true)
    val liquidGlass = MutableStateFlow(true)
    val syncedLyrics = MutableStateFlow(true)
    val blurUnfocusedLyrics = MutableStateFlow(true)
    val filterNonMusicAudio = MutableStateFlow(true)
    val autoplay = MutableStateFlow(true)
    val crossfadeSeconds = MutableStateFlow(0)

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("aura_settings", Context.MODE_PRIVATE)
        themeMode.value = prefs.getInt("theme_mode", 0)
        dolbyAtmos.value = prefs.getBoolean("dolby_atmos", true)
        liquidGlass.value = prefs.getBoolean("liquid_glass", true)
        syncedLyrics.value = prefs.getBoolean("synced_lyrics", true)
        blurUnfocusedLyrics.value = prefs.getBoolean("blur_lyrics", true)
        filterNonMusicAudio.value = prefs.getBoolean("filter_non_music", true)
        autoplay.value = prefs.getBoolean("autoplay", true)
        crossfadeSeconds.value = prefs.getInt("crossfade", 0)
    }

    fun setThemeMode(value: Int) {
        themeMode.value = value
        prefs.edit().putInt("theme_mode", value).apply()
    }

    fun setDolbyAtmos(value: Boolean) {
        dolbyAtmos.value = value
        prefs.edit().putBoolean("dolby_atmos", value).apply()
    }

    fun setLiquidGlass(value: Boolean) {
        liquidGlass.value = value
        prefs.edit().putBoolean("liquid_glass", value).apply()
    }

    fun setSyncedLyrics(value: Boolean) {
        syncedLyrics.value = value
        prefs.edit().putBoolean("synced_lyrics", value).apply()
    }

    fun setBlurUnfocusedLyrics(value: Boolean) {
        blurUnfocusedLyrics.value = value
        prefs.edit().putBoolean("blur_lyrics", value).apply()
    }

    fun setFilterNonMusicAudio(value: Boolean) {
        filterNonMusicAudio.value = value
        prefs.edit().putBoolean("filter_non_music", value).apply()
    }

    fun setAutoplay(value: Boolean) {
        autoplay.value = value
        prefs.edit().putBoolean("autoplay", value).apply()
    }

    fun setCrossfadeSeconds(value: Int) {
        crossfadeSeconds.value = value
        prefs.edit().putInt("crossfade", value).apply()
    }
}
