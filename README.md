<p align="center">
  <h1 align="center">🤖 📖 Compose Stories</h1>
  <p align="center">
    A lightweight, customizable <b>Instagram-like Stories player</b> for Jetpack Compose.<br/>
    Supports images, videos, progress bars, gestures, titles, and custom styling.
  </p>

  <p align="center">
    <a href="https://github.com/Xiryl/compose-stories/actions/workflows/lint.yml">
      <img src="https://github.com/Xiryl/compose-stories/actions/workflows/lint.yml/badge.svg" alt="Detekt Status"/>
    </a>
    <a href="https://central.sonatype.com/">
      <img src="https://img.shields.io/maven-central/v/com.github.xiryl/compose-stories.svg?label=Maven%20Central" alt="Maven Central"/>
    </a>
    <a href="https://www.apache.org/licenses/LICENSE-2.0">
      <img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License"/>
    </a>
  </p>

  <p align="center">
    <img src="https://github.com/Xiryl/compose-stories/blob/main/ot/preview.gif?raw=true" width="240" alt="Compose Stories demo 1"/>
    <img src="https://github.com/Xiryl/compose-stories/blob/main/ot/demo1.png?raw=true" width="240" alt="Compose Stories demo 2"/>
    <img src="https://github.com/Xiryl/compose-stories/blob/main/ot/demo2.png?raw=true" width="240" alt="Compose Stories demo 3"/>
  </p>
</p>

---

## 🧭 Table of Contents

