package de.uni.tuebingen.tlceval.features.processor.crop

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.uni.tuebingen.tlceval.custom_views.CropRect
import kotlinx.coroutines.launch
import timber.log.Timber


data class Rotation(
    var rotation: Int, var orientation: Int, var rotationDirection: Int
)

class CropViewModel(private val model: CropModel) :
    ViewModel() {

    suspend fun initModel(): Boolean {
        val success = model.initialize()
        _imagePath.postValue(model.currentPath.absolutePath)
        previousSelectedRotation = model.suggestOrientationFromPrevious()
        // TODO figure out how to restore the orientation
        return success
    }

    val cropRect: LiveData<CropRect?>
        get() = _cropRect
    private val _cropRect = MutableLiveData<CropRect?>(null)

    val rotation: LiveData<Rotation>
        get() = _rotation

    private val _rotation = MutableLiveData(Rotation(0, 0, 0))

    private fun rotationToOrientation(rotation: Int): Int {
        var mod360 = rotation.rem(360)
        if (mod360 < 0) mod360 += 360
        return mod360
    }

    private var originalRotation: Int? = null
    private var previousSelectedRotation: Int? = null

    val save: LiveData<Unit?>
        get() = _save

    private val _save = MutableLiveData<Unit?>(null)

    val imagePath: LiveData<String?>
        get() = _imagePath

    private val _imagePath = MutableLiveData<String?>(null)

    fun saveRect() {
        _save.value = Unit
    }

    fun resetRect() {
        originalRotation?.let {
            _rotation.value = Rotation(
                rotation = it,
                rotationDirection = 0,
                orientation = rotationToOrientation(it)
            )
        }
        requestSuggestedRect()
    }

    suspend fun abort(navigateUp: () -> Unit) {
        model.abort()
        navigateUp()
    }

    fun setRotation(rotation: Int) {
        Timber.d("Initial: $originalRotation, $rotation")
        if (originalRotation == null) {
            originalRotation = rotation
        }
        _rotation.value = Rotation(
            rotation = rotation,
            rotationDirection = 0,
            orientation = rotationToOrientation(rotation)
        )
        Timber.d("Updated: $originalRotation, ${_rotation.value}")
    }

    fun requestSuggestedRect() {
        viewModelScope.launch {
            originalRotation?.let { oR ->
                previousSelectedRotation?.let { pR ->
                    Timber.d("Setting rotation with prev rot: $pR, $oR")
                    setRotation(pR - oR)
                }
                _cropRect.value = model.suggestRect()
            }
        }
    }

    private fun rotate(degree: Int) {
        _rotation.value = _rotation.value?.let {
            val newRotation = it.rotation + degree
            Rotation(
                rotation = newRotation,
                rotationDirection = degree,
                orientation = rotationToOrientation(newRotation)
            )
        }
    }

    fun rotateLeft() {
        rotate(-90)
    }

    fun rotateRight() {
        rotate(90)
    }

    fun applyWarp(rect: CropRect, navigateToBlob: (Long) -> Unit) {
        Timber.d("Apply warp...")
        viewModelScope.launch {
            rotation.value?.let {
                Timber.d("Cropping with rotation: $it")
                Timber.d("Original rotation: $originalRotation")
                // TODO this does not work. Find out how to do it...
                val orientation = rotationToOrientation(it.orientation - (originalRotation ?: 0))
                Timber.d("Proposed orientation: $orientation")
                try {
                    model.performWarpCrop(
                        rect,
                        orientation
                    )
                } catch (e: Exception) {
                }

                Timber.d("Navigating to blobs select view!")
                navigateToBlob(model.timestamp)
            }
        }
    }
}