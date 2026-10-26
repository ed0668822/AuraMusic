package com.music.aura.data.stats

import android.content.Context
import android.util.Log
import com.music.aura.data.model.LocalSong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

/**
 * Motor de estadísticas de Aura Music (Portado de BitChord).
 * Guarda los datos en archivos JSON por mes, sin base de datos ni red.
 */
object ListeningStats {

    private lateinit var directory: File

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private var open: OpenBucket? = null
    private val lock = Any()
    private var dirty = false

    private val writer = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeLock = Mutex()

    @Volatile
    private var version = 0L

    private var cached: Cached? = null

    private class Cached(
        val period: ReplayPeriod,
        val version: Long,
        val day: LocalDate,
        val summary: ReplaySummary,
    )

    fun init(context: Context) {
        directory = File(context.filesDir, DIRECTORY)
        writer.launch { synchronized(lock) { bucketFor(YearMonth.now()) } }
    }

    private val ready: Boolean get() = this::directory.isInitialized

    // ── Escritura ───────────────────────────────────────────────────────────

    fun record(song: LocalSong, playedMs: Long, countsAsPlay: Boolean) {
        if (!ready) return
        if (playedMs <= 0 && !countsAsPlay) return
        if (song.id.isBlank()) return

        val now = System.currentTimeMillis()
        val at = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())
        val dayKey = "${at.year}-${"%02d".format(at.monthValue)}-${"%02d".format(at.dayOfMonth)}"

