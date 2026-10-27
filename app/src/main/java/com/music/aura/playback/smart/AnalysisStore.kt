package com.music.aura.playback.smart

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap

class AnalysisStore(
    private val directoryOf: () -> File,
) {

    private val directory by lazy { File(directoryOf(), DIRECTORY) }

    private val known = ConcurrentHashMap<String, Boolean>()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load(trackId: String): TrackAnalysis? {
        if (trackId.isBlank()) return null
        if (known[trackId] == false) return null
        val file = File(directory, fileNameFor(trackId))
        if (!file.exists()) {
            known[trackId] = false
            return null
        }
        return runCatching {
            val stored = json.decodeFromString(Stored.serializer(), file.readText())
            require(stored.version == SCHEMA_VERSION) { "schema ${stored.version}" }
            stored.toAnalysis(trackId)
        }
            .onFailure {
                AnalysisLog.warn("Discarding unreadable analysis for $trackId", it)
                file.delete()
                known[trackId] = false
            }
            .getOrNull()
    }

    fun save(trackId: String, analysis: TrackAnalysis) {
        if (trackId.isBlank() || !analysis.isUsable) return
        runCatching {
            directory.mkdirs()
            val file = File(directory, fileNameFor(trackId))
            val temporary = File(directory, file.name + ".tmp")
            try {
                temporary.writeText(json.encodeToString(Stored.serializer(), Stored.of(analysis)))
                moveInto(temporary, file)
            } finally {
                temporary.delete()
            }
            known[trackId] = true
        }.onFailure { AnalysisLog.warn("Could not store analysis for $trackId", it) }
        prune()
    }

    private fun moveInto(temporary: File, file: File) {
        try {
            Files.move(
                temporary.toPath(),
                file.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun prune() {
        val files = directory.listFiles() ?: return
        if (files.size <= MAX_ENTRIES) return
        files.sortedBy { it.lastModified() }
            .take(files.size - MAX_ENTRIES)
            .forEach { it.delete() }
    }

    private fun fileNameFor(trackId: String): String = "${trackId.hashCode().toUInt()}_${trackId.length}.json"

    @Serializable
    private data class Stored(
        val version: Int = SCHEMA_VERSION,
        val duration: Double = 0.0,
        val bpm: Double = 0.0,
        val beatInterval: Double = 0.0,
        val beatConfidence: Double = 0.0,
        val firstBeat: Double = 0.0,
        val downbeats: List<Double> = emptyList(),
        val phraseBoundaries: List<Double> = emptyList(),
        val key: String = "",
        val keyConfidence: Double = 0.0,
        val audibleStartTime: Double? = null,
        val pickupTime: Double? = null,
        val introEndTime: Double = 0.0,
        val outroStartTime: Double = 0.0,
        val contentEndTime: Double = 0.0,
        val mixInTime: Double = 0.0,
        val mixOutTime: Double = 0.0,
        val mixInCandidates: List<StoredCue> = emptyList(),
        val mixOutCandidates: List<StoredCue> = emptyList(),
        val energyCurve: List<StoredEnergy> = emptyList(),
        val lowEnergyCurve: List<StoredEnergy> = emptyList(),
        val vocalActivityMask: List<Double> = emptyList(),
        val vocalProbability: Double = 0.0,
    ) {
        fun toAnalysis(trackId: String) = TrackAnalysis(
            status = TrackAnalysis.STATUS_READY,
            trackId = trackId,
            duration = duration,
            bpm = bpm,
            beatInterval = beatInterval,
            beatConfidence = beatConfidence,
            firstBeat = firstBeat,
            downbeats = downbeats,
            phraseBoundaries = phraseBoundaries,
            key = key,
            keyConfidence = keyConfidence,
            audibleStartTime = audibleStartTime,
            pickupTime = pickupTime,
            introEndTime = introEndTime,
            outroStartTime = outroStartTime,
            contentEndTime = contentEndTime,
            mixInTime = mixInTime,
            mixOutTime = mixOutTime,
            mixInCandidates = mixInCandidates.map { it.toCue() },
            mixOutCandidates = mixOutCandidates.map { it.toCue() },
            energyCurve = energyCurve.map { it.toSample() },
            lowEnergyCurve = lowEnergyCurve.map { it.toSample() },
            vocalActivityMask = vocalActivityMask,
            vocalProbability = vocalProbability,
        )

        companion object {
            fun of(analysis: TrackAnalysis) = Stored(
                duration = analysis.duration,
                bpm = analysis.bpm,
                beatInterval = analysis.beatInterval,
                beatConfidence = analysis.beatConfidence,
                firstBeat = analysis.firstBeat,
                downbeats = analysis.downbeats.map(::round),
                phraseBoundaries = analysis.phraseBoundaries.map(::round),
                key = analysis.key,
                keyConfidence = analysis.keyConfidence,
                audibleStartTime = analysis.audibleStartTime,
                pickupTime = analysis.pickupTime,
                introEndTime = analysis.introEndTime,
                outroStartTime = analysis.outroStartTime,
                contentEndTime = analysis.contentEndTime,
                mixInTime = analysis.mixInTime,
                mixOutTime = analysis.mixOutTime,
                mixInCandidates = analysis.mixInCandidates.map(StoredCue::of),
                mixOutCandidates = analysis.mixOutCandidates.map(StoredCue::of),
                energyCurve = analysis.energyCurve.map(StoredEnergy::of),
                lowEnergyCurve = analysis.lowEnergyCurve.map(StoredEnergy::of),
                vocalActivityMask = analysis.vocalActivityMask.map(::round),
                vocalProbability = analysis.vocalProbability,
            )
        }
    }

    @Serializable
    private data class StoredCue(val time: Double, val score: Double, val type: String) {
        fun toCue() = MixCandidate(time = time, score = score, type = type)
        companion object {
            fun of(cue: MixCandidate) = StoredCue(round(cue.time), round(cue.score), cue.type)
        }
    }

    @Serializable
    private data class StoredEnergy(val time: Double, val energy: Double) {
        fun toSample() = EnergySample(time = time, energy = energy)
        companion object {
            fun of(sample: EnergySample) = StoredEnergy(round(sample.time), round(sample.energy))
        }
    }

    private companion object {
        const val DIRECTORY = "smart_analysis"
        const val SCHEMA_VERSION = 1
        const val MAX_ENTRIES = 2_000

        fun round(value: Double): Double =
            if (value.isFinite()) Math.round(value * 1000.0) / 1000.0 else 0.0
    }
}
