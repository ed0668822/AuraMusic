package com.music.aura.playback

data class BoundedQueueEditState(
    val contextGeneration: Long = 0L,
    val desiredShuffle: Boolean? = null,
    val refreshPending: Boolean = false,
    val topUpPending: Boolean = false,
) {
    val hasPendingEdits: Boolean
        get() = desiredShuffle != null || refreshPending || topUpPending
}

data class BoundedQueueEdit(
    val contextGeneration: Long,
    val desiredShuffle: Boolean?,
    val refresh: Boolean,
    val topUp: Boolean,
)

data class BoundedQueueSettlement(
    val state: BoundedQueueEditState,
    val edit: BoundedQueueEdit?,
) {
    val drained: Boolean get() = edit != null
}

object BoundedQueueEdits {

    fun requestShuffle(
        state: BoundedQueueEditState,
        desiredShuffle: Boolean,
        contextGeneration: Long = state.contextGeneration,
    ): BoundedQueueEditState = state.accepts(contextGeneration).let { accepted ->
        if (!accepted) state else state.copy(desiredShuffle = desiredShuffle)
    }

    fun requestRefresh(
        state: BoundedQueueEditState,
        contextGeneration: Long = state.contextGeneration,
    ): BoundedQueueEditState = state.accepts(contextGeneration).let { accepted ->
        if (!accepted) state else state.copy(refreshPending = true)
    }

    fun requestTopUp(
        state: BoundedQueueEditState,
        contextGeneration: Long = state.contextGeneration,
    ): BoundedQueueEditState = state.accepts(contextGeneration).let { accepted ->
        if (!accepted) state else state.copy(topUpPending = true)
    }

    fun invalidateContext(state: BoundedQueueEditState): BoundedQueueEditState {
        check(state.contextGeneration < Long.MAX_VALUE) { "context generation exhausted" }
        return BoundedQueueEditState(contextGeneration = state.contextGeneration + 1L)
    }

    fun defer(state: BoundedQueueEditState): BoundedQueueSettlement =
        BoundedQueueSettlement(state = state, edit = null)

    fun settle(
        state: BoundedQueueEditState,
        settledGeneration: Long,
    ): BoundedQueueSettlement {
        if (settledGeneration != state.contextGeneration || !state.hasPendingEdits) {
            return BoundedQueueSettlement(state = state, edit = null)
        }
        return BoundedQueueSettlement(
            state = state.copy(
                desiredShuffle = null,
                refreshPending = false,
                topUpPending = false,
            ),
            edit = BoundedQueueEdit(
                contextGeneration = state.contextGeneration,
                desiredShuffle = state.desiredShuffle,
                refresh = state.refreshPending,
                topUp = state.topUpPending,
            ),
        )
    }

    private fun BoundedQueueEditState.accepts(contextGeneration: Long): Boolean =
        contextGeneration == this.contextGeneration
}
