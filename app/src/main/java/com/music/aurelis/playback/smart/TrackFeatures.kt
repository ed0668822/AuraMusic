package com.music.aurelis.playback.smart

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

object TrackFeatures {

    val available: Boolean get() = NativeAnalysisLibrary.available

    val sampleRate: Double by lazy { if (available) nativeSampleRate() else 11_025.0 }

    fun analyze(samples: FloatArray, durationSeconds: Double): Features? {
        if (!available || samples.isEmpty()) return null
        val json = runCatching { nativeAnalyze(samples, sampleRate, durationSeconds) }
            .onFailure { AnalysisLog.warn("Native analysis failed", it) }
            .getOrNull() ?: return null
        return runCatching { parse(Json.parseToJsonElement(json).jsonObject) }
            .onFailure { AnalysisLog.warn("Could not parse analysis output", it) }
            .getOrNull()
    }

    fun resample(samples: FloatArray, inputRate: Double, outputRate: Double = sampleRate): FloatArray? {
        if (!available || samples.isEmpty() || inputRate <= 0 || outputRate <= 0) return null
        return nativeResample(samples, inputRate, outputRate).takeIf { it.isNotEmpty() }
    }

    data class Features(
        val duration: Double,
        val bpm: Double,
        val beatInterval: Double,
        val firstBeat: Double,
        val beatConfidence: Double,
        val key: String,
        val keyConfidence: Double,
        val audibleStartTime: Double,
        val pickupTime: Double,
        val introEndTime: Double,
        val outroStartTime: Double,
        val contentEndTime: Double,
        val mixInTime: Double,
        val mixOutTime: Double,
        val vocalProbability: Double,
        val downbeats: List<Double>,
        val phraseBoundaries: List<Double>,
        val vocalActivityMask: List<Double>,
        val energyCurve: List<EnergySample>,
        val lowEnergyCurve: List<EnergySample>,
        val mixInCandidates: List<MixCandidate>,
        val mixOutCandidates: List<MixCandidate>,
    )

    fun parse(root: JsonObject): Features = Features(
        duration = root.number("duration"),
        bpm = root.number("bpm"),
        beatInterval = root.number("beatInterval"),
        firstBeat = root.number("firstBeat"),
        beatConfidence = root.number("beatConfidence"),
        key = root.text("key"),
        keyConfidence = root.number("keyConfidence"),
        audibleStartTime = root.number("audibleStartTime"),
        pickupTime = root.number("pickupTime"),
        introEndTime = root.number("introEndTime"),
        outroStartTime = root.number("outroStartTime"),
        contentEndTime = root.number("contentEndTime"),
        mixInTime = root.number("mixInTime"),
        mixOutTime = root.number("mixOutTime"),
        vocalProbability = root.number("vocalProbability"),
        downbeats = root.doubles("downbeats"),
        phraseBoundaries = root.doubles("phraseBoundaries"),
        vocalActivityMask = root.doubles("vocalActivityMask"),
        energyCurve = root.energyCurve("energyCurve"),
        lowEnergyCurve = root.energyCurve("lowEnergyCurve"),
        mixInCandidates = root.cuePoints("mixInCandidates"),
        mixOutCandidates = root.cuePoints("mixOutCandidates"),
    )

    private fun JsonObject.number(name: String): Double =
        (this[name]?.jsonPrimitive?.doubleOrNull ?: 0.0).orZero()

    private fun JsonObject.text(name: String): String =
        this[name]?.jsonPrimitive?.contentOrNull ?: ""

    private fun JsonObject.array(name: String): JsonArray? =
        runCatching { this[name]?.jsonArray }.getOrNull()

    private fun JsonObject.doubles(name: String): List<Double> {
        val array = array(name) ?: return emptyList()
        return array.mapNotNull { it.jsonPrimitive.doubleOrNull?.takeIf(Double::isFinite) }
    }

    private fun JsonObject.energyCurve(name: String): List<EnergySample> {
        val array = array(name) ?: return emptyList()
        return array.mapNotNull { element ->
            val point = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
            val time = point["t"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
            val energy = point["e"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
            if (time.isFinite() && energy.isFinite()) EnergySample(time, energy) else null
        }
    }

    private fun JsonObject.cuePoints(name: String): List<MixCandidate> {
        val array = array(name) ?: return emptyList()
        return array.mapNotNull { element ->
            val point = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
            val time = point["t"]?.jsonPrimitive?.doubleOrNull?.takeIf(Double::isFinite)
                ?: return@mapNotNull null
            MixCandidate(
                time = time,
                score = point.number("s"),
                type = point.text("y"),
            )
        }
    }

    @JvmStatic private external fun nativeAnalyze(samples: FloatArray, sampleRate: Double, duration: Double): String
    @JvmStatic private external fun nativeSampleRate(): Double
    @JvmStatic private external fun nativeResample(samples: FloatArray, inputRate: Double, outputRate: Double): FloatArray
}
