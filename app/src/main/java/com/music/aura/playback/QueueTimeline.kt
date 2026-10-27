package com.music.aura.playback

import com.music.aura.data.model.LocalSong
import com.music.aura.data.model.PlaybackSourceType
import com.music.aura.data.model.QueueTier
import java.util.UUID

/**
 * Information about where a queue or track was started from in the UI.
 */
data class QueueSource(
    val title: String,
    val type: PlaybackSourceType,
    val id: String? = null,
)

/**
 * Result of constructing a context queue, containing the reconstructed timeline
 * and the 0-based start index representing the user's tapped track.
 */
data class ContextQueueResult(
    val timeline: List<LocalSong>,
    val startIndex: Int,
)

/**
 * Plan for executing a queue jump, indicating whether the operation is a pure seek,
 * the index permutation of original timeline items to keep/reorder, the new target index,
 * and whether the target track should be promoted to [QueueTier.CONTEXT].
 */
data class JumpOrderPlan(
    val isPureSeek: Boolean,
    val newOrderIndices: List<Int>,
    val newTargetIndex: Int,
    val promoteTargetToContext: Boolean = false,
)

/**
 * The two-tier queue's rules, as plain list edits.
 *
 * CONTEXT: the album, playlist, artist or folder being played.
 * USER_QUEUE: tracks manually added via "Play Next" or "Add to Queue".
 */
object QueueTimeline {

    /**
     * Converts a [LocalSong] into a queue entry belonging to [tier].
     *
     * Invariant: [LocalSong.queueEntryId] is assigned ONCE when entering the queue.
     */
    fun LocalSong.asQueueEntry(tier: QueueTier): LocalSong = copy(
        queueTier = tier,
        queueEntryId = queueEntryId ?: UUID.randomUUID().toString(),
    )

    private fun upcomingUserQueue(timeline: List<LocalSong>, currentIndex: Int): List<LocalSong> =
        if (currentIndex in timeline.indices) {
            timeline.subList(currentIndex + 1, timeline.size)
                .filter { it.queueTier == QueueTier.USER_QUEUE }
        } else {
            emptyList()
        }

    private fun LocalSong.from(source: QueueSource): LocalSong = copy(
        playbackSource = source.title,
        playbackSourceType = source.type,
        playbackSourceId = source.id,
    )

    /**
     * Constructs an interleaved queue for starting a Context (Album, Playlist, Artist, Folder):
     *
     * Invariant: [Preceding Context] + [Selected Track] + [Preserved USER_QUEUE] + [Following Context Tracks].
     */
    fun buildContextQueue(
        currentTimeline: List<LocalSong>,
        currentIndex: Int,
        newContextSongs: List<LocalSong>,
        selectedIndex: Int,
        contextSource: QueueSource,
    ): ContextQueueResult {
        if (newContextSongs.isEmpty()) return ContextQueueResult(emptyList(), 0)

        val upcomingUserQueue = upcomingUserQueue(currentTimeline, currentIndex)
        val contextEntries = newContextSongs.map { it.from(contextSource).asQueueEntry(QueueTier.CONTEXT) }

        val safeIndex = selectedIndex.coerceIn(contextEntries.indices)
        val precedingContext = contextEntries.subList(0, safeIndex)
        val selected = contextEntries[safeIndex]
        val followingContext = contextEntries.subList(safeIndex + 1, contextEntries.size)

        return ContextQueueResult(
            timeline = precedingContext + listOf(selected) + upcomingUserQueue + followingContext,
            startIndex = precedingContext.size,
        )
    }

    /**
     * Constructs a queue for playing a one-off song (from Search, Home):
     *
     * Invariant: [Tapped Track] + [Preserved USER_QUEUE].
     */
    fun buildOneOffQueue(
        currentTimeline: List<LocalSong>,
        currentIndex: Int,
        tappedSong: LocalSong,
        source: QueueSource,
    ): List<LocalSong> =
        listOf(tappedSong.from(source).asQueueEntry(QueueTier.CONTEXT)) +
            upcomingUserQueue(currentTimeline, currentIndex)

    /**
     * Finds the insertion index for user-queued tracks.
     *
     * Invariant:
     * - "Play Next" (isNext = true) inserts at the head of USER_QUEUE.
     * - "Add to Queue" (isNext = false) inserts at the tail of USER_QUEUE.
     */
    fun findUserQueueInsertionIndex(
        timeline: List<LocalSong>,
        currentIndex: Int,
        isNext: Boolean,
    ): Int {
        if (timeline.isEmpty()) return 0
        val start = (currentIndex + 1).coerceIn(0, timeline.size)
        if (isNext) return start
        for (i in start until timeline.size) {
            if (timeline[i].queueTier != QueueTier.USER_QUEUE) return i
        }
        return timeline.size
    }