        synchronized(lock) {
            val bucket = bucketFor(YearMonth.from(at))
            val track = bucket.tracks.getOrPut(song.id) {
                TrackEntry(
                    id = song.id,
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    art = song.artworkUri,
                )
            }
            track.ms += playedMs
            track.last = now
            if (track.album == null) track.album = song.album
            if (track.art == null) track.art = song.artworkUri
            if (countsAsPlay) track.plays++

            val trackDay = bucket.trackDays.computeIfAbsent(dayKey) { DayStats() }
            trackDay.ms += playedMs
            if (countsAsPlay) trackDay.plays++

            track.days.computeIfAbsent(dayKey) { DayStats() }.also { d ->
                d.ms += playedMs
                if (countsAsPlay) d.plays++
            }

            primaryArtist(song.artist)?.let { name ->
                val artist = bucket.artists.getOrPut(name.lowercase(Locale.ROOT)) {
                    NameEntry(name = name, art = song.artworkUri)
                }
                artist.ms += playedMs
                if (countsAsPlay) artist.plays++

                val artistDay = bucket.artistDays.computeIfAbsent(dayKey) { DayStats() }
                artistDay.ms += playedMs
                if (countsAsPlay) artistDay.plays++

                artist.days.computeIfAbsent(dayKey) { DayStats() }.also { d ->
                    d.ms += playedMs
                    if (countsAsPlay) d.plays++
                }
            }

            song.album?.trim()?.takeIf { it.isNotEmpty() }?.let { name ->
                val album = bucket.albums.getOrPut(albumKey(name, song.artist)) {
                    NameEntry(name = name, sub = song.artist, art = song.artworkUri)
                }
                album.ms += playedMs
                if (countsAsPlay) album.plays++

                val albumDay = bucket.albumDays.computeIfAbsent(dayKey) { DayStats() }
                albumDay.ms += playedMs
                if (countsAsPlay) albumDay.plays++

                album.days.computeIfAbsent(dayKey) { DayStats() }.also { d ->
                    d.ms += playedMs
                    if (countsAsPlay) d.plays++
                }
            }

            bucket.hours[at.hour] += playedMs
            val day = at.dayOfMonth
            bucket.days[day] = (bucket.days[day] ?: 0L) + playedMs
            dirty = true
        }
    }

    fun flush() {
        pending()?.let { (key, snapshot) -> writer.launch { write(key, snapshot) } }
    }

    private suspend fun flushAndAwait() {
        pending()?.let { (key, snapshot) -> writer.launch { write(key, snapshot) }.join() }
    }

    private fun pending(): Pair<String, StoredBucket>? {
        if (!ready) return null
        return synchronized(lock) {
            if (!dirty) return null
            val bucket = open ?: return null
            dirty = false
            bucket.key to bucket.snapshot()
        }
    }

    private suspend fun write(key: String, snapshot: StoredBucket) = writeLock.withLock {
        runCatching {
            directory.mkdirs()
            val file = File(directory, "$key.json")
            val temporary = File(directory, "$key.json.tmp")
            temporary.writeText(json.encodeToString(StoredBucket.serializer(), snapshot))
            if (!temporary.renameTo(file)) {
                if (file.delete() && temporary.renameTo(file)) Unit else temporary.delete()
            }
            version++
        }.onFailure { Log.w(TAG, "Could not write listening bucket $key", it) }
    }

    private fun bucketFor(month: YearMonth): OpenBucket {
        val key = month.toString()
        open?.takeIf { it.key == key }?.let { return it }
        open?.let { previous ->
            val stale = previous.snapshot()
            writer.launch { write(previous.key, stale) }
        }
        val loaded = read(key) ?: StoredBucket(month = key)
        return OpenBucket.of(key, loaded).also {
            open = it
            prune()
        }
    }

    // ── Lectura ─────────────────────────────────────────────────────────────

    suspend fun summary(period: ReplayPeriod): ReplaySummary = withContext(Dispatchers.IO) {
        flushAndAwait()
        val today = LocalDate.now()
        cached?.takeIf { it.period == period && it.version == version && it.day == today }
            ?.let { return@withContext it.summary }

        val merged = MergedBucket()
        when {
            period.needsDayFilter() -> {
                val range = period.range(today)!!
                months().filter { month ->
                    val start = month.atDay(1)
                    val end = month.atEndOfMonth()
                    !(end < range.first || start > range.second)
                }.forEach { month -> read(month.toString())?.let { bucket -> merged.add(bucket, range) } }
            }
            else -> {
                months().filter { period.covers(it, today) }
                    .forEach { month -> read(month.toString())?.let(merged::add) }
            }
        }
        merged.toSummary(period, today).also {
            cached = Cached(period, version, today, it)
        }
    }

    private fun months(): List<YearMonth> {
        if (!ready) return emptyList()
        val files = directory.listFiles() ?: return emptyList()
        return files.mapNotNull { file ->
            file.name.removeSuffix(".json").takeIf { it != file.name }
                ?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
        }.sorted()
    }

    private fun read(key: String): StoredBucket? {
        val file = File(directory, "$key.json")
        if (!file.exists()) return null
        return runCatching { json.decodeFromString(StoredBucket.serializer(), file.readText()) }
            .onFailure { Log.w(TAG, "Discarding unreadable bucket $key", it) }
            .getOrNull()
    }

    private fun prune() {
        val existing = months()
        if (existing.size > KEEP_MONTHS) {
            existing.take(existing.size - KEEP_MONTHS).forEach {
                File(directory, "$it.json").delete()
            }
        }
        val bucket = open ?: return
        bucket.tracks.trimTo(MAX_TRACKS) { it.ms }
        bucket.artists.trimTo(MAX_NAMES) { it.ms }
        bucket.albums.trimTo(MAX_NAMES) { it.ms }
    }

    private inline fun <T : Any> List<T>.mergedBy(
        into: LinkedHashMap<String, T>,
        key: (T) -> String,
    ): LinkedHashMap<String, T> {
        forEach { entry ->
            val k = key(entry)
            val existing = into[k]
            if (existing == null) {
                into[k] = entry
            } else {
                @Suppress("UNCHECKED_CAST")
                (existing as NameEntry).absorb(entry as NameEntry)
            }
        }
        return into
    }

    private inline fun <K, V> MutableMap<K, V>.trimTo(limit: Int, crossinline weight: (V) -> Long) {
        if (size <= limit) return
        entries.sortedBy { weight(it.value) }
            .take(size - limit)
            .forEach { remove(it.key) }
    }

    // ── Modelos Internos ────────────────────────────────────────────────────

    private class OpenBucket(
        val key: String,
        val tracks: MutableMap<String, TrackEntry>,
        val artists: MutableMap<String, NameEntry>,
        val albums: MutableMap<String, NameEntry>,
        val hours: LongArray,
        val days: MutableMap<Int, Long>,
        val trackDays: MutableMap<String, DayStats> = mutableMapOf(),
        val artistDays: MutableMap<String, DayStats> = mutableMapOf(),
        val albumDays: MutableMap<String, DayStats> = mutableMapOf(),
    ) {
        fun snapshot() = StoredBucket(
            month = key,
            tracks = tracks.values.toList(),
            artists = artists.map { (key, entry) -> entry.copy(key = key) },
            albums = albums.map { (key, entry) -> entry.copy(key = key) },
            hours = hours.toList(),
            days = days.toMap(),
            trackDays = trackDays.mapValues { it.value },
            artistDays = artistDays.mapValues { it.value },
            albumDays = albumDays.mapValues { it.value },
        )

        companion object {
            fun of(key: String, stored: StoredBucket) = OpenBucket(
                key = key,
                tracks = stored.tracks.associateByTo(LinkedHashMap()) { it.id },
                artists = stored.artists
                    .map { it.copy(name = primaryArtist(it.name) ?: it.name) }
                    .mergedBy(LinkedHashMap()) { it.name.lowercase(Locale.ROOT) },
                albums = stored.albums.mergedBy(LinkedHashMap()) { albumKey(it.name, it.sub.orEmpty()) },
                hours = LongArray(24) { stored.hours.getOrElse(it) { 0L } },
                days = stored.days.toMutableMap(),
                trackDays = stored.trackDays?.toMutableMap() ?: mutableMapOf(),
                artistDays = stored.artistDays?.toMutableMap() ?: mutableMapOf(),
                albumDays = stored.albumDays?.toMutableMap() ?: mutableMapOf(),
            )
        }
    }

    private class MergedBucket {
        val tracks = HashMap<String, TrackEntry>()
        val artists = HashMap<String, NameEntry>()
        val albums = HashMap<String, NameEntry>()
        val hours = LongArray(24)
        val days = HashMap<String, Long>()
        val trackDays = HashMap<String, DayStats>()
        val artistDays = HashMap<String, DayStats>()
        val albumDays = HashMap<String, DayStats>()
        var earliest: String? = null

        fun add(bucket: StoredBucket) {
            bucket.tracks.forEach { entry ->
                tracks.merge(entry.id, entry.copy()) { a, b -> a.also { it.absorb(b) } }
            }
            bucket.artists.forEach { entry ->
                val lead = entry.copy(name = primaryArtist(entry.name) ?: entry.name)
                artists.merge(lead.name.lowercase(Locale.ROOT), lead) { a, b -> a.also { it.absorb(b) } }
            }
            bucket.albums.forEach { entry ->
                val key = albumKey(entry.name, entry.sub.orEmpty())
                albums.merge(key, entry.copy()) { a, b -> a.also { it.absorb(b) } }
            }
            repeat(24) { hours[it] += bucket.hours.getOrElse(it) { 0L } }
            bucket.days.forEach { (day, ms) ->
                val date = "${bucket.month}-%02d".format(day)
                days[date] = (days[date] ?: 0L) + ms
            }
            if (earliest == null || bucket.month < earliest!!) earliest = bucket.month
        }

        fun addDaySubs(bucket: StoredBucket) {
            bucket.trackDays?.forEach { (dateStr, stats) ->
                trackDays.merge(dateStr, stats) { a, b -> DayStats(a.ms + b.ms, a.plays + b.plays) }
            }
            bucket.artistDays?.forEach { (dateStr, stats) ->
                artistDays.merge(dateStr, stats) { a, b -> DayStats(a.ms + b.ms, a.plays + b.plays) }
            }
            bucket.albumDays?.forEach { (dateStr, stats) ->
                albumDays.merge(dateStr, stats) { a, b -> DayStats(a.ms + b.ms, a.plays + b.plays) }
            }
        }

        fun add(bucket: StoredBucket, range: Pair<LocalDate, LocalDate>) {
            bucket.tracks.forEach { entry ->
                tracks.merge(entry.id, entry.copy()) { a, b -> a.also { it.absorb(b) } }
            }
            bucket.artists.forEach { entry ->
                val lead = entry.copy(name = primaryArtist(entry.name) ?: entry.name)
                artists.merge(lead.name.lowercase(Locale.ROOT), lead) { a, b -> a.also { it.absorb(b) } }
            }
            bucket.albums.forEach { entry ->
                val key = albumKey(entry.name, entry.sub.orEmpty())
                albums.merge(key, entry.copy()) { a, b -> a.also { it.absorb(b) } }
            }
            addDaySubs(bucket)
            bucket.days.forEach { (day, ms) ->
                val candidate = "${bucket.month}-%02d".format(day)
                val date = runCatching { LocalDate.parse(candidate.replace("-", "/")) }.getOrNull() ?: return@forEach
                if (date >= range.first && date <= range.second) {
                    days[candidate] = (days[candidate] ?: 0L) + ms
                }
            }
        }

        fun toSummary(period: ReplayPeriod, today: LocalDate): ReplaySummary {
            val weekRange = if (period.needsDayFilter()) period.range(today) else null

            val rankedSongs = if (weekRange != null) {
                tracks.values.mapNotNull { entry ->
                    val (ms, plays) = entry.weekRange(weekRange)
                    if (ms > 0) entry to (ms to plays) else null
                }.sortedWith(compareByDescending<Pair<TrackEntry, Pair<Long, Int>>> { it.second.first }.thenByDescending { it.second.second })
                 .map { (entry, msPlays) ->
                     val (ms, plays) = msPlays
                     RankedSong(
                         song = LocalSong(
                             id = entry.id, title = entry.title, artist = entry.artist,
                             album = entry.album, artworkUri = entry.art, contentUri = "",
                             durationMs = 0L, filePath = null, trackNumber = null, year = null
                         ), ms = ms, plays = plays
                     )
                 }
            } else {
                tracks.values.sortedWith(compareByDescending<TrackEntry> { it.ms }.thenByDescending { it.plays })
                 .map {
                     RankedSong(
                         song = LocalSong(
                             id = it.id, title = it.title, artist = it.artist,
                             album = it.album, artworkUri = it.art, contentUri = "",
                             durationMs = 0L, filePath = null, trackNumber = null, year = null
                         ), ms = it.ms, plays = it.plays
                     )
                 }
            }

            val rankedArtists = if (weekRange != null) {
                artists.values.mapNotNull { entry ->
                    val (ms, plays) = entry.weekRange(weekRange)
                    if (ms > 0) entry to (ms to plays) else null
                }.sortedWith(compareByDescending<Pair<NameEntry, Pair<Long, Int>>> { it.second.first }.thenByDescending { it.second.second })
                 .map { (entry, msPlays) ->
                     RankedEntry(entry.name, entry.sub, entry.art, null, msPlays.first, msPlays.second)
                 }
            } else {
                artists.values.sortedWith(compareByDescending<NameEntry> { it.ms }.thenByDescending { it.plays })
                 .map { RankedEntry(it.name, it.sub, it.art, null, it.ms, it.plays) }
            }

            val rankedAlbums = if (weekRange != null) {
                albums.values.mapNotNull { entry ->
                    val (ms, plays) = entry.weekRange(weekRange)
                    if (ms > 0) entry to (ms to plays) else null
                }.sortedWith(compareByDescending<Pair<NameEntry, Pair<Long, Int>>> { it.second.first }.thenByDescending { it.second.second })
                 .map { (entry, msPlays) -> RankedEntry(entry.name, entry.sub, entry.art, null, msPlays.first, msPlays.second) }
            } else {
                albums.values.sortedWith(compareByDescending<NameEntry> { it.ms }.thenByDescending { it.plays })
                 .map { RankedEntry(it.name, it.sub, it.art, null, it.ms, it.plays) }
            }

            val busiest = days.entries.maxByOrNull { it.value }
            return ReplaySummary(
                period = period,
                label = period.label(today),
                totalMs = rankedSongs.sumOf { it.ms },
                totalPlays = rankedSongs.sumOf { it.plays },
                songs = rankedSongs,
                artists = rankedArtists,
                albums = rankedAlbums,
                genres = emptyList(), // Géneros removidos por ser 100% local
                hourOfDay = hours.toList(),
                busiestDay = busiest?.key,
                busiestDayMs = busiest?.value ?: 0L,
                distinctSongs = rankedSongs.size,
                distinctArtists = rankedArtists.size,
                distinctAlbums = rankedAlbums.size,
                since = earliest,
            )
        }
    }

    private fun albumKey(name: String, artist: String): String =
        name.lowercase(Locale.ROOT) + ALBUM_KEY_SEPARATOR + artist.lowercase(Locale.ROOT)

    private val ALBUM_KEY_SEPARATOR = Char(31).toString()
    
    fun primaryArtist(credit: String): String? {
        val name = credit.split(CREDIT_SEPARATORS).firstOrNull { it.isNotBlank() }?.trim()?.takeIf { it.isNotEmpty() }
        return name
    }

    private val CREDIT_SEPARATORS = Regex(
        """\s*,\s*|\s+&\s+|\s+x\s+|\s+feat\.?\s+|\s+ft\.?\s+|\s+featuring\s+""",
        RegexOption.IGNORE_CASE,
    )

    private const val TAG = "AuraListening"
    private const val DIRECTORY = "listening"
    private const val KEEP_MONTHS = 36
    private const val MAX_TRACKS = 600
    private const val MAX_NAMES = 400
}

