package de.uni.tuebingen.tlceval.features.processor.crop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.asFlow
import androidx.navigation.NavController
import de.uni.tuebingen.tlceval.Screen
import de.uni.tuebingen.tlceval.composables.AppBar
import de.uni.tuebingen.tlceval.features.processor.crop.composables.ButtonRow
import de.uni.tuebingen.tlceval.features.processor.crop.composables.CropViewCompose
import kotlinx.coroutines.launch
import timber.log.Timber


@Composable
fun CropViewWithButtons(
    navController: NavController, vm: CropViewModel,
) {
    val cropRectFlow = vm.cropRect.asFlow()
    val rotateFlow = vm.rotation.asFlow()
    val saveFlow = vm.save.asFlow()
    val imgPath by vm.imagePath.observeAsState()

    var isSaving by remember {
        mutableStateOf(false)
    }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            AppBar(onNavigateBack = {
                coroutineScope.launch { vm.abort { navController.navigateUp() } }
            })
        },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background)
            ) {
                if (imgPath != null) {
                    CropViewCompose(
                        imagePath = imgPath!!,
                        cropRectFlow = cropRectFlow,
                        rotationFlow = rotateFlow,
                        finishFlow = saveFlow,
                        onFinish = { rect ->
                            vm.applyWarp(rect) {
                                navController.navigate("processor/$it/spot") {}
                            }
                        },
                        onOrientationSet = {
                            Timber.d("Original rotation retrieved: $it")
                            vm.setRotation(it)
                            vm.requestSuggestedRect()
                       },
                    )
                }
                ButtonRow(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    doOnReset = { vm.resetRect() },
                    doOnRotateLeft = { vm.rotateLeft() },
                    doOnRotateRight = { vm.rotateRight() },
                    doOnSave = {
                        isSaving = true
                        vm.saveRect()
                    }
                )
                if (isSaving) {
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
    )
}