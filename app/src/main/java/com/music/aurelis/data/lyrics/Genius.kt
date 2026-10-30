// Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.data.lyrics

import com.music.aurelis.data.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.TextNode
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

object Genius {

    private const val USER_AGENT = "Aurelis"

    private val json by lazy { Json { ignoreUnknownKeys = true; isLenient = true } }

    private val httpClient by lazy {
        Http.client.newBuilder()
            .callTimeout(8, TimeUnit.SECONDS)
            .connectTimeout(4, TimeUnit.SECONDS)
            .build()
    }

    suspend fun lyrics(title: String, artist: String): List<LyricLine>? = withContext(Dispatchers.IO) {
        runCatching { scrapeLyrics(title, artist) }.getOrNull()
    }

    private fun scrapeLyrics(title: String, artist: String): List<LyricLine>? {
        val cleanTitle = cleanQuery(title)
        val cleanArtist = cleanQuery(artist)

        val searchAttempts = mutableListOf<SearchAttempt>()
        if (cleanArtist.isNotBlank() && cleanTitle.isNotBlank()) {
            searchAttempts.add(SearchAttempt("$cleanArtist $cleanTitle".trim(), cleanTitle, cleanArtist))
        }
        if (cleanTitle.isNotBlank()) {
            searchAttempts.add(SearchAttempt(cleanTitle, cleanTitle, cleanArtist))
        }

        val songUrl = searchAttempts.distinctBy { it.query }.asSequence()
            .mapNotNull { attempt -> searchSongUrl(attempt.query, attempt.title, attempt.artist) }
            .firstOrNull() ?: return null

        val html = httpGet(songUrl) ?: return null
        return parseHtml(html)
    }

    private fun searchSongUrl(query: String, targetTitle: String, targetArtist: String): String? {
        val url = "https://genius.com/api/search/multi?q=${URLEncoder.encode(query, "UTF-8")}"
        val responseBody = httpGet(url) ?: return null

        return runCatching {
            val root = json.parseToJsonElement(responseBody).jsonObject
            val responseObj = root["response"]?.jsonObject ?: return null
            val sections = responseObj["sections"]?.jsonArray ?: return null

            val songSection = sections.firstOrNull {
                (it as? JsonObject)?.get("type")?.jsonPrimitive?.contentOrNull == "song"
            }?.jsonObject ?: return null

            val hits = songSection["hits"]?.jsonArray ?: return null
            val candidates = hits.mapNotNull { (it as? JsonObject)?.get("result")?.jsonObject }

            val best = bestMatch(candidates, targetTitle, targetArtist)
            best?.get("url")?.jsonPrimitive?.contentOrNull
        }.getOrNull()
    }

    private fun bestMatch(
        candidates: List<JsonObject>,
        targetTitle: String,
        targetArtist: String,
    ): JsonObject? {
        if (candidates.isEmpty()) return null
        val normTitle = targetTitle.lowercase(Locale.ROOT)
        val normArtist = targetArtist.lowercase(Locale.ROOT)

        val scored = candidates.mapNotNull { item ->
            val title = item["title"]?.jsonPrimitive?.contentOrNull?.lowercase(Locale.ROOT) ?: ""
            val artist = item["artist_names"]?.jsonPrimitive?.contentOrNull?.lowercase(Locale.ROOT) ?: ""
            var score = 0

            val titleMatches = normTitle.isNotBlank() && (title == normTitle || title.contains(normTitle) || normTitle.contains(title))
            val artistMatches = normArtist.isNotBlank() && (artist == normArtist || artist.contains(normArtist) || normArtist.contains(artist))

            if (!titleMatches && !artistMatches) return@mapNotNull null

            if (title == normTitle) score += 50
            else if (titleMatches) score += 25
            if (artistMatches) {
                if (artist == normArtist) score += 40 else score += 20
            }
            if (score <= 0) return@mapNotNull null
            Pair(item, score)
        }
        return scored.maxByOrNull { it.second }?.first
    }

    fun parseHtml(html: String): List<LyricLine>? = runCatching { parseHtmlUnsafe(html) }.getOrNull()

