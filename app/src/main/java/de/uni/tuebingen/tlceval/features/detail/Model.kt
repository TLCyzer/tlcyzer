package de.uni.tuebingen.tlceval.features.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import de.uni.tuebingen.tlceval.data.CaptureAndSpots
import de.uni.tuebingen.tlceval.data.Spot
import de.uni.tuebingen.tlceval.data.daos.CaptureDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import timber.log.Timber

class DetailModel(val timestamp: Long, val captureDao: CaptureDao) :
    KoinComponent {
    val imagePath: LiveData<String?>
        get() = _imagePath

    private val _imagePath = MutableLiveData<String?>(null)

    val references: LiveData<List<Pair<Int, Float>>>
        get() = _references

    private val _references = MutableLiveData<List<Pair<Int, Float>>>(listOf())

    val blobPosition: LiveData<List<Pair<Int, Int>>>
        get() = _blobPosition

    private val _blobPosition = MutableLiveData<List<Pair<Int, Int>>>(listOf())

    private lateinit var captureSpot: CaptureAndSpots

    val captureName: LiveData<String?>
        get() = _captureName

    private val _captureName = MutableLiveData<String?>(null)

    suspend fun initialize(): Boolean {
        return withContext(Dispatchers.IO) {

            var maybeCapturesSpots: List<CaptureAndSpots>? = null
            var everythingPresent = false
            while (!everythingPresent) {
                maybeCapturesSpots = captureDao.loadSpotByTimestamps(longArrayOf(timestamp))
                if (maybeCapturesSpots.isNotEmpty()) {
                    if (maybeCapturesSpots.first().spot.isNotEmpty()) {
                        everythingPresent = true
                        continue
                    }
                }

                delay(200L)
            }
            Timber.d("Found spots capture entries for $timestamp: ${maybeCapturesSpots?.size}")
            if (maybeCapturesSpots?.isNotEmpty() == true) {
                captureSpot = maybeCapturesSpots.first()
                Timber.d("Containing ${captureSpot.spot.size} spots")
                if (captureSpot.capture.cropPath != null) {
                    _imagePath.postValue(captureSpot.capture.cropPath) // This is safe
                }

                _references.postValue(captureSpot.spot.map { spotToCoordinatePair(it) })
                _blobPosition.postValue(captureSpot.spot.map { Pair(it.center.x, it.center.y) })
                _captureName.postValue(captureSpot.capture.agentName)

                return@withContext true
            }
            return@withContext false
        }
    }

    suspend fun changeName(newName: String) {
        val newCapture = captureSpot.capture.copy(agentName = newName)

        captureDao.updateCaptures(newCapture)
        initialize()
    }


    private fun spotToCoordinatePair(spot: Spot): Pair<Int, Float> {
        Timber.d("spotToCoordinatePair: $spot")
        return Pair(spot.center.x, spot.percentage)
    }
}