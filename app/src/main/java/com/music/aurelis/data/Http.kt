// BitChord © 2026 Kushagra Singh / Aurelis © 2026. GPL-3.0-or-later.
package com.music.aurelis.data

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Shared HTTP client for the whole app.
 *
 * Lyric providers derive their own client from this one so the connection
 * pool, DNS cache and TLS session cache are common — only their deadlines
 * differ (lyrics want short ones; a hung provider must not hold up the race).
 */
object Http {

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
