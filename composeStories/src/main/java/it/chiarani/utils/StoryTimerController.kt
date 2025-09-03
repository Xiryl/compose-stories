package it.chiarani.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.nanoseconds

private const val DEFAULT_FRAME_MS = 16L
private const val PAUSE_POLL_INTERVAL_MS = 50L

/**
 * A composable timer designed to drive story-like progress indicators (e.g., Instagram stories).
 *
 * Behavior:
 * - Emits progress updates in the [0f..1f] range via [onProgress].
 * - Invokes [onCompleted] when [durationMs] expires.
 * - Automatically restarts when [currentIndex] changes or when [StoryTimerController.reset] is called.
 * - Pauses the progression when [isPaused] is true (progress stays visually frozen).
 *
 * Usage:
 * - Use this for fixed-duration content such as images.
 * - For video content, ignore this timer and sync with the player’s progress callbacks instead.
 *
 * @param currentIndex Index of the currently displayed story. Changing this restarts the timer.
 * @param durationMs Total duration for the current story in milliseconds. If `null`, the timer is disabled.
 * @param isPaused If true, the timer halts progression until resumed.
 * @param onProgress Called with the current progress [0f..1f] at approximately every [frameMs] interval.
 * @param onCompleted Called exactly once when progress reaches 1f (story duration completed).
 * @param frameMs Frame update interval in milliseconds. Defaults to 16ms (~60fps).
 *
 * @return [StoryTimerController] allowing external reset of the timer.
 */
@Composable
fun rememberStoryTimer(
    currentIndex: Int,
    durationMs: Long?,
    isPaused: Boolean,
    onProgress: (Float) -> Unit,
    onCompleted: () -> Unit,
    frameMs: Long = DEFAULT_FRAME_MS
): StoryTimerController {
    // External trigger to restart the timer.
    var resetKey by remember { mutableIntStateOf(0) }

    // Keep the latest pause value across recompositions inside the coroutine.
    val pausedState by rememberUpdatedState(isPaused)

    LaunchedEffect(currentIndex, resetKey, durationMs) {
        onProgress(0f)

        if (durationMs == null) return@LaunchedEffect
        val totalDurationMs = durationMs.coerceAtLeast(1L)

        // Accumulated *active* time (does not include pauses).
        var activeElapsedMs = 0L

        // We increment from this reference when not paused.
        var lastTickNs = System.nanoTime()

        var completed = false
        while (isActive && !completed) {
            val nowNs = System.nanoTime()

            if (!pausedState) {
                // Only accumulate when not paused.
                val deltaMs = (nowNs - lastTickNs)
                    .nanoseconds
                    .inWholeMilliseconds
                    .coerceAtLeast(0L)

                activeElapsedMs += deltaMs

                val progress = (activeElapsedMs.toFloat() / totalDurationMs)
                    .coerceIn(0f, 1f)
                onProgress(progress)

                if (progress >= 1f) {
                    onCompleted()
                    completed = true
                } else {
                    // Normal cadence while running.
                    delay(frameMs)
                }
            } else {
                // While paused, keep progress frozen and just poll occasionally.
                delay(PAUSE_POLL_INTERVAL_MS)
            }

            // Always update the reference tick to "now".
            lastTickNs = nowNs
        }
    }

    return remember {
        StoryTimerController(
            resetCallback = { resetKey++ }
        )
    }
}

/**
 * Controller object returned by [rememberStoryTimer].
 *
 * Provides external control methods such as [reset].
 */
class StoryTimerController internal constructor(
    private val resetCallback: () -> Unit
) {
    /** Restarts the timer from 0. */
    fun reset() = resetCallback()
}
