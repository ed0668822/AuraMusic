package com.music.aurelis.data.settings

/** Where one track stands in Automix's analysis. */
enum class TrackAnalysisState {
    WAITING,
    ANALYSING,
    ANALYSED,
    REFINING,
    FAILED,
}

/** Both sides of the next transition, for stats for nerds. */
data class SmartAnalysis(
    val current: TrackAnalysisState = TrackAnalysisState.WAITING,
    val next: TrackAnalysisState = TrackAnalysisState.WAITING,
)

/**
 * A span of the playing track, in fractions of its duration, that the next
 * transition is planned to occupy.
 */
data class TransitionWindow(val start: Float, val end: Float)

/**
 * An Automix blend in flight, as the scrubber draws it.
 */
data class MixBlend(
    val beatMs: Float,
    val beatAnchorNanos: Long,
    val playing: Boolean,
)
