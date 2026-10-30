// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.playback

import com.music.aurelis.data.model.LocalSong
import java.util.Collections

fun LocalSong.contextEntryKey(): String =
    queueEntryId?.takeIf { it.isNotBlank() }?.let { "$videoId@$it" } ?: videoId

enum class ContinuationState { AVAILABLE, IN_FLIGHT, EXHAUSTED, BLOCKED }

enum class ContinuationDiagnostic { DUPLICATE_PAGE, NO_PROGRESS, REPEATED_CONTINUATION, REQUEST_FAILED }

data class ContextSnapshot(
    val contextToken: String,
    val sourceKey: String,
    val browseId: String?,
    val songs: List<LocalSong>,
    val version: Long,
    val continuationToken: String?,
    val continuationState: ContinuationState,
    val diagnostic: ContinuationDiagnostic? = null,
    val seenContinuationTokens: List<String> = emptyList(),
)

data class ContinuationPage(val songs: List<LocalSong>, val continuationToken: String?)

data class ContinuationRequest(
    val requestId: Long,
    val contextToken: String,
    val sourceKey: String,
    val browseId: String?,
    val version: Long,
    val continuationToken: String,
)

enum class AppendStatus { APPENDED, NO_NEW_SONGS, STALE_CONTEXT, NO_ACTIVE_CONTEXT }

data class AppendResult(
    val status: AppendStatus,
    val snapshot: ContextSnapshot?,
    val addedSongs: List<LocalSong> = emptyList(),
)

enum class ContinuationResultStatus { COMPLETED, FAILED, REJECTED_NO_PROGRESS, STALE_REQUEST }

data class ContinuationResult(
    val status: ContinuationResultStatus,
    val snapshot: ContextSnapshot?,
    val addedSongs: List<LocalSong> = emptyList(),
    val diagnostic: ContinuationDiagnostic? = null,
    val error: Throwable? = null,
)

class ContextRegistry(onGrowth: (ContextSnapshot) -> Unit = {}) {
    private val lock = Any()
    private var active: MutableContext? = null
    private var nextVersion = 0L
    private var nextRequestId = 0L
    private var nextListenerId = 0L
    private val growthListeners = LinkedHashMap<Long, (ContextSnapshot) -> Unit>()

    init { growthListeners[nextListenerId++] = onGrowth }

    fun register(
        contextToken: String,
        sourceKey: String,
        browseId: String?,
        songs: List<LocalSong>,
        continuationToken: String? = null,
    ): ContextSnapshot = synchronized(lock) {
        val token = continuationToken.normalizedToken()
        val version = newVersionLocked()
        active = MutableContext(
            contextToken = contextToken,
            sourceKey = sourceKey,
            browseId = browseId,
            songs = immutableSongs(songs),
            version = version,
            continuationToken = token,
            continuationState = if (token == null) ContinuationState.EXHAUSTED else ContinuationState.AVAILABLE,
            diagnostic = null,
            seenContinuationTokens = token?.let { linkedSetOf(it) } ?: linkedSetOf(),
            request = null,
        )
        active!!.snapshot()
    }

    fun restore(snapshot: ContextSnapshot): ContextSnapshot {
        require(snapshot.contextToken.isNotBlank())
        require(snapshot.sourceKey.isNotBlank())
        val songs = immutableRestoredSongs(snapshot.songs)
        val token = snapshot.continuationToken.normalizedToken()
        val seenTokens = LinkedHashSet<String>(snapshot.seenContinuationTokens.size + 1)
        snapshot.seenContinuationTokens.forEach { require(seenTokens.add(it)) }
        token?.let(seenTokens::add)
        return synchronized(lock) {
            val state = if (snapshot.continuationState == ContinuationState.IN_FLIGHT) ContinuationState.AVAILABLE
                        else snapshot.continuationState
            val version = newVersionLocked()
            active = MutableContext(
                contextToken = snapshot.contextToken,
                sourceKey = snapshot.sourceKey,
                browseId = snapshot.browseId,
                songs = songs,
                version = version,
                continuationToken = token,
                continuationState = state,
                diagnostic = snapshot.diagnostic,
                seenContinuationTokens = seenTokens,
                request = null,
            )
            active!!.snapshot()
        }
    }

    fun snapshot(): ContextSnapshot? = synchronized(lock) { active?.snapshot() }

    fun append(contextToken: String, songs: List<LocalSong>, expectedVersion: Long? = null): AppendResult {
        var callback: ContextSnapshot? = null
        val result = synchronized(lock) {
            val state = active ?: return@synchronized AppendResult(AppendStatus.NO_ACTIVE_CONTEXT, null)
            if (state.contextToken != contextToken || (expectedVersion != null && state.version != expectedVersion)) {
                return@synchronized AppendResult(AppendStatus.STALE_CONTEXT, state.snapshot())
            }
            val added = newSongs(state.songs, songs)
            if (added.isEmpty()) return@synchronized AppendResult(AppendStatus.NO_NEW_SONGS, state.snapshot())
            if (state.continuationState == ContinuationState.IN_FLIGHT) {
                state.request = null
                state.continuationState = if (state.continuationToken == null) ContinuationState.EXHAUSTED else ContinuationState.AVAILABLE
            }
            state.songs = immutableSongs(state.songs + added)
            state.version = newVersionLocked()
            state.diagnostic = null
            callback = state.snapshot()
            AppendResult(AppendStatus.APPENDED, callback, immutableSongs(added))
        }
        callback?.let(::notifyGrowth)
        return result
    }

