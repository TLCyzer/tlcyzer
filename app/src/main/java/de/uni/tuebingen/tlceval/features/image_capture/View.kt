package de.uni.tuebingen.tlceval.features.image_capture

import android.Manifest
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionRequired
import com.google.accompanist.permissions.rememberPermissionState
import de.uni.tuebingen.tlceval.Screen
import de.uni.tuebingen.tlceval.features.image_capture.composables.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

@ExperimentalCoilApi
@ExperimentalAnimationApi
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraViewWithRequest(
    viewModel: CaptureViewModel,
    navController: NavController,
    navigateToSettingsScreen: () -> Unit,
) {
    val imageCapture = remember { viewModel.setupImageCapture() }
    val activate: Boolean by viewModel.captureReady.observeAsState(false)
    val latestCaptureFile: File? by viewModel.latestCaptureFile.observeAsState()
    val latestCaptureNameable: Boolean by viewModel.isLatestNameable.observeAsState(false)

    // Track if the user doesn't want to see the rationale any more.
    val coroutineScope = rememberCoroutineScope()
    val doNotShowRationale = false

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    val toGallery = {
        Timber.d("Going to gallery...")
        try {
            navController.navigate(Screen.Gallery.route) {
                launchSingleTop = true
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception while launching gallery")
        }
        Timber.d("Done")
    }

    val (showDialog, setShowDialog) = remember { mutableStateOf(false) }
    val onName: (String) -> Unit = { viewModel.nameLatest(it) }
    val onOpenDialog = { setShowDialog(true) }
    val onDialogClose = { setShowDialog(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        PermissionRequired(
            permissionState = cameraPermissionState,
            permissionNotGrantedContent = {
                if (doNotShowRationale) {
                    NeverAskAgainScreen(navigateToSettingsScreen, toGallery)
                } else {
                    Rationale(
                        onDoNotShowRationale = { coroutineScope.launch { viewModel.toggleDoNotAskAgain() } },
                        onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                    )
                }
            },
            permissionNotAvailableContent = {
                PermissionDenied(navigateToSettingsScreen)
            }
        ) {
            SimpleCameraPreview(imageCapture)
            CaptureControls(
                shutterActivate = activate,
                latest_capture_image = latestCaptureFile,
                isNameable = latestCaptureNameable,
                doOnCapture = { viewModel.takePicture(imageCapture) },
                doOnGalleryClick = toGallery,
                openDialog = onOpenDialog,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
            )
            NamingDialog(
                showDialog = showDialog,
                onClose = onDialogClose,
                doOnName = onName,
                imageFile = latestCaptureFile, // This is save as it is only visible when file exists
            )
        }
    }
}