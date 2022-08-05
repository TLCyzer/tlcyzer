package de.uni.tuebingen.tlceval.features.gallery

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import de.uni.tuebingen.tlceval.composables.AppBar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.androidx.compose.getViewModel
import timber.log.Timber

@ExperimentalCoilApi
@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@Composable
fun GalleryScreen(navController: NavController) {
    val vm = getViewModel<GalleryViewModel>()

    GalleryView(vm, navController)
}