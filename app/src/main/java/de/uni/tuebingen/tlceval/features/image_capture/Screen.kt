package de.uni.tuebingen.tlceval.features.image_capture

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel

@ExperimentalCoilApi
@ExperimentalAnimationApi
@Composable
fun CaptureScreen(navController: NavController) {
    val vm = getViewModel<CaptureViewModel>()
    val appContext: Context = get()

    CameraViewWithRequest(vm, navController, navigateToSettingsScreen = {
        startActivity(
            appContext,
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", appContext.packageName, null)
            ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK), null
        )
    })
}