@Serializable
data class DayStats(var ms: Long = 0L, var plays: Int = 0)

@Serializable
data class TrackEntry(
    val id: String,
    val title: String = "",
    val artist: String = "",
    var album: String? = null,
    var art: String? = null,
    var ms: Long = 0L,
    var plays: Int = 0,
    var last: Long = 0L,
    var days: MutableMap<String, DayStats> = mutableMapOf(),
) {
    fun absorb(other: TrackEntry) {
        ms += other.ms; plays += other.plays; last = maxOf(last, other.last)
        if (album == null) album = other.album
        if (art == null) art = other.art
        other.days.forEach { (k, v) -> days.computeIfAbsent(k) { DayStats() }.also { it.ms += v.ms; it.plays += v.plays } }
    }
    fun weekRange(range: Pair<LocalDate, LocalDate>): Pair<Long, Int> {
        var tMs = 0L; var tPl = 0
        if (days.isNotEmpty()) {
            days.forEach { (k, v) -> val d = runCatching { LocalDate.parse(k) }.getOrNull(); if (d != null && d >= range.first && d <= range.second) { tMs += v.ms; tPl += v.plays } }
        } else return ms to plays
        return tMs to tPl
    }
}

@Serializable
data class NameEntry(
    val name: String = "",
    val sub: String? = null,
    var art: String? = null,
    var ms: Long = 0L,
    var plays: Int = 0,
    val key: String? = null,
    var days: MutableMap<String, DayStats> = mutableMapOf(),
) {
    fun absorb(other: NameEntry) {
        ms += other.ms; plays += other.plays
        if (art == null) art = other.art
        other.days.forEach { (k, v) -> days.computeIfAbsent(k) { DayStats() }.also { it.ms += v.ms; it.plays += v.plays } }
    }
    fun weekRange(range: Pair<LocalDate, LocalDate>): Pair<Long, Int> {
        var tMs = 0L; var tPl = 0
        if (days.isNotEmpty()) {
            days.forEach { (k, v) -> val d = runCatching { LocalDate.parse(k) }.getOrNull(); if (d != null && d >= range.first && d <= range.second) { tMs += v.ms; tPl += v.plays } }
        } else return ms to plays
        return tMs to tPl
    }
}

