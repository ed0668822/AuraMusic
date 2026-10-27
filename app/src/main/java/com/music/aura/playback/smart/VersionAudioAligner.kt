package com.music.aura.playback.smart

import android.content.Context
import android.media.MediaDataSource
import android.net.Uri
import android.util.Log
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.system.measureTimeMillis

/**
 * Analyzes audio waveforms/energy envelopes to find the exact time alignment
 * offset between two versions of the same track.
 *
 * Adapted for Aura Music: operates on local file URIs only.
 */
@UnstableApi
object VersionAudioAligner {

    private const val TAG = "AuraVersionAligner"
    private const val HOP_MS = 10.0
    private const val WIN_MS = 40.0
    private const val MAX_LAG_SECONDS = 40.0
    private const val MIN_CONFIDENCE = 0.40

    private val offsetCache = ConcurrentHashMap<Pair<String, String>, Long>()

    enum class CutPhase { FETCHING, MEASURING, ALIGNED, FAILED }

    data class AlignmentStatus(
        val sourceId: String,
        val targetId: String,
        val phase: CutPhase,
        val offsetMs: Long? = null,
        val fromCache: Boolean = false,
    )

    val status = MutableStateFlow<AlignmentStatus?>(null)

    private val attempt = AtomicLong()

    fun beginAttempt(sourceId: String, targetId: String): Long {
        val token = attempt.incrementAndGet()
        status.value = AlignmentStatus(sourceId, targetId, CutPhase.FETCHING)
        return token
    }

    fun publishPhase(
        token: Long,
        phase: CutPhase,
        offsetMs: Long? = null,
        fromCache: Boolean = false,
    ) {
        if (attempt.get() != token) return
        val current = status.value ?: return
        status.value = current.copy(phase = phase, offsetMs = offsetMs, fromCache = fromCache)
    }

    fun getCachedOffsetMs(sourceId: String, targetId: String): Long? {
        if (sourceId == targetId) return 0L
        return offsetCache[sourceId to targetId]
    }

    /**
     * Calculates or retrieves the time offset between two local tracks.
     * null means it could not be measured.
     */
    suspend fun findOffsetMs(
        context: Context,
        sourceUri: Uri,
        targetUri: Uri,
        sourceId: String,
        targetId: String,
    ): Long? = withContext(Dispatchers.IO) {
        if (sourceId == targetId) return@withContext 0L
        val cached = offsetCache[sourceId to targetId]
        if (cached != null) return@withContext cached

        val sourceDs = LocalAudioSource.open(context.contentResolver, sourceUri)
        val targetDs = LocalAudioSource.open(context.contentResolver, targetUri)

        if (sourceDs == null || targetDs == null) {
            safeClose(sourceDs)
            safeClose(targetDs)
            Log.w(TAG, "Alignment: cannot open one or both URIs")
            return@withContext null
        }

        try {
            var decodes: Pair<Pair<AudioDecoder.Pcm, Double>?, Pair<AudioDecoder.Pcm, Double>?> =
                null to null
            measureTimeMillis {
                decodes = coroutineScope {
                    val source = async(Dispatchers.IO) {
                        AudioDecoder.decodeRegion(sourceDs, 0.0, 45.0)
                    }
                    val target = async(Dispatchers.IO) {
                        AudioDecoder.decodeRegion(targetDs, 0.0, 45.0)
                    }
                    source.await() to target.await()
                }
            }
            val (sourceDecode, targetDecode) = decodes

            if (sourceDecode == null || targetDecode == null) {
                return@withContext null
            }

            val (pcmSource, startSourceSec) = sourceDecode
            val (pcmTarget, startTargetSec) = targetDecode

            if (pcmSource.samples.isEmpty() || pcmTarget.samples.isEmpty()) {
                return@withContext null
            }

            val envelopes = computeEnergyEnvelope(pcmSource.samples, pcmSource.sampleRate) to
                computeEnergyEnvelope(pcmTarget.samples, pcmTarget.sampleRate)

            val correlation = crossCorrelate(envelopes.first, envelopes.second)

            val bestLagFrames = correlation.lagFrames
            val confidence = correlation.confidence

            if (confidence >= MIN_CONFIDENCE) {
                val floor = correlation.floor
                val ceiling = correlation.ceiling
                if (bestLagFrames == floor || bestLagFrames == ceiling) {
                    Log.w(TAG, "Alignment: peak pinned to edge — window too short")
                    return@withContext null
                }

                val lagMs = (bestLagFrames * HOP_MS).toLong()
                val startDiffMs = ((startTargetSec - startSourceSec) * 1000.0).toLong()
                val totalOffsetMs = lagMs + startDiffMs

                offsetCache[sourceId to targetId] = totalOffsetMs
                offsetCache[targetId to sourceId] = -totalOffsetMs
                return@withContext totalOffsetMs
            }

            Log.w(TAG, "Alignment: confidence $confidence below $MIN_CONFIDENCE")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to compute audio alignment", e)
        } finally {
            safeClose(sourceDs)
            safeClose(targetDs)
        }

        null
    }

