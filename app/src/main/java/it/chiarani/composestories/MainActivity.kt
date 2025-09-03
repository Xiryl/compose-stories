package it.chiarani.composestories

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.chiarani.composestories.ui.theme.ComposeStoriesTheme
import it.chiarani.library.composables.StoryPlayer
import it.chiarani.library.domain.StoryGestureZones
import it.chiarani.library.domain.StoryPlayerConfig
import it.chiarani.library.domain.StoryPlayerState
import it.chiarani.library.domain.StoryProgressBarStyle
import it.chiarani.library.domain.StorySource
import it.chiarani.library.domain.StorySpec
import it.chiarani.library.domain.StoryTitleConfig
import it.chiarani.utils.rememberStoryTimer
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ComposeStoriesTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { inner ->
                    DemoHost(modifier = Modifier.padding(inner))
                }
            }
        }
    }
}

private enum class DemoScreen { Menu, Viewer }

@Composable
private fun DemoHost(modifier: Modifier = Modifier) {
    var screen by remember { mutableStateOf(DemoScreen.Menu) }
    var state by remember {
        mutableStateOf(
            StoryPlayerState(
                stories = emptyList(),
                playerConfig = StoryPlayerConfig()
            )
        )
    }

    val scheme = MaterialTheme.colorScheme
    val typo = MaterialTheme.typography

    val customConfig = remember(scheme, typo) {
        StoryPlayerConfig(
            showDebugUi = true,
            gestureZones = StoryGestureZones(),
            progressBarStyle = StoryProgressBarStyle(
                trackColor = scheme.onSurface.copy(alpha = 0.25f),
                progressColor = scheme.tertiary,
                height = 6.dp,
                gap = 6.dp,
                cornerRadius = 3.dp
            ),
            titleConfig = StoryTitleConfig(
                storyTitleTextStyle = typo.titleMedium,
                align = TextAlign.Center
            )
        )
    }

    when (screen) {
        DemoScreen.Menu -> DemoMenu(
            modifier = modifier.fillMaxSize(),
            onOpenImages = { debug ->
                state = StoryPlayerState(
                    stories = demoImageStories,
                    playerConfig = StoryPlayerConfig(showDebugUi = debug)
                )
                screen = DemoScreen.Viewer
            },
            onOpenImagesCustom = { debug ->
                state = StoryPlayerState(
                    stories = demoImageStories,
                    playerConfig = customConfig.copy(showDebugUi = debug)
                )
                screen = DemoScreen.Viewer
            },
            onOpenVideo = { debug ->
                state = StoryPlayerState(
                    stories = demoVideoStories,
                    playerConfig = StoryPlayerConfig(showDebugUi = debug)
                )
                screen = DemoScreen.Viewer
            }
        )

        DemoScreen.Viewer -> DemoViewer(
            state = state,
            onStateChange = { state = it },
            onDismiss = { screen = DemoScreen.Menu }
        )
    }
}


@Composable
private fun DemoMenu(
    modifier: Modifier = Modifier,
    onOpenImages: (debug: Boolean) -> Unit,
    onOpenImagesCustom: (debug: Boolean) -> Unit,
    onOpenVideo: (debug: Boolean) -> Unit
) {
    var enableDebug by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Compose Stories – Demo", style = MaterialTheme.typography.headlineSmall)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(checked = enableDebug, onCheckedChange = { enableDebug = it })
            Text("Enable debug overlay")
        }

        Button(onClick = { onOpenImages(enableDebug) }, modifier = Modifier.fillMaxWidth()) {
            Text("Preview Images")
        }
        Button(onClick = { onOpenImagesCustom(enableDebug) }, modifier = Modifier.fillMaxWidth()) {
            Text("Preview Images (custom style)")
        }
        Button(onClick = { onOpenVideo(enableDebug) }, modifier = Modifier.fillMaxWidth()) {
            Text("Preview Video")
        }
    }
}


@Composable
private fun DemoViewer(
    state: StoryPlayerState,
    onStateChange: (StoryPlayerState) -> Unit,
    onDismiss: () -> Unit
) {
    var isPaused by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    val latestState by rememberUpdatedState(state)

    val current = state.currentStory
    val durationMs = current?.durationMs
    val isVideo = current?.source is StorySource.VideoUri

    rememberStoryTimer(
        currentIndex = state.currentIndex,
        durationMs = if (isVideo) null else durationMs,
        isPaused = isPaused,
        onProgress = { p ->
            val s = latestState
            onStateChange(s.copy(progress = p))
        },
        onCompleted = {
            val s = latestState
            advanceOrDismiss(s, onStateChange, onDismiss)
            Toast.makeText(ctx, "Auto → Next/Close", Toast.LENGTH_SHORT).show()
        }
    )

    StoryPlayer(
        state = state,
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        title = current?.id ?: "",

        onPrev = {
            val s = latestState
            val prev = (s.currentIndex - 1).coerceAtLeast(0)
            onStateChange(s.copy(currentIndex = prev, progress = 0f))
            Toast.makeText(ctx, "Prev", Toast.LENGTH_SHORT).show()
        },
        onNext = {
            val s = latestState
            advanceOrDismiss(s, onStateChange, onDismiss)
            Toast.makeText(ctx, "Next/Close", Toast.LENGTH_SHORT).show()
        },
        onPauseChanged = { paused ->
            isPaused = paused
            Toast.makeText(ctx, if (paused) "Paused" else "Resumed", Toast.LENGTH_SHORT).show()
        },
        onDismiss = {
            Toast.makeText(ctx, "Dismiss", Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    )
}

private fun advanceOrDismiss(
    state: StoryPlayerState,
    onStateChange: (StoryPlayerState) -> Unit,
    onDismiss: () -> Unit
) {
    val last = state.stories.lastIndex
    if (state.currentIndex < last) {
        onStateChange(state.copy(currentIndex = state.currentIndex + 1, progress = 0f))
    } else {
        onDismiss()
    }
}


private val demoImageStories = listOf(
    StorySpec(
        id = "Sunset",
        source = StorySource.ImageUrl("https://images.unsplash.com/photo-1501973801540-537f08ccae7b?q=80&w=1200&auto=format&fit=crop"),
        durationMs = 5000
    ),
    StorySpec(
        id = "Forest",
        source = StorySource.ImageUrl("https://images.unsplash.com/photo-1469474968028-56623f02e42e?q=80&w=1200&auto=format&fit=crop"),
        durationMs = 6000
    ),
    StorySpec(
        id = "City",
        source = StorySource.ImageUrl("https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=1200&auto=format&fit=crop"),
        durationMs = 5000
    )
)

private val demoVideoStories = listOf(
    StorySpec(
        id = "Intro video",
        source = StorySource.VideoUri(
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4".toUri()
        ),
    )
)