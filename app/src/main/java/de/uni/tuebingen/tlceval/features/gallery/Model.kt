package de.uni.tuebingen.tlceval.features.gallery

import com.google.gson.Gson
import de.uni.tuebingen.tlceval.GallerySettings
import de.uni.tuebingen.tlceval.data.Capture
import de.uni.tuebingen.tlceval.data.daos.CaptureDao
import de.uni.tuebingen.tlceval.utils.NamingConstants.SHARE_ZIP
import de.uni.tuebingen.tlceval.utils.zipFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class GalleryModel(private val outputPath: File, private val captureDao: CaptureDao) {

    fun getAll(sort_type: GallerySettings.SortType): Flow<List<Capture>> {
        return when (sort_type) {
            GallerySettings.SortType.AGENT_NAME -> captureDao.getAllSortedByNameAscending()
            GallerySettings.SortType.DATE -> captureDao.getAllSortedByTimestampDescending()
            GallerySettings.SortType.UNRECOGNIZED -> throw IllegalArgumentException("Sort type is not defined")
        }
    }

    suspend fun getAllCapturesFromTimestamp(timestamps: List<Long>): List<Capture> {
        return timestamps.mapNotNull { captureDao.findByTimestamp(it) }
    }

    suspend fun delete(captures: List<Capture>) {
        withContext(Dispatchers.IO) {
            captures.forEach {
                // Delete folder
                File(it.path).parentFile?.deleteRecursively()
                // Delete from DB
                captureDao.delete(it)
            }
        }
    }

    suspend fun prepareShare(captures: List<Capture>): File? {
        val timestamps: LongArray = captures.map { it.timestamp }.toLongArray()

        // First write the data json
        val dirsToZip = withContext(Dispatchers.IO) {
            val capfullInfo = captureDao.loadFullInfoByTimestamps(timestamps)
            capfullInfo.forEach {
                val parentPath = File(it.capture.path).parentFile?.absolutePath
                if (parentPath != null) {
                    val jsonFile = File("$parentPath/capture.json")
                    val jsonString = Gson().toJson(it)

                    jsonFile.writeText(jsonString)
                }
            }

            captures.map { File(it.path) }
        }
        return shareItems(dirsToZip)
    }

    suspend fun shareItems(toShare: List<File>): File? {
        return withContext(Dispatchers.IO) {
            val shareZip = File(outputPath, SHARE_ZIP)

            if (shareZip.exists()) {
                shareZip.delete()
            }

            val onlyDirectories = toShare.map {
                if (it.isDirectory) {
                    it
                } else {
                    it.parentFile
                }
            }

            if (onlyDirectories.zipFiles(shareZip.absolutePath)) {
                shareZip
            } else {
                null
            }
        }
    }
}