@Serializable
data class StoredBucket(
    val version: Int = 1, val month: String,
    val tracks: List<TrackEntry> = emptyList(),
    val artists: List<NameEntry> = emptyList(),
    val albums: List<NameEntry> = emptyList(),
    val hours: List<Long> = List(24) { 0L },
    val days: Map<Int, Long> = emptyMap(),
    val trackDays: Map<String, DayStats>? = null,
    val artistDays: Map<String, DayStats>? = null,
    val albumDays: Map<String, DayStats>? = null,
)

data class RankedEntry(val title: String, val subtitle: String?, val artworkUrl: String?, val browseId: String?, val ms: Long, val plays: Int)
data class RankedSong(val song: LocalSong, val ms: Long, val plays: Int)

enum class ReplayPeriod(val chip: String) {
    THIS_WEEK("This week"), THIS_MONTH("This month"), THIS_YEAR("This year"), ALL_TIME("All time");
    fun range(today: LocalDate): Pair<LocalDate, LocalDate>? = when (this) {
        THIS_WEEK -> { val start = today.with(java.time.DayOfWeek.MONDAY); start to today }
        THIS_MONTH -> { val ym = YearMonth.from(today); ym.atDay(1) to ym.atEndOfMonth() }
        THIS_YEAR -> LocalDate.of(today.year, 1, 1) to LocalDate.of(today.year, 12, 31)
        ALL_TIME -> null
    }
    fun covers(month: YearMonth, today: LocalDate): Boolean = when (this) {
        THIS_WEEK -> { val range = month.atDay(1)..month.atEndOfMonth(); val weekStart = today.with(java.time.DayOfWeek.MONDAY); !(today < range.start || weekStart > range.endInclusive) }
        THIS_MONTH -> month == YearMonth.from(today)
        THIS_YEAR -> month.year == today.year
        ALL_TIME -> true
    }
    fun needsDayFilter(): Boolean = this == THIS_WEEK
    fun label(today: LocalDate): String = when (this) {
        THIS_WEEK -> "This week"
        THIS_MONTH -> YearMonth.from(today).month.name.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase(Locale.ROOT) } + " ${today.year}"
        THIS_YEAR -> today.year.toString()
        ALL_TIME -> "All time"
    }
}

data class ReplaySummary(
    val period: ReplayPeriod, val label: String, val totalMs: Long, val totalPlays: Int,
    val songs: List<RankedSong>, val artists: List<RankedEntry>, val albums: List<RankedEntry>,
    val genres: List<RankedEntry>, val hourOfDay: List<Long>, val busiestDay: String?, val busiestDayMs: Long,
    val distinctSongs: Int, val distinctArtists: Int, val distinctAlbums: Int, val since: String?,
) {
    val minutes: Long get() = totalMs / 60_000
    val hours: Long get() = totalMs / 3_600_000
    val isEmpty: Boolean get() = songs.isEmpty()
    val peakHour: Int? get() = hourOfDay.withIndex().filter { it.value > 0 }.maxByOrNull { it.value }?.index
}
