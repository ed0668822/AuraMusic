// Aurelis © 2026. GPL-3.0-or-later
package com.music.aurelis.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.StateFlow

interface AppUiHost {
    val swipeToPlayNext: StateFlow<Boolean>
    val pinnedPlaylists: StateFlow<List<String>>

    @Composable
    fun downloadedIds(): Set<String>
}

object AppUi {
    @Volatile
    private var installed: AppUiHost? = null

    fun install(host: AppUiHost) {
        installed = host
    }

    val host: AppUiHost
        get() = checkNotNull(installed) { "AppUi.install was not called" }
}

val LocalPullToRefreshEnabled = staticCompositionLocalOf { true }
