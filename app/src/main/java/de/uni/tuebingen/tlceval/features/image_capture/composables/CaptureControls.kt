package de.uni.tuebingen.tlceval.features.image_capture.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

@ExperimentalAnimationApi
@Composable
fun CaptureControls(
    shutterActivate: Boolean,
    latest_capture_image: File?,
    isNameable: Boolean,
    doOnCapture: () -> Unit,
    doOnGalleryClick: () -> Unit,
    openDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End
    ) {
        AnimatedVisibility(visible = isNameable) {
            Button(
                onClick = openDialog,
                modifier = Modifier.padding(horizontal = 0.dp, vertical = 16.dp)
            ) {
                Text("Set Name")
            }
        }
        Row(
            modifier = modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.size(72.dp))
            ShutterButton(shutterActivate, doOnCapture)
            GalleryButton(latest_capture_image, doOnGalleryClick)
        }
    }

}