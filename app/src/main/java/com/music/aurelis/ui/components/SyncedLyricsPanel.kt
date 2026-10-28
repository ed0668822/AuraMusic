package com.music.aurelis.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.aurelis.data.lyrics.Lyrics
import kotlinx.coroutines.delay

@Composable
fun SyncedLyricsPanel(
    lyrics: Lyrics,
    positionMs: Long,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val activeIndex = lyrics.activeIndexAt(positionMs)
    
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            delay(100)
            val targetIndex = (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(targetIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 150.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(lyrics.lines) { index, line ->
            val isActive = index == activeIndex
            val isPast = index < activeIndex

            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.05f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "lyricScale"
            )
            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else if (isPast) 0.3f else 0.5f,
                label = "lyricAlpha"
            )

            Text(
                text = line.text,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = if (isActive) 24.sp else 20.sp,
                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium
                ),
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .scale(scale)
                    .alpha(alpha)
            )
        }
    }
}
