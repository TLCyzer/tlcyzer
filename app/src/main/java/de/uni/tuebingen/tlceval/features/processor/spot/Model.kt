package de.uni.tuebingen.tlceval.features.processor.spot

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import de.uni.tuebingen.tlceval.data.Capture
import de.uni.tuebingen.tlceval.data.Point
import de.uni.tuebingen.tlceval.data.Spot
import de.uni.tuebingen.tlceval.data.daos.CaptureDao
import de.uni.tuebingen.tlceval.data.daos.SpotDao
import de.uni.tuebingen.tlceval.di.TLC_PROCESSOR_SCOPE
import de.uni.tuebingen.tlceval.ni.TlcProcessor
import de.uni.tuebingen.tlceval.utils.NamingConstants.BLOBS
import de.uni.tuebingen.tlceval.utils.NamingConstants.WARPED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import timber.log.Timber
import java.io.File

data class Circle(
    val x: Int,
    val y: Int,
    val radius: Int
)

data class CircleMaybeReference(
    val circle: Circle,
    val reference: Int?,
    val isReference: Boolean
)

interface BlobInteraction {
    fun selectBlob(id: Int)
    fun deselectBlob()
    fun moveBlob(x: Int, y: Int)
    fun alterRadius(radius: Int)
    fun getBlob(): LiveData<Pair<Int, Circle>?>
    fun deleteBlob()
    fun addBlob(x: Int, y: Int, radius: Int)
    fun toggleReferenceValue()
    fun isReferenceValue(): LiveData<Boolean?>
    fun getReferenceValue(): LiveData<Int?>
    fun setReferenceValue(value: Int?)
    fun getBlobs(): LiveData<Map<Int, CircleMaybeReference>>
}

sealed class SelectionState {
    data class Selected(val id: Int) : SelectionState()
    object NotSelected : SelectionState()
}

