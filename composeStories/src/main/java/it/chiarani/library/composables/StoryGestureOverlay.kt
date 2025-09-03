package it.chiarani.library.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.chiarani.library.domain.StoryGestureZones
import it.chiarani.library.domain.defaultGestureZones
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val LONG_PRESS_MS = 220L
private const val DEFAULT_DEBOUNCE_MS = 160L
private val DEFAULT_SWIPE_THRESHOLD_DP: Dp = 96.dp

private val OVERLAY_SHAPE = RoundedCornerShape(6.dp)
private val OVERLAY_PADDING = 6.dp
private val EDGE_PADDING = 4.dp
private const val EDGE_ALPHA = 0.5f

@Suppress("MagicNumber")
private val COLOR_TAP_LEFT = Color(0xFF4CAF50)

@Suppress("MagicNumber")
private val COLOR_TAP_RIGHT = Color(0xFF2196F3)

@Suppress("MagicNumber")
private val COLOR_LONG_PRESS = Color(0xFFFFC107)

@Suppress("MagicNumber")
private val COLOR_SWIPE_LEFT = Color(0xFFE91E63)

@Suppress("MagicNumber")
private val COLOR_SWIPE_RIGHT = Color(0xFF9C27B0)

@Suppress("MagicNumber")
private val COLOR_SWIPE_DOWN = Color(0xFFFF5722)

/**
 * Draws an invisible gesture layer and dispatches taps/long-press/swipes to the caller.
 *
 * The composable is intentionally UI-agnostic: it does not paint anything unless
 * [showDebugOverlay] is true. In that case an overlay illustrates the active areas.
 *
 * Behavior:
 * - Tap on left/right zones -> previous/next callbacks.
 * - Long-press in center zone -> onLongPress(true) until release, then onLongPress(false).
 * - Horizontal drag -> swipeLeft/swipeRight on threshold.
 * - Vertical drag -> swipeDown on threshold.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun StoryGesturesLayer(
    modifier: Modifier = Modifier,
    storyGestureZones: StoryGestureZones = StoryGestureZones(),
    onTapLeft: () -> Unit,
    onTapRight: () -> Unit,
    onLongPress: (paused: Boolean) -> Unit,
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
    onSwipeDown: () -> Unit = {},
    inputDebounceMs: Long = DEFAULT_DEBOUNCE_MS,
    swipeThresholdDp: Dp = DEFAULT_SWIPE_THRESHOLD_DP,
    showDebugOverlay: Boolean = LocalInspectionMode.current
) {
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { swipeThresholdDp.toPx() }

    var locked by remember { mutableStateOf(false) }
    var accY by remember { mutableFloatStateOf(0f) }
    var accX by remember { mutableFloatStateOf(0f) }

    val scope = rememberCoroutineScope()
    var debounceJob by remember { mutableStateOf<Job?>(null) }

    Box(
        modifier = modifier
            .pointerInput(storyGestureZones) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val downChange = event.changes.firstOrNull { it.pressed }
                        if (downChange == null) {
                            // nothing
                        } else {
                            val startPos = downChange.position
                            val widthPx = size.width.toFloat()
                            val heightPx = size.height.toFloat()

                            val rects = computeRects(widthPx, heightPx, storyGestureZones)

                            var longPressed = false
                            val longPressJob = scope.launch {
                                delay(LONG_PRESS_MS)
                                if (downChange.pressed && rects.center.contains(startPos)) {
                                    longPressed = true
                                    onLongPress(true)
                                }
                            }

                            var released = false
                            while (!released) {
                                val ev = awaitPointerEvent()
                                val change = ev.changes.firstOrNull()
                                released = (change == null || !change.pressed)
                            }

                            longPressJob.cancel()

                            if (longPressed) {
                                onLongPress(false)
                            } else if (!locked) {
                                handleTapWithDebounce(
                                    startPosX = startPos.x,
                                    startPosY = startPos.y,
                                    rects = rects,
                                    onTapLeft = onTapLeft,
                                    onTapRight = onTapRight,
                                    lock = { locked = true },
                                    unlock = {
                                        debounceJob?.cancel()
                                        debounceJob = scope.launch {
                                            delay(inputDebounceMs)
                                            locked = false
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { deltaX -> accX += deltaX },
                onDragStopped = {
                    when {
                        accX <= -swipeThresholdPx -> onSwipeLeft()
                        accX >= swipeThresholdPx -> onSwipeRight()
                    }
                    accX = 0f
                }
            )
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { deltaY -> accY += deltaY },
                onDragStopped = {
                    if (accY >= swipeThresholdPx) onSwipeDown()
                    accY = 0f
                }
            )
    ) {
        if (showDebugOverlay) {
            DebugZonesOverlay(zones = storyGestureZones)
        }
    }
}

/** Geometry holder for precomputed active rectangles. */
private data class GestureRects(
    val leftTap: Rect,
    val rightTap: Rect,
    val center: Rect
)