- [✨ Features](#-features)
- [📦 Installation](#-installation)
- [🚀 Quick Start](#-quick-start)
- [⏱️ Progress Control (Timer)](#️-progress-control-with-rememberstorytimer)
- [🎨 Customization Reference](#-customization-reference)
- [📖 Full Customization Example](#-full-customization-example)
- [📲 Demo App](#-demo-app)
- [🤝 Contributing](#-contributing)
- [📜 License](#-license)
- [⭐ Support](#-support)

---

## ✨ Features

- 🖼️ Image and video support (resources, URLs, URIs)
- 🎬 Built-in video playback (`MediaPlayer` + `TextureView`)
- ⏱️ Automatic progress bar with customizable style
- 🎨 Theming support (colors, typography, spacing, corner radius, alignment)
- 👆 Gesture layer (tap left/right, swipe down to dismiss, long press to pause)
- 🔧 Easy to integrate, lightweight, minimal deps
- 🛠️ Debug overlay to visualize gesture zones

---

## 📦 Installation

Add **Maven Central**:

```kotlin
repositories {
    mavenCentral()
}
```

Then add the dependency:

```kotlin
dependencies {
    implementation("com.github.xiryl:compose-stories:<version>")
}
```

---

## 🚀 Quick Start

```kotlin
val state = remember {
    StoryPlayerState(
        stories = listOf(
            StorySpec(
                id = "Photo 1",
                source = StorySource.ImageUrl("https://some-url.com/image.png"),
                durationMs = 5000
            ),
            StorySpec(
                id = "Intro Video",
                source = StorySource.VideoUri("https://some-url/coolvideo.mp4".toUri())
            )
        ),
        playerConfig = StoryPlayerConfig()
    )
}

StoryPlayer(
    state = state,
    title = "Demo Story",
    onPrev = { /* go back */ },
    onNext = { /* go forward */ },
    onPauseChanged = { /* handle pause */ },
    onDismiss = { /* close viewer */ }
)
```

---

## ⏱️ Progress Control with `rememberStoryTimer`

`rememberStoryTimer` is built-in to manage story progress. For **images**, it advances the progress bar automatically; for **videos**, progress is driven by playback so the timer is disabled.

<details>
<summary><b>Show timer usage</b></summary>

```kotlin
val current = state.currentStory
val isVideo = current?.source is StorySource.VideoUri
val durationMs = current?.durationMs ?: DEFAULT_STORY_DURATION_MS

rememberStoryTimer(
    currentIndex = state.currentIndex,
    durationMs = if (isVideo) null else durationMs, // disable for videos
    isPaused = isPaused,
    onProgress = { p -> state = state.copy(progress = p) },
    onCompleted = {
        val next = (state.currentIndex + 1).coerceAtMost(state.stories.lastIndex)
        state = state.copy(currentIndex = next, progress = 0f)
    }
)
```
</details>

---

## 🎨 Customization Reference

| Element | Type | What it controls |
|---|---|---|
| `StoryProgressBarStyle` | data class | Height, gap, corner radius, track color, progress color |
| `StoryTitleConfig` | data class | Text style (`TextStyle?`) and alignment (`TextAlign`) |
| `StoryGestureZones` | data class | Tap fractions (left/right), long-press center area, swipe edges |
| `StoryPlayerConfig` | data class | Combines debug overlay, progress bar style, title style, gesture zones |
| `StorySpec` | data class | Per-story `id`, `source`, `durationMs`, plus `contentScale`, `imageAlignment`, `contentDescription` |

### Gesture Customization
<details>
<summary><b>Show gesture customization</b></summary>

```kotlin
val customZones = StoryGestureZones(
    tapLeftFraction = 0.25f,
    tapRightFraction = 0.25f,
    longPressCenterWidth = 0.8f,
    longPressCenterHeight = 0.8f,
    swipeLeftEdge = EdgeFraction(0.15f),
    swipeRightEdge = EdgeFraction(0.15f),
    swipeDownEdge = EdgeFraction(0.20f)
)

val config = StoryPlayerConfig(gestureZones = customZones)
```
</details>

### Title Customization
<details>
<summary><b>Show title customization</b></summary>

```kotlin
val config = StoryPlayerConfig(
    titleConfig = StoryTitleConfig(
        storyTitleTextStyle = MaterialTheme.typography.titleMedium,
        align = TextAlign.Center
    )
)
```
</details>

---

## 📖 Full Customization Example

<details>
<summary><b>Show full example (config + player + timer)</b></summary>

```kotlin
val customConfig = StoryPlayerConfig(
    showDebugUi = true,
    progressBarStyle = StoryProgressBarStyle(
        height = 8.dp,
        gap = 6.dp,
        cornerRadius = 4.dp,
        trackColor = Color.Gray.copy(alpha = 0.4f),
        progressColor = Color.Magenta
    ),
    gestureZones = StoryGestureZones(
        tapLeftFraction = 0.25f,
        tapRightFraction = 0.25f,
        longPressCenterWidth = 0.8f,
        longPressCenterHeight = 0.9f,
        swipeLeftEdge = EdgeFraction(0.15f),
        swipeRightEdge = EdgeFraction(0.15f),
        swipeDownEdge = EdgeFraction(0.20f)
    ),
    titleConfig = StoryTitleConfig(
        storyTitleTextStyle = MaterialTheme.typography.titleLarge.copy(color = Color.Yellow),
        align = TextAlign.Center
    )
)

var uiState by remember {
    mutableStateOf(
        StoryPlayerState(
            stories = listOf(
                StorySpec(
                    id = "Custom 1",
                    source = StorySource.ImageUrl("https://picsum.photos/800/1200"),
                    durationMs = 4000
                ),
                StorySpec(
                    id = "Custom 2",
                    source = StorySource.VideoUri(
                        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4".toUri()
                    )
                )
            ),
            playerConfig = customConfig
        )
    )
}

var isPaused by remember { mutableStateOf(false) }

val current = uiState.currentStory
val isVideo = current?.source is StorySource.VideoUri
val durationMs = current?.durationMs ?: DEFAULT_STORY_DURATION_MS

rememberStoryTimer(
    currentIndex = uiState.currentIndex,
    durationMs = if (isVideo) null else durationMs,
    isPaused = isPaused,
    onProgress = { p -> uiState = uiState.copy(progress = p) },
    onCompleted = {
        val next = (uiState.currentIndex + 1).coerceAtMost(uiState.stories.lastIndex)
        uiState = uiState.copy(currentIndex = next, progress = 0f)
    }
)

StoryPlayer(
    state = uiState,
    title = current?.id ?: "Story",
    onPrev = {
        val prev = (uiState.currentIndex - 1).coerceAtLeast(0)
        uiState = uiState.copy(currentIndex = prev, progress = 0f)
    },
    onNext = {
        val next = (uiState.currentIndex + 1).coerceAtMost(uiState.stories.lastIndex)
        uiState = uiState.copy(currentIndex = next, progress = 0f)
    },
    onPauseChanged = { paused -> isPaused = paused },
    onDismiss = { /* close viewer */ }
)
```
</details>

---

## 📲 Demo App

Clone the repo and run the `app/` module to try the demo:

- Preview images
- Preview video
- Custom styled stories
- Debug overlay toggle

---

## 🤝 Contributing

1. Fork the repo  
2. Create a branch: `git checkout -b feature/my-feature`  
3. Commit: `git commit -m 'Add feature'`  
4. Push: `git push origin feature/my-feature`  
5. Open a pull request  

Run before submitting:

```bash
./gradlew detekt
```

---

## 📜 License

Distributed under the **Apache 2.0** License.  
See [LICENSE](LICENSE) for details.

---

## ⭐ Support

If you enjoy this library, please give it a **star** ⭐ on GitHub!
