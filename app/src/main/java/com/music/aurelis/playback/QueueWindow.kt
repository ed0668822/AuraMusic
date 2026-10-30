// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.playback

import kotlin.random.Random

const val WINDOW_AHEAD = 25
const val WINDOW_LOW_WATER = 12
const val LOW_WATER = WINDOW_LOW_WATER

data class QueueWindowEntry(val canonicalIndex: Int, val cycleId: Long)

data class QueueWindowCycleSnapshot(val cycleId: Long, val order: List<Int>)

data class QueueWindowSnapshot(
    val canonicalSize: Int, val order: List<Int>, val cursor: Int,
    val currentCanonicalIndex: Int?, val shuffleEnabled: Boolean,
    val shuffleSeed: Long, val cycleId: Long, val consumedThisCycle: List<Int>,
    val lastCycleFinalCanonicalIndex: Int?,
    val plannedCycles: List<QueueWindowCycleSnapshot> = emptyList(),
)

class QueueWindow(canonicalSize: Int, shuffleSeed: Long = DEFAULT_SHUFFLE_SEED) {
    init { require(canonicalSize >= 0) }

    private var canonicalCount = canonicalSize
    private var seed = shuffleSeed
    private var logicalOrder: IntArray = IntArray(canonicalSize) { it }
    private var inversePositions: IntArray = IntArray(canonicalSize) { it }
    private var logicalCursor: Int = -1
    private var currentIndex: Int? = null
    private var shuffled: Boolean = false
    private var activeCycleId: Long = 0L
    private val consumed = LinkedHashSet<Int>()
    private var lastCycleFinal: Int? = null
    private val plannedCycles = LinkedHashMap<Long, IntArray>()

    val canonicalSize: Int get() = canonicalCount
    val canonicalN: Int get() = canonicalCount
    val n: Int get() = canonicalCount
    val order: List<Int> get() = logicalOrder.toList()
    val posOf: List<Int> get() = inversePositions.toList()
    val cursor: Int get() = logicalCursor
    val currentCanonicalIndex: Int? get() = currentIndex
    val cycleId: Long get() = activeCycleId
    val shuffleSeed: Long get() = seed
    val isShuffleEnabled: Boolean get() = shuffled
    val shuffle: Boolean get() = shuffled
    val consumedThisCycle: Set<Int> get() = consumed.toSet()
    val current: QueueWindowEntry? get() = currentIndex?.let { QueueWindowEntry(it, activeCycleId) }

    fun startAt(canonicalIndex: Int, shuffle: Boolean = shuffled): QueueWindow {
        require(canonicalIndex in 0 until canonicalCount)
        shuffled = shuffle
        activeCycleId = 0L
        currentIndex = canonicalIndex
        consumed.clear()
        lastCycleFinal = null
        plannedCycles.clear()
        logicalOrder = if (shuffle) shuffledOrderAnchoredAt(canonicalIndex, activeCycleId)
                       else IntArray(canonicalCount) { it }
        rebuildInverse()
        logicalCursor = inversePositions[canonicalIndex]
        consumed += canonicalIndex
        return this
    }

