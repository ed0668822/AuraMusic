package com.music.aura.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.music.aura.R
import com.music.aura.ui.components.PAGE_GUTTER
import com.music.aura.ui.components.ReplayCardRowSkeleton
import com.music.aura.ui.replay.ReplayCardRow
import com.music.aura.ui.replay.ReplayHeroCard
import com.music.aura.ui.replay.ReplayStoryPage

/**
 * The phone's library links: All Songs and Folders.
 * Simplified for local-only playback.
 */
data class LibraryLink(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String,
)

@Composable
fun libraryLinks(): List<LibraryLink> {
    return listOf(
        LibraryLink(
            title = stringResource(R.string.all_songs),
            subtitle = stringResource(R.string.audio_files_on_device),
            icon = Icons.Rounded.MusicNote,
            route = "local:all",
        ),
        LibraryLink(
            title = stringResource(R.string.folders),
            subtitle = stringResource(R.string.browse_by_folder),
            icon = Icons.Rounded.Folder,
            route = "local:folders",
        ),
    )
}

/**
 * The phone's way in to Replay.
 */
@Composable
fun LibraryReplayEntry(
    cards: List<ReplayHeroCard>,
    loading: Boolean,
    holder: String,
    memberSince: String?,
    onOpenReplay: (ReplayStoryPage) -> Unit,
) {
    if (loading) {
        ReplayCardRowSkeleton()
    } else if (cards.isNotEmpty()) {
        ReplayCardRow(
            cards = cards,
            holder = holder,
            memberSince = memberSince,
            onCardClick = onOpenReplay,
            modifier = Modifier.padding(vertical = 6.dp),
            contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
        )
    }
}
