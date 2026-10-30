// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.playback

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.music.aurelis.data.model.LocalSong
import com.music.aurelis.data.model.QueueTier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

object QueueShuffle {

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    fun setEnabled(enabled: Boolean) { _enabled.value = enabled }

    @Volatile private var original: List<String> = emptyList()

    internal class CanonicalCtx(
        val items: List<MediaItem>,
        val byEntry: Map<String, Int>,
        val byMediaId: Map<String, Int>,
    )

    @Volatile private var ctx: CanonicalCtx? = null

    internal var canonicalContext: List<MediaItem>
        get() = ctx?.items ?: emptyList()
        set(value) { if (value.isEmpty()) clearCanonicalContext() else setCanonicalContext(value) }

    internal var isCrossfadeTransitioning: (() -> Boolean)? = null

    fun setCanonicalContext(items: List<MediaItem>) {
        val byEntry = HashMap<String, Int>(items.size)
        val byMediaId = HashMap<String, Int>(items.size)
        val stamped = items.mapIndexed { idx, item ->
            val entryId = item.queueEntryId ?: UUID.randomUUID().toString()
            val stampedItem = withQueueMetadata(item, newEntryId = entryId, newCanonicalIndex = idx)
            byEntry[entryId] = idx
            byMediaId.putIfAbsent(item.mediaId, idx)
            stampedItem
        }
        ctx = CanonicalCtx(stamped, byEntry, byMediaId)
    }

    fun clearCanonicalContext() { ctx = null }

    internal fun isCanonicalContextValid(player: Player, c: CanonicalCtx? = ctx): Boolean {
        val currentCtx = c ?: return false
        if (currentCtx.items.isEmpty()) return false
        val count = player.mediaItemCount
        val cur = player.currentMediaItemIndex
        if (cur !in 0 until count) return true
        val currentItem = player.getMediaItemAt(cur)
        if (currentItem.queueTier != QueueTier.CONTEXT) return true
        val entryId = currentItem.queueEntryId
        return if (entryId != null) currentCtx.byEntry.containsKey(entryId)
               else currentCtx.byMediaId.containsKey(currentItem.mediaId)
    }

    fun getCanonicalIndex(item: MediaItem): Int? = getCanonicalIndex(item, ctx)

    internal fun getCanonicalIndex(item: MediaItem, c: CanonicalCtx?): Int? {
        val extraIndex = item.canonicalIndex
        if (extraIndex != null && extraIndex >= 0) return extraIndex
        val currentCtx = c ?: return null
        val entryId = item.queueEntryId
        if (entryId != null) {
            val fromProv = currentCtx.byEntry[entryId]
            if (fromProv != null) return fromProv
        }
        return currentCtx.byMediaId[item.mediaId]
    }

    fun withQueueMetadata(
        item: MediaItem,
        newEntryId: String? = item.queueEntryId,
        newCanonicalIndex: Int? = item.canonicalIndex,
        newTier: QueueTier = item.queueTier,
        newContextCycleId: Long? = item.contextCycleId,
    ): MediaItem {
        val currentExtras = item.mediaMetadata.extras ?: Bundle()
        val newExtras = Bundle(currentExtras).apply {
            if (newEntryId != null) putString(EXTRA_QUEUE_ENTRY_ID, newEntryId)
            if (newCanonicalIndex != null) putInt(EXTRA_CANONICAL_INDEX, newCanonicalIndex)
            if (newContextCycleId != null) putLong(EXTRA_CONTEXT_CYCLE_ID, newContextCycleId)
            putString(EXTRA_QUEUE_TIER, newTier.name)
        }
        val newMetadata = item.mediaMetadata.buildUpon().setExtras(newExtras).build()
        val newItem = item.buildUpon().setMediaMetadata(newMetadata).build()
        if (newEntryId != null) mediaItemEntryIds[newItem] = newEntryId
        if (newCanonicalIndex != null) mediaItemCanonicalIndices[newItem] = newCanonicalIndex
        mediaItemTiers[newItem] = newTier
        return newItem
    }

    internal fun isNextItemImminent(player: Player): Boolean {
        if (isCrossfadeTransitioning?.invoke() == true) return true
        val duration = runCatching { player.duration }.getOrDefault(0L)
        val position = runCatching { player.currentPosition }.getOrDefault(0L)
        if (duration > 0 && duration != androidx.media3.common.C.TIME_UNSET && position >= 0) {
            if (duration - position <= 5000L) return true
        }
        return false
    }

    fun toggle(player: Player) {
        val c = ctx
        if (c != null && !isCanonicalContextValid(player, c)) clearCanonicalContext()
        if (_enabled.value) restore(player) else shuffle(player)
    }

    fun enableForNextQueue() {
        original = emptyList()
        clearCanonicalContext()
        _enabled.value = true
    }

