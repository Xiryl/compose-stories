package it.chiarani.library.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun StoryBackgroundWrapper(
    modifier: Modifier = Modifier,
    background: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.background(Color.Black)) {
        // Layer 1: background (image/video)
        background()

        // Layer 2: top gradient + app bar minimal
        TopGradientScrim()
    }
}

@Composable
private fun TopGradientScrim() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)
                )
            )
    )
}
