package com.music.aura.ui.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DynamicThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val _artworkColors = MutableStateFlow<ArtworkColors?>(null)
    val artworkColors: StateFlow<ArtworkColors?> = _artworkColors.asStateFlow()

    private var lastArtworkUri: String? = null

    fun extractForArtwork(artworkUri: String?) {
        if (artworkUri == lastArtworkUri) return
        lastArtworkUri = artworkUri

        if (artworkUri.isNullOrBlank()) {
            _artworkColors.value = null
            return
        }

        viewModelScope.launch {
            _artworkColors.value = ArtworkPalette.extractColors(getApplication(), artworkUri)
        }
    }

    fun clear() {
        lastArtworkUri = null
        _artworkColors.value = null
    }
}
