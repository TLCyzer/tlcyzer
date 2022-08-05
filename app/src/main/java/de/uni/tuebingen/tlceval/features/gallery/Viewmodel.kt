package de.uni.tuebingen.tlceval.features.gallery

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.datastore.core.DataStore
import androidx.lifecycle.*
import de.uni.tuebingen.tlceval.GallerySettings
import de.uni.tuebingen.tlceval.data.Capture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.androidx.compose.get
import timber.log.Timber
import java.io.File
import kotlin.collections.set

class GalleryViewModel(
    private val datastore: DataStore<GallerySettings>,
    private val galleryModel: GalleryModel,
) :
    ViewModel() {

    private val _selectionModeEnabled: MutableLiveData<Boolean> = MutableLiveData(false)
    val selectionModeEnabled: LiveData<Boolean>
        get() = _selectionModeEnabled

    fun toggleSelectionMode() {
        _selectionModeEnabled.value?.let {
            Timber.d("${_selectionModeEnabled.value} - ${timestampsSelected.value}")
            _selectionModeEnabled.value = !it
            if (it) { // If it was enabled...
                // Clear the selected timestamps
                timestampsSelected.value = mapOf()
            }
            Timber.d("${_selectionModeEnabled.value} - ${timestampsSelected.value}")
        }
    }


    val showListFlow: Flow<Boolean> = datastore.data
        .map { settings ->
            settings.showList
        }

    val sortFlow: Flow<GallerySettings.SortType> = datastore.data
        .map { settings ->
            settings.sortType
        }

    suspend fun toggleList() {
        datastore.updateData { currentSettings ->
            currentSettings.toBuilder().setShowList(!currentSettings.showList).build()
        }
    }

    suspend fun toggleSort() {
        datastore.updateData { currentSettings ->
            val newSort = when (currentSettings.sortType) {
                GallerySettings.SortType.AGENT_NAME -> GallerySettings.SortType.DATE
                GallerySettings.SortType.DATE -> GallerySettings.SortType.AGENT_NAME
                else -> GallerySettings.SortType.DATE
            }
            currentSettings.toBuilder().setSortType(newSort).build()
        }
    }

    private val timestampsSelected: MutableLiveData<Map<Long, Boolean>> =
        MutableLiveData(mapOf())

    fun setTimestampState(timestamp: Long) {
        timestampsSelected.value?.let { map ->
            val state = checkSelected(map, timestamp)
            val mutableCopy = map.toMutableMap()
            mutableCopy[timestamp] = !state
            timestampsSelected.value = mutableCopy.toMap()
        }
    }


    private fun checkSelected(map: Map<Long, Boolean>, timestamp: Long): Boolean {
        return map[timestamp] ?: false
    }

    fun getTimestampsSelected(): LiveData<List<Long>> {
        return Transformations.map(
            timestampsSelected
        ) { map -> map.filter { it.value }.map { it.key } }
    }

    suspend fun deleteSelectedTimestamps() {
        timestampsSelected.value?.let { ts ->
            val timestamps = ts.filter { it.value }.map { it.key }
            Timber.d("Delete requested: $timestamps")
            viewModelScope.launch {
                if (selectionModeEnabled.value == true) {
                    Timber.d("Selection mode active. Deleting $timestamps...")
                    val captures = galleryModel.getAllCapturesFromTimestamp(timestamps)
                    galleryModel.delete(captures)

                    timestampsSelected.postValue(mapOf())
                    toggleSelectionMode()
                }
            }
        }
    }

    @ExperimentalCoroutinesApi
    suspend fun shareSelectedTimestamps(context: Context): Intent? {
        return timestampsSelected.value?.let { ts ->
            val timestamps = ts.filter { it.value }.map { it.key }
            Timber.d("Share requested: $timestamps")
            if (selectionModeEnabled.value == true) {
                Timber.d("Selection mode active. Sharing $timestamps...")
                val captures = galleryModel.getAllCapturesFromTimestamp(timestamps)
                // export all gallery data to json
                // zip folders
                val zipFile = galleryModel.prepareShare(captures)

                zipFile?.let {
                    val sendIntent = Intent()
                    sendIntent.action = Intent.ACTION_SEND
                    val uri = FileProvider.getUriForFile(
                        context,
                        context.applicationContext.packageName + ".fileprovider",
                        it
                    )
                    sendIntent.putExtra(Intent.EXTRA_STREAM, uri)
                    sendIntent.type = "application/zip"

                    timestampsSelected.value = mapOf()
                    toggleSelectionMode()

                    sendIntent
                    // TODO create share intent
                }
            } else {
                null
            }
        }
    }

    @ExperimentalCoroutinesApi
    fun getCaptures(): Flow<List<Capture>> {
        return sortFlow.flatMapLatest { galleryModel.getAll(it) }
    }

    suspend fun isCaptureProcessed(timestamp: Long): Boolean {
        val captures = galleryModel.getAllCapturesFromTimestamp(listOf(timestamp))
        if (captures.size != 1) {
            return false
        }
        val capture = captures[0]

        return if (capture.cropPath != null && capture.backgroundSubtractPath != null) {
            val cropFile = File(capture.cropPath)
            val backgroundFile = File(capture.backgroundSubtractPath)
            cropFile.exists() && backgroundFile.exists()
        } else {
            false
        }
    }

}