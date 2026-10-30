// Aurelis © 2026. GPL-3.0-or-later.
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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

interface ShelfRowChrome {
    @Composable
    fun Wrap(state: LazyListState, row: @Composable () -> Unit)
}

private object NoShelfRowChrome : ShelfRowChrome {
    @Composable
    override fun Wrap(state: LazyListState, row: @Composable () -> Unit) = row()
}

val LocalShelfRowChrome = staticCompositionLocalOf<ShelfRowChrome> { NoShelfRowChrome }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShelfRow(
    contentPadding: PaddingValues,
    horizontalArrangement: Arrangement.Horizontal,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    LocalShelfRowChrome.current.Wrap(state) {
        LazyRow(
            modifier = modifier,
            state = state,
            contentPadding = contentPadding,
            horizontalArrangement = horizontalArrangement,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = state),
            content = content,
        )
    }
}
