package de.uni.tuebingen.tlceval.features.gallery.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import de.uni.tuebingen.tlceval.R
import de.uni.tuebingen.tlceval.data.Capture
import de.uni.tuebingen.tlceval.data.formatTimestamp
import java.io.File


@ExperimentalCoilApi
@Composable
fun GridElement(
    capture: Capture, inSelectMode: Boolean,
    selectedTimestamps: List<Long>,
    onClick: (timestamp: Long) -> Unit,
    onLongClick: (timestamp: Long) -> Unit,
    toggleSelected: (timestamp: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { /* Called when the gesture starts */ },
                    onDoubleTap = { /* Called on Double Tap */ },
                    onLongPress = { onLongClick(capture.timestamp) },
                    onTap = { if (!inSelectMode) onClick(capture.timestamp) }
                )
            },
        elevation = 4.dp,

    ) {
        Box() {
            Column {
                Image(
                    painter = rememberImagePainter(
                        data = File(capture.path),
                    ) {
                        crossfade(true)
                        fallback(R.drawable.ic_photo_placeholder)
                    }, contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f, false)            // crop the image if it's not a square
                )
                Column(Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 12.dp)) {
                    Text(
                        text = capture.agentName ?: "Not Named",
                        style = MaterialTheme.typography.h6
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = capture.formatTimestamp(),
                        style = MaterialTheme.typography.subtitle1
                    )
                }
            }
            if (inSelectMode)
                Checkbox(
                    checked = selectedTimestamps.contains(capture.timestamp),
                    onCheckedChange = { toggleSelected(capture.timestamp) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )
        }

    }
}