package de.uni.tuebingen.tlceval.features.image_capture.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.accompanist.insets.navigationBarsWithImePadding
import java.io.File
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

@Composable
fun NamingDialog(
    showDialog: Boolean,
    onClose: () -> Unit,
    doOnName: (String) -> Unit,
    imageFile: File?,
    initialName: String = "",
) {
    var textState by remember { mutableStateOf(TextFieldValue(text = initialName)) }
    if (showDialog && imageFile != null) {
        Dialog(
            onDismissRequest = onClose
        ) {
            Surface(
                elevation = 24.dp,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .clipToBounds()
                    .fillMaxWidth()
                    .heightIn(10.dp, 600.dp)
                    .wrapContentHeight()
                    .navigationBarsWithImePadding()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageFile).build()
                            ),
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f, true)

                        )
                        Column(modifier = Modifier.weight(1f, true)) {
                            Text(
                                "Set Name",
                                style = MaterialTheme.typography.h4,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            Text(
                                "Please specify the name of the agent:",
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                            )
                        }
                    }
                    TextField(
                        value = textState,
                        onValueChange = { textState = it },
                        label = { Text("Agent Name") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 32.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 16.dp)
                    ) {
                        Button(onClick = onClose) {
                            Text(text = "Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            doOnName(textState.text)
                            onClose()
                        }) {
                            Text(text = "Save")
                        }
                    }
                }
            }
        }
    }
}