    fun startingOrder(songs: List<LocalSong>, startIndex: Int): List<LocalSong> {
        original = songs.map { "${it.queueEntryId ?: it.videoId}_${it.queueTier.name}" }
        val contextSongs = songs.filter { it.queueTier == QueueTier.CONTEXT }
        if (contextSongs.isNotEmpty()) {
            setCanonicalContext(contextSongs.mapIndexed { idx, s ->
                val entryId = s.queueEntryId ?: UUID.randomUUID().toString()
                withQueueMetadata(s.toMediaItem(), newEntryId = entryId, newCanonicalIndex = idx)
            })
        }
        val rest = songs.filterIndexed { i, _ -> i != startIndex }
        val userQueue = rest.filter { it.queueTier == QueueTier.USER_QUEUE }
        val context = rest.filter { it.queueTier == QueueTier.CONTEXT }
        val shuffledContext = if (context.size > 1) {
            val s = context.shuffled()
            if (s == context) s.drop(1) + s.first() else s
        } else context
        val autoplay = rest.filter { it.queueTier == QueueTier.AUTOPLAY }.shuffled()
        return listOf(songs[startIndex]) + userQueue + shuffledContext + autoplay
    }

    private fun shuffle(player: Player) {
        val c = ctx
        if (c != null && !isCanonicalContextValid(player, c)) clearCanonicalContext()
        val items = player.queueItems()
        val currentIndex = player.currentMediaItemIndex
        val imminent = isNextItemImminent(player)
        val from = if (imminent) currentIndex + 2 else currentIndex + 1
        original = items.map { it.key() }
        if (currentIndex !in items.indices || from >= items.size) { _enabled.value = true; return }
        if (ctx == null) {
            val contextItems = items.filter { it.queueTier == QueueTier.CONTEXT }
            if (contextItems.isNotEmpty()) setCanonicalContext(contextItems)
        }
        val upcoming = items.drop(from)
        val userQueueIndices = upcoming.indices.filter { upcoming[it].queueTier == QueueTier.USER_QUEUE }
        val contextIndices = upcoming.indices.filter { upcoming[it].queueTier == QueueTier.CONTEXT }
        val autoplayIndices = upcoming.indices.filter { upcoming[it].queueTier == QueueTier.AUTOPLAY }
        val shuffledContext = if (contextIndices.size > 1) avoidIdentityShuffle(contextIndices, contextIndices.shuffled())
                              else contextIndices
        val order = userQueueIndices + shuffledContext + autoplayIndices
        val ok = reorder(player, from, order.toIntArray())
        if (ok) _enabled.value = true
    }

    internal fun avoidIdentityShuffle(original: List<Int>, shuffled: List<Int>): List<Int> {
        if (original.size <= 1 || shuffled != original) return shuffled
        return shuffled.drop(1) + shuffled.first()
    }

    private fun restore(player: Player) {
        var c = ctx
        if (c != null && !isCanonicalContextValid(player, c)) { clearCanonicalContext(); c = null }
        val items = player.queueItems()
        val currentIndex = player.currentMediaItemIndex
        val imminent = isNextItemImminent(player)
        val from = if (imminent) currentIndex + 2 else currentIndex + 1
        if (currentIndex !in items.indices || currentIndex + 1 >= items.size) {
            original = emptyList(); _enabled.value = false; return
        }
        val currentItem = items[currentIndex]
        val isCurrentContext = currentItem.queueTier == QueueTier.CONTEXT
        var currentCanonicalIndex: Int? = null
        if (isCurrentContext) currentCanonicalIndex = getCanonicalIndex(currentItem, c)
        else {
            for (i in (currentIndex - 1) downTo 0) {
                val itm = items[i]
                if (itm.queueTier == QueueTier.CONTEXT) {
                    val cIdx = getCanonicalIndex(itm, c)
                    if (cIdx != null) { currentCanonicalIndex = cIdx; break }
                }
            }
            if (currentCanonicalIndex == null) {
                for (i in (currentIndex + 1) until items.size) {
                    val itm = items[i]
                    if (itm.queueTier == QueueTier.CONTEXT) {
                        val cIdx = getCanonicalIndex(itm, c)
                        if (cIdx != null) { currentCanonicalIndex = cIdx - 1; break }
                    }
                }
            }
        }
        val resolvedCurrentIndex = if (isCurrentContext) currentCanonicalIndex ?: getCanonicalIndex(currentItem, c) else currentCanonicalIndex
        val canonicalList = c?.items ?: emptyList()
        if (resolvedCurrentIndex == null || canonicalList.isEmpty()) {
            if (from < items.size) {
                val upcoming = items.drop(from)
                val currentId = if (imminent) items[currentIndex + 1].key() else currentItem.key()
                val restored = restoreOrder(upcoming.map { it.key() }, original, currentId)
                val order = sections(restored, upcoming)
                reorder(player, from, order.toIntArray())
            }
            original = emptyList(); _enabled.value = false; return
        }
        val existingByCanonical = HashMap<Int, MediaItem>()
        val unplacedHistory = ArrayList<MediaItem>()
        val unplacedUpcoming = ArrayList<MediaItem>()
        for (i in items.indices) {
            if (i == currentIndex) continue
            val item = items[i]
            if (item.queueTier == QueueTier.CONTEXT) {
                val idx = getCanonicalIndex(item, c)
                if (idx != null && idx in canonicalList.indices && !existingByCanonical.containsKey(idx)) existingByCanonical[idx] = item
                else if (i < currentIndex) unplacedHistory += item
                else unplacedUpcoming += item
            }
        }
        val precedingRange = if (isCurrentContext) 0 until resolvedCurrentIndex else 0 until (resolvedCurrentIndex + 1).coerceAtLeast(0)
        val followingRange = (resolvedCurrentIndex + 1).coerceAtLeast(0) until canonicalList.size
        val precedingContext = unplacedHistory + precedingRange.mapNotNull { existingByCanonical[it] }
        val followingContext = followingRange.mapNotNull { existingByCanonical[it] } + unplacedUpcoming
        val historyUserQueue = items.filterIndexed { idx, it -> idx < currentIndex && it.queueTier == QueueTier.USER_QUEUE }
        val userQueue = items.drop(currentIndex + 1).filter { it.queueTier == QueueTier.USER_QUEUE }
        val autoplay = items.drop(currentIndex + 1).filter { it.queueTier == QueueTier.AUTOPLAY }
        val cur = player.currentMediaItemIndex
        if (imminent && currentIndex + 1 < items.size) {
            val nextItem = items[currentIndex + 1]
            val nextKey = nextItem.key()
            val cleanPreceding = precedingContext.filter { it.key() != nextKey }
            val cleanHistory = historyUserQueue + cleanPreceding
            val rawUpcoming = userQueue.filter { it.key() != nextKey } + followingContext.filter { it.key() != nextKey } + autoplay.filter { it.key() != nextKey }
            replaceChanged(player, cur + 2, player.mediaItemCount, rawUpcoming)
            if (cur > 0 || cleanHistory.isNotEmpty()) replaceChanged(player, 0, cur, cleanHistory)
        } else {
            val upcoming = userQueue + followingContext + autoplay
            val history = historyUserQueue + precedingContext
            replaceChanged(player, cur + 1, player.mediaItemCount, upcoming)
            if (cur > 0 || history.isNotEmpty()) replaceChanged(player, 0, cur, history)
        }
        original = emptyList()
        _enabled.value = false
    }

