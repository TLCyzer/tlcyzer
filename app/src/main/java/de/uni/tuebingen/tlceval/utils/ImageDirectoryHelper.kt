package de.uni.tuebingen.tlceval.utils

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import de.uni.tuebingen.tlceval.data.Capture
import timber.log.Timber
import java.io.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


fun createCaptureDir(outputDirectory: File, curMillis: Long): File {
    val dir = File(outputDirectory, "$curMillis")
    dir.apply { mkdirs() }
    return dir
}

fun saveSharedImage(context: Context, uri: Uri, outputDir: File): Capture? {
    var inputStream: InputStream? = null
    var newFileName: File? = null
    var capture: Capture? = null
    Timber.d("Trying to import $uri")
    try {
        inputStream = context.contentResolver.openInputStream(uri)
        val exifInterface = inputStream?.let { ExifInterface(it) }
        val dateTime = exifInterface?.getAttribute(ExifInterface.TAG_DATETIME)
        val millis = dateTime?.let { parseDateTime(it) } ?: System.currentTimeMillis()
        val captureDir = createCaptureDir(outputDir, millis)

        Timber.d("Creating folder $captureDir")
        val fileName = File(captureDir, NamingConstants.IMG)
        capture = Capture(
            millis,
            fileName.absolutePath,
            agentName = null,
            backgroundSubtractPath = null,
            cropPath = null,
            hasDarkSpots = null,
        )
        newFileName = fileName
    } catch (e: IOException) { // Handle any errors
        Timber.e(e, "Failed to save image")
    } finally {
        if (inputStream != null) {
            try {
                inputStream.close()
            } catch (ignored: IOException) {
                Timber.e(ignored, "Failed to close input stream")
            }
        }
        newFileName = newFileName?.let { createFileFromInputStream(context, uri, it) }
    }

    return if (newFileName != null) capture else null
}

private fun createFileFromInputStream(
    context: Context, uri: Uri,
    output: File
): File? {
    var inputStream: InputStream? = null
    Timber.d("Trying to write image to folder")
    try {
        inputStream = context.contentResolver.openInputStream(uri)

        try {
            val outputStream: OutputStream = FileOutputStream(output)
            val buffer = ByteArray(1024)
            var length: Int
            if (inputStream != null) {
                while (inputStream.read(buffer).also { length = it } > 0) {
                    outputStream.write(buffer, 0, length)
                }
            }
            outputStream.close()
            inputStream?.close()
            return output
        } catch (e: IOException) {
            println("error in creating a file")
            e.printStackTrace()
        }
    } catch (e: IOException) { // Handle any errors
    } finally {
        if (inputStream != null) {
            try {
                inputStream.close()
            } catch (ignored: IOException) {
            }
        }
    }
    return null
}

private fun parseDateTime(dateTime: String): Long? {
    val simpleDateFormat = SimpleDateFormat("yyyy:MM:dd hh:mm:ss")

    var d: Date? = null

    try {
        d = simpleDateFormat.parse(dateTime)
    } catch (e: ParseException) { // TODO Auto-generated catch block
        e.printStackTrace()
    }

    return d?.time
}