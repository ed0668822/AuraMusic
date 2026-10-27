package com.music.aura.playback

import androidx.media3.common.Player
import com.music.aura.data.model.LocalSong
import com.music.aura.data.model.QueueTier

/**
 * The tier of the queue row at [index].
 */
private fun Player.queueTierAt(index: Int): QueueTier = getMediaItemAt(index).queueTier

/**
 * Headless orchestrator for two-tier queue operations.
 *
 * Owns timeline construction, tier assignment, deterministic queue identity assignment,
 * and user-queue pruning invariants. The list rules themselves live in [QueueTimeline];
 * this applies them to a [Player].
 */
object QueueCoordinator {

    /** @see QueueTimeline.asQueueEntry */
    fun LocalSong.asQueueEntry(tier: QueueTier): LocalSong = with(QueueTimeline) { asQueueEntry(tier) }

    /** @see QueueTimeline.buildContextQueue */
    fun buildContextQueue(
        currentTimeline: List<LocalSong>,
        currentIndex: Int,
        newContextSongs: List<LocalSong>,
        selectedIndex: Int,
        contextSource: QueueSource,
    ): ContextQueueResult = QueueTimeline.buildContextQueue(
        currentTimeline, currentIndex, newContextSongs, selectedIndex, contextSource,
    )

    /** @see QueueTimeline.buildOneOffQueue */
    fun buildOneOffQueue(
        currentTimeline: List<LocalSong>,
        currentIndex: Int,
        tappedSong: LocalSong,
        source: QueueSource,
    ): List<LocalSong> = QueueTimeline.buildOneOffQueue(currentTimeline, currentIndex, tappedSong, source)

    /** @see QueueTimeline.findUserQueueInsertionIndex */
    fun findUserQueueInsertionIndex(
        timeline: List<LocalSong>,
        currentIndex: Int,
        isNext: Boolean,
    ): Int = QueueTimeline.findUserQueueInsertionIndex(timeline, currentIndex, isNext)

    /**
     * Clears only the upcoming USER_QUEUE items from the player's timeline.
     *
     * Invariant: CONTEXT tracks are completely untouched.
     */
    fun clearUserQueue(player: Player, tierAt: (Int) -> QueueTier = player::queueTierAt) {
        val currentIndex = player.currentMediaItemIndex
        val count = player.mediaItemCount
        if (count == 0) return

        val userQueueIndices = mutableListOf<Int>()
        for (i in (currentIndex + 1) until count) {
            if (tierAt(i) == QueueTier.USER_QUEUE) {
                userQueueIndices.add(i)
            }
        }

        for (i in userQueueIndices.asReversed()) {
            player.removeMediaItem(i)
        }
    }

    /**
     * Prunes played USER_QUEUE items once playback has transitioned into CONTEXT.
     */
    fun consumePlayedUserQueue(player: Player, tierAt: (Int) -> QueueTier = player::queueTierAt) {
        val currentIndex = player.currentMediaItemIndex
        if (currentIndex <= 0) return

        if (player.currentMediaItem == null) return
        if (tierAt(currentIndex) != QueueTier.CONTEXT) return

        val playedIndices = mutableListOf<Int>()
        for (i in 0 until currentIndex) {
            if (tierAt(i) == QueueTier.USER_QUEUE) {
                playedIndices.add(i)
            }
        }

        for (i in playedIndices.asReversed()) {
            player.removeMediaItem(i)
        }
    }

    /** @see QueueTimeline.buildJumpQueue */
    fun buildJumpQueue(
        currentTimeline: List<LocalSong>,
        currentIndex: Int,
        targetIndex: Int,
    ): List<LocalSong>? = QueueTimeline.buildJumpQueue(currentTimeline, currentIndex, targetIndex)

    /**
     * Executes a semantic queue jump on [player], preserving history.
     */
    fun jumpToQueueItem(
        player: Player,
        targetIndex: Int,
        cachedTimeline: List<LocalSong>? = null,
        tierAt: (Int) -> QueueTier = player::queueTierAt,
        positionMs: Long = 0L,
        play: Boolean = true,
    ) {
        val currentIndex = player.currentMediaItemIndex
        val count = player.mediaItemCount
        if (targetIndex !in 0 until count) return

        val tiers = cachedTimeline?.takeIf { it.size == count }?.map { it.queueTier }
            ?: (0 until count).map { tierAt(it) }
        val plan = QueueTimeline.computeJumpOrder(tiers, currentIndex, targetIndex) ?: return

        if (plan.isPureSeek) {
            player.seekTo(plan.newTargetIndex, positionMs)
            if (play) player.play()
            return
        }

        val existingItems = (0 until count).map { player.getMediaItemAt(it) }
        val newItems = plan.newOrderIndices.map { idx ->
            val item = existingItems[idx]
            if (idx == targetIndex && plan.promoteTargetToContext) {
                QueueShuffle.withQueueMetadata(item, newTier = QueueTier.CONTEXT)
            } else {
                item
            }
        }

        val historyKept = plan.newOrderIndices.take(currentIndex + 1) == (0..currentIndex).toList()
        if (historyKept) {
            QueueShuffle.replaceChanged(player, currentIndex + 1, count, newItems.drop(currentIndex + 1))
            player.seekTo(plan.newTargetIndex, positionMs)
        } else {
            player.setMediaItems(newItems, plan.newTargetIndex, positionMs)
        }
        if (runCatching { player.playbackState }.getOrNull() == Player.STATE_IDLE) {
            player.prepare()
        }
        if (play) player.play()
    }
}
