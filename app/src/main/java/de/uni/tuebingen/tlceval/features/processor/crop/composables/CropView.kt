package de.uni.tuebingen.tlceval.features.processor.crop.composables

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.ORIENTATION_USE_EXIF
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.PAN_LIMIT_CENTER
import de.uni.tuebingen.tlceval.custom_views.CropRect
import de.uni.tuebingen.tlceval.custom_views.CropView
import de.uni.tuebingen.tlceval.features.processor.crop.Rotation
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

@Composable
fun CropViewCompose(
    imagePath: String,
    cropRectFlow: Flow<CropRect?>,
    rotationFlow: Flow<Rotation>,
    finishFlow: Flow<Unit?>,
    onFinish: (CropRect) -> Unit,
    onOrientationSet: (Int) -> Unit,
) {
    val rotation by rotationFlow.collectAsState(initial = Rotation(0, 0, 0))
    val finish by finishFlow.collectAsState(initial = null)
    val cropRect by cropRectFlow.collectAsState(initial = null)

    var isProcessing by  remember {
        mutableStateOf(false)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            CropView(context, onOrientationSet = onOrientationSet).apply {
                setImage(ImageSource.uri(imagePath))
                this.orientation = ORIENTATION_USE_EXIF

                setPanLimit(PAN_LIMIT_CENTER)

                Timber.d("Initial: ${this.orientation}, ${this.appliedOrientation}")

            }
        },
        update = {
            it.setRotation(rotation.rotationDirection, rotation.orientation, rotation.rotation)

            cropRect?.let { crect -> it.setSourcePoints(crect) }

            if (finish != null && !isProcessing) {
                isProcessing = true
                it.sPoints?.let { points -> onFinish(points) }
            }
        }
    )
}