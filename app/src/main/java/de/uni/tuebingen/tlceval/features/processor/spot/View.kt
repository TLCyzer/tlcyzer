package de.uni.tuebingen.tlceval.features.processor.spot

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.asFlow
import androidx.navigation.NavController
import de.uni.tuebingen.tlceval.Screen
import de.uni.tuebingen.tlceval.features.processor.spot.composables.BlobViewCompose
import de.uni.tuebingen.tlceval.features.processor.spot.composables.BottomSheet
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@Composable
fun BlobViewWithButtons(
    navController: NavController, vm: BlobViewModel
) {
    val imgPath by vm.imagePath.observeAsState()

    val enoughReference by vm.enoughReferenceSelected().observeAsState(false)
    val blobsDark by vm.blobsDark.observeAsState(false)
    val spotSelect by vm.spotSelected.observeAsState(false)
    val isReference by vm.blobUpdates.isReferenceValue().observeAsState(false)
    val referencePercentage by vm.blobUpdates.getReferenceValue().observeAsState(80)
    val blobRadius by vm.blobSize.observeAsState()
    val isProcessing by vm.isProcessing.observeAsState()

    val coroutineScope = rememberCoroutineScope()

    // TODO add loading state
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (imgPath != null) {
            BlobViewCompose(
                imagePath = imgPath!!,
                backgroundPathFlow = vm.backgroundFitPath.asFlow(),
                backgroundVisibleFlow = vm.backgroundFitVisible.asFlow(),
                blobFlow = vm.blobUpdates.getBlobs().asFlow(),
                blobSizeFlow = vm.defaultBlobSize.asFlow(),
                onDimensionAvailable = { height, width, density ->
                    vm.setHeightWidthAndDensity(
                        height,
                        width,
                        density
                    )
                },
                blobSelectionListener = vm,
            )
        }
        // Buttons
        BottomSheet(
            modifier = Modifier.align(Alignment.BottomCenter),
            areBlobsDark = blobsDark,
            onSetBlobsDark = { coroutineScope.launch { vm.toggleBlobsDark() } },
            hasEnoughReferences = enoughReference,
            isReference = isReference ?: false,
            onSetReference = { vm.blobUpdates.toggleReferenceValue() },
            referencePercentage = referencePercentage?.toString() ?: "",
            onSetReferencePercentage = { str ->
                if (str.isEmpty()) vm.blobUpdates.setReferenceValue(null)
                else {
                    val filteredString = str.filter { it.isDigit() }.toInt()
                    vm.blobUpdates.setReferenceValue(filteredString)
                }
            },
            isSelected = spotSelect,
            selectedSize = blobRadius ?: 0f,
            onSizeSelection = { vm.alterRadius(it) },
            onAdd = { vm.addNewBlob() },
            onDelete = { vm.blobUpdates.deleteBlob() },
            onAccept = {
                vm.integrateAndFit()
                navController.navigate("detail/${vm.getCaptureTimestamp()}") {
                    popUpTo(Screen.Gallery.route)
                    launchSingleTop = true
                }
            }
        )
        if (isProcessing == true) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background.copy(alpha = 0.6f))
            ) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}