package de.uni.tuebingen.tlceval.features.processor.spot

import androidx.lifecycle.*
import de.uni.tuebingen.tlceval.custom_views.BlobSelection
import kotlinx.coroutines.launch

data class BlobSize(
    val defaultRadius: Int,
    val minRadius: Int,
    val maxRadius: Int,
    val strokeRadius: Int,
)

fun BlobSize.slider0To1ToRadius(value: Float): Int? {
    if (value in 0.0..1.0) {
        val range = maxRadius - minRadius
        val multipliedRange = (range * value).toInt()
        return multipliedRange + minRadius
    }
    return null
}

fun BlobSize.convertRadiusToSlider0to1(radius: Int): Float {
    val subMin = radius - minRadius
    val range = maxRadius - minRadius
    val ret = subMin.toFloat() / range.toFloat()
    return ret
}

class BlobViewModel(private val model: BlobModel) :
    ViewModel(), BlobSelection {

    private var heightWidthAndDensity: Triple<Int, Int, Int>? = null

    fun setHeightWidthAndDensity(height: Int, width: Int, density: Int) {
        heightWidthAndDensity = Triple(height, width, density)
        calculateBlobSize()
    }

    private val _defaultBlobsSize = MutableLiveData<BlobSize>(null)
    val defaultBlobSize: LiveData<BlobSize>
        get() = _defaultBlobsSize

    val blobSize: LiveData<Float?> = model.getBlob().map { maybe_id_circle ->
        if (maybe_id_circle != null) {
            val circle = maybe_id_circle.second
            _defaultBlobsSize.value?.convertRadiusToSlider0to1(circle.radius)
        } else {
            null
        }
    }

    private val _isProcessing = MutableLiveData(false)
    val isProcessing: LiveData<Boolean>
        get() = _isProcessing

    fun getCaptureTimestamp(): Long {
        return model.timestamp
    }

    private fun calculateBlobSize() {
        heightWidthAndDensity?.let {
            val density = it.third
            val strokeWidth = (density / 100f).toInt()
            val minSpotSize = (density / 20f).toInt()
            val maxSpotSize = (density / 2f).toInt()
            val defaultSpotSize = (maxSpotSize - minSpotSize) / 2 + minSpotSize
            val toPost = BlobSize(
                defaultRadius = defaultSpotSize,
                minRadius = minSpotSize,
                maxRadius = maxSpotSize,
                strokeRadius = strokeWidth,
            )
            _defaultBlobsSize.value = toPost
        }
    }

    fun addNewBlob() {
        heightWidthAndDensity?.let { hwd ->
            val height = hwd.first
            val width = hwd.second
            _defaultBlobsSize.value?.let {
                val radius = it.defaultRadius
                // TODO this does not insert the blob in the center. Find out how
                blobUpdates.addBlob(width / 2, height / 2, radius)
            }
        }
    }

    suspend fun initModel(): Boolean {
        val success = model.initialize()
        _blobsDark.value = model.hasDarkSpots()
        model.clearSpots()
        requestBlobs()
        return success
    }

    val imagePath: LiveData<String?>
        get() = model.imagePath

    val backgroundFitPath: LiveData<String?>
        get() = model.backgroundFitPath

    val spotSelected: LiveData<Boolean> =
        model.selectionState.map { it != SelectionState.NotSelected }

    private val _backgroundFitVisible = MutableLiveData<Boolean>(false)
    val backgroundFitVisible: LiveData<Boolean>
        get() = _backgroundFitVisible

    private val _blobsDark = MutableLiveData<Boolean>(false)
    val blobsDark: LiveData<Boolean>
        get() = _blobsDark

    val blobUpdates: BlobInteraction = model

    fun toggleBlobsFit() {
        _backgroundFitVisible.value = _backgroundFitVisible.value?.not()
    }

    suspend fun toggleBlobsDark() {
        _blobsDark.value = _blobsDark.value?.not() ?: true
        model.clearSpots()
        requestBlobs()
    }

    fun enoughReferenceSelected(): LiveData<Boolean> {
        return model.getReferenceValues().map {
            it.size >= 2
        }
    }

    private suspend fun requestBlobs() {
        _isProcessing.value = true
        viewModelScope.launch {
            try {
                model.detectSpots(blobsDark.value ?: false)
                _isProcessing.postValue(false)
            } catch (e: Exception) {
                model.abort()
            }
        }
    }

    suspend fun abort(navigateUp: () -> Unit) {
        model.abort()
        navigateUp()
    }

    fun integrateAndFit() {
        viewModelScope.launch {
            //TODO Check if we have at least two references
            try {
                model.integrateAndFitPercentages()
            } catch (e: Exception) {
                model.abort()
            }
            // TODO move to detail view
        }
    }

    fun blobEditingMenuVisible(): LiveData<Boolean> {
        return model.selectionState.map {
            when (it) {
                SelectionState.NotSelected -> false
                else -> true
            }
        }
    }

    fun alterRadius(value: Float) {
        _defaultBlobsSize.value?.slider0To1ToRadius(value)?.let {
            model.alterRadius(it)
        }
    }

    override fun selected(id: Int) {
        blobUpdates.selectBlob(id)
    }

    override fun deselect() {
        blobUpdates.deselectBlob()
    }

    override fun moved(x: Int, y: Int) {
        blobUpdates.moveBlob(x, y)
    }
}