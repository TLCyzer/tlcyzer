package de.uni.tuebingen.tlceval.features.processor.crop.composables

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.navigationBarsPadding
import de.uni.tuebingen.tlceval.R

@Composable
fun ButtonRow(
    modifier: Modifier,
    doOnReset: () -> Unit,
    doOnRotateLeft: () -> Unit,
    doOnRotateRight: () -> Unit,
    doOnSave: () -> Unit) {
    Surface(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding()
            .fillMaxWidth()
            .wrapContentHeight(),
        elevation = 4.dp,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colors.surface
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = doOnReset) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_undo),
                    contentDescription = "Reset",
                    tint = MaterialTheme.colors.onSurface
                )
            }
            IconButton(onClick = doOnRotateLeft) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_rotate_left),
                    contentDescription = "Rotate Left",
                    tint = MaterialTheme.colors.onSurface
                )
            }
            IconButton(onClick = doOnRotateRight) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_rotate_right),
                    contentDescription = "Rotate Right",
                    tint = MaterialTheme.colors.onSurface
                )
            }
            IconButton(onClick = doOnSave) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_accept),
                    contentDescription = "Accept",
                    tint = MaterialTheme.colors.onSurface
                )
            }
        }
    }
}