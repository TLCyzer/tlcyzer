package de.uni.tuebingen.tlceval.features.processor.crop

import android.graphics.PointF
import de.uni.tuebingen.tlceval.custom_views.CropRect
import de.uni.tuebingen.tlceval.custom_views.sortFromList
import de.uni.tuebingen.tlceval.data.Capture
import de.uni.tuebingen.tlceval.data.Point
import de.uni.tuebingen.tlceval.data.Rect
import de.uni.tuebingen.tlceval.data.daos.CaptureDao
import de.uni.tuebingen.tlceval.data.daos.RectDao
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


class CropModel(val timestamp: Long, val captureDao: CaptureDao, val rectDao: RectDao) :
    KoinComponent {
    lateinit var currentPath: File
    private lateinit var processorScope: Scope
    private lateinit var processor: TlcProcessor
    private lateinit var capture: Capture
    private var rect: Rect? = null

    suspend fun initialize(): Boolean {
        return withContext(Dispatchers.IO) {
            val maybeCapture = captureDao.findByTimestamp(timestamp)

            val captureRect = captureDao.loadRectByTimestamps(longArrayOf(timestamp))
            if (captureRect.isNotEmpty()) {
                rect = captureRect.first().rect

                for (entry in captureRect) {
                    entry.rect?.let { rectDao.delete(it) }
                }
            }

            if (maybeCapture != null) {
                capture = maybeCapture
                currentPath = File(capture.path)
                processorScope =
                    getKoin().getOrCreateScope(currentPath.absolutePath, named(TLC_PROCESSOR_SCOPE))
                processor =
                    processorScope.get(parameters = { parametersOf(currentPath.absolutePath) })
                Timber.d("Created warp crop model; Processor: $processor | ${processorScope.hashCode()}")
                return@withContext true
            }
            return@withContext false
        }
    }

    suspend fun abort() {
        val newCapture = capture.copy(cropPath = null, backgroundSubtractPath = null, hasDarkSpots = null)
        captureDao.updateCaptures(newCapture)
        rect?.let { rectDao.delete(it) }

        processorScope.close()

    }

    fun suggestOrientationFromPrevious(): Int? {
        return rect?.orientation
    }

    suspend fun suggestRect(): CropRect {
        if (rect != null) {
            return rect?.let {
                CropRect(
                    PointF(it.top_left.x.toFloat(), it.top_left.y.toFloat()),
                    PointF(it.top_right.x.toFloat(), it.top_right.y.toFloat()),
                    PointF(it.bottom_right.x.toFloat(), it.bottom_right.y.toFloat()),
                    PointF(it.bottom_left.x.toFloat(), it.bottom_left.y.toFloat())
                )
            }!!
        } else {
            return withContext(Dispatchers.Default) {
                val corners = processor.detectPlate()
                    .toList()
                    .chunked(2)
                    .map { corner ->
                        PointF(
                            corner[0].toFloat(),
                            corner[1].toFloat()
                        )
                    } // TODO: check if corners are implemented the same way in android
                    .take(4) // Just for safety
                val rect = CropRect()
                rect.sortFromList(corners)
                rect
            }
        }
    }

    private var isProcessing = false

    suspend fun performWarpCrop(rect: CropRect, orientation: Int): Boolean {
        if (!isProcessing) { // Guard to not trigger processing twice
            Timber.d("Start warping")
            isProcessing = true
            return withContext(Dispatchers.IO) {
                Timber.d("Launched!")
                val corners = listOf(rect.topLeft, rect.topRight, rect.bottomRight, rect.bottomLeft)
                    .map { listOf(it.x.toInt(), it.y.toInt()) }
                    .flatten().toIntArray()

                Timber.d("Corners: $corners, ${corners.size}")

                Timber.d("Warping with orientation: ${orientation.toLong()}")
                var success = processor.warpPlate(corners, orientation.toLong())
                Timber.d("Warp plate : $success")
                while (!success) {
                    Timber.d("Unwarping was not successful. Additional try with altered corner")
                    corners[0] += 1
                    corners[1] += 1
                    success = processor.warpPlate(corners, orientation.toLong())
                }

                Timber.d("Success: $success")

                // Save rect
                val points = corners.asIterable().chunked(2).map { xy -> Point(xy[0], xy[1]) }
                val saveRect = Rect(
                    captureTimestamp = timestamp,
                    top_left = points[0],
                    top_right = points[1],
                    bottom_right = points[2],
                    bottom_left = points[3],
                    orientation = orientation,
                )
                Timber.d("Saving rect: $saveRect")

                val ret = currentPath.parentFile?.let {
                    // Only update database if everything is present

                    val warpedPath = File(it.absolutePath + "/$WARPED")
                    val blobPath = File(it.absolutePath + "/$BLOBS")

                    Timber.d("Files exist: Warped - ${warpedPath.exists()} | Blobs - ${blobPath.exists()}")
                    val updateCapture = capture.copy(cropPath = warpedPath.absolutePath)
                    rectDao.insertAll(saveRect)
                    captureDao.updateCaptures(updateCapture)

                    Timber.d("Database updated!")

                    true
                } == true

                return@withContext ret
            }
        }
        Timber.d("Warping already in progress...")
        return false
    }
}