    internal fun restoreOrder(upcoming: List<String>, original: List<String>, currentId: String? = null): List<Int> {
        val positions = HashMap<String, ArrayDeque<Int>>(upcoming.size)
        upcoming.forEachIndexed { index, id -> positions.getOrPut(id) { ArrayDeque() }.addLast(index) }
        val placed = BooleanArray(upcoming.size)
        val out = ArrayList<Int>(upcoming.size)
        val currentIndex = if (currentId != null) original.indexOf(currentId) else -1
        val searchOrder: Iterable<String> = if (currentIndex in 0 until original.size) original.subList(currentIndex + 1, original.size) else original
        for (id in searchOrder) {
            val index = positions[id]?.removeFirstOrNull() ?: continue
            placed[index] = true
            out += index
        }
        for (index in upcoming.indices) if (!placed[index]) out += index
        return out
    }

    private fun sections(order: List<Int>, upcoming: List<MediaItem>): List<Int> =
        order.filter { upcoming[it].queueTier == QueueTier.USER_QUEUE } +
            order.filter { upcoming[it].queueTier == QueueTier.CONTEXT } +
            order.filter { upcoming[it].queueTier == QueueTier.AUTOPLAY }

    internal fun replaceChanged(player: Player, from: Int, to: Int, new: List<MediaItem>): Boolean {
        if (from < 0 || to < from || to > player.mediaItemCount) return false
        val old = (from until to).map { player.getMediaItemAt(it).key() }
        val n = new.map { it.key() }
        var p = 0
        while (p < old.size && p < n.size && old[p] == n[p]) p++
        var s = 0
        while (s < old.size - p && s < n.size - p && old[old.size - 1 - s] == n[n.size - 1 - s]) s++
        if (p == old.size && p == n.size) return false
        player.replaceMediaItems(from + p, to - s, new.subList(p, n.size - s))
        return true
    }

    internal fun MediaItem.key(): String = "${queueEntryId ?: mediaId}_${queueTier.name}"

    internal val originalOrder: List<String> get() = original

    internal fun reorder(player: Player, from: Int, order: IntArray): Boolean {
        if (order.isEmpty() || from < 0 || from + order.size > player.mediaItemCount) return false
        val target = List(order.size) { player.getMediaItemAt(from + order[it]) }
        replaceChanged(player, from, from + order.size, target)
        return true
    }

    private fun Player.queueItems(): List<MediaItem> = List(mediaItemCount) { getMediaItemAt(it) }
}

const val EXTRA_CONTEXT_CYCLE_ID = "aurelis.contextCycleId"

val MediaItem.contextCycleId: Long?
    get() = mediaMetadata.extras?.takeIf { it.containsKey(EXTRA_CONTEXT_CYCLE_ID) }?.getLong(EXTRA_CONTEXT_CYCLE_ID)
