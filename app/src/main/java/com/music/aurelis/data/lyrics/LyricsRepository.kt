package com.music.aurelis.data.lyrics

import android.content.Context
import android.net.Uri
import com.music.aurelis.data.model.LocalSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

/**
 * Lector de letras 100% local.
 *
 * Orden de búsqueda:
 *  1. Sidecar `.lrc` junto al archivo (mismo nombre, extensión `.lrc`).
 *  2. Letras embebidas en el contenedor del audio:
 *     - MP4/M4A: átomo `©lyr`.
 *     - FLAC: comentario Vorbis `LYRICS` o `UNSYNCEDLYRICS`.
 *     - Matroska/WebM: `SimpleTag` `LYRICS`.
 *
 * Sin red, sin caché: si no hay letras en disco, devuelve null.
 */
object LyricsRepository {

    private const val MAX_TAG_BYTES = 8 * 1024 * 1024

    /**
     * Devuelve las letras de [song], o null si no hay forma de leerlas.
     * Nunca lanza — cualquier error se traduce en null.
     */
    suspend fun load(context: Context, song: LocalSong): Lyrics? = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Sidecar .lrc junto al archivo (si tenemos path)
            song.filePath?.let { path ->
                val audio = File(path)
                if (audio.exists()) {
                    val lrc = File(audio.parentFile ?: return@let, audio.nameWithoutExtension + ".lrc")
                    if (lrc.isFile && lrc.length() in 1..MAX_TAG_BYTES) {
                        Lyrics.parse(lrc.readText())?.let { return@runCatching it }
                    }
                }
            }

            // 2. Letras embebidas
            val uri = Uri.parse(song.contentUri)
            val stream = if (uri.scheme == "file") {
                uri.path?.let { File(it).takeIf(File::exists)?.inputStream() }
            } else {
                context.contentResolver.openInputStream(uri)
            } ?: return@runCatching null

