package de.uni.tuebingen.tlceval.features.processor.crop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import de.uni.tuebingen.tlceval.Screen
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CropScreen(navController: NavController, timestamp: Long?) {
    if (timestamp == null) {
        navController.navigateUp()
    }
    val vm = getViewModel<CropViewModel>(parameters = { parametersOf(timestamp) })

    // Check timestamp okay
    LaunchedEffect(timestamp) {
        val success = vm.initModel()
        if (!success) {
            // If not back to gallery
            navController.navigateUp()
            // TODO show error toast
        }
    }

    // Start view
    CropViewWithButtons(navController, vm)
}