    companion object {
        const val WINDOW_AHEAD: Int = com.music.aurelis.playback.WINDOW_AHEAD
        const val WINDOW_LOW_WATER: Int = com.music.aurelis.playback.WINDOW_LOW_WATER
        const val LOW_WATER: Int = com.music.aurelis.playback.LOW_WATER

        fun startAt(canonicalSize: Int, canonicalIndex: Int, shuffle: Boolean = false, shuffleSeed: Long = DEFAULT_SHUFFLE_SEED): QueueWindow =
            QueueWindow(canonicalSize, shuffleSeed).startAt(canonicalIndex, shuffle)

        fun restore(snapshot: QueueWindowSnapshot): QueueWindow {
            require(snapshot.canonicalSize >= 0)
            require(snapshot.order.size == snapshot.canonicalSize)
            require(snapshot.cycleId >= 0L)
            require(snapshot.cursor in -1 until snapshot.canonicalSize)
            val restored = QueueWindow(snapshot.canonicalSize, snapshot.shuffleSeed)
            restored.logicalOrder = snapshot.order.toIntArray()
            restored.validatePermutation(restored.logicalOrder)
            restored.rebuildInverse()
            restored.logicalCursor = snapshot.cursor
            restored.currentIndex = snapshot.currentCanonicalIndex
            restored.shuffled = snapshot.shuffleEnabled
            restored.activeCycleId = snapshot.cycleId
            restored.lastCycleFinal = snapshot.lastCycleFinalCanonicalIndex
            restored.consumed += snapshot.consumedThisCycle
            require((restored.logicalCursor == -1) == (restored.currentIndex == null))
            if (restored.currentIndex != null) {
                require(restored.logicalOrder[restored.logicalCursor] == restored.currentIndex)
            }
            snapshot.plannedCycles.forEach { planned ->
                val permutation = planned.order.toIntArray()
                restored.validatePermutation(permutation)
                restored.plannedCycles[planned.cycleId] = permutation
            }
            return restored
        }

        fun fromSnapshot(snapshot: QueueWindowSnapshot): QueueWindow = restore(snapshot)
    }

    fun extendTo(newCanonicalSize: Int): QueueWindow {
        require(newCanonicalSize >= canonicalCount)
        if (newCanonicalSize == canonicalCount) return this
        val firstNew = canonicalCount
        canonicalCount = newCanonicalSize
        logicalOrder += freshOrder(firstNew, activeCycleId)
        plannedCycles.entries.forEach { it.setValue(it.value + freshOrder(firstNew, it.key)) }
        rebuildInverse()
        return this
    }

    fun slice(fromPos: Int, count: Int, repeatAll: Boolean = false): List<QueueWindowEntry> {
        require(fromPos in 0..logicalOrder.size)
        if (count <= 0 || logicalOrder.isEmpty()) return emptyList()
        val result = ArrayList<QueueWindowEntry>(count)
        var cycle = activeCycleId
        var orderForCycle = logicalOrder
        var position = fromPos
        while (result.size < count && orderForCycle.isNotEmpty()) {
            if (position == orderForCycle.size) {
                if (!repeatAll) break
                cycle = followingCycleId(cycle)
                orderForCycle = plannedOrder(cycle)
                position = 0
                continue
            }
            result += QueueWindowEntry(orderForCycle[position], cycle)
            position += 1
        }
        return result
    }

    fun upcoming(count: Int = WINDOW_AHEAD, repeatAll: Boolean = false): List<QueueWindowEntry> {
        val boundedCount = count.coerceAtMost(WINDOW_AHEAD)
        if (boundedCount <= 0 || currentIndex == null || logicalOrder.isEmpty()) return emptyList()
        val result = ArrayList<QueueWindowEntry>(boundedCount)
        var cycle = activeCycleId
        var orderForCycle = logicalOrder
        var position = logicalCursor + 1
        while (result.size < boundedCount && orderForCycle.isNotEmpty()) {
            while (position < orderForCycle.size && result.size < boundedCount) {
                val canonical = orderForCycle[position]
                if (cycle != activeCycleId || !shuffled || canonical !in consumed) {
                    result += QueueWindowEntry(canonical, cycle)
                }
                position += 1
            }
            if (result.size >= boundedCount || !repeatAll) break
            cycle = followingCycleId(cycle)
            orderForCycle = plannedOrder(cycle)
            position = 0
        }
        return result
    }

    fun upcomingContext(count: Int = WINDOW_AHEAD, repeatAll: Boolean = false) = upcoming(count, repeatAll)
    fun upcomingCount(repeatAll: Boolean = false): Int = upcoming(WINDOW_AHEAD, repeatAll).size

