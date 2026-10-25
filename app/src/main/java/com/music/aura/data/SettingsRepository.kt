package com.music.aura.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Preferencias de la app, persistidas en SharedPreferences.
 *
 * Solo ajustes de interfaz: tema. Nada de red, nada de cuentas.
 */
object SettingsRepository {

    private const val PREFS_NAME = "aura_settings"
    private const val KEY_THEME_MODE = "theme_mode"

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
}