/** Build rectangles for left/right tap and central long-press based on current size and zones. */
private fun computeRects(
    widthPx: Float,
    heightPx: Float,
    zones: StoryGestureZones
): GestureRects {
    val left = Rect(0f, 0f, widthPx * zones.tapLeftFraction, heightPx)
    val right = Rect(widthPx * (1f - zones.tapRightFraction), 0f, widthPx, heightPx)
    val centerW = widthPx * zones.longPressCenterWidth
    val centerH = heightPx * zones.longPressCenterHeight
    val center = Rect(
        (widthPx - centerW) / 2f,
        (heightPx - centerH) / 2f,
        (widthPx + centerW) / 2f,
        (heightPx + centerH) / 2f
    )
    return GestureRects(leftTap = left, rightTap = right, center = center)
}

/**
 * Handles a left/right tap with a simple debounce strategy:
 * - Locks immediately, calls the appropriate callback,
 * - Schedules unlock after [unlock] is invoked (external job delays the unlock).
 */
private fun handleTapWithDebounce(
    startPosX: Float,
    startPosY: Float,
    rects: GestureRects,
    onTapLeft: () -> Unit,
    onTapRight: () -> Unit,
    lock: () -> Unit,
    unlock: () -> Unit
) {
    val p = androidx.compose.ui.geometry.Offset(startPosX, startPosY)
    when {
        rects.leftTap.contains(p) -> {
            lock()
            onTapLeft()
            unlock()
        }

        rects.rightTap.contains(p) -> {
            lock()
            onTapRight()
            unlock()
        }

        else -> {
            // tap in center does nothing
        }
    }
}


/**
 * Visual overlay to illustrate gesture zones while debugging.
 * Split into small composables to reduce method length and complexity.
 */
@Suppress("FunctionNaming") // Compose functions are PascalCase by convention
@Composable
private fun BoxScope.DebugZonesOverlay(zones: StoryGestureZones) {
    val labelStyle = MaterialTheme.typography.labelSmall
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .matchParentSize()
            .align(Alignment.Center)
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        fun fracW(f: Float) = (widthPx * f).coerceAtLeast(0f)
        fun fracH(f: Float) = (heightPx * f).coerceAtLeast(0f)

        val leftW = with(density) { fracW(zones.tapLeftFraction).toDp() }
        val rightW = with(density) { fracW(zones.tapRightFraction).toDp() }
        val centerW = with(density) { fracW(zones.longPressCenterWidth).toDp() }
        val centerH = with(density) { fracH(zones.longPressCenterHeight).toDp() }
        val swipeLeftW = with(density) { fracW(zones.swipeLeftEdge.value).toDp() }
        val swipeRightW = with(density) { fracW(zones.swipeRightEdge.value).toDp() }
        val swipeDownH = with(density) { fracH(zones.swipeDownEdge.value).toDp() }

        overlayTapLeft(leftW, labelStyle)
        overlayTapRight(rightW, labelStyle)
        overlayLongPress(centerW, centerH, labelStyle)
        overlaySwipeLeft(swipeLeftW, labelStyle)
        overlaySwipeRight(swipeRightW, labelStyle)
        overlaySwipeDown(swipeDownH, labelStyle)
    }
}

