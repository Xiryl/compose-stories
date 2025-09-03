package it.chiarani.library.domain

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

const val DEFAULT_STORY_DURATION_MS = 5_000L

sealed class StorySource {
    data class ImageRes(@DrawableRes val resId: Int) : StorySource()
    data class ImageUrl(val url: String) : StorySource()
    data class VideoUri(val uri: Uri) : StorySource()
}

data class StorySpec(
    val id: String,
    val source: StorySource,
    val contentScale: ContentScale = ContentScale.Crop,
    val imageAlignment: Alignment = Center,
    val contentDescription: String? = null,
    val durationMs: Long = DEFAULT_STORY_DURATION_MS
)


data class StoryProgressBarStyle(
    val height: Dp = 4.dp,
    val gap: Dp = 4.dp,
    val cornerRadius: Dp = 2.dp,
    val trackColor: Color = Color.White.copy(alpha = 0.35f),
    val progressColor: Color = Color.White,
)

data class StoryTitleConfig(
    val storyTitleTextStyle: TextStyle? = null,
    val align: TextAlign = TextAlign.Start
)

data class StoryPlayerConfig(
    val showDebugUi: Boolean = false,
    val progressBarStyle: StoryProgressBarStyle = StoryProgressBarStyle(),
    val gestureZones: StoryGestureZones = defaultGestureZones,
    val titleConfig: StoryTitleConfig = StoryTitleConfig()
)

data class StoryPlayerState(
    val stories: List<StorySpec>,
    val currentIndex: Int = 0,
    val progress: Float = 0f,
    val playerConfig: StoryPlayerConfig
) {
    val currentStory: StorySpec?
        get() = stories.getOrNull(currentIndex)
}

val defaultGestureZones = StoryGestureZones(
    tapLeftFraction = 0.33f,
    tapRightFraction = 0.33f,
    longPressCenterWidth = 0.7f,
    longPressCenterHeight = 0.8f,
    swipeLeftEdge = EdgeFraction(value = 0.18f),
    swipeRightEdge = EdgeFraction(value = 0.18f),
    swipeDownEdge = EdgeFraction(value = 0.20f)
)

val mockStories = listOf(
    StorySpec(
        id = "1",
        source = StorySource.ImageUrl("")
    ),
    StorySpec(
        id = "2",
        source = StorySource.ImageUrl(""),
    )
)

internal var mockState =
    StoryPlayerState(stories = mockStories, playerConfig = StoryPlayerConfig(showDebugUi = false))
