package de.uni.tuebingen.tlceval.features.image_capture.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import coil.transform.CircleCropTransformation
import de.uni.tuebingen.tlceval.R
import java.io.File

@Composable
fun ShutterButton(active: Boolean, doOnClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
//    val isPressed by interactionSource.collectIsPressedAsState()
    val ripple = rememberRipple(bounded = false)

    Surface(
        color = MaterialTheme.colors.primary,
        border = BorderStroke(8.dp, MaterialTheme.colors.primaryVariant),
        shape = CircleShape,
        elevation = 16.dp,
        modifier = Modifier
            .size(96.dp)
            .clickable(
                interactionSource = interactionSource,
                enabled = active,
                onClick = doOnClick,
                indication = ripple
            )
    ) {

    }

}

@OptIn(ExperimentalCoilApi::class)
@Composable
fun GalleryButton(file: File?, doOnClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
//    val isPressed by interactionSource.collectIsPressedAsState()
    val ripple = rememberRipple(bounded = false)
    Surface(
        color = MaterialTheme.colors.primary,
        border = BorderStroke(4.dp, MaterialTheme.colors.secondary),
        shape = CircleShape,
        elevation = 8.dp,
        modifier = Modifier.clickable(
            interactionSource = interactionSource,
            enabled = true,
            onClick = doOnClick,
            indication = ripple
        )
    ) {
        Image(
            painter = rememberImagePainter(
                data = file,
            ) {
                crossfade(true)
                fallback(R.drawable.ic_photo_placeholder)
                transformations(CircleCropTransformation())
            },
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
        )
    }
}