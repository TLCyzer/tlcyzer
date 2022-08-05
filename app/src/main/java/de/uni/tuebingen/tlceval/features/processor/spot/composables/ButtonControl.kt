package de.uni.tuebingen.tlceval.features.processor.spot.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.navigationBarsWithImePadding
import de.uni.tuebingen.tlceval.R


@ExperimentalAnimationApi
@Composable
fun BottomSheet(
    areBlobsDark: Boolean,
    onSetBlobsDark: (Boolean) -> Unit,
    hasEnoughReferences: Boolean,
    isReference: Boolean,
    onSetReference: (Boolean) -> Unit,
    referencePercentage: String,
    onSetReferencePercentage: (String) -> Unit,
    isSelected: Boolean,
    selectedSize: Float,
    onSizeSelection: (Float) -> Unit,
    onAdd: () -> Unit,
    onDelete: () -> Unit,
    onAccept: () -> Unit,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(10.dp, 300.dp)
            .wrapContentHeight()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsWithImePadding(),
        elevation = 4.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            AnimatedVisibility(visible = isSelected) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                    Text(
                        text = "Size",
                        style = MaterialTheme.typography.subtitle2,
                    )
                    Slider(
                        value = selectedSize,
                        onValueChange = onSizeSelection,
                        steps = 100
                    )

                    Row(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = CenterVertically
                    ) {
                        Checkbox(checked = isReference, onCheckedChange = onSetReference)
                        Text(
                            text = "Is Reference Value?",
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        AnimatedVisibility(visible = isReference) {
                            TextField(
                                value = referencePercentage,
                                onValueChange = { str ->
                                    onSetReferencePercentage(str)
                                },
                                label = { Text("Enter Percentage") },
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.padding(start = 4.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }

                }

                Divider(color = MaterialTheme.colors.background, thickness = 1.dp)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = CenterVertically
            ) {
                Checkbox(checked = areBlobsDark, onCheckedChange = onSetBlobsDark)
                Text(
                    text = "Are the spots dark?",
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = CenterVertically
            ) {
                IconButton(onClick = onAdd) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = "Add Spot",
                        tint = MaterialTheme.colors.onSurface
                    )
                }
                AnimatedVisibility(visible = isSelected) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = "Delete Spot",
                            tint = MaterialTheme.colors.onSurface
                        )
                    }
                }
                AnimatedVisibility(visible = hasEnoughReferences) {
                    IconButton(onClick = onAccept) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_accept),
                            contentDescription = "Accept",
                            tint = MaterialTheme.colors.onSurface
                        )
                    }
                }
                AnimatedVisibility(visible = !hasEnoughReferences) {
                    Text(
                        text = "Not enough reference Values set",
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.error,
                        maxLines = 2,
                        modifier = Modifier
                            .width(128.dp)
                            .align(CenterVertically)
                    )

                }
            }
        }
    }
}