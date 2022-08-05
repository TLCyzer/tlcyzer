package de.uni.tuebingen.tlceval.features.detail

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.asFlow
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.statusBarsPadding
import com.google.accompanist.insets.ui.Scaffold
import de.uni.tuebingen.tlceval.R
import de.uni.tuebingen.tlceval.Screen
import de.uni.tuebingen.tlceval.composables.AppBar
import de.uni.tuebingen.tlceval.composables.DefaultToolbarHeight
import de.uni.tuebingen.tlceval.features.detail.composables.ImageViewCompose
import de.uni.tuebingen.tlceval.features.detail.composables.LineChartCompose
import de.uni.tuebingen.tlceval.features.image_capture.composables.NamingDialog
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

@ExperimentalAnimationApi
@ExperimentalCoilApi
@Composable
fun DetailView(viewModel: DetailViewModel, navController: NavController, timestamp: Long) {
    val (showDialog, setShowDialog) = remember { mutableStateOf(false) }
    val captureName by viewModel.captureName.observeAsState()
    val onOpenDialog = {
        Timber.d("Opening Naming Dialog")
        setShowDialog(true)
    }
    val onDialogClose = {
        Timber.d("Closing Naming Dialog")
        setShowDialog(false)
    }

    val imagePath by viewModel.imageFile.observeAsState()
    val chartVisible by viewModel.chartVisible.observeAsState()

    val coroutineScope = rememberCoroutineScope()

    var dimensions by remember {
        mutableStateOf<Pair<Int, Int>?>(null)
    }

    Scaffold(
        topBar = {
            AppBar(
                title = captureName ?: "Not Named",
                onNavigateBack = {
                    navController.navigateUp()
                },
                actions = {
                    IconButton(onClick = onOpenDialog) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_edit_24),
                            contentDescription = "Rename Agent",
                            tint = MaterialTheme.colors.onSurface
                        )
                    }
                    IconButton(onClick = {
                        navController.navigate("processor/$timestamp/crop") {
                            popUpTo(Screen.Gallery.route)
                        }
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_redo),
                            contentDescription = "Redo Processing",
                            tint = MaterialTheme.colors.onSurface
                        )

                    }
                }
            )
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
        ) {
            // display image
            if (imagePath != null) {
                ImageViewCompose(
                    imagePath = imagePath!!,
                    onDimensionAvailable = { w, h -> dimensions = Pair(w, h) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(top = DefaultToolbarHeight)

                )
            }

            // display chart
            LineChartCompose(
                chartDataFlow = viewModel.chartData.asFlow(),
                maxWidth = dimensions?.first,
                chartVisibleFlow = viewModel.chartVisible.asFlow(),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = DefaultToolbarHeight)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            )

            // toggle button for both
            if (dimensions != null) {
                FloatingActionButton(
                    content = {
                        Icon(
                            painter = painterResource(id = if (chartVisible == true) R.drawable.ic_image else R.drawable.ic_show_chart),
                            contentDescription = if (chartVisible == true) "Show image" else "Show chart",
                            tint = MaterialTheme.colors.onSurface
                        )
                    },
                    onClick = { viewModel.toggleChartVisible() },
                    elevation = FloatingActionButtonDefaults.elevation(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .navigationBarsPadding()
                )
            }

            NamingDialog(
                showDialog = showDialog,
                onClose = onDialogClose,
                doOnName = { coroutineScope.launch { viewModel.changeNameOfCapture(it) } },
                imageFile = imagePath?.let { File(it) },
                initialName = captureName ?: ""
            )

        }

    }
}