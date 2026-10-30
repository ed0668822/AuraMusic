// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.aurelis.R
import com.music.aurelis.ui.components.*
import com.music.aurelis.ui.icons.AurelisIcons

data class LibraryLink(val title: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
fun LibraryScreen(
    listState: LazyListState,
    links: List<LibraryLink>,
    onShowAll: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    showTitle: Boolean = true,
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        if (showTitle) {
            item {
                Text(
                    text = stringResource(R.string.library),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
                )
            }
        }
        if (links.isNotEmpty()) {
            item(key = "links") { LibraryLinkList(links = links) }
        }
    }
}

@Composable
private fun LibraryLinkList(links: List<LibraryLink>) {
    Column(Modifier.padding(top = 4.dp, bottom = 22.dp)) {
        links.forEachIndexed { index, link ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .clickable { link.onClick() }
                    .padding(horizontal = PAGE_GUTTER),
            ) {
                Icon(
                    imageVector = link.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = link.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = AurelisIcons.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .6f),
                    modifier = Modifier.size(20.dp),
                )
            }
            if (index < links.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = PAGE_GUTTER + 24.dp + 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
fun LibraryGridPage(
    shelf: com.music.aurelis.data.model.LocalAlbum,
    gridState: LazyGridState,
    onAlbumClick: (com.music.aurelis.data.model.LocalAlbum) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(140.dp),
        state = gridState,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(LIBRARY_GRID_SPACING),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier.fillMaxSize().padding(horizontal = PAGE_GUTTER),
    ) {
        items(1, key = { shelf.key }) {
            AlbumGridCard(album = shelf, onClick = { onAlbumClick(shelf) })
        }
    }
}

@Composable
private fun AlbumGridCard(
    album: com.music.aurelis.data.model.LocalAlbum,
    onClick: () -> Unit,
) {
    Column(Modifier.clickable(onClick = onClick)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (album.artworkUri != null) {
                coil3.compose.AsyncImage(
                    model = album.artworkUri,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(
            text = album.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
