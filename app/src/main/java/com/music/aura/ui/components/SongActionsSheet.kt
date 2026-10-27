package com.music.aura.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlaylistRemove
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.aura.R
import com.music.aura.data.model.LocalSong
import com.music.aura.ui.theme.ArtworkPalette
import com.music.aura.ui.theme.rememberArtworkPalette

@Composable
fun SongActionsSheet(
    song: LocalSong,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onOpenAlbum: () -> Unit,
    onOpenArtist: () -> Unit,
    modifier: Modifier = Modifier,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    showSleepTimer: Boolean = false,
    onLyricsOffset: (() -> Unit)? = null,
    presentation: SongActionsPresentation = SongActionsPresentation.Sheet,
) {
    var showingSongInfo by remember(song.id) { mutableStateOf(false) }
    var pickingSleepTimer by remember { mutableStateOf(false) }
    val menu = presentation == SongActionsPresentation.Menu

    if (menu && showingSongInfo) {
        SongInfoDialog(song) { showingSongInfo = false }
    }

    val songArtUri: Uri? = song.artworkUri?.let { runCatching { Uri.parse(it) }.getOrNull() }

    val palette = if (menu) {
        neutralMenuPalette()
    } else {
        rememberArtworkPalette(songArtUri)
    }

    val rows: @Composable ColumnScope.() -> Unit = {
        ActionRow(
            icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
            label = stringResource(R.string.play_next),
            accent = palette.accent,
            onClick = onPlayNext,
        )
        ActionRow(
            icon = Icons.AutoMirrored.Rounded.QueueMusic,
            label = stringResource(R.string.add_to_queue),
            accent = palette.accent,
            onClick = onAddToQueue,
        )
        ActionRow(
            icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
            label = stringResource(R.string.add_to_playlist),
            accent = palette.accent,
            onClick = onAddToPlaylist,
        )
        onRemoveFromPlaylist?.let {
            ActionRow(
                icon = Icons.Rounded.PlaylistRemove,
                label = stringResource(R.string.remove_from_playlist),
                accent = palette.accent,
                onClick = it,
            )
        }
        GroupSeparator(palette)
        ActionRow(
            Icons.Rounded.Album,
            stringResource(R.string.open_album),
            accent = palette.accent,
        ) { onOpenAlbum() }
        ActionRow(
            Icons.Rounded.Person,
            stringResource(R.string.open_artist),
            accent = palette.accent,
        ) { onOpenArtist() }
        ActionRow(Icons.Rounded.Info, stringResource(R.string.song_info), accent = palette.accent) {
            showingSongInfo = true
        }
        if (showSleepTimer) {
            ActionRow(
                icon = Icons.Rounded.Bedtime,
                label = stringResource(R.string.sleep_timer),
                accent = palette.accent,
            ) { pickingSleepTimer = true }
        }
        onLyricsOffset?.let {
            ActionRow(
                icon = Icons.Rounded.Tune,
                label = stringResource(R.string.lyrics_offset),
                accent = palette.accent,
                onClick = it,
            )
        }
    }

    if (menu) {
        CompositionLocalProvider(LocalActionRowStyle provides ActionRowStyle.Menu) {
            Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()), content = rows)
        }
        return
    }

    TintedSheet(palette = palette, artworkUri = songArtUri, modifier = modifier) {
        if (pickingSleepTimer) {
            SleepTimerPicker(palette = palette, onBack = { pickingSleepTimer = false })
            return@TintedSheet
        }
        if (showingSongInfo) {
            SongInfoPage(song = song, palette = palette, onBack = { showingSongInfo = false })
            return@TintedSheet
        }

        SheetTrackHeader(song, subtitleColor = palette.onBackgroundVariant)
        HorizontalDivider(thickness = 0.5.dp, color = palette.divider)
        rows()
        Spacer(Modifier.height(24.dp))
    }
}

enum class SongActionsPresentation { Sheet, Menu }

internal enum class ActionRowStyle { Sheet, Menu }

internal val LocalActionRowStyle = staticCompositionLocalOf { ActionRowStyle.Sheet }

@Composable
private fun neutralMenuPalette(): ArtworkPalette {
    val scheme = MaterialTheme.colorScheme
    return remember(scheme) {
        ArtworkPalette(
            background = scheme.surfaceContainerHigh,
            wash = scheme.surfaceContainerHigh,
            elevated = scheme.surfaceContainerHighest,
            accent = scheme.primary,
            onBackground = scheme.onSurface,
            onBackgroundVariant = scheme.onSurfaceVariant,
            divider = scheme.onSurface.copy(alpha = 0.08f),
        )
    }
}

@Composable
private fun GroupSeparator(palette: ArtworkPalette) {
    if (LocalActionRowStyle.current == ActionRowStyle.Menu) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(palette.divider),
        )
    } else {
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 6.dp),
            thickness = 0.5.dp,
            color = palette.divider,
        )
    }
}

@Composable
private fun TintedSheet(
    palette: ArtworkPalette,
    artworkUri: Uri?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.85f
    Box(
        modifier
            .fillMaxWidth()
            .clip(SHEET_SHAPE),
    ) {
        ArtworkBackdrop(
            palette = palette,
            artworkUri = artworkUri,
            modifier = Modifier.matchParentSize(),
            washFraction = 0.75f,
        )
        Column(Modifier.fillMaxWidth().heightIn(max = maxHeight)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(width = 34.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(palette.onBackground.copy(alpha = 0.35f)),
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                content = content,
            )
        }
    }
}

private val SHEET_SHAPE = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

@Composable
private fun SleepTimerPicker(palette: ArtworkPalette, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 22.dp, bottom = 8.dp),
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
                    text = stringResource(R.string.sleep_timer),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = palette.divider)

        val presets = listOf(5, 10, 15, 30, 45, 60)
        presets.forEach { minutes ->
            SleepOption(
                label = if (minutes == 60) {
                    stringResource(R.string.one_hour)
                } else {
                    pluralStringResource(R.plurals.minute_count, minutes, minutes)
                },
                selected = false,
                accent = palette.accent,
            ) {
                onBack()
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SleepOption(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
internal fun ActionRow(
    icon: ImageVector,
    label: String,
    value: String? = null,
    tint: Color? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val menu = LocalActionRowStyle.current == ActionRowStyle.Menu
    val foreground = if (menu) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onBackground
    val alpha = if (enabled) 1f else 0.4f
    val iconView: @Composable () -> Unit = {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = (tint ?: foreground).copy(alpha = alpha),
            modifier = Modifier.size(if (menu) 20.dp else 22.dp),
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .then(
                if (menu) {
                    Modifier.heightIn(min = 46.dp).padding(horizontal = 18.dp, vertical = 11.dp)
                } else {
                    Modifier.padding(horizontal = 22.dp, vertical = 15.dp)
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!menu) {
            iconView()
            Spacer(Modifier.width(18.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = foreground.copy(alpha = alpha),
            maxLines = if (menu) 2 else Int.MAX_VALUE,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = accent.copy(alpha = alpha),
                maxLines = 1,
            )
        }
        if (menu) {
            Spacer(Modifier.width(14.dp))
            iconView()
        }
    }
}

@Composable
internal fun SheetTrackHeader(
    song: LocalSong,
    modifier: Modifier = Modifier,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.artworkUri,
            contentDescription = null,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun SheetHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 4.dp),
    )
}
