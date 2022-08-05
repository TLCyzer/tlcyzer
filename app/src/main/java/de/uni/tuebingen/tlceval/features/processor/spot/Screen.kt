package de.uni.tuebingen.tlceval.features.processor.spot

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import de.uni.tuebingen.tlceval.Screen
import de.uni.tuebingen.tlceval.composables.AppBar
import kotlinx.coroutines.launch
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

@ExperimentalAnimationApi
@Composable
fun SpotScreen(navController: NavController, timestamp: Long?) {
    Timber.d("In Spot screen")
    // Check timestamp okay
    if (timestamp == null) {
        navController.navigateUp()
    }
    val vm = getViewModel<BlobViewModel>(parameters = { parametersOf(timestamp) })

    val (isInit, setInit) = remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Check timestamp okay
    LaunchedEffect(timestamp) {
        val vmInit = vm.initModel()
        Timber.d("Blob model init: $vmInit")
        setInit(vmInit)
        if (!vmInit) {
            Timber.d("Going back to gallery")
            // If not back to gallery
            navController.navigateUp()
            // TODO show error toast
        }
    }

    Scaffold(
        topBar = {
            AppBar(onNavigateBack = {
                coroutineScope.launch {
                    vm.abort {
                        navController.navigateUp()
                    }
                }
            })
        },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background),
                contentAlignment = Alignment.Center
            ) {
                if (isInit) {
                    BlobViewWithButtons(navController, vm)
                } else {
                    CircularProgressIndicator()
                }
            }
        }
    )
}
