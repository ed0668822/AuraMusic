package com.music.aura.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.aura.R
import com.music.aura.data.model.LocalSong
import com.music.aura.ui.icons.AuraIcons

enum class LocalBrowseType { ALBUM, ARTIST, PLAYLIST, OTHER }

data class BrowseTarget(
    val id: String?,
    val title: String,
    val subtitle: String,
    val artworkUri: Uri? = null,
    val type: LocalBrowseType = LocalBrowseType.OTHER,
    val songs: List<LocalSong> = emptyList(),
    val isLocalPlaylist: Boolean = false,
    val fromCard: Boolean = true,
)

@Composable
fun BrowseActionsSheet(
    target: BrowseTarget,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    modifier: Modifier = Modifier,
    onPlay: (() -> Unit)? = null,
    onShuffle: (() -> Unit)? = null,
    onOpen: (() -> Unit)? = null,
    onRename: ((String) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    presentation: SongActionsPresentation = SongActionsPresentation.Sheet,
) {
    val menu = presentation == SongActionsPresentation.Menu
    var confirmingDelete by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalActionRowStyle provides if (menu) ActionRowStyle.Menu else ActionRowStyle.Sheet) {
        Column(
            modifier
                .fillMaxWidth()
                .then(if (menu) Modifier.verticalScroll(rememberScrollState()) else Modifier),
        ) {
            if (!menu) {
                BrowseSheetHeader(target)
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
            }

            onPlay?.let { ActionRow(Icons.Rounded.PlayArrow, stringResource(R.string.play), onClick = it) }
            onShuffle?.let { ActionRow(AuraIcons.Shuffle, stringResource(R.string.shuffle), onClick = it) }
            ActionRow(
                Icons.AutoMirrored.Rounded.PlaylistPlay,
                stringResource(R.string.play_next),
                onClick = onPlayNext,
            )
            ActionRow(
                Icons.AutoMirrored.Rounded.QueueMusic,
                stringResource(R.string.add_to_queue),
                onClick = onAddToQueue,
            )
            onOpen?.let {
                ActionRow(
                    AuraIcons.ChevronRight,
                    when (target.type) {
                        LocalBrowseType.ALBUM -> stringResource(R.string.open_album)
                        LocalBrowseType.ARTIST -> stringResource(R.string.open_artist)
                        LocalBrowseType.PLAYLIST -> stringResource(R.string.open_playlist)
                        LocalBrowseType.OTHER -> stringResource(R.string.open)
                    },
                    onClick = it,
                )
            }
            if (onRename != null) {
                ActionRow(Icons.Rounded.Edit, stringResource(R.string.rename)) {
                    // TODO: Abrir formulario de renombrar
                }
            }
            if (onDelete != null) {
                if (confirmingDelete) {
                    ActionRow(
                        icon = Icons.Rounded.DeleteForever,
                        label = stringResource(R.string.delete_playlist_confirmation, target.title),
                        tint = MaterialTheme.colorScheme.error,
                        onClick = onDelete,
                    )
                } else {
                    ActionRow(Icons.Rounded.Delete, stringResource(R.string.delete_playlist)) { confirmingDelete = true }
                }
            }
            if (!menu) Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BrowseSheetHeader(target: BrowseTarget) {
    val shape = if (target.type == LocalBrowseType.ARTIST) CircleShape else RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = target.artworkUri,
            contentDescription = null,
            modifier = Modifier
                .size(52.dp)
                .clip(shape)
                .thumbnailBorder(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = target.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = target.subtitle.ifBlank {
                    when (target.type) {
                        LocalBrowseType.ALBUM -> stringResource(R.string.album)
                        LocalBrowseType.PLAYLIST -> stringResource(R.string.playlist)
                        LocalBrowseType.ARTIST -> stringResource(R.string.artist)
                        LocalBrowseType.OTHER -> ""
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
