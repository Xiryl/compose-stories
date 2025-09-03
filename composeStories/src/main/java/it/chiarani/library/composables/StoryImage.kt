package it.chiarani.library.composables

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlin.random.Random

@Composable
internal fun StoryImage(
    modifier: Modifier = Modifier,
    @DrawableRes source: Int?,
    contentScale: ContentScale = ContentScale.Crop,
    imageAlignment: Alignment = Center,
    contentDescription: String? = null
) {

    val painter = if (LocalInspectionMode.current) {
        ColorPainter(
            Color(
                red = Random.nextFloat(),
                green = Random.nextFloat(),
                blue = Random.nextFloat(),
                alpha = 1f
            )
        )
    } else {
        rememberAsyncImagePainter(
            ImageRequest.Builder(LocalContext.current)
                .data(data = source)
                .apply(block = fun ImageRequest.Builder.() {
                    crossfade(true)
                }).build(),
        )
    }

    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        alignment = imageAlignment
    )
}

@Composable
internal fun StoryImageUrl(
    modifier: Modifier = Modifier,
    sourceUrl: String?,
    contentScale: ContentScale = ContentScale.Crop,
    imageAlignment: Alignment = Center,
    contentDescription: String? = null
) {

    val painter = if (LocalInspectionMode.current) {
        ColorPainter(
            Color(
                red = Random.nextFloat(),
                green = Random.nextFloat(),
                blue = Random.nextFloat(),
                alpha = 1f
            )
        )
    } else {
        rememberAsyncImagePainter(
            ImageRequest.Builder(LocalContext.current)
                .data(data = sourceUrl)
                .apply(block = fun ImageRequest.Builder.() {
                    crossfade(true)
                }).build(),
        )
    }

    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        alignment = imageAlignment
    )
}

@Suppress("UnusedPrivateMember")
@Preview
@Composable
private fun PreviewStoryImage() {
    StoryImage(source = null)
}