    /**
     * Computes the jump plan and index permutation for tapping a track at [targetIndex].
     *
     * Invariants:
     * 1. Backward or same track: pure seek.
     * 2. CONTEXT: if no USER_QUEUE items were crossed, pure seek. Otherwise, pin user queue after target.
     * 3. USER_QUEUE: Bypassed manual queue items are pruned.
     */
    fun computeJumpOrder(
        tiers: List<QueueTier>,
        currentIndex: Int,
        targetIndex: Int,
    ): JumpOrderPlan? {
        if (currentIndex !in tiers.indices || targetIndex !in tiers.indices) return null
        if (targetIndex <= currentIndex) {
            return JumpOrderPlan(
                isPureSeek = true,
                newOrderIndices = tiers.indices.toList(),
                newTargetIndex = targetIndex,
            )
        }

        val targetTier = tiers[targetIndex]
        return when (targetTier) {
            QueueTier.CONTEXT -> {
                val crossedUserQueue = (currentIndex + 1 until targetIndex).filter { tiers[it] == QueueTier.USER_QUEUE }
                if (crossedUserQueue.isEmpty()) {
                    JumpOrderPlan(
                        isPureSeek = true,
                        newOrderIndices = tiers.indices.toList(),
                        newTargetIndex = targetIndex,
                    )
                } else {
                    val precedingHistory = (0..currentIndex).toList()
                    val bypassedContext = (currentIndex + 1 until targetIndex).filter { tiers[it] == QueueTier.CONTEXT }
                    val target = listOf(targetIndex)
                    val followingUserQueue = (targetIndex + 1 until tiers.size).filter { tiers[it] == QueueTier.USER_QUEUE }
                    val allUpcomingUserQueue = crossedUserQueue + followingUserQueue
                    val followingContext = (targetIndex + 1 until tiers.size).filter { tiers[it] == QueueTier.CONTEXT }

                    val order = precedingHistory + bypassedContext + target + allUpcomingUserQueue + followingContext
                    val newTarget = precedingHistory.size + bypassedContext.size
                    JumpOrderPlan(isPureSeek = false, newOrderIndices = order, newTargetIndex = newTarget)
                }
            }
            QueueTier.USER_QUEUE -> {
                val history = (0..currentIndex).toList()
                val target = listOf(targetIndex)
                val subsequentUserQueue = (targetIndex + 1 until tiers.size).filter { tiers[it] == QueueTier.USER_QUEUE }
                val upcomingContext = (currentIndex + 1 until tiers.size).filter { tiers[it] == QueueTier.CONTEXT }

                val order = history + target + subsequentUserQueue + upcomingContext
                JumpOrderPlan(isPureSeek = false, newOrderIndices = order, newTargetIndex = history.size)
            }
        }
    }

    /**
     * Reconstructs the upcoming queue when a listener taps an item in the queue.
     */
    fun buildJumpQueue(
        currentTimeline: List<LocalSong>,
        currentIndex: Int,
        targetIndex: Int,
    ): List<LocalSong>? {
        val tiers = currentTimeline.map { it.queueTier }
        val plan = computeJumpOrder(tiers, currentIndex, targetIndex) ?: return null
        if (plan.isPureSeek && targetIndex <= currentIndex) return null

        val targetSong = currentTimeline[targetIndex]
        return plan.newOrderIndices.drop(plan.newTargetIndex).map { idx ->
            val song = currentTimeline[idx]
            if (idx == targetIndex && plan.promoteTargetToContext) {
                val sourceTitle = targetSong.playbackSource?.ifBlank { targetSong.title } ?: targetSong.title
                song.copy(
                    playbackSource = sourceTitle,
                    playbackSourceType = targetSong.playbackSourceType ?: PlaybackSourceType.QUEUE,
                ).asQueueEntry(QueueTier.CONTEXT)
            } else {
                song
            }
        }
    }

    /** Indices of the upcoming USER_QUEUE rows — what "Clear" removes. */
    fun upcomingUserQueueIndices(tiers: List<QueueTier>, currentIndex: Int): List<Int> =
        ((currentIndex + 1).coerceAtLeast(0) until tiers.size).filter { tiers[it] == QueueTier.USER_QUEUE }

    /**
     * Indices of played USER_QUEUE rows to prune once playback has moved into CONTEXT.
     */
    fun playedUserQueueIndices(tiers: List<QueueTier>, currentIndex: Int): List<Int> {
        if (currentIndex !in tiers.indices || currentIndex == 0) return emptyList()
        if (tiers[currentIndex] != QueueTier.CONTEXT) return emptyList()
        return (0 until currentIndex).filter { tiers[it] == QueueTier.USER_QUEUE }
    }

    /**
     * The order a queue goes in when it is started while shuffle is on.
     */
    fun shuffledStartingOrder(songs: List<LocalSong>, startIndex: Int): List<LocalSong> {
        if (songs.isEmpty()) return songs
        val at = startIndex.coerceIn(songs.indices)
        val rest = songs.filterIndexed { i, _ -> i != at }
        return listOf(songs[at]) +
            rest.filter { it.queueTier == QueueTier.USER_QUEUE } +
            rest.filter { it.queueTier == QueueTier.CONTEXT }.shuffled()
    }

    /**
     * Shuffle applied to what is still to come: USER_QUEUE is never shuffled,
     * CONTEXT is shuffled within its own section.
     */
    fun shuffledUpcomingOrder(upcoming: List<QueueTier>): List<Int> {
        val user = upcoming.indices.filter { upcoming[it] == QueueTier.USER_QUEUE }
        val context = upcoming.indices.filter { upcoming[it] == QueueTier.CONTEXT }
        return user + avoidIdentityShuffle(context, context.shuffled())
    }

    /**
     * A permutation that puts restored upcoming rows back in their sections.
     */
    fun sectionedOrder(restored: List<Int>, upcoming: List<QueueTier>): List<Int> =
        restored.filter { upcoming[it] == QueueTier.USER_QUEUE } +
            restored.filter { upcoming[it] == QueueTier.CONTEXT }

    /**
     * Never returns a section's unchanged order when at least two tracks can move.
     */
    fun avoidIdentityShuffle(original: List<Int>, shuffled: List<Int>): List<Int> {
        if (original.size <= 1 || shuffled != original) return shuffled
        return shuffled.drop(1) + shuffled.first()
    }
}
