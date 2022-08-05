package de.uni.tuebingen.tlceval.features.detail.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView


@Composable
fun ImageViewCompose(
    imagePath: String, onDimensionAvailable: (Int, Int) -> Unit, modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            SubsamplingScaleImageView(context).apply {
                this.setImage(ImageSource.uri(imagePath))
                this.setOnImageEventListener(object :
                    SubsamplingScaleImageView.OnImageEventListener {
                    override fun onReady() {
                        onDimensionAvailable(sWidth, sHeight)
                    }

                    override fun onImageLoaded() {}
                    override fun onPreviewLoadError(e: Exception?) {}
                    override fun onImageLoadError(e: Exception?) {}
                    override fun onTileLoadError(e: Exception?) {}
                    override fun onPreviewReleased() {}
                })
            }
        },
    )
}