            stream.use { input ->
                val bytes = input.readAtMost(MAX_TAG_BYTES)
                val raw = parseEmbedded(bytes) ?: return@runCatching null
                Lyrics.parse(raw)
            }
        }.getOrNull()
    }

    /** Prueba los tres contenedores en orden hasta encontrar letras. */
    private fun parseEmbedded(bytes: ByteArray): String? = when {
        bytes.startsWith(FLAC_MAGIC) -> flac(bytes)
        bytes.startsWith(MATROSKA_MAGIC) -> matroska(bytes)
        bytes.isMp4() -> mp4(bytes)
        else -> null
    }?.takeIf { it.isNotBlank() }

    /* ---------- MP4 / M4A ---------- */

    private fun mp4(bytes: ByteArray): String? {
        val moov = topLevelBox(bytes, "moov") ?: return null
        return ilstText(bytes, moov.first, moov.last + 1)
    }

    private fun topLevelBox(bytes: ByteArray, type: String): IntRange? {
        var pos = 0
        while (pos + 8 <= bytes.size) {
            val declared = readU32(bytes, pos)
            var headerLen = 8
            var size = declared
            if (declared == 1L) {
                if (pos + 16 > bytes.size) return null
                size = readU64(bytes, pos + 8)
                headerLen = 16
            } else if (declared == 0L) {
                size = (bytes.size - pos).toLong()
            }
            if (size < headerLen || size > Int.MAX_VALUE) return null
            val end = (pos + size).toInt().coerceAtMost(bytes.size)
            if (String(bytes, pos + 4, 4, Charsets.ISO_8859_1) == type) return pos until end
            pos += size.toInt()
        }
        return null
    }

    private fun ilstText(bytes: ByteArray, from: Int, endExclusive: Int): String? {
        var at = from
        while (true) {
            val found = bytes.indexOf(LYR_ATOM, at, endExclusive) ?: return null
            val data = bytes.indexOf(DATA_ATOM, found, endExclusive) ?: return null
            dataText(bytes, data, endExclusive)?.let { return it }
            at = found + LYR_ATOM.size
        }
    }

    private fun dataText(bytes: ByteArray, dataAt: Int, endExclusive: Int): String? {
        val start = dataAt - 4
        if (start < 0 || dataAt + 12 > endExclusive) return null
        val size = readU32(bytes, start).toInt()
        if (size <= 16 || start + size > endExclusive) return null
        if (readU32(bytes, dataAt + 4).toInt() != 1) return null
        return String(bytes, dataAt + 12, start + size - (dataAt + 12), Charsets.UTF_8)
    }

    /* ---------- FLAC ---------- */

    private fun flac(bytes: ByteArray): String? {
        var pos = FLAC_MAGIC.size
        while (pos + 4 <= bytes.size) {
            val flags = bytes[pos].toInt() and 0xFF
            val length = ((bytes[pos + 1].toInt() and 0xFF) shl 16) or
                ((bytes[pos + 2].toInt() and 0xFF) shl 8) or
                (bytes[pos + 3].toInt() and 0xFF)
            val start = pos + 4
            if (start + length > bytes.size) return null
            if (flags and 0x7F == FLAC_VORBIS_COMMENT) {
                return vorbisComment(bytes, start, start + length)
            }
            if (flags and 0x80 != 0) return null
            pos = start + length
        }
        return null
    }

    private fun vorbisComment(bytes: ByteArray, start: Int, end: Int): String? {
        var pos = start
        fun u32(): Int? {
            if (pos + 4 > end) return null
            val v = (bytes[pos].toInt() and 0xFF) or
                ((bytes[pos + 1].toInt() and 0xFF) shl 8) or
                ((bytes[pos + 2].toInt() and 0xFF) shl 16) or
                ((bytes[pos + 3].toInt() and 0xFF) shl 24)
            pos += 4
            return v
        }
        val vendor = u32() ?: return null
        pos += vendor
        val count = u32() ?: return null
        var plain: String? = null
        repeat(count.coerceAtMost(4_096)) {
            val length = u32() ?: return plain
            if (length < 0 || pos + length > end) return plain
            val entry = String(bytes, pos, length, Charsets.UTF_8)
            pos += length
            val name = entry.substringBefore('=').uppercase()
            val value = entry.substringAfter('=', "")
            if ((name == "LYRICS" || name == "UNSYNCEDLYRICS") && value.isNotBlank() && plain == null) {
                plain = value
            }
        }
        return plain
    }

    /* ---------- Matroska / WebM ---------- */

    private fun matroska(bytes: ByteArray): String? {
        val needle = "LYRICS".toByteArray(Charsets.US_ASCII)
        var from = 0
        while (true) {
            val at = bytes.indexOf(needle, from, bytes.size) ?: return null
            from = at + needle.size
            if (at < 3 || bytes[at - 3] != ID_TAGNAME[0] || bytes[at - 2] != ID_TAGNAME[1]) continue
            if ((bytes[at - 1].toInt() and 0x7F) != needle.size) continue
            val string = bytes.indexOf(ID_TAGSTRING, from, bytes.size) ?: continue
            val size = readVint(bytes, string + 2) ?: continue
            val valueAt = string + 2 + size.width
            if (size.value <= 0 || valueAt + size.value > bytes.size) continue
            val value = String(bytes, valueAt, size.value.toInt(), Charsets.UTF_8)
            if (value.isNotBlank()) return value
        }
    }

    private class Vint(val value: Long, val width: Int)

    private fun readVint(bytes: ByteArray, offset: Int): Vint? {
        if (offset >= bytes.size) return null
        val first = bytes[offset].toInt() and 0xFF
        if (first == 0) return null
        var width = 1
        var mask = 0x80
        while (first and mask == 0) {
            mask = mask shr 1
            width++
        }
        if (offset + width > bytes.size) return null
        var value = (first and mask.inv() and 0xFF).toLong()
        for (i in 1 until width) value = (value shl 8) or (bytes[offset + i].toLong() and 0xFF)
        return Vint(value, width)
    }

    /* ---------- Bytes helpers ---------- */

    private fun InputStream.readAtMost(max: Int): ByteArray {
        val out = ByteArrayOutputStream(minOf(max, 1 shl 16))
        val buffer = ByteArray(1 shl 16)
        var total = 0
        while (total < max) {
            val read = read(buffer, 0, minOf(buffer.size, max - total))
            if (read <= 0) break
            out.write(buffer, 0, read)
            total += read
        }
        return out.toByteArray()
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (size < prefix.size) return false
        return prefix.indices.all { this[it] == prefix[it] }
    }

    private fun ByteArray.isMp4(): Boolean =
        size > 12 && this[4] == 'f'.code.toByte() && this[5] == 't'.code.toByte() &&
            this[6] == 'y'.code.toByte() && this[7] == 'p'.code.toByte()

    private fun ByteArray.indexOf(needle: ByteArray, from: Int, until: Int): Int? {
        if (needle.isEmpty()) return null
        val last = minOf(until, size) - needle.size
        var i = from.coerceAtLeast(0)
        outer@ while (i <= last) {
            for (j in needle.indices) {
                if (this[i + j] != needle[j]) { i++; continue@outer }
            }
            return i
        }
        return null
    }

    private fun readU32(b: ByteArray, off: Int): Long =
        ((b[off].toLong() and 0xFF) shl 24) or ((b[off + 1].toLong() and 0xFF) shl 16) or
            ((b[off + 2].toLong() and 0xFF) shl 8) or (b[off + 3].toLong() and 0xFF)

    private fun readU64(b: ByteArray, off: Int): Long {
        var v = 0L
        for (i in 0 until 8) v = (v shl 8) or (b[off + i].toLong() and 0xFF)
        return v
    }

    private const val FLAC_VORBIS_COMMENT = 4
    private val FLAC_MAGIC = "fLaC".toByteArray(Charsets.US_ASCII)
    private val MATROSKA_MAGIC = byteArrayOf(0x1A, 0x45, 0xDF.toByte(), 0xA3.toByte())
    private val DATA_ATOM = "data".toByteArray(Charsets.ISO_8859_1)
    private val LYR_ATOM = byteArrayOf(0xA9.toByte()) + "lyr".toByteArray(Charsets.ISO_8859_1)
    private val ID_TAGNAME = byteArrayOf(0x45, 0xA3.toByte())
    private val ID_TAGSTRING = byteArrayOf(0x44, 0x87.toByte())
}
