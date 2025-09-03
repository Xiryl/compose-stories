package it.chiarani.library.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.chiarani.library.domain.StoryProgressBarStyle

@Composable
internal fun StoryProgressBar(
    modifier: Modifier = Modifier,
    totalSegments: Int,
    currentIndex: Int,
    progress: Float,
    storyProgressBarStyle: StoryProgressBarStyle
) {

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(storyProgressBarStyle.height)
    ) {
        val gapPx = storyProgressBarStyle.gap.toPx()
        val segW = (size.width - gapPx * (totalSegments - 1)) / totalSegments
        val radius = storyProgressBarStyle.cornerRadius.toPx()

        for (i in 0 until totalSegments) {
            val left = i * (segW + gapPx)
            // track
            drawRoundRect(
                color = storyProgressBarStyle.trackColor,
                topLeft = Offset(left, 0f),
                size = Size(segW, size.height),
                cornerRadius = CornerRadius(radius, radius)
            )
            // fill
            val pct = when {
                i < currentIndex -> 1f
                i == currentIndex -> progress.coerceIn(0f, 1f)
                else -> 0f
            }
            if (pct > 0f) {
                drawRoundRect(
                    color = storyProgressBarStyle.progressColor,
                    topLeft = Offset(left, 0f),
                    size = Size(segW * pct, size.height),
                    cornerRadius = CornerRadius(radius, radius)
                )
            }
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview
@Composable
private fun PreviewStoryProgressBar() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            StoryProgressBar(
                totalSegments = 5,
                currentIndex = 2,
                progress = 0.8f,
                storyProgressBarStyle = StoryProgressBarStyle()
            )
        }
    }
}
