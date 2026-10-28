package com.music.aurelis.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShelfRow(
    contentPadding: PaddingValues,
    horizontalArrangement: Arrangement.Horizontal,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    LazyRow(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        horizontalArrangement = horizontalArrangement,
        flingBehavior = rememberSnapFlingBehavior(lazyListState = state),
        content = content,
    )
}
