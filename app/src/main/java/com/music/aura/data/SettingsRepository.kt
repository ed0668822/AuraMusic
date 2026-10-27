package com.music.aura.data

import android.content.Context
import android.content.SharedPreferences
import com.music.aura.playback.EqualizerPreset
import kotlinx.coroutines.flow.MutableStateFlow

enum class EqualizerMode { DYNAMIC, MANUAL }

object SettingsRepository {
    private lateinit var prefs: SharedPreferences

    // ── Existing ──────────────────────────────────────────────────────────────
    val themeMode = MutableStateFlow(0)
    val dolbyAtmos = MutableStateFlow(true)
    val liquidGlass = MutableStateFlow(true)
    val reduceAnimation = MutableStateFlow(false)
    val reduceDynamicBlur = MutableStateFlow(false)
    val syncedLyrics = MutableStateFlow(true)
    val blurUnfocusedLyrics = MutableStateFlow(true)
    val filterNonMusicAudio = MutableStateFlow(true)
    val autoplay = MutableStateFlow(true)
    val crossfadeSeconds = MutableStateFlow(0)

    // ── Equalizer ─────────────────────────────────────────────────────────────
    val equalizerEnabled = MutableStateFlow(false)
    val equalizerMode = MutableStateFlow(EqualizerMode.DYNAMIC)
    val equalizerToneX = MutableStateFlow(0)
    val equalizerToneY = MutableStateFlow(0)
    val equalizerFocused = MutableStateFlow(false)
    val equalizerBalance = MutableStateFlow(0f)
    val equalizerBands = MutableStateFlow(List(7) { 0f })
    val equalizerPreset = MutableStateFlow(EqualizerPreset.FLAT)

    // ── Spatial / Loudness ────────────────────────────────────────────────────
    val spatialAudio = MutableStateFlow(false)
    val loudnessNormalization = MutableStateFlow(false)

    // ── Player ────────────────────────────────────────────────────────────────
    val hideVolumeBar = MutableStateFlow(false)
    val showNerdStats = MutableStateFlow(false)
    val lyricsOffsetMs = MutableStateFlow(0)

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("aura_settings", Context.MODE_PRIVATE)
        themeMode.value = prefs.getInt("theme_mode", 0)
        dolbyAtmos.value = prefs.getBoolean("dolby_atmos", true)
        liquidGlass.value = prefs.getBoolean("liquid_glass", true)
        reduceAnimation.value = prefs.getBoolean("reduce_animation", false)
        reduceDynamicBlur.value = prefs.getBoolean("reduce_dynamic_blur", false)
        syncedLyrics.value = prefs.getBoolean("synced_lyrics", true)
        blurUnfocusedLyrics.value = prefs.getBoolean("blur_lyrics", true)
        filterNonMusicAudio.value = prefs.getBoolean("filter_non_music", true)
        autoplay.value = prefs.getBoolean("autoplay", true)
        crossfadeSeconds.value = prefs.getInt("crossfade", 0)

        equalizerEnabled.value = prefs.getBoolean("eq_enabled", false)
        equalizerMode.value = EqualizerMode.entries.getOrElse(prefs.getInt("eq_mode", 0)) { EqualizerMode.DYNAMIC }
        equalizerToneX.value = prefs.getInt("eq_tone_x", 0)
        equalizerToneY.value = prefs.getInt("eq_tone_y", 0)
        equalizerFocused.value = prefs.getBoolean("eq_focused", false)
        equalizerBalance.value = prefs.getFloat("eq_balance", 0f)
        equalizerBands.value = (0 until 7).map { prefs.getFloat("eq_band_$it", 0f) }
        equalizerPreset.value = runCatching {
            EqualizerPreset.valueOf(prefs.getString("eq_preset", "FLAT") ?: "FLAT")
        }.getOrDefault(EqualizerPreset.FLAT)