class BlobModel(val timestamp: Long, val captureDao: CaptureDao, val spotDao: SpotDao) :
    KoinComponent, BlobInteraction {

    val imagePath: LiveData<String?>
        get() = _imagePath

    private val _imagePath = MutableLiveData<String?>(null)

    val backgroundFitPath: LiveData<String?>
        get() = _backgroundFitPath

    private val _backgroundFitPath = MutableLiveData<String?>(null)

    private lateinit var processorScope: Scope
    private lateinit var processor: TlcProcessor
    private lateinit var capture: Capture
    private var spots: List<Spot> = listOf()

    suspend fun initialize(): Boolean {
        return withContext(Dispatchers.IO) {
            val maybeCapture = captureDao.findByTimestamp(timestamp)

            val captureSpot = captureDao.loadSpotByTimestamps(longArrayOf(timestamp))
            if (captureSpot.isNotEmpty()) {
                spots = captureSpot.first().spot

                for (entry in captureSpot) {
                    entry.spot.forEach { spotDao.delete(it) }
                }
            }

            if (maybeCapture != null) {
                capture = maybeCapture
                if (capture.cropPath != null) {
                    _imagePath.postValue(capture.cropPath) // This is safe
                }
                val currentPath = File(capture.path)
                processorScope =
                    getKoin().getOrCreateScope(
                        currentPath.absolutePath,
                        named(TLC_PROCESSOR_SCOPE)
                    )
                processor =
                    processorScope.get(parameters = { parametersOf(currentPath.absolutePath) })
                Timber.d("Created spot model model; Processor: $processor | ${processorScope.hashCode()}")
                return@withContext true
            }
            return@withContext false
        }
    }

    private val _spots = MutableLiveData<Map<Int, CircleMaybeReference>>(emptyMap())

    private val _selectionState = MutableLiveData<SelectionState>(SelectionState.NotSelected)
    val selectionState: LiveData<SelectionState>
        get() = _selectionState

    private val defaultReferencePercentage = 100


    fun clearSpots() {
        _spots.postValue(emptyMap())
    }


    suspend fun abort() {
        imagePath.value?.let {
            val warped = File(File(it).parent, WARPED)
            warped.delete()
        }

        val newCapture = capture.copy(cropPath = null, backgroundSubtractPath = null, hasDarkSpots = null)
        captureDao.updateCaptures(newCapture)
        spots.forEach { spotDao.delete(it) }

        processorScope.close()
    }

    fun getReferenceValues(): LiveData<Map<Int, CircleMaybeReference>> {
        return _spots.map { allSpots ->
            allSpots.filter {
                it.value.isReference
            }
        }
    }

    suspend fun hasDarkSpots(): Boolean {
        if (capture.hasDarkSpots != null)
            return capture.hasDarkSpots!!
        return withContext(Dispatchers.Default) {
            processor.hasPotentialDarkBlobs()
        }
    }

    suspend fun detectSpots(darkSpots: Boolean) {
        if (spots.isNotEmpty()) {
            Timber.d("Reusing existing values: $spots")
            processor.fitBackground(darkSpots)
            _spots.postValue(spots.sortedBy { spot -> spot.center.x }.mapIndexed { index, spot ->
                (index + 1) to CircleMaybeReference(
                    Circle(spot.center.x, spot.center.y, spot.radius),
                    if (spot.isReference) {
                        spot.percentage.toInt()
                    } else {
                        null
                    },
                    spot.isReference
                )
            }.toMap())
            processor.detectBlobs()
            _backgroundFitPath.postValue(capture.backgroundSubtractPath)
        } else {
            return withContext(Dispatchers.Default) {
                if (_spots.value.isNullOrEmpty()) {
                    processor.fitBackground(darkSpots)
                    val newSpots =
                        mutableMapOf<Int, CircleMaybeReference>()
                    newSpots.putAll(
                        processor.detectBlobs().toList().chunked(4)
                            .map { c ->
                                Pair(
                                    c[0],
                                    CircleMaybeReference(
                                        Circle(c[1], c[2], c[3]),
                                        defaultReferencePercentage,
                                        false
                                    )
                                )
                            }.toMap()
                    )
                    _spots.postValue(newSpots.toMap())

                    val newCapture = capture.copy(
                        backgroundSubtractPath = File(
                            File(capture.path).parent,
                            BLOBS
                        ).absolutePath,
                        hasDarkSpots = darkSpots,
                    )
                    capture = newCapture
                    captureDao.updateCaptures(capture)

                    _backgroundFitPath.postValue(capture.backgroundSubtractPath)
                }
            }
        }
    }

    suspend fun integrateAndFitPercentages() {
        return withContext(Dispatchers.Default) {
            if (!_spots.value.isNullOrEmpty()) {
                val blobs: Map<Int, CircleMaybeReference> = _spots.value!!
                val blobCoordinates: IntArray = blobs.map { (id, circleRef) ->
                    val circle = circleRef.circle
                    listOf(id, circle.x, circle.y, circle.radius)
                }.flatten().toIntArray()
                val blobReferences: FloatArray = blobs.map { (id, circleRef) ->
                    if (circleRef.isReference && circleRef.reference != null) {
                        listOf(id.toFloat(), circleRef.reference.toFloat())
                    } else {
                        emptyList()
                    }
                }.flatten().toFloatArray()

                Timber.d("Coordinates: ${blobCoordinates.joinToString()}")
                Timber.d("References: ${blobReferences.joinToString()}")

                val integrations = processor.integrateBlobs(blobCoordinates, 0.15f)
                val percentages = processor.fitPercentages(blobReferences)

                val integrationsMap: Map<Int, Int> = integrations.toList().chunked(2).map { idInt ->
                    Pair(idInt[0], idInt[1])
                }.toMap()
                val percentagesMap: Map<Int, Float> =
                    percentages.toList().chunked(2).map { idPerc ->
                        Pair(idPerc[0].toInt(), idPerc[1])
                    }.toMap()

                val blobInformations: List<Spot> = blobs.keys.map { id ->
                    Spot(
                        captureTimestamp = capture.timestamp,
                        center = Point(blobs.getValue(id).circle.x, blobs.getValue(id).circle.y),
                        radius = blobs.getValue(id).circle.radius,
                        integrationValue = integrationsMap.getValue(id),
                        percentage = percentagesMap.getValue(id),
                        isReference = blobs.getValue(id).isReference,
                    )
                }

                spotDao.insertAll(*blobInformations.toTypedArray())

                processorScope.close()
            }
        }
    }

    override fun selectBlob(id: Int) {
        _selectionState.postValue(SelectionState.Selected(id))
    }

    override fun deselectBlob() {
        _selectionState.postValue(SelectionState.NotSelected)
    }

    override fun moveBlob(x: Int, y: Int) {
        executeWhenSelected { id ->
            _spots.value?.toMutableMap()?.let { smap ->
                if (smap.containsKey(id)) {
                    val circle = smap[id]!!.circle
                    smap[id] = smap[id]!!.copy(circle = circle.copy(x = x, y = y))

                    _spots.postValue(smap.toMap())
                }
            }
        }
    }

    override fun alterRadius(radius: Int) {
        executeWhenSelected { id ->
            _spots.value?.toMutableMap()?.let { smap ->
                if (smap.containsKey(id)) {
                    val circle = smap[id]!!.circle
                    smap[id] = smap[id]!!.copy(circle = circle.copy(radius = radius))

                    _spots.postValue(smap.toMap())
                }
            }
        }
    }

    override fun getBlob(): LiveData<Pair<Int, Circle>?> {
        return _selectionState.switchMap { selection ->
            _spots.map { smap ->
                when (selection) {
                    is SelectionState.Selected -> {
                        val id: Int = selection.id

                        if (smap.containsKey(id)) {
                            Pair(id, smap.getValue(id).circle)
                        } else {
                            null
                        }
                    }
                    else -> {
                        null
                    }
                }
            }
        }
    }

    override fun deleteBlob() {
        executeWhenSelected { id ->
            _spots.value?.toMutableMap()?.let { smap ->
                if (smap.containsKey(id)) {
                    smap.remove(id)
                    _spots.postValue(smap.toMap())
                }

                _selectionState.postValue(SelectionState.NotSelected)
            }
        }

    }

    override fun addBlob(x: Int, y: Int, radius: Int) {
        Timber.d("Add Blob: $x, $y, $radius")
        _spots.value?.let { spots ->
            var s = spots.toMutableMap()
            val maxKey: Int = s.keys.maxOrNull() ?: 0
            val newKey: Int = maxKey + 1

            val newSpot =
                CircleMaybeReference(Circle(x, y, radius), defaultReferencePercentage, false)
            if (s.isNullOrEmpty()) {
                s = mutableMapOf(Pair(newKey, newSpot))
            } else {
                s[newKey] = newSpot
            }

            _spots.value = s
            _selectionState.postValue(SelectionState.Selected(newKey))
        }
    }

    override fun toggleReferenceValue() {
        Timber.d("Toggling reference of Blob: ${_selectionState.value}")
        executeWhenSelected { id ->
            _spots.value?.toMutableMap()?.let { smap ->
                if (smap.containsKey(id)) {
                    val isRef = smap[id]?.isReference == true

                    smap[id] = smap[id]!!.copy(isReference = !isRef)
                    _spots.postValue(smap.toMap())
                }
            }
        }
    }

    override fun isReferenceValue(): LiveData<Boolean?> {
        return _spots.map { smap ->
            when (val selection = _selectionState.value) {
                is SelectionState.Selected -> {
                    val id: Int = selection.id

                    smap[id]?.isReference
                }
                else -> {
                    null
                }
            }
        }
    }

    override fun getReferenceValue(): LiveData<Int?> {
        return _spots.map { smap ->
            val maybeOut = when (val selection = _selectionState.value) {
                is SelectionState.Selected -> {
                    val id: Int = selection.id
                    smap[id]?.reference
                }
                else -> {
                    null
                }
            }

            maybeOut
        }
    }

    override fun setReferenceValue(value: Int?) {
        Timber.d("Setting reference of Blob: ${_selectionState.value} to $value")
        executeWhenSelected { id ->
            _spots.value?.toMutableMap()?.let { smap ->
                val oldVal = smap[id]?.reference
                if (smap.containsKey(id) && oldVal != value) {
                    smap[id] = smap[id]!!.copy(reference = value)
                    _spots.postValue(smap.toMap())
                }
            }
        }
    }

    override fun getBlobs(): LiveData<Map<Int, CircleMaybeReference>> {
        return _spots
    }


    private fun executeWhenSelected(func: (id: Int) -> Unit) {
        when (val selection = _selectionState.value) {
            is SelectionState.Selected -> {
                val id: Int = selection.id
                func(id)
            }
            else -> {
                //Ignore
            }
        }
    }

}
