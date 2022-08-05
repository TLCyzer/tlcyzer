package de.uni.tuebingen.tlceval.features.processor.spot.composables

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import de.uni.tuebingen.tlceval.custom_views.BlobSelection
import de.uni.tuebingen.tlceval.custom_views.BlobSetterView
import de.uni.tuebingen.tlceval.features.processor.spot.BlobSize
import de.uni.tuebingen.tlceval.features.processor.spot.CircleMaybeReference
import kotlinx.coroutines.flow.Flow

@Composable
fun BlobViewCompose(
    imagePath: String,
    backgroundPathFlow: Flow<String?>,
    backgroundVisibleFlow: Flow<Boolean>,
    blobFlow: Flow<Map<Int, CircleMaybeReference>>,
    blobSizeFlow: Flow<BlobSize>,
    onDimensionAvailable: (Int, Int, Int) -> Unit,
    blobSelectionListener: BlobSelection,
) {
    val backgroundVisible by backgroundVisibleFlow.collectAsState(initial = false)
    val backgroundPath by backgroundPathFlow.collectAsState(initial = null)
    val blobs by blobFlow.collectAsState(initial = null)
    val blobSize by blobSizeFlow.collectAsState(initial = null)

    var oldBackgroundVisible by remember {
        mutableStateOf(false)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            BlobSetterView(context, onDimensionAvailable = onDimensionAvailable).apply {
                setImage(ImageSource.uri(imagePath))
                setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_CENTER)
                selectionListener = blobSelectionListener
            }
        },
        update = {
            if (oldBackgroundVisible != backgroundVisible) {
                oldBackgroundVisible = backgroundVisible
                if (backgroundVisible && backgroundPath != null) {
                    it.setImage(ImageSource.uri(backgroundPath!!), it.state)
                } else {
                    it.setImage(ImageSource.uri(imagePath), it.state)
                }
            }

            blobs?.let { blobMap ->
                it.setSourceCircles(blobMap.mapValues { mapVal -> mapVal.value.circle })
            }

            if (blobSize != null) {
                it.setDefaultSpotSize(blobSize!!.defaultRadius)
                it.setStrokeWidth(blobSize!!.strokeRadius)
            }
        }
    )
}