    fun advanceAutomatically(repeatAll: Boolean = false): QueueWindowEntry? {
        currentIndex ?: return null
        if (logicalOrder.isEmpty()) return null
        if (shuffled) {
            for (position in (logicalCursor + 1) until logicalOrder.size) {
                if (logicalOrder[position] !in consumed) return moveWithinCycle(position)
            }
        } else if (logicalCursor + 1 < logicalOrder.size) {
            return moveWithinCycle(logicalCursor + 1)
        }
        if (!repeatAll) return null
        return transitionToNextCycle()
    }

    fun advance(repeatAll: Boolean = false) = advanceAutomatically(repeatAll)
    fun nextAutomatic(repeatAll: Boolean = false) = advanceAutomatically(repeatAll)

    fun next(repeatAll: Boolean = false): QueueWindowEntry? {
        if (currentIndex == null || logicalOrder.isEmpty()) return null
        val nextPosition = logicalCursor + 1
        if (nextPosition < logicalOrder.size) return moveWithinCycle(nextPosition)
        if (!repeatAll) return null
        return transitionToNextCycle()
    }

    fun previous(): QueueWindowEntry? {
        if (currentIndex == null || logicalCursor <= 0) return null
        return moveWithinCycle(logicalCursor - 1)
    }

    fun jumpTo(canonicalIndex: Int): QueueWindowEntry {
        require(canonicalIndex in 0 until canonicalCount)
        check(currentIndex != null)
        val position = inversePositions[canonicalIndex]
        check(position >= 0)
        return moveWithinCycle(position)
    }

    fun seekTo(canonicalIndex: Int) = jumpTo(canonicalIndex)

    fun setShuffle(enabled: Boolean, shuffleSeed: Long = seed): QueueWindow {
        if (enabled == shuffled) return this
        shuffled = enabled
        if (enabled) seed = shuffleSeed
        plannedCycles.clear()
        val current = currentIndex ?: run { logicalOrder = IntArray(canonicalCount) { it }; rebuildInverse(); return this }
        logicalOrder = if (enabled) shuffledOrderAnchoredAt(current, activeCycleId)
                       else IntArray(canonicalCount) { it }
        rebuildInverse()
        logicalCursor = inversePositions[current]
        consumed += current
        return this
    }

    fun toggleShuffle(enabled: Boolean = !shuffled): QueueWindow = setShuffle(enabled)

    fun commit(entry: QueueWindowEntry): Boolean {
        if (currentIndex == null || entry.canonicalIndex !in 0 until canonicalCount) return false
        if (entry.cycleId == activeCycleId) {
            val position = inversePositions[entry.canonicalIndex]
            if (position < 0) return false
            moveWithinCycle(position)
            return true
        }
        if (activeCycleId == Long.MAX_VALUE || entry.cycleId != activeCycleId + 1L) return false
        val nextOrder = plannedOrder(entry.cycleId)
        val position = nextOrder.indexOf(entry.canonicalIndex)
        if (position < 0) return false
        selectPlannedCycle(entry.cycleId, nextOrder, position)
        return true
    }

    fun snapshot(): QueueWindowSnapshot = QueueWindowSnapshot(
        canonicalSize = canonicalCount,
        order = logicalOrder.toList(),
        cursor = logicalCursor,
        currentCanonicalIndex = currentIndex,
        shuffleEnabled = shuffled,
        shuffleSeed = seed,
        cycleId = activeCycleId,
        consumedThisCycle = consumed.toList(),
        lastCycleFinalCanonicalIndex = lastCycleFinal,
        plannedCycles = plannedCycles.map { (cycle, perm) -> QueueWindowCycleSnapshot(cycle, perm.toList()) },
    )

    private fun moveWithinCycle(position: Int): QueueWindowEntry {
        check(position in logicalOrder.indices)
        val previousBoundaryFinal = boundaryFinal()
        if (shuffled && position > logicalCursor) {
            for (skipped in (logicalCursor + 1)..position) consumed += logicalOrder[skipped]
        }
        logicalCursor = position
        currentIndex = logicalOrder[position]
        consumed += logicalOrder[position]
        if (boundaryFinal() != previousBoundaryFinal) plannedCycles.clear()
        return QueueWindowEntry(logicalOrder[position], activeCycleId)
    }

