// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.music.aurelis.R
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.theme.ArtworkPalette
import java.util.Locale
import java.util.concurrent.TimeUnit

private val INFO_BODY_MAX_HEIGHT = 360.dp

@Composable
internal fun SongInfoDialog(song: LocalSong, onDismiss: () -> Unit) {
    val onSurface = MaterialTheme.colorScheme.onSurface

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .width(320.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 19.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.song_info),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.W600),
                    color = onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = song.title,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 17.sp),
                    color = onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (song.artist.isNotBlank()) {
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 17.sp),
                        color = onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = onSurface.copy(alpha = 0.12f))

            Column(Modifier.heightIn(max = INFO_BODY_MAX_HEIGHT).verticalScroll(rememberScrollState())) {
                val durationFormatted = formatDuration(song.durationMs)
                val sizeFormatted = formatFileSize(song.sizeBytes)

                val rows = listOf(
                    stringResource(R.string.song_info_album) to song.album,
                    stringResource(R.string.song_info_artist) to song.artist,
                    stringResource(R.string.song_info_year) to song.year?.toString(),
                    stringResource(R.string.song_info_duration) to durationFormatted,
                    stringResource(R.string.song_info_size) to sizeFormatted,
                    stringResource(R.string.song_info_path) to song.filePath,
                )
                rows.forEachIndexed { index, (label, value) ->
                    if (index > 0) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = onSurface.copy(alpha = 0.08f),
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                    SongInfoRow(label = label, value = value)
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = onSurface.copy(alpha = 0.12f))

            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.done),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.W600),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun SongInfoRow(label: String, value: String?) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
            color = onSurface.copy(alpha = 0.6f),
        )
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            Text(
                text = value ?: "—",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                color = onSurface,
                textAlign = TextAlign.End,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun SongInfoPage(song: LocalSong, palette: ArtworkPalette, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 22.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.song_info),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = listOf(song.title, song.artist).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = palette.divider)

        val durationFormatted = formatDuration(song.durationMs)
        val sizeFormatted = formatFileSize(song.sizeBytes)

        listOf(
            stringResource(R.string.song_info_album) to song.album,
            stringResource(R.string.song_info_artist) to song.artist,
            stringResource(R.string.song_info_year) to song.year?.toString(),
            stringResource(R.string.song_info_duration) to durationFormatted,
            stringResource(R.string.song_info_size) to sizeFormatted,
            stringResource(R.string.song_info_path) to song.filePath,
        ).forEach { (label, value) ->
            SongInfoPageRow(label, value ?: "—", palette)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SongInfoPageRow(label: String, value: String, palette: ArtworkPalette) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.width(16.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = palette.accent,
                textAlign = TextAlign.End,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return "%d:%02d".format(Locale.ROOT, minutes, seconds)
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> "%.2f GB".format(Locale.ROOT, bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> "%.1f MB".format(Locale.ROOT, bytes / 1_048_576.0)
        bytes >= 1024 -> "%d KB".format(Locale.ROOT, bytes / 1024)
        else -> "$bytes B"
    }
}