    fun addGrowthListener(listener: (ContextSnapshot) -> Unit): Long = synchronized(lock) {
        val id = nextListenerId++
        growthListeners[id] = listener
        id
    }

    fun removeGrowthListener(id: Long): Boolean = synchronized(lock) { growthListeners.remove(id) != null }

    fun claimContinuation(expectedContextToken: String? = null): ContinuationRequest? = synchronized(lock) {
        val state = active ?: return@synchronized null
        if (expectedContextToken != null && state.contextToken != expectedContextToken) return@synchronized null
        if (state.continuationState != ContinuationState.AVAILABLE || state.continuationToken == null) return@synchronized null
        val request = ContinuationRequest(
            requestId = nextRequestId++,
            contextToken = state.contextToken,
            sourceKey = state.sourceKey,
            browseId = state.browseId,
            version = state.version,
            continuationToken = state.continuationToken!!,
        )
        state.request = request
        state.continuationState = ContinuationState.IN_FLIGHT
        request
    }

    fun complete(request: ContinuationRequest, page: ContinuationPage): ContinuationResult {
        var callback: ContextSnapshot? = null
        val result = synchronized(lock) {
            val state = active
            if (state == null || !state.matches(request)) return@synchronized staleResult(state)
            val nextToken = page.continuationToken.normalizedToken()
            val added = newSongs(state.songs, page.songs)
            val diagnostic = when {
                added.isEmpty() && page.songs.isEmpty() -> ContinuationDiagnostic.NO_PROGRESS
                added.isEmpty() -> ContinuationDiagnostic.DUPLICATE_PAGE
                nextToken != null && nextToken in state.seenContinuationTokens -> ContinuationDiagnostic.REPEATED_CONTINUATION
                else -> null
            }
            if (diagnostic != null) {
                state.request = null
                state.continuationState = ContinuationState.BLOCKED
                state.diagnostic = diagnostic
                return@synchronized ContinuationResult(ContinuationResultStatus.REJECTED_NO_PROGRESS, state.snapshot(), diagnostic = diagnostic)
            }
            state.request = null
            state.songs = immutableSongs(state.songs + added)
            state.continuationToken = nextToken
            state.continuationState = if (nextToken == null) ContinuationState.EXHAUSTED
                                       else { state.seenContinuationTokens += nextToken; ContinuationState.AVAILABLE }
            state.version = newVersionLocked()
            state.diagnostic = null
            callback = state.snapshot()
            ContinuationResult(ContinuationResultStatus.COMPLETED, callback, immutableSongs(added))
        }
        callback?.let(::notifyGrowth)
        return result
    }

    fun fail(request: ContinuationRequest, error: Throwable? = null): ContinuationResult = synchronized(lock) {
        val state = active
        if (state == null || !state.matches(request)) return@synchronized staleResult(state)
        state.request = null
        state.continuationState = if (state.continuationToken == null) ContinuationState.EXHAUSTED else ContinuationState.AVAILABLE
        state.diagnostic = ContinuationDiagnostic.REQUEST_FAILED
        ContinuationResult(ContinuationResultStatus.FAILED, state.snapshot(),
            diagnostic = ContinuationDiagnostic.REQUEST_FAILED, error = error)
    }

    private fun staleResult(state: MutableContext?) = ContinuationResult(ContinuationResultStatus.STALE_REQUEST, state?.snapshot())

    private fun notifyGrowth(snapshot: ContextSnapshot) {
        val listeners = synchronized(lock) { growthListeners.values.toList() }
        listeners.forEach { it(snapshot) }
    }

    private fun newVersionLocked(): Long { nextVersion += 1; return nextVersion }

    private class MutableContext(
        val contextToken: String, val sourceKey: String, val browseId: String?,
        var songs: List<LocalSong>, var version: Long, var continuationToken: String?,
        var continuationState: ContinuationState, var diagnostic: ContinuationDiagnostic?,
        val seenContinuationTokens: MutableSet<String>, var request: ContinuationRequest?,
    ) {
        fun snapshot() = ContextSnapshot(
            contextToken = contextToken, sourceKey = sourceKey, browseId = browseId,
            songs = songs, version = version, continuationToken = continuationToken,
            continuationState = continuationState, diagnostic = diagnostic,
            seenContinuationTokens = Collections.unmodifiableList(seenContinuationTokens.toList()),
        )
        fun matches(c: ContinuationRequest) =
            continuationState == ContinuationState.IN_FLIGHT && request == c &&
                c.contextToken == contextToken && c.version == version && c.continuationToken == continuationToken
    }

    private companion object {
        fun String?.normalizedToken(): String? = this?.takeIf { it.isNotBlank() }

        fun immutableRestoredSongs(songs: List<LocalSong>): List<LocalSong> {
            val ids = HashSet<String>(songs.size)
            songs.forEach { require(ids.add(it.contextEntryKey())) }
            return Collections.unmodifiableList(songs.toList())
        }

        fun immutableSongs(songs: Collection<LocalSong>): List<LocalSong> {
            val byVideoId = LinkedHashMap<String, LocalSong>(songs.size)
            songs.forEach { byVideoId.putIfAbsent(it.contextEntryKey(), it) }
            return Collections.unmodifiableList(byVideoId.values.toList())
        }

        fun newSongs(existing: List<LocalSong>, candidates: List<LocalSong>): List<LocalSong> {
            val known = existing.asSequence().mapTo(HashSet()) { it.contextEntryKey() }
            val added = ArrayList<LocalSong>()
            candidates.forEach { if (known.add(it.contextEntryKey())) added += it }
            return added
        }
    }
}
