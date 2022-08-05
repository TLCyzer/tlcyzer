package de.uni.tuebingen.tlceval.features.image_capture

import de.uni.tuebingen.tlceval.data.Capture
import de.uni.tuebingen.tlceval.data.daos.CaptureDao
import de.uni.tuebingen.tlceval.utils.createCaptureDir
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.io.File

class CaptureModel(private val captureDao: CaptureDao) {

    fun getLatestCapture(): Flow<Capture> {
        Timber.d("Setting up latest capture checks")
        return captureDao.getLatest()
    }

    suspend fun getCaptureFromTimestamp(timestamp: Long): Capture? {
        return captureDao.findByTimestamp(timestamp)
    }

    suspend fun updateCapture(capture: Capture) {
        captureDao.updateCaptures(capture)
    }

    suspend fun saveCapture(timestamp: Long, path: String) {
        val capture = Capture(timestamp, path, null, null, null, null)
        Timber.d("Saving $capture")
        captureDao.insertAll(capture)
    }
}