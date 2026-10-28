package com.music.aurelis.ui

import androidx.compose.runtime.staticCompositionLocalOf

object AppUi {
    @Volatile
    private var installed: AppUiHost? = null

    fun install(host: AppUiHost) {
        installed = host
    }

    val host: AppUiHost
        get() = checkNotNull(installed) { "AppUi.install was not called" }
}

interface AppUiHost {
    val reduceAnimation: Boolean
    val reduceDynamicBlur: Boolean
}

val LocalPullToRefreshEnabled = staticCompositionLocalOf { true }
