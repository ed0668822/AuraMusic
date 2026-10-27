package com.music.aura.playback.smart

/**
 * Stored offline analysis for one track, in that track's own timeline seconds.
 *
 * This is the contract between the analyser and the transition policy.
 * Nothing here is PCM: the policy and planner read these fields and their
 * confidences, and never touch audio.
 */
data class TrackAnalysis(
    val status: String = "",
    val trackId: String = "",
    val duration: Double = 0.0,

    val bpm: Double = 0.0,
    val beatInterval: Double = 0.0,
    val beatConfidence: Double = 0.0,
    val downbeats: List<Double> = emptyList(),
    val phraseBoundaries: List<Double> = emptyList(),
    val firstBeat: Double = 0.0,

    val key: String = "",
    val keyConfidence: Double = 0.0,

    val audibleStartTime: Double? = null,
    val pickupTime: Double? = null,
    val introEndTime: Double = 0.0,
    val contentEndTime: Double = 0.0,
    val outroStartTime: Double = 0.0,

    val mixInTime: Double = 0.0,
    val mixOutTime: Double = 0.0,
    val mixInCandidates: List<MixCandidate> = emptyList(),
    val mixOutCandidates: List<MixCandidate> = emptyList(),

    val energyCurve: List<EnergySample> = emptyList(),
    val lowEnergyCurve: List<EnergySample> = emptyList(),
    val vocalActivityMask: List<Double> = emptyList(),
    val vocalProbability: Double = 0.0,
) {
    val isUsable: Boolean get() = status == STATUS_READY && bpm > 0

    companion object {
        const val STATUS_READY = "ready"
    }
}

/** One point on an energy curve. */
data class EnergySample(val time: Double, val energy: Double)

/** A candidate point for a transition to enter or leave on. */
data class MixCandidate(val time: Double, val score: Double, val type: String)

/** A ranked MixCandidate. */
data class RankedMixCandidate(
    val time: Double,
    val score: Double,
    val type: String,
    val rankScore: Double,
    val discardedMusicSeconds: Double = 0.0,
    val measured: Boolean = true,
)

/** Where a transition should end on the outgoing track. */
data class MixOutAnchor(
    val time: Double,
    val type: String,
    val discardedMusicSeconds: Double,
)

/** The verdict on how ambitious a transition the stored evidence supports. */
data class TransitionPolicyVerdict(
    val tier: TransitionTier,
    val reasons: List<String>,
    val beatConfidence: Double,
)

/** The degradation ladder. */
enum class TransitionTier {
    BEATMATCHED,
    DJ_ASSISTED,
    PLAIN_CROSSFADE,
}