    private fun safeClose(ds: MediaDataSource?) {
        if (ds != null) {
            runCatching { ds.close() }
        }
    }

    private fun computeEnergyEnvelope(
        samples: FloatArray,
        sampleRate: Double,
        hopMs: Double = HOP_MS,
        winMs: Double = WIN_MS,
    ): FloatArray {
        val hopSamples = max(1, (sampleRate * (hopMs / 1000.0)).toInt())
        val winSamples = max(hopSamples, (sampleRate * (winMs / 1000.0)).toInt())
        val numFrames = max(0, (samples.size - winSamples) / hopSamples)
        if (numFrames <= 0) return FloatArray(0)

        val env = FloatArray(numFrames)
        for (i in 0 until numFrames) {
            val start = i * hopSamples
            val end = min(samples.size, start + winSamples)
            var sumSq = 0.0
            for (j in start until end) {
                val s = samples[j]
                sumSq += s * s
            }
            env[i] = sqrt(sumSq / (end - start)).toFloat()
        }
        return env
    }

    data class Correlation(val lagFrames: Int, val confidence: Double, val floor: Int, val ceiling: Int)

    fun crossCorrelate(
        sourceEnv: FloatArray,
        targetEnv: FloatArray,
        hopMs: Double = HOP_MS,
        maxLagSec: Double = MAX_LAG_SECONDS,
    ): Correlation {
        if (sourceEnv.isEmpty() || targetEnv.isEmpty()) return Correlation(0, 0.0, 0, 0)

        val maxLagFrames = (maxLagSec * 1000.0 / hopMs).toInt()
        val minOverlapFrames = (3.0 * 1000.0 / hopMs).toInt()

        var bestLag = 0
        var bestCorrelation = -1.0

        val minLag = max(-maxLagFrames, -(sourceEnv.size - minOverlapFrames))
        val maxLag = min(maxLagFrames, targetEnv.size - minOverlapFrames)

        val crossTerms = crossTermsByLag(sourceEnv, targetEnv)

        val srcPrefix = DoubleArray(sourceEnv.size + 1)
        val srcSqPrefix = DoubleArray(sourceEnv.size + 1)
        for (i in sourceEnv.indices) {
            val v = sourceEnv[i].toDouble()
            srcPrefix[i + 1] = srcPrefix[i] + v
            srcSqPrefix[i + 1] = srcSqPrefix[i] + v * v
        }
        val tgtPrefix = DoubleArray(targetEnv.size + 1)
        val tgtSqPrefix = DoubleArray(targetEnv.size + 1)
        for (i in targetEnv.indices) {
            val v = targetEnv[i].toDouble()
            tgtPrefix[i + 1] = tgtPrefix[i] + v
            tgtSqPrefix[i + 1] = tgtSqPrefix[i] + v * v
        }

        for (lag in minLag..maxLag) {
            val srcStart = max(0, -lag)
            val tgtStart = max(0, lag)
            val overlapLen = min(sourceEnv.size - srcStart, targetEnv.size - tgtStart)
            if (overlapLen < minOverlapFrames) continue

            val sumSrc = srcPrefix[srcStart + overlapLen] - srcPrefix[srcStart]
            val sumTgt = tgtPrefix[tgtStart + overlapLen] - tgtPrefix[tgtStart]
            val sumSrcSq = srcSqPrefix[srcStart + overlapLen] - srcSqPrefix[srcStart]
            val sumTgtSq = tgtSqPrefix[tgtStart + overlapLen] - tgtSqPrefix[tgtStart]

            val num = crossTerms[lag + sourceEnv.size - 1] - sumSrc * sumTgt / overlapLen
            val denSrc = sumSrcSq - sumSrc * sumSrc / overlapLen
            val denTgt = sumTgtSq - sumTgt * sumTgt / overlapLen

            val denom = sqrt(denSrc * denTgt)
            if (denom > 1e-6) {
                val r = num / denom
                if (r > bestCorrelation) {
                    bestCorrelation = r
                    bestLag = lag
                }
            }
        }

        return Correlation(bestLag, bestCorrelation, minLag, maxLag)
    }

