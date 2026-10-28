package com.music.aurelis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.music.aurelis.R
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.components.MessageState
import com.music.aurelis.ui.components.PAGE_GUTTER
import com.music.aurelis.ui.components.PlayingAccent
import com.music.aurelis.ui.components.ROW_DIVIDER_INSET
import com.music.aurelis.ui.components.SearchField
import com.music.aurelis.ui.components.SongRow
import com.music.aurelis.ui.components.songListSkeleton
import com.music.aurelis.ui.components.contextClick

@Composable
fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<LocalSong>?,
    listState: LazyListState,
    focusRequested: Boolean,
    onFocusHandled: () -> Unit,
    onSongClick: (List<LocalSong>, Int) -> Unit,
    onSongLongPress: (LocalSong) -> Unit,
    onSongSwipe: (LocalSong) -> Unit,
    history: List<String>,
    onSubmit: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onHistoryClick: (String) -> Unit,
    onHistoryRemove: (String) -> Unit,
    onHistoryClear: () -> Unit,
    currentSong: LocalSong?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    topPadding: Dp,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(focusRequested) {
        if (focusRequested) {
            focusRequester.requestFocus()
            keyboardController?.show()
            onFocusHandled()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(top = topPadding)) {
            SearchField(
                query = query,
                onQueryChange = onQueryChange,
                onSubmit = onSubmit,
                focusRequester = focusRequester,
                placeholder = stringResource(R.string.search_hint),
                modifier = Modifier.padding(start = PAGE_GUTTER, end = PAGE_GUTTER, bottom = 4.dp),
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
        ) {
            when {
                results == null && query.isEmpty() -> {
                    if (history.isEmpty()) {
                        item { MessageState(stringResource(R.string.search_empty)) }
                    } else {
                        recentSearches(
                            history = history,
                            onClick = onHistoryClick,
                            onRemove = onHistoryRemove,
                            onClear = onHistoryClear,
                        )
                    }
                }
                results == null -> {
                    item { MessageState(stringResource(R.string.search_library_empty)) }
                }
                results.isEmpty() -> {
                    item {
                        MessageState(
                            stringResource(R.string.search_library_no_results, query),
                        )
                    }
                }
                else -> {
                    itemsIndexed(results, key = { index, song -> "local_${song.id}_$index" }) { index, song ->
                        SongRow(
                            song = song,
                            onClick = { onSongClick(results, index) },
                            onLongPress = { onSongLongPress(song) },
                            onSwipeToQueue = { onSongSwipe(song) },
                            isCurrent = currentSong?.id == song.id,
                            isPlaying = isPlaying && currentSong?.id == song.id,
                            searchPlayingStyle = true,
                            activeTint = PlayingAccent,
                        )
                        if (index < results.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = ROW_DIVIDER_INSET),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.recentSearches(
    history: List<String>,
    onClick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
) {
    item(key = "recent:header") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PAGE_GUTTER, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.recent_searches),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.clear),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .clickable(onClick = onClear)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
    itemsIndexed(history, key = { _, term -> "recent:$term" }) { _, term ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(term) }
                .padding(start = PAGE_GUTTER, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = term,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onRemove(term) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
