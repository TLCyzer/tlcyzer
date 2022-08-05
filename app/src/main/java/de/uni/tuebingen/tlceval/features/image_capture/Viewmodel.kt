package de.uni.tuebingen.tlceval.features.image_capture

import android.net.Uri
import android.view.Surface.ROTATION_0
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.datastore.core.DataStore
import androidx.lifecycle.*
import de.uni.tuebingen.tlceval.CaptureSettings
import de.uni.tuebingen.tlceval.data.Capture
import de.uni.tuebingen.tlceval.utils.NamingConstants
import de.uni.tuebingen.tlceval.utils.createCaptureDir
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.io.File
import java.util.concurrent.Executors

class CaptureViewModel(
    private val datastore: DataStore<CaptureSettings>,
    private val captureModel: CaptureModel,
    private val outputDirectory: File
) :
    ViewModel() {

    sealed class CaptureState {
        object Ready : CaptureState()
        object Capturing : CaptureState()
        data class Success(val uri: Uri) : CaptureState()
        data class Failure(val throwable: Throwable) : CaptureState()
    }

    val doNotAskAgainFlow: Flow<Boolean> = datastore.data
        .map { settings ->
            settings.doNotAskAgain
        }

    suspend fun toggleDoNotAskAgain() {
        datastore.updateData { currentSettings ->
            currentSettings.toBuilder().setDoNotAskAgain(!currentSettings.doNotAskAgain).build()
        }
    }

    private val _latestCapture: LiveData<Capture?> = liveData {
        emit(null)
        emitSource(captureModel.getLatestCapture().distinctUntilChanged().asLiveData())
    }

    val latestCaptureFile: LiveData<File?> = Transformations.map(_latestCapture) { input ->
        input?.path?.let { File(it) }
    }

    val isLatestNameable: LiveData<Boolean> = Transformations.map(_latestCapture) { input ->
        input?.let { it.agentName.isNullOrEmpty() } ?: false
    }

    val getLatestTimestamp: LiveData<Long?> = Transformations.map(_latestCapture) { input ->
        input?.timestamp
    }

    private val _captureResult: MutableLiveData<CaptureState> =
        MutableLiveData<CaptureState>(CaptureState.Ready)


    val captureReady: LiveData<Boolean> = Transformations.map(_captureResult) { input ->
        when (input) {
            CaptureState.Capturing -> false
            else -> true
        }
    }

    fun setupImageCapture(): ImageCapture {
        return ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetRotation(ROTATION_0)
            .setFlashMode(ImageCapture.FLASH_MODE_OFF)
            .build()
    }

    fun takePicture(imageCapture: ImageCapture) {
        Timber.d("Taking Picture...")
        viewModelScope.launch {
            when (_captureResult.value) {
                is CaptureState.Capturing -> return@launch
                else -> {
                    _captureResult.value = CaptureState.Capturing
                    capture(imageCapture)

                }
            }
        }
    }

    fun nameLatest(newName: String) {
        Timber.d("Saving name of latest")
        viewModelScope.launch {
            Timber.d("Waiting for capturemodel")
            captureModel.getLatestCapture().distinctUntilChanged().take(1).collectLatest {
                val currentCapture = it.copy(agentName = newName)
                captureModel.updateCapture(currentCapture)
            }
        }
    }

    private fun capture(imageCapture: ImageCapture) {
        // Create output file to hold the image
        val curMillis = System.currentTimeMillis()
        val dir = createCaptureDir(outputDirectory, curMillis)
        val image = File(dir, NamingConstants.IMG)

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(image)
            .build()

        imageCapture.takePicture(outputOptions, Executors.newSingleThreadExecutor(),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val captureFileUri = output.savedUri ?: Uri.fromFile(image)
                    // Add to database
                    viewModelScope.launch {
                        captureModel.saveCapture(curMillis, captureFileUri.path ?: image.path)
                        //Camera is ready again
                        _captureResult.postValue(CaptureState.Success(captureFileUri))
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    _captureResult.value = CaptureState.Failure(exception)
                }
            })
    }


}