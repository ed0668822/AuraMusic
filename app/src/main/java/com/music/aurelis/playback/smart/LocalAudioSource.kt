package com.music.aurelis.playback.smart

import android.content.ContentResolver
import android.media.MediaDataSource
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import android.util.Log
import java.io.IOException
import java.util.Locale

/**
 * A reader over a track that is already on the device.
 * Adapted for Aurelis Music: all tracks are local, so this is the primary source.
 */
internal object LocalAudioSource {

    private const val TAG = "AurelisLocalAudio"

    fun isLocal(uri: Uri): Boolean = when (uri.scheme?.lowercase(Locale.ROOT)) {
        ContentResolver.SCHEME_FILE, ContentResolver.SCHEME_CONTENT -> true
        else -> false
    }

    fun open(resolver: ContentResolver, uri: Uri): MediaDataSource? {
        val descriptor = runCatching { resolver.openFileDescriptor(uri, "r") }
            .onFailure { Log.w(TAG, "Cannot open $uri for analysis", it) }
            .getOrNull() ?: return null
        val size = descriptor.statSize
        if (size <= 0L) {
            Log.w(TAG, "Skipping $uri for analysis: not a seekable file")
            runCatching { descriptor.close() }
            return null
        }
        return Source(descriptor, size)
    }

    private class Source(
        private val descriptor: ParcelFileDescriptor,
        private val length: Long,
    ) : MediaDataSource() {

        override fun getSize(): Long = length

        override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
            if (position < 0 || position >= length) return -1
            if (size <= 0) return 0
            val wanted = minOf(size.toLong(), length - position).toInt()
            return try {
                Os.pread(descriptor.fileDescriptor, buffer, offset, wanted, position)
                    .takeIf { it > 0 } ?: -1
            } catch (error: ErrnoException) {
                throw IOException("pread of $length bytes at $position failed", error)
            }
        }

        override fun close() {
            runCatching { descriptor.close() }
        }
    }
}