    private fun crossTermsByLag(source: FloatArray, target: FloatArray): DoubleArray {
        val n = source.size
        val m = target.size
        var fftLen = 1
        while (fftLen < n + m) fftLen = fftLen shl 1

        val srcRe = DoubleArray(fftLen)
        val srcIm = DoubleArray(fftLen)
        for (i in 0 until n) srcRe[i] = source[i].toDouble()
        val tgtRe = DoubleArray(fftLen)
        val tgtIm = DoubleArray(fftLen)
        for (i in 0 until m) tgtRe[i] = target[i].toDouble()

        fft(srcRe, srcIm)
        fft(tgtRe, tgtIm)

        for (i in 0 until fftLen) {
            val a = srcRe[i]
            val b = -srcIm[i]
            val c = tgtRe[i]
            val d = tgtIm[i]
            srcRe[i] = a * c - b * d
            srcIm[i] = a * d + b * c
        }
        fft(srcRe, srcIm, inverse = true)

        val result = DoubleArray(n + m - 1)
        for (lag in -(n - 1) until m) {
            val bin = ((lag % fftLen) + fftLen) % fftLen
            result[lag + n - 1] = srcRe[bin]
        }
        return result
    }

    private fun fft(re: DoubleArray, im: DoubleArray, inverse: Boolean = false) {
        val n = re.size

        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j or bit
            if (i < j) {
                var t = re[i]; re[i] = re[j]; re[j] = t
                t = im[i]; im[i] = im[j]; im[j] = t
            }
        }

        var len = 2
        while (len <= n) {
            val angle = 2.0 * PI / len * (if (inverse) 1.0 else -1.0)
            val stepRe = cos(angle)
            val stepIm = sin(angle)
            for (start in 0 until n step len) {
                var twiddleRe = 1.0
                var twiddleIm = 0.0
                for (k in 0 until len / 2) {
                    val evenRe = re[start + k]
                    val evenIm = im[start + k]
                    val oddRe = re[start + k + len / 2] * twiddleRe - im[start + k + len / 2] * twiddleIm
                    val oddIm = re[start + k + len / 2] * twiddleIm + im[start + k + len / 2] * twiddleRe
                    re[start + k] = evenRe + oddRe
                    im[start + k] = evenIm + oddIm
                    re[start + k + len / 2] = evenRe - oddRe
                    im[start + k + len / 2] = evenIm - oddIm
                    val nextRe = twiddleRe * stepRe - twiddleIm * stepIm
                    twiddleIm = twiddleRe * stepIm + twiddleIm * stepRe
                    twiddleRe = nextRe
                }
            }
            len = len shl 1
        }

        if (inverse) {
            for (i in 0 until n) {
                re[i] /= n
                im[i] /= n
            }
        }
    }
}
