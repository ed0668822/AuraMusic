// Ported from BitChord. © 2026 Kushagra Singh (BitChord/Aurelis). GPL-3.0-or-later.
package com.music.aurelis.data.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.concurrent.atomic.AtomicReference

object LyricsPlus {

    private val MIRRORS = listOf(
        "https://lyricsplus.prjktla.my.id",
        "https://lyricsplus.atomix.one",
        "https://lyricsplus.binimum.org",
        "https://lyricsplus.prjktla.workers.dev",
        "https://lyricsplus-seven.vercel.app",
        "https://lyrics-plus-backend.vercel.app",
    )

    private val lastGood = AtomicReference<String?>(null)

    suspend fun lyrics(
        title: String,
        artist: String,
        durationMs: Long,
        album: String? = null,
        isrc: String? = null,
    ): List<LyricLine>? = coroutineScope {
        val hosts = lastGood.get()
            ?.let { listOf(it) + MIRRORS.filterNot { mirror -> mirror == it } }
            ?: MIRRORS

        val pending = hosts.map { host ->
            host to async(Dispatchers.IO) { fetch(host, title, artist, durationMs, album, isrc) }
        }.toMutableList()

        try {
            while (pending.isNotEmpty()) {
                val (host, lines) = select {
                    pending.forEach { (host, job) -> job.onAwait { host to it } }
                }
                pending.removeAll { it.first == host }
                if (!lines.isNullOrEmpty()) {
                    lastGood.set(host)
                    return@coroutineScope lines
                }
            }
            null
        } finally {
            pending.forEach { it.second.cancel() }
        }
    }

    private suspend fun fetch(
        host: String,
        title: String,
        artist: String,
        durationMs: Long,
        album: String?,
        isrc: String?,
    ): List<LyricLine>? = withContext(Dispatchers.IO) {
        val url = "$host/v2/lyrics/get".toHttpUrl().newBuilder()
            .addQueryParameter("title", title)
            .addQueryParameter("artist", artist)
            .apply {
                val seconds = durationMs / 1000
                if (seconds > 0) addQueryParameter("duration", seconds.toString())
                if (!album.isNullOrBlank()) addQueryParameter("album", album)
                if (!isrc.isNullOrBlank()) addQueryParameter("isrc", isrc)
            }
            .build()

        val body = lyricsGet(url.toString()) ?: return@withContext null
        val response = runCatching { lyricsJson.decodeFromString<Response>(body) }.getOrNull()
            ?: return@withContext null
        parse(response).takeIf { it.isNotEmpty() }
    }

    fun parse(response: Response): List<LyricLine> {
        val sung = response.lyrics.orEmpty().mapNotNull { line ->
            val start = line.time ?: return@mapNotNull null
            val words = mergeSyllables(line.syllabus.orEmpty())
            val built = when {
                words.isNotEmpty() -> LyricLine(
                    timeMs = minOf(start, words.first().startMs),
                    text = words.joinToString(" ") { it.text },
                    words = words,
                )
                !line.text.isNullOrBlank() -> LyricLine(
                    timeMs = start,
                    text = line.text.trim(),
                    sungUntilMs = line.duration?.takeIf { it > 0 }?.let { start + it },
                )
                else -> null
            }
            built?.to(line.element)
        }.sortedBy { it.first.timeMs }

        val sides = lineAlignments(sung.map { singerOf(it.second) }, response.agentTypes())
        return sung.mapIndexed { index, (line, element) ->
            val side = if (sides[index] == LyricAlignment.End || element.saysOpposite()) {
                LyricAlignment.End
            } else {
                LyricAlignment.Start
            }
            line.copy(alignment = side)
        }.withInstrumentalGaps()
    }

    private fun singerOf(element: JsonElement?): String? =
        (element as? JsonObject)?.get("singer")?.let { it as? JsonPrimitive }?.contentOrNull

    private fun cleanContent(primitive: JsonPrimitive): String? =
        if (primitive.isString) primitive.content else null

    private fun JsonElement?.saysOpposite(): Boolean {
        val array = runCatching { this?.jsonArray }.getOrNull() ?: return false
        return array.any { entry ->
            val tag = runCatching { entry.jsonPrimitive }.getOrNull()?.let(::cleanContent)
            tag == "opposite" || tag == "right"
        }
    }

    private fun mergeSyllables(syllables: List<Syllable>): List<LyricWord> = wordsFromRuns(
        syllables.mapNotNull { syllable ->
            val text = syllable.text ?: return@mapNotNull null
            val time = syllable.time ?: return@mapNotNull null
            TimedRun(time, time + (syllable.duration ?: 0L), text)
        },
        spacingIsExplicit = true,
    )

    @Serializable
    data class Response(
        val type: String? = null,
        val lyrics: List<Line>? = null,
        val metadata: Metadata? = null,
    ) {
        fun agentTypes(): Map<String, String> = buildMap {
            metadata?.agents?.forEach { (id, agent) ->
                val type = agent.type ?: return@forEach
                put(agent.alias ?: id, type)
            }
        }
    }

    @Serializable
    data class Metadata(val agents: Map<String, Agent>? = null)

    @Serializable
    data class Agent(val type: String? = null, val alias: String? = null)

    @Serializable
    data class Line(
        val time: Long? = null,
        val duration: Long? = null,
        val text: String? = null,
        @SerialName("syllabus") val syllabus: List<Syllable>? = null,
        val element: JsonElement? = null,
    )

    @Serializable
    data class Syllable(
        val time: Long? = null,
        val duration: Long? = null,
        val text: String? = null,
    )
}
