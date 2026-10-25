package com.music.aura.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Controles de transporte del reproductor:
 * anterior, play/pausa, siguiente.
 */
@Composable
fun PlayerControls(
    isPlaying: Boolean,
    isLoading: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Anterior
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Anterior",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(36.dp),
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        // Play / Pausa / Loading
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(72.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
            )
        } else {
            FilledIconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(72.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    modifier = Modifier.size(40.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(24.dp))

        // Siguiente
        IconButton(
            onClick = onNext,
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Siguiente",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}
