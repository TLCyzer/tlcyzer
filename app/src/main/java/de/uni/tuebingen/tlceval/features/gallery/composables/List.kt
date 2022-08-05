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

@OptIn(ExperimentalCoilApi::class)
@Composable
fun ListElement(
    capture: Capture,
    inSelectMode: Boolean,
    selectedTimestamps: List<Long>,
    onClick: (timestamp: Long) -> Unit,
    onLongClick: (timestamp: Long) -> Unit,
    toggleSelected: (timestamp: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .padding(vertical = 6.dp, horizontal = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { /* Called when the gesture starts */ },
                    onDoubleTap = { /* Called on Double Tap */ },
                    onLongPress = { onLongClick(capture.timestamp) },
                    onTap = { onClick(capture.timestamp) }
                )
            },
        elevation = 4.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = rememberImagePainter(
                    data = File(capture.path),
                ) {
                    crossfade(true)
                    fallback(R.drawable.ic_photo_placeholder)
                }, contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)           // crop the image if it's not a square
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.SpaceAround,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = capture.agentName ?: "Not Named",
                        style = MaterialTheme.typography.h6
                    )
                    Text(
                        text = capture.formatTimestamp(),
                        style = MaterialTheme.typography.subtitle1
                    )
                }
                if (inSelectMode)
                    Checkbox(
                        checked = selectedTimestamps.contains(capture.timestamp),
                        onCheckedChange = { toggleSelected(capture.timestamp) },
                        modifier = Modifier.padding(16.dp)
                    )
            }
        }
    }
}