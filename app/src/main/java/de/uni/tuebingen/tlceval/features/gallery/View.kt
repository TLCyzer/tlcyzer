package de.uni.tuebingen.tlceval.features.gallery

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import de.uni.tuebingen.tlceval.GallerySettings
import de.uni.tuebingen.tlceval.R
import de.uni.tuebingen.tlceval.composables.AppBar
import de.uni.tuebingen.tlceval.composables.DefaultToolbarHeight
import de.uni.tuebingen.tlceval.features.gallery.composables.GridElement
import de.uni.tuebingen.tlceval.features.gallery.composables.ListElement
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.roundToInt


//@ExperimentalFoundationApi
//inline fun <T : Any> LazyGridScope.itemsLazyIndexed(
//    items: List<T>,
//    crossinline itemContent: @Composable LazyItemScope.(index: Int, item: T?) -> Unit
//) {
//    items(lazyPagingItems.itemCount) { index ->
//        itemContent(index, lazyPagingItems[index])
//    }
//}

@ExperimentalCoilApi
@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@Composable
fun GalleryView(viewModel: GalleryViewModel, navController: NavController) {

    val scrollState = rememberLazyListState()

    val totalTopHeight = DefaultToolbarHeight//.plus(statusBarTop)
    val toolbarHeightPx =
        (with(LocalDensity.current) { (totalTopHeight).roundToPx().toFloat() })

    // Offset to collapse toolbar
    val toolbarOffsetHeightPx = remember { mutableStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newOffset = toolbarOffsetHeightPx.value + delta
                toolbarOffsetHeightPx.value = newOffset.coerceIn(-toolbarHeightPx, 0f)
                return Offset.Zero
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val lazyCaptureItems by viewModel.getCaptures().collectAsState(initial = null)
    val showList by viewModel.showListFlow.collectAsState(initial = true)
    val sortType by viewModel.sortFlow.collectAsState(initial = GallerySettings.SortType.DATE)

    Timber.d("$lazyCaptureItems")

    val context = LocalContext.current
    val navigateToDetailsOrProcessing: (Long) -> Unit = { timestamp ->
        coroutineScope.launch {
            if (viewModel.isCaptureProcessed(timestamp)) {
                navController.navigate("detail/$timestamp") {
                    launchSingleTop = true
                }
            } else {
                navController.navigate("processor/$timestamp/crop") {}
            }
        }
    }

    val shareItems = {
        coroutineScope.launch {
            viewModel.shareSelectedTimestamps(context)?.let { intent ->
                startActivity(context, intent, null)
            }
        }
    }
    val deleteItems = {
        coroutineScope.launch {
            viewModel.deleteSelectedTimestamps()
        }
    }

    val selectedTimestamps by viewModel.getTimestampsSelected().observeAsState(initial = listOf())
    val isSelectionModeEnabled by viewModel.selectionModeEnabled.observeAsState(false)

    val gridSize = with(LocalConfiguration.current) {
        (this.screenWidthDp / 164f).roundToInt()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .background(MaterialTheme.colors.background)
    ) {
        LazyColumn(
            state = scrollState,
            contentPadding = PaddingValues(
                top = DefaultToolbarHeight,
                bottom = 8.dp,
                start = 8.dp,
                end = 8.dp
            ),
        ) {
            if (lazyCaptureItems == null) {
                item {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            } else {
                if (lazyCaptureItems?.isEmpty() == true) {
                    item {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "No captures were taken.",
                                style = MaterialTheme.typography.subtitle1,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 32.dp)
                            )
                        }
                    }
                } else {
                    items(
                        items = lazyCaptureItems!!.chunked(
                            if (showList) {
                                1
                            } else {
                                gridSize
                            }
                        ), key = { capture ->
                            capture.first().timestamp
                        }
                    ) { captures ->
                        if (showList) {
                            val item = captures.first()
                            ListElement(
                                capture = item,
                                inSelectMode = isSelectionModeEnabled,
                                selectedTimestamps = selectedTimestamps,
                                onClick = {
                                    if (isSelectionModeEnabled) {
                                        viewModel.setTimestampState(it)
                                    } else {
                                        navigateToDetailsOrProcessing(it)
                                    }
                                },
                                onLongClick = {
                                    viewModel.setTimestampState(it)
                                    viewModel.toggleSelectionMode()
                                },
                                toggleSelected = { viewModel.setTimestampState(it) }
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                captures.forEach { capture ->

                                    GridElement(
                                        capture = capture,
                                        inSelectMode = isSelectionModeEnabled,
                                        selectedTimestamps = selectedTimestamps,
                                        onClick = {
                                            if (isSelectionModeEnabled) {
                                                viewModel.setTimestampState(it)
                                            } else {
                                                navigateToDetailsOrProcessing(it)
                                            }
                                        },
                                        onLongClick = {
                                            viewModel.setTimestampState(it)
                                            viewModel.toggleSelectionMode()
                                        },
                                        toggleSelected = { viewModel.setTimestampState(it) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (captures.size != gridSize) {
                                    for (i in captures.size until gridSize)
                                        Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                    }
                }
            }
        }

        AppBar(
            onNavigateBack = { navController.navigateUp() },
            actions = {
//                if (!isSelectionModeEnabled) {
//                    IconButton(onClick = { /*TODO*/ }) {
//                        Icon(
//                            painter = painterResource(id = R.drawable.ic_baseline_search_24),
//                            contentDescription = "Search by Name",
//                            tint = MaterialTheme.colors.onSurface
//                        )
//                    }
//                }
                IconButton(onClick = { coroutineScope.launch { viewModel.toggleList() } }) {
                    Icon(
                        painter = painterResource(id = if (showList) R.drawable.ic_baseline_grid_on_24 else R.drawable.ic_baseline_view_list_24),
                        contentDescription = if (showList) "Show List" else "Show Grid",
                        tint = MaterialTheme.colors.onSurface
                    )
                }
                IconButton(onClick = { coroutineScope.launch { viewModel.toggleSort() } }) {
                    Icon(
                        painter = painterResource(id = if (sortType == GallerySettings.SortType.DATE) R.drawable.ic_sort_alphabetical else R.drawable.ic_sort_numeric),
                        contentDescription = if (sortType == GallerySettings.SortType.DATE) "Sort Alphabetical" else "Sort Numerical",
                        tint = MaterialTheme.colors.onSurface
                    )
                }
                if (isSelectionModeEnabled) {
                    IconButton(onClick = { shareItems() }) { // TODO add loading animation
                        Icon(
                            painter = painterResource(id = R.drawable.ic_share),
                            contentDescription = "Share Analysis",
                            tint = MaterialTheme.colors.onSurface
                        )
                    }
                    IconButton(onClick = {deleteItems() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = "Delete Analysis",
                            tint = MaterialTheme.colors.onSurface
                        )
                    }
                }
            },
            modifier = Modifier.offset {
                IntOffset(
                    x = 0,
                    y = toolbarOffsetHeightPx.value.roundToInt()
                )
            })
    }
}

