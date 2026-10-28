package com.music.aurelis.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.music.aurelis.R
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.ui.components.MessageState
import com.music.aurelis.ui.components.PlayingAccent
import com.music.aurelis.ui.components.ROW_DIVIDER_INSET
import com.music.aurelis.ui.components.SongRow
import com.music.aurelis.ui.components.songListSkeleton

@Composable
fun HistoryScreen(
    songs: List<LocalSong>,
    loading: Boolean,
    listState: LazyListState,
    onSongClick: (List<LocalSong>, Int) -> Unit,
    onSongLongPress: (LocalSong) -> Unit,
    onSongSwipe: (LocalSong) -> Unit,
    contentPadding: PaddingValues,
    currentSong: LocalSong? = null,
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        when {
            loading -> songListSkeleton(count = 10, keyPrefix = "skeleton:history")
            songs.isEmpty() -> item(key = "history:empty") {
                MessageState(stringResource(R.string.history_empty))
            }
            else -> {
                items(songs.size, key = { "${songs[it].id}:$it" }) { index ->
                    val song = songs[index]
                    SongRow(
                        song = song,
                        onClick = { onSongClick(songs, index) },
                        onLongPress = { onSongLongPress(song) },
                        onSwipeToQueue = { onSongSwipe(song) },
                        isCurrent = currentSong?.id == song.id,
                        isPlaying = isPlaying && currentSong?.id == song.id,
                        searchPlayingStyle = true,
                        activeTint = PlayingAccent,
                    )
                    if (index < songs.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = ROW_DIVIDER_INSET),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }
    }
}
