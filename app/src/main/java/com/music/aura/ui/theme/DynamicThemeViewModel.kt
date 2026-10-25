package com.music.aura.ui.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel que gestiona los colores dinámicos extraídos del artwork.
 * Observa los cambios de canción y extrae los colores correspondientes.
 */
class DynamicThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val _artworkColors = MutableStateFlow<ArtworkColors?>(null)
    val artworkColors: StateFlow<ArtworkColors?> = _artworkColors.asStateFlow()

    private var lastArtworkUri: String? = null

    /**
     * Extrae colores para un artwork dado.
     * Solo procesa si la URI cambió (evita trabajo redundante).
     */
    fun extractForArtwork(artworkUri: String?) {
        if (artworkUri == lastArtworkUri) return
        lastArtworkUri = artworkUri

        if (artworkUri.isNullOrBlank()) {
            _artworkColors.value = null
            return
        }

        viewModelScope.launch {
            val context = getApplication<Application>()
            val colors = ArtworkPalette.extractColors(context, artworkUri)
            _artworkColors.value = colors
        }
    }

    /**
     * Limpia los colores (cuando no hay canción).
     */
    fun clear() {
        lastArtworkUri = null
        _artworkColors.value = null
    }
}
