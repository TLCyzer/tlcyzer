package de.uni.tuebingen.tlceval.features.detail

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import de.uni.tuebingen.tlceval.Screen
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber


@ExperimentalCoilApi
@ExperimentalAnimationApi
@Composable
fun DetailScreen(navController: NavController, timestamp: Long?) {
    Timber.d("In Detail screen")
    // Check timestamp okay
    if (timestamp == null) {
        navController.navigateUp()
    }
    val vm = getViewModel<DetailViewModel>(parameters = { parametersOf(timestamp) })

    val (isInit, setInit) = remember { mutableStateOf(false) }

    LaunchedEffect(timestamp) {
        val vmInit = vm.initModel()
        setInit(vmInit)
        if (!vmInit) {
            Timber.d("Going back to gallery")
            // If not back to gallery
            navController.navigateUp()
            // TODO show error toast
        }
    }

    if (isInit) {
        DetailView(viewModel = vm, navController = navController, timestamp = timestamp!!)
    } else {
        CircularProgressIndicator()
    }
}
