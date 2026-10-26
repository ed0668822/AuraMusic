package com.music.aura.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.music.aura.data.model.LocalSong
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Todo lo que el player necesita de la aplicación Aura Music.
 *
 * Adaptado de BitChord: se eliminan Canvas, Party, Cast, Watch,
 * streaming, traducción/romanización de letras