    private fun transitionToNextCycle(): QueueWindowEntry {
        val nextCycle = followingCycleId(activeCycleId)
        val nextOrder = plannedOrder(nextCycle)
        selectPlannedCycle(nextCycle, nextOrder, 0)
        return current!!
    }

    private fun selectPlannedCycle(cycle: Long, permutation: IntArray, position: Int) {
        lastCycleFinal = currentIndex
        activeCycleId = cycle
        logicalOrder = permutation
        plannedCycles.remove(cycle)
        rebuildInverse()
        logicalCursor = position
        currentIndex = logicalOrder[position]
        consumed.clear()
        for (consumedPosition in 0..position) consumed += logicalOrder[consumedPosition]
    }

    private fun plannedOrder(cycle: Long): IntArray {
        plannedCycles[cycle]?.let { return it }
        val previousCycle = cycle - 1L
        val previousFinal = if (previousCycle == activeCycleId) boundaryFinal() else plannedOrder(previousCycle).lastOrNull()
        return orderForCycle(cycle, previousFinal).also { plannedCycles[cycle] = it }
    }

    private fun boundaryFinal(): Int? {
        for (position in logicalOrder.lastIndex downTo (logicalCursor + 1)) {
            val canonical = logicalOrder[position]
            if (!shuffled || canonical !in consumed) return canonical
        }
        return currentIndex
    }

    private fun followingCycleId(cycle: Long): Long {
        check(cycle < Long.MAX_VALUE)
        return cycle + 1L
    }

    private fun freshOrder(firstNew: Int, cycle: Long): IntArray {
        val fresh = (firstNew until canonicalCount).toList()
        return if (shuffled && currentIndex != null) fresh.shuffled(Random(seedForExtension(cycle, firstNew))).toIntArray()
               else fresh.toIntArray()
    }

    private fun shuffledOrderAnchoredAt(anchor: Int, cycle: Long): IntArray {
        val rest = (0 until canonicalCount).filter { it != anchor }.shuffled(Random(seedForCycle(cycle)))
        return (listOf(anchor) + rest).toIntArray()
    }

    private fun orderForCycle(cycle: Long, previousFinal: Int?): IntArray {
        if (!shuffled) return IntArray(canonicalCount) { it }
        val result = (0 until canonicalCount).toMutableList()
        result.shuffle(Random(seedForCycle(cycle)))
        if (canonicalCount >= 2 && previousFinal != null && result.firstOrNull() == previousFinal) {
            val swapWith = result.indexOfFirst { it != previousFinal }
            if (swapWith >= 0) {
                val first = result[0]
                result[0] = result[swapWith]
                result[swapWith] = first
            }
        }
        return result.toIntArray()
    }

    private fun seedForCycle(cycle: Long): Long = seed xor (cycle * -7046029254386353131L)
    private fun seedForExtension(cycle: Long, firstNew: Int): Long =
        seed xor (cycle * -3335678366873096957L) xor canonicalCount.toLong() xor (firstNew.toLong() shl 32)

    private fun rebuildInverse() {
        inversePositions = IntArray(canonicalCount) { -1 }
        validatePermutation(logicalOrder)
        logicalOrder.forEachIndexed { position, canonical -> inversePositions[canonical] = position }
    }

    private fun validatePermutation(permutation: IntArray) {
        require(permutation.size == canonicalCount)
        val seen = BooleanArray(canonicalCount)
        permutation.forEach { canonical ->
            require(canonical in 0 until canonicalCount)
            require(!seen[canonical])
            seen[canonical] = true
        }
    }
}

private const val DEFAULT_SHUFFLE_SEED: Long = 0x4D59535155455545L