    private fun parseHtmlUnsafe(html: String): List<LyricLine>? {
        val doc = Jsoup.parse(html)

        var containers = doc.select("div[data-lyrics-container=true]")
        if (containers.isEmpty()) containers = doc.select("div.lyrics")
        if (containers.isEmpty()) return null

        val fullTextBuilder = StringBuilder()

        for (container in containers) {
            container.select(
                "[data-exclude-from-selection=true], " +
                    ".LyricsHeader__Container, " +
                    ".SongBioPreview__Container, " +
                    ".InreadAd__Container, " +
                    "button, script, style",
            ).remove()

            container.select("br").forEach { it.replaceWith(TextNode("\n")) }
            container.select("p").forEach { it.prepend("\n") }

            val text = container.wholeText()
            if (text.isNotBlank()) fullTextBuilder.append(text).append("\n")
        }

        val rawLyrics = fullTextBuilder.toString()
        if (rawLyrics.isBlank()) return null

        val cleaned = stripArtifacts(rawLyrics)
        return textToLyricLines(cleaned).takeIf { it.isNotEmpty() }
    }

    fun stripArtifacts(raw: String): String {
        return raw
            .replace('\u00A0', ' ')
            .replace('\u200B', ' ')
            .replace('\uFEFF', ' ')
            .replace(YOU_MIGHT_ALSO_LIKE, "")
            .trim()
            .replace(TRAILING_EMBED, "")
            .trim()
    }

    fun textToLyricLines(text: String): List<LyricLine> {
        val lines = text.lines()
        val result = mutableListOf<LyricLine>()
        var lastWasGap = false

        for (rawLine in lines) {
            val line = rawLine.trim().replace(TRAILING_EMBED, "").trim()
            if (line.isEmpty()) {
                if (!lastWasGap && result.isNotEmpty()) {
                    result.add(LyricLine(timeMs = 0L, text = ""))
                    lastWasGap = true
                }
            } else {
                result.add(LyricLine(timeMs = 0L, text = line))
                lastWasGap = false
            }
        }

        while (result.isNotEmpty() && result.first().isGap) result.removeAt(0)
        while (result.isNotEmpty() && result.last().isGap) result.removeAt(result.lastIndex)
        return result
    }

    fun isSectionHeader(text: String): Boolean = isSectionHeaderLine(text)

    private fun isSectionHeaderLine(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.startsWith("[") && trimmed.endsWith("]") && trimmed.length in 3..60
    }

    private fun cleanQuery(text: String): String {
        val cleaned = text
            .replace(DECORATIVE_CHARS, " ")
            .replace(NOISE, " ")
            .replace(PRODUCER_TAGS, " ")
            .substringBefore(" | ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return cleaned.ifBlank { text.trim() }
    }

    private data class SearchAttempt(val query: String, val title: String, val artist: String)

    private fun httpGet(url: String): String? = runCatching {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,application/json,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) response.body?.string() else null
        }
    }.getOrNull()

    private val DECORATIVE_CHARS by lazy { Regex("""[♪♫★☆【】《》「」~_]""") }
    private val PRODUCER_TAGS by lazy { Regex("""(?i)\b(?:prod(?:uced)?\.?(?:\s+by)?)\s+.*$""") }
    private val NOISE by lazy {
        Regex(
            """\s*[(\[]\s*(?:from|feat\.?|ft\.?|featuring|with|prod\.?|produced by|official|lyrical|video|audio|remix|music video|visualizer|mv|hd|4k|hq|full song)[^)\]]*[)\]]|""" +
                """\s*\b(?:official\s+(?:music\s+)?(?:video|audio)|lyrical(?:\s+video)?|full\s+song|4k\s+video|hd\s+video|music\s+video)\b""",
            RegexOption.IGNORE_CASE,
        )
    }
    private val YOU_MIGHT_ALSO_LIKE by lazy { Regex("""\d*You might also like""", RegexOption.IGNORE_CASE) }
    private val TRAILING_EMBED by lazy { Regex("""\d*Embed\s*$""", RegexOption.IGNORE_CASE) }
}