@Composable
private fun BoxScope.overlayTapLeft(width: Dp, textStyle: androidx.compose.ui.text.TextStyle) {
    Box(
        Modifier
            .width(width)
            .fillMaxHeight()
            .align(Alignment.CenterStart)
            .background(COLOR_TAP_LEFT.copy(alpha = 0.25f), OVERLAY_SHAPE)
            .border(1.dp, COLOR_TAP_LEFT, OVERLAY_SHAPE)
            .padding(OVERLAY_PADDING)
    ) {
        Text("TAP LEFT", color = Color.White, style = textStyle, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BoxScope.overlayTapRight(width: Dp, textStyle: androidx.compose.ui.text.TextStyle) {
    Box(
        Modifier
            .width(width)
            .fillMaxHeight()
            .align(Alignment.CenterEnd)
            .background(COLOR_TAP_RIGHT.copy(alpha = 0.25f), OVERLAY_SHAPE)
            .border(1.dp, COLOR_TAP_RIGHT, OVERLAY_SHAPE)
            .padding(OVERLAY_PADDING)
    ) {
        Text(
            "TAP RIGHT",
            color = Color.White,
            style = textStyle,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun BoxScope.overlayLongPress(
    width: Dp,
    height: Dp,
    textStyle: androidx.compose.ui.text.TextStyle
) {
    Box(
        Modifier
            .width(width)
            .height(height)
            .align(Alignment.Center)
            .background(COLOR_LONG_PRESS.copy(alpha = 0.22f), OVERLAY_SHAPE)
            .border(1.dp, COLOR_LONG_PRESS, OVERLAY_SHAPE)
            .padding(OVERLAY_PADDING)
    ) {
        Text("LONG PRESS", color = Color.White, style = textStyle, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BoxScope.overlaySwipeLeft(width: Dp, textStyle: androidx.compose.ui.text.TextStyle) {
    Box(
        Modifier
            .width(width)
            .fillMaxHeight()
            .align(Alignment.CenterStart)
            .alpha(EDGE_ALPHA)
            .background(COLOR_SWIPE_LEFT.copy(alpha = 0.18f))
            .padding(EDGE_PADDING)
    ) {
        Text("SWIPE LEFT EDGE", color = Color.White, style = textStyle)
    }
}

@Composable
private fun BoxScope.overlaySwipeRight(width: Dp, textStyle: androidx.compose.ui.text.TextStyle) {
    Box(
        Modifier
            .width(width)
            .fillMaxHeight()
            .align(Alignment.CenterEnd)
            .alpha(EDGE_ALPHA)
            .background(COLOR_SWIPE_RIGHT.copy(alpha = 0.18f))
            .padding(EDGE_PADDING)
    ) {
        Text(
            "SWIPE RIGHT EDGE",
            color = Color.White,
            style = textStyle,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun BoxScope.overlaySwipeDown(height: Dp, textStyle: androidx.compose.ui.text.TextStyle) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .align(Alignment.TopCenter)
            .alpha(EDGE_ALPHA)
            .background(COLOR_SWIPE_DOWN.copy(alpha = 0.18f))
            .padding(EDGE_PADDING)
    ) {
        Text("SWIPE DOWN EDGE", color = Color.White, style = textStyle)
    }
}


@Suppress("FunctionNaming", "UnusedPrivateMember")
@Preview
@Composable
private fun Preview_storyGesturesLayer_Debug() {
    Box(Modifier.fillMaxSize()) {
        StoryGesturesLayer(
            modifier = Modifier.fillMaxSize(),
            storyGestureZones = defaultGestureZones,
            onTapLeft = {},
            onTapRight = {},
            onLongPress = {},
            onSwipeLeft = {},
            onSwipeRight = {},
            onSwipeDown = {},
            showDebugOverlay = true
        )
    }
}
