package com.music.aurelis.playback.smart

/** Loads the analyser's native library, however the platform keeps it. */
object NativeAnalysisLibrary {

    private var loader: (() -> Unit)? = null

    fun install(loader: () -> Unit) {
        this.loader = loader
    }

    val available: Boolean by lazy {
        runCatching {
            val install = loader
            if (install != null) install() else System.loadLibrary(LIBRARY_NAME)
        }.onFailure { AnalysisLog.warn("Analysis library unavailable; Automix will not run", it) }
            .isSuccess
    }

    const val LIBRARY_NAME = "aurelis_analysis"
}
