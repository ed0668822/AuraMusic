package com.music.aurelis.playback

import androidx.media3.common.Player
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.QueueTier

private fun Player.queueTierAt(index: Int): QueueTier = getMediaItemAt(index).queueTier

object QueueCoordinator {

    fun LocalSong.asQueueEntry(tier: QueueTier): LocalSong = with(QueueTimeline) { asQueueEntry(tier) }

    fun buildContextQueue(
        currentTimeline: List<LocalSong>,
        currentIndex: Int,
        newContextSongs: List<LocalSong>,
        selectedIndex: Int,
        contextSource: QueueSource,
    ): ContextQueueResult = QueueTimeline.buildContextQueue(currentTimeline, currentIndex, newContextSongs, selectedIndex, contextSource)

    fun buildOneOffQueue(
        currentTimeline: List<LocalSong>,
        currentIndex: Int,
        tappedSong: LocalSong,
        source: QueueSource,
    ): List<LocalSong> = QueueTimeline.buildOneOffQueue(currentTimeline, currentIndex, tappedSong, source)

    fun findUserQueueInsertionIndex(
        timeline: List<LocalSong>,
        currentIndex: Int,
        isNext: Boolean,
    ): Int = QueueTimeline.findUserQueueInsertionIndex(timeline, currentIndex, isNext)

    fun clearUserQueue(player: Player, tierAt: (Int) -> QueueTier = player::queueTierAt) {
        val currentIndex = player.currentMediaItemIndex
        val count = player.mediaItemCount
        if (count == 0) return

        val userQueueIndices = mutableListOf<Int>()
        for (i in (currentIndex + 1) until count) {
            if (tierAt(i) == QueueTier.USER_QUEUE) userQueueIndices.add(i)
        }
        for (i in userQueueIndices.asReversed()) player.removeMediaItem(i)
    }

    fun consumePlayedUserQueue(player: Player, tierAt: (Int) -> QueueTier = player::queueTierAt) {
        val currentIndex = player.currentMediaItemIndex
        if (currentIndex <= 0) return
        if (player.currentMediaItem == null) return
        if (tierAt(currentIndex) != QueueTier.CONTEXT) return

        val playedIndices = mutableListOf<Int>()
        for (i in 0 until currentIndex) {
            if (tierAt(i) == QueueTier.USER_QUEUE) playedIndices.add(i)
        }
        for (i in playedIndices.asReversed()) player.removeMediaItem(i)
    }

    fun buildJumpQueue(
        currentTimeline: List<LocalSong>,
        currentIndex: Int,
        targetIndex: Int,
    ): List<LocalSong>? = QueueTimeline.buildJumpQueue(currentTimeline, currentIndex, targetIndex)

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

        val tiers = cachedTimeline?.takeIf { it.size == count }?.map { it.queueTier } ?: (0 until count).map { tierAt(it) }
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
        if (runCatching { player.playbackState }.getOrNull() == Player.STATE_IDLE) player.prepare()
        if (play) player.play()
    }
}