        spatialAudio.value = prefs.getBoolean("spatial_audio", false)
        loudnessNormalization.value = prefs.getBoolean("loudness_norm", false)
        hideVolumeBar.value = prefs.getBoolean("hide_volume_bar", false)
        showNerdStats.value = prefs.getBoolean("show_nerd_stats", false)
        lyricsOffsetMs.value = prefs.getInt("lyrics_offset_ms", 0)
    }

    // ── Existing setters ──────────────────────────────────────────────────────
    fun setThemeMode(value: Int) { themeMode.value = value; prefs.edit().putInt("theme_mode", value).apply() }
    fun setDolbyAtmos(value: Boolean) { dolbyAtmos.value = value; prefs.edit().putBoolean("dolby_atmos", value).apply() }
    fun setLiquidGlass(value: Boolean) { liquidGlass.value = value; prefs.edit().putBoolean("liquid_glass", value).apply() }
    fun setReduceAnimation(value: Boolean) { reduceAnimation.value = value; prefs.edit().putBoolean("reduce_animation", value).apply() }
    fun setReduceDynamicBlur(value: Boolean) { reduceDynamicBlur.value = value; prefs.edit().putBoolean("reduce_dynamic_blur", value).apply() }
    fun setSyncedLyrics(value: Boolean) { syncedLyrics.value = value; prefs.edit().putBoolean("synced_lyrics", value).apply() }
    fun setBlurUnfocusedLyrics(value: Boolean) { blurUnfocusedLyrics.value = value; prefs.edit().putBoolean("blur_lyrics", value).apply() }
    fun setFilterNonMusicAudio(value: Boolean) { filterNonMusicAudio.value = value; prefs.edit().putBoolean("filter_non_music", value).apply() }
    fun setAutoplay(value: Boolean) { autoplay.value = value; prefs.edit().putBoolean("autoplay", value).apply() }
    fun setCrossfadeSeconds(value: Int) { crossfadeSeconds.value = value; prefs.edit().putInt("crossfade", value).apply() }

    // ── Equalizer setters ─────────────────────────────────────────────────────
    fun setEqualizerEnabled(value: Boolean) { equalizerEnabled.value = value; prefs.edit().putBoolean("eq_enabled", value).apply() }
    fun setEqualizerMode(value: EqualizerMode) { equalizerMode.value = value; prefs.edit().putInt("eq_mode", value.ordinal).apply() }
    fun setEqualizerTone(x: Int, y: Int) {
        equalizerToneX.value = x; equalizerToneY.value = y
        prefs.edit().putInt("eq_tone_x", x).putInt("eq_tone_y", y).apply()
    }
    fun setEqualizerFocused(value: Boolean) { equalizerFocused.value = value; prefs.edit().putBoolean("eq_focused", value).apply() }
    fun setEqualizerBalance(value: Float) { equalizerBalance.value = value; prefs.edit().putFloat("eq_balance", value).apply() }
    fun setEqualizerBands(bands: List<Float>) {
        equalizerBands.value = bands
        val editor = prefs.edit()
        bands.forEachIndexed { i, v -> editor.putFloat("eq_band_$i", v) }
        editor.apply()
    }
    fun setEqualizerPreset(value: EqualizerPreset) {
        equalizerPreset.value = value
        prefs.edit().putString("eq_preset", value.name).apply()
        if (value != EqualizerPreset.CUSTOM) {
            setEqualizerBands(value.bands)
        }
    }

    // ── Spatial / Loudness setters ────────────────────────────────────────────
    fun setSpatialAudio(value: Boolean) { spatialAudio.value = value; prefs.edit().putBoolean("spatial_audio", value).apply() }
    fun setLoudnessNormalization(value: Boolean) { loudnessNormalization.value = value; prefs.edit().putBoolean("loudness_norm", value).apply() }

    // ── Player setters ────────────────────────────────────────────────────────
    fun setHideVolumeBar(value: Boolean) { hideVolumeBar.value = value; prefs.edit().putBoolean("hide_volume_bar", value).apply() }
    fun setShowNerdStats(value: Boolean) { showNerdStats.value = value; prefs.edit().putBoolean("show_nerd_stats", value).apply() }
    fun setLyricsOffsetMs(value: Int) { lyricsOffsetMs.value = value; prefs.edit().putInt("lyrics_offset_ms", value).apply() }
}
