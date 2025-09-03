package it.chiarani.library.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.chiarani.library.domain.StoryPlayerState
import it.chiarani.library.domain.StorySource
import it.chiarani.library.domain.mockState

@Composable
fun StoryPlayer(
    modifier: Modifier = Modifier,
    state: StoryPlayerState,
    title: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPauseChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    storyTitle: (@Composable () -> Unit)? = null
) {
    val current = state.currentStory ?: return
    var videoSetPaused by remember { mutableStateOf<(Boolean) -> Unit>({}) }
    var videoUiProgress by remember { mutableStateOf<Float?>(null) }

    Box(modifier) {
        StoryBackgroundWrapper(
            modifier = Modifier.matchParentSize(),
            background = {
                when (val s = current.source) {
                    is StorySource.ImageRes -> StoryImage(
                        modifier = Modifier.fillMaxSize(),
                        source = s.resId,
                        contentScale = current.contentScale,
                        imageAlignment = current.imageAlignment,
                        contentDescription = current.contentDescription
                    )

                    is StorySource.ImageUrl -> StoryImageUrl(
                        modifier = Modifier.fillMaxSize(),
                        sourceUrl = s.url,
                        contentScale = current.contentScale,
                        imageAlignment = current.imageAlignment,
                        contentDescription = current.contentDescription
                    )

                    is StorySource.VideoUri -> StoryVideo(
                        modifier = Modifier.fillMaxSize(),
                        uri = s.uri,
                        onProgress = { p -> videoUiProgress = p },
                        onCompleted = onNext,
                        onSetPausedBinder = { setter -> videoSetPaused = setter }
                    )
                }
            }
        )

        Column {
            StoryProgressBar(
                totalSegments = state.stories.size,
                currentIndex = state.currentIndex,
                progress = videoUiProgress ?: state.progress,
                storyProgressBarStyle = state.playerConfig.progressBarStyle,
                modifier = Modifier
                    .padding(top = 12.dp, start = 8.dp, end = 8.dp)
            )

            storyTitle?.invoke() ?: StoryTitle(title, state)
        }


        StoryGesturesLayer(
            modifier = Modifier.fillMaxSize(),
            onTapLeft = onPrev,
            onTapRight = onNext,
            onLongPress = { paused ->
                videoSetPaused(paused)
                onPauseChanged(paused)
            },
            onSwipeDown = onDismiss,
            showDebugOverlay = state.playerConfig.showDebugUi,
            storyGestureZones = state.playerConfig.gestureZones
        )
    }
}

@Composable
private fun StoryTitle(title: String, state: StoryPlayerState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = state.playerConfig.titleConfig.storyTitleTextStyle
                ?: MaterialTheme.typography.titleSmall.copy(
                    color = Color.White
                ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            textAlign = state.playerConfig.titleConfig.align
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(
    name = "StoryPlayer - mockState image",
    widthDp = 360,
    heightDp = 640,
    showBackground = true
)
@Composable
private fun PreviewStoryPlayer_Image() {
    StoryPlayer(
        state = mockState.copy(currentIndex = 0, progress = 0.5f),
        modifier = Modifier.fillMaxSize(),
        onNext = {}, onPrev = {}, onDismiss = {}, onPauseChanged = {},
        title = "Story 1"
    )
}

@Suppress("UnusedPrivateMember")
@Preview(name = "StoryPlayer - mockState image1")
@Composable
private fun PreviewStoryPlayer_Image1() {
    StoryPlayer(
        state = mockState.copy(currentIndex = 1, progress = 0.5f),
        onNext = {},
        onPrev = {},
        onDismiss = {},
        onPauseChanged =
            {},
        title = "Story 1"
    )
}
