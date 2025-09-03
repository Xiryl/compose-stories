package it.chiarani.library.composables

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import it.chiarani.utils.VideoTextureView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val UPDATE_MS = 200

@Suppress("CyclomaticComplexMethod")
@Composable
internal fun StoryVideo(
    modifier: Modifier,
    uri: Uri?,
    onSetPausedBinder: ((Boolean) -> Unit) -> Unit,
    onProgress: (Float) -> Unit = {},
    onCompleted: () -> Unit = {}
) {
    var viewRef by remember { mutableStateOf<VideoTextureView?>(null) }
    val onProgressState by rememberUpdatedState(onProgress)
    val onCompletedState by rememberUpdatedState(onCompleted)

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            VideoTextureView(ctx).also { v ->
                viewRef = v
                onSetPausedBinder { paused -> if (paused) v.pause() else v.play() }
                uri?.let { v.bindUrl(it, playWhenReady = true) }
            }
        },
        update = { v ->
            if (uri != null) v.bindUrl(uri, playWhenReady = true)
        },
        onRelease = { v ->
            v.release()
            if (viewRef === v) viewRef = null
        }
    )

    LaunchedEffect(viewRef) {
        val v = viewRef ?: return@LaunchedEffect
        var startedPlayback = false
        var lastPos = -1

        while (isActive && v === viewRef) {
            val dur = v.durationMs() ?: 0
            val pos = v.currentPositionMs() ?: 0

            if (!startedPlayback && pos > 0) {
                if (lastPos in 0..<pos) startedPlayback = true
            }
            lastPos = pos

            if (startedPlayback && dur > 0) {
                onProgressState((pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f))
                if (dur - pos <= UPDATE_MS) {
                    onCompletedState()
                    break
                }
            }

            delay(timeMillis = 100)
        }
    }
}
