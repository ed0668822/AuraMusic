// Ported from BitChord. © 2026 Kushagra Singh (BitChord/Aurelis). GPL-3.0-or-later.
package com.music.aurelis.data.lyrics

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

object TtmlLyrics {

    private val SKIPPED_ROLES = setOf("x-translation", "x-roman")
    private const val BACKGROUND_ROLE = "x-bg"

    fun parse(ttml: String): List<LyricLine> = runCatching {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            harden("http://apache.org/xml/features/disallow-doctype-decl")
            harden("http://xml.org/sax/features/external-general-entities", false)
            harden("http://xml.org/sax/features/external-parameter-entities", false)
            harden(XMLConstants.FEATURE_SECURE_PROCESSING)
            runCatching { isExpandEntityReferences = false }
        }
        val document = factory.newDocumentBuilder().parse(InputSource(StringReader(ttml)))
        val paragraphs = document.getElementsByTagName("p")

        val sung = ArrayList<Pair<LyricLine, String?>>(paragraphs.length)
        for (i in 0 until paragraphs.length) {
            val paragraph = paragraphs.item(i) as? Element ?: continue
            val line = lineFrom(paragraph) ?: continue
            sung += line to paragraph.qualified("ttm:agent").takeIf { it.isNotEmpty() }
        }
        sung.sortBy { it.first.timeMs }

        val sides = lineAlignments(sung.map { it.second }, agentTypes(document))
        sung.mapIndexed { index, (line, _) -> line.copy(alignment = sides[index]) }
            .withInstrumentalGaps()
    }.getOrDefault(emptyList())

    private fun DocumentBuilderFactory.harden(feature: String, value: Boolean = true) {
        runCatching { setFeature(feature, value) }
    }

    private fun agentTypes(document: org.w3c.dom.Document): Map<String, String> {
        val agents = document.getElementsByTagName("ttm:agent")
            .takeIf { it.length > 0 }
            ?: document.getElementsByTagName("agent")
        val types = HashMap<String, String>(agents.length)
        for (i in 0 until agents.length) {
            val agent = agents.item(i) as? Element ?: continue
            val id = agent.qualified("xml:id")
            val type = agent.getAttribute("type")
            if (id.isNotEmpty() && type.isNotEmpty()) types[id] = type
        }
        return types
    }

    private fun Element.qualified(name: String): String {
        getAttribute(name).takeIf { it.isNotEmpty() }?.let { return it }
        val local = name.substringAfter(':')
        val found = attributes ?: return ""
        for (i in 0 until found.length) {
            val attribute = found.item(i) ?: continue
            if (attribute.nodeName == name ||
                attribute.nodeName == local ||
                attribute.localName == local
            ) {
                return attribute.nodeValue.orEmpty()
            }
        }
        return ""
    }

    private fun lineFrom(paragraph: Element): LyricLine? {
        val pieces = mutableListOf<Piece>()
        val backingPieces = mutableListOf<Piece>()
        collect(paragraph, pieces, backingPieces)
        val words = mergeIntoWords(pieces)
        val backing = mergeIntoWords(backingPieces).takeIf { it.isNotEmpty() }?.let {
            LyricLine(
                timeMs = it.first().startMs,
                text = it.joinToString(" ") { word -> word.text },
                words = it,
            )
        }

        if (words.isEmpty()) {
            val text = paragraph.textContent?.trim().orEmpty()
            val begin = time(paragraph.getAttribute("begin")) ?: return null
            if (text.isEmpty()) return null
            val end = time(paragraph.getAttribute("end"))?.takeIf { it > begin }
            return LyricLine(timeMs = begin, text = text, sungUntilMs = end)
        }

        val begin = time(paragraph.getAttribute("begin")) ?: words.first().startMs
        return LyricLine(
            timeMs = minOf(begin, words.first().startMs),
            text = words.joinToString(" ") { it.text },
            words = words,
            background = backing,
        )
    }

    private fun collect(node: Node, out: MutableList<Piece>, backing: MutableList<Piece>) {
        val children = node.childNodes
        for (i in 0 until children.length) {
            when (val child = children.item(i)) {
                is Element -> {
                    val role = child.qualified("ttm:role")
                    if (role in SKIPPED_ROLES) continue
                    val sink = if (role == BACKGROUND_ROLE) backing else out
                    val begin = time(child.getAttribute("begin"))
                    val end = time(child.getAttribute("end"))
                    if (begin != null && end != null && !hasTimedChild(child)) {
                        sink += Piece.Timed(child.textContent.orEmpty(), begin, end)
                    } else {
                        collect(child, sink, backing)
                    }
                }
                else -> if (child.nodeType == Node.TEXT_NODE) {
                    val text = child.textContent.orEmpty()
                    if (text.isNotEmpty()) out += Piece.Text(text)
                }
            }
        }
    }

    private fun hasTimedChild(element: Element): Boolean {
        val children = element.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i) as? Element ?: continue
            if (child.getAttribute("begin").isNotEmpty() || hasTimedChild(child)) return true
        }
        return false
    }

    private fun mergeIntoWords(pieces: List<Piece>): List<LyricWord> {
        val words = mutableListOf<LyricWord>()
        val current = StringBuilder()
        val syllables = mutableListOf<LyricSyllable>()
        var start = 0L
        var end = 0L
        var timed = false

        fun flush() {
            val text = current.toString().trim()
            current.setLength(0)
            if (text.isNotEmpty() && timed) {
                words += LyricWord(start, end, text, LyricSyllable.normalized(syllables, text.length))
            }
            syllables.clear()
            timed = false
        }

        pieces.forEach { piece ->
            when (piece) {
                is Piece.Text -> when {
                    piece.text.isBlank() -> flush()
                    timed -> current.append(piece.text)
                    else -> Unit
                }
                is Piece.Timed -> {
                    if (piece.text.isBlank()) return@forEach
                    if (piece.text.first().isWhitespace()) flush()
                    if (current.isEmpty()) start = piece.start
                    val from = current.length
                    current.append(piece.text.trim())
                    syllables += LyricSyllable(piece.start, piece.end, from, current.length)
                    end = piece.end
                    timed = true
                    if (piece.text.last().isWhitespace()) flush()
                }
            }
        }
        flush()
        return words
    }

    fun time(value: String?): Long? {
        val raw = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (raw.endsWith("ms")) return raw.dropLast(2).toDoubleOrNull()?.toLong()
        val stripped = raw.removeSuffix("s")
        val parts = stripped.split(':')
        val seconds = when (parts.size) {
            1 -> parts[0].toDoubleOrNull()
            2 -> parts[0].toDoubleOrNull()?.let { m -> parts[1].toDoubleOrNull()?.let { m * 60 + it } }
            3 -> parts[0].toDoubleOrNull()?.let { h ->
                parts[1].toDoubleOrNull()?.let { m ->
                    parts[2].toDoubleOrNull()?.let { h * 3600 + m * 60 + it }
                }
            }
            else -> null
        } ?: return null
        return (seconds * 1000).toLong()
    }

    private sealed interface Piece {
        data class Text(val text: String) : Piece
        data class Timed(val text: String, val start: Long, val end: Long) : Piece
    }
}
