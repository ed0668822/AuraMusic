package com.music.aura.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.aura.data.model.LocalSong

/**
 * Fila individual de una canción en la lista de la biblioteca.
 *
 * Muestra artwork, título (con badge explícito si `isExplicit == true`),
 * artista, badge de calidad (si `sourceQualityBadge()` no es null),
 * barras de ecualizador cuando suena y duración.
 *
 * Fase 2 Wave 2 — Decisión A (Opción C): el badge de calidad solo aparece
 * cuando `LocalSong.sourceQuality` esté poblado. Hasta Fase 8 (NerdStats
 * real) ese campo es `null`, así que el badge no se dibuja; la integración
 * ya está lista para cuando el escáner empiece a rellenarlo.
 */
@Composable
fun SongListItem(
    song: LocalSong,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleColor = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Artwork del álbum
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            if (song.artworkUri != null) {
                AsyncImage(
                    model = song.artworkUri,
                    contentDescription = "Artwork de ${song.album ?: song.title}",
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = "♪",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Título (con badge explícito si aplica) y artista
        Column(modifier = Modifier.weight(1f)) {
            ExplicitSongTitle(
                song = song,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Badge de calidad (Opción A: junto a la duración, a la derecha)
        song.sourceQualityBadge()?.let { quality ->
            SourceQualityBadge(
                quality = quality,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Indicador de reproducción o duración
        if (isPlaying) {
            EqualizerBars(isPlaying = true)
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = song.durationText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
