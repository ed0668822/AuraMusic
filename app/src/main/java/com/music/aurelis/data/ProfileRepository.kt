package com.music.aurelis.data

import android.content.Context
import android.content.SharedPreferences

object ProfileRepository {
    private const val PREFS_NAME = "aurelis_profile"
    private const val KEY_NAME = "user_name"
    private const val KEY_PHOTO = "user_photo"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var userName: String
        get() = if (::prefs.isInitialized) prefs.getString(KEY_NAME, "Aurelis User") ?: "Aurelis User" else "Aurelis User"
        set(value) = prefs.edit().putString(KEY_NAME, value).apply()

    var userPhotoUri: String?
        get() = if (::prefs.isInitialized) prefs.getString(KEY_PHOTO, null) else null
        set(value) = prefs.edit().putString(KEY_PHOTO, value).apply()
}
