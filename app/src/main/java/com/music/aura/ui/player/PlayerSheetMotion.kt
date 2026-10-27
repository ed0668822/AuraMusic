package com.music.aura.ui.player

import android.view.Window
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.WindowInfo
import com.music.aura.ui.components.MiniPlayerPull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

/**
 * The spring every open and close runs on: critically damped, so it never
 * bounces, with about 0.45s of response.
 */
private const val SLIDE_STIFFNESS = 195f

/** Close enough to stop, in pixels. */
private const val SLIDE_SETTLED_PX = 0.5f

/** How old the sheet's last movement can be and still count as its speed. */
private const val SHEET_VELOCITY_FRESH_MS = 80f

/** How long a sheet waits to be laid out before an open starts regardless. */
private const val LAYOUT_WAIT_MS = 300L

/**
 * How long after the sheet appears its window gets its exit animation back.
 */
private const val WINDOW_ENTER_MS = 600L

/**
 * Where the player sheet stands while the app moves it, rather than M3.
 *
 * M3's sheet can only be told to show or hide, on its own curve, and has no
 * way to be told where to stand. That covers neither the pull up from the
 * mini player, which has to follow the finger, nor an open and close slow
 * enough to watch the artwork travel. So for all three the sheet is left open
 * and [held] says where its content really is: how far below fully open, in
 * pixels. The content makes up the difference itself ([contentOffsetPx]), and
 * the [PlayerDock] reads [position], so the artwork flies the same way however
 * the player is being moved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Stable
class PlayerSheetMotion(
    private val scope: CoroutineScope,
    private val dock: PlayerDock,
    private val shown: () -> Boolean,
    private val setShown: (Boolean) -> Unit,
) : MiniPlayerPull {
    /** NaN while the sheet stands where M3 put it. */
    var held by mutableFloatStateOf(Float.NaN)
        private set

    /** Whether the app, not the sheet, is placing the player. */
    val holding: Boolean get() = !held.isNaN()

    /** The sheet, while it is up. Set by the host. */
    var sheet: SheetState? = null

    /** The sheet's window, while it is up. */
    private var window: Window? = null
    private var windowAnimations = 0
    private var windowRestore: Job? = null

    private var pullStartNanos = 0L
    private var pullStartAt = 0f

    private var sheetSampleAt = Float.NaN
    private var sheetSampleNanos = 0L
    private var sheetVelocity = 0f

    /** Set by the host once the sheet has been laid out at all. */
    var laidOut by mutableStateOf(false)

    /**
     * The window, for its height before the sheet has ever been measured.
     */
    var windowInfo: WindowInfo? = null

    /** How fast a release has to be, in pixels a second, to be a flick. */
    var flingVelocity = 0f

    private var settle: Job? = null

    private var closing = false

    private fun travel(): Float =
        dock.sheetTravel.takeIf { it > 0f } ?: (windowInfo?.containerSize?.height ?: 0).toFloat()

    private fun sheetOffset(): Float = try {
        sheet?.requireOffset() ?: Float.NaN
    } catch (_: IllegalStateException) {
        Float.NaN
    }

    /** How far below fully open the player is. */
    fun position(): Float = if (holding) held else sheetOffset()

    /**
     * What the sheet's content adds to the sheet's own offset to stand at
     * [held]. In whole pixels on both sides.
     */
    fun contentOffsetPx(): Int {
        val target = held
        val sheetAt = sheetOffset().takeUnless { it.isNaN() } ?: 0f
        if (target.isNaN()) {
            sampleSheet(sheetAt)
            return 0
        }
        return target.toInt() - sheetAt.toInt()
    }

    private fun sampleSheet(at: Float) {
        val now = System.nanoTime()
        if (!sheetSampleAt.isNaN()) {
            val seconds = (now - sheetSampleNanos) / 1e9f
            if (seconds > 0f) {
                val instant = (at - sheetSampleAt) / seconds
                sheetVelocity = sheetVelocity * 0.4f + instant * 0.6f
            }
        }
        sheetSampleAt = at
        sheetSampleNanos = now
    }

    /** The sheet's speed, if it was still moving a moment ago. */
    private fun recentSheetVelocity(): Float =
        if ((System.nanoTime() - sheetSampleNanos) / 1e6f < SHEET_VELOCITY_FRESH_MS) sheetVelocity else 0f

    /** A tap on the mini player. */
    fun open() {
        if (shown()) return
        settle?.cancel()
        closing = false
        val travel = travel()
        held = travel
        setShown(true)
        settle = scope.launch {
            withTimeoutOrNull(LAYOUT_WAIT_MS) { snapshotFlow { laidOut }.first { it } }
            slide(from = travel, to = 0f)
            held = Float.NaN
        }
    }

    /**
     * The sheet's window, from the host as it appears.
     */
    fun attachWindow(window: Window?) {
        this.window = window ?: return
        windowAnimations = window.attributes.windowAnimations
        window.setWindowAnimations(0)
        windowRestore?.cancel()
        windowRestore = scope.launch {
            delay(WINDOW_ENTER_MS)
            window.setWindowAnimations(windowAnimations)
        }
    }

    private fun quietWindow() {
        windowRestore?.cancel()
        window?.setWindowAnimations(0)
    }

    /** From the sheet: whatever asked for it to hide, this is how it goes. */
    fun close() {
        if (!shown() || closing) return
        closing = true
        quietWindow()
        settle?.cancel()
        val from = position().takeUnless { it.isNaN() } ?: run {
            dismissQuietly()
            return
        }
        val velocity = if (holding) 0f else recentSheetVelocity()
        held = from
        settle = scope.launch {
            slide(from = from, to = travel(), velocity = velocity.coerceAtLeast(0f))
            dismissQuietly()
        }
    }

    override fun drag(delta: Float) {
        if (!holding) {
            if (delta >= 0f || shown()) return
            settle?.cancel()
            closing = false
            held = travel()
            pullStartAt = held
            pullStartNanos = System.nanoTime()
            setShown(true)
        }
        settle?.cancel()
        held = (held + delta).coerceIn(0f, travel())
    }

    override fun release(velocity: Float) {
        if (!holding) return
        val travel = travel()
        val seconds = (System.nanoTime() - pullStartNanos) / 1e9f
        val average = if (seconds > 0f) (held - pullStartAt) / seconds else 0f
        val speed = if (abs(average) > abs(velocity)) average else velocity
        val flick = abs(speed) >= flingVelocity
        val open = if (flick) speed < 0f else held < travel / 2f
        if (!open) {
            closing = true
            quietWindow()
        }
        val from = held
        settle = scope.launch {
            slide(from = from, to = if (open) 0f else travel, velocity = speed)
            if (open) held = Float.NaN else dismissQuietly()
        }
    }

    /** From the host, once the sheet has left composition. */
    fun onSheetGone() {
        closing = false
        sheetSampleAt = Float.NaN
        sheetVelocity = 0f
        settle?.cancel()
        windowRestore?.cancel()
        held = Float.NaN
        laidOut = false
        sheet = null
        window = null
    }

    private suspend fun slide(from: Float, to: Float, velocity: Float = 0f) {
        val travel = travel()
        animate(
            initialValue = from,
            targetValue = to,
            initialVelocity = velocity,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = SLIDE_STIFFNESS,
                visibilityThreshold = SLIDE_SETTLED_PX,
            ),
        ) { value, _ ->
            held = value.coerceIn(0f, travel)
        }
    }

    private fun dismissQuietly() {
        quietWindow()
        setShown(false)
    }
}
