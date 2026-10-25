package com.music.aura.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Preferencias de la app, persistidas en SharedPreferences.
 *
 * Solo ajustes de interfaz: tema y offset de letras. Nada de red, nada de cuentas.
 */
object SettingsRepository {

    private const val PREFS_NAME = "aura_settings"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_LYRICS_OFFSET_MS = "lyrics_offset_ms"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Modo de tema:
     * 0 = Oscuro (por defecto)
     * 1 = Claro
     * 2 = Seguir sistema
     */
    var themeMode: Int
        get() = prefs.getInt(KEY_THEME_MODE, 0)
        set(value) = prefs.edit().putInt(KEY_THEME_MODE, value).apply()

    /**
     * Ajuste fino de sincronización de letras en milisegundos.
     * Positivo retrasa la letra; negativo la adelanta.
     */
    var lyricsOffsetMs: Int
        get() = prefs.getInt(KEY_LYRICS_OFFSET_MS, 0)
        set(value) = prefs.edit().putInt(KEY_LYRICS_OFFSET_MS, value).apply()
}
