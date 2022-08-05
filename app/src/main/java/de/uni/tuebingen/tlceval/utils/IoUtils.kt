package de.uni.tuebingen.tlceval.utils

import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.widget.AppCompatEditText
import timber.log.Timber
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

const val BUFFER = 2048

fun List<File>.zipFiles(zipFileName: String): Boolean {
    try {
        val dest = FileOutputStream(zipFileName)
        val out = ZipOutputStream(
            BufferedOutputStream(
                dest
            )
        )

        for (f in this) {
            Timber.d("Adding ${f.absolutePath}")
            zipFiles(out, f, f.name)
        }

        out.close()
        return true
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return false
}


private fun zipFiles(zipOut: ZipOutputStream, sourceFile: File, parentDirPath: String) {
    val data = ByteArray(BUFFER)

    val files = sourceFile.listFiles() ?: arrayOf(sourceFile)
    for (f in files) {
        if (f.isDirectory) {
            val path = if (parentDirPath == "") {
                f.name
            } else {
                parentDirPath + File.separator + f.name
            }

            Timber.d("The file is a directory ${f.absolutePath} - path is: $path")
            val entry = ZipEntry(path + File.separator)
            entry.time = f.lastModified()
            entry.isDirectory
            entry.size = f.length()
            zipOut.putNextEntry(entry)
            //Call recursively to add files within this directory
            zipFiles(zipOut, f, path)
        } else {
            FileInputStream(f).use { fi ->
                BufferedInputStream(fi).use { origin ->
                    val path = parentDirPath + File.separator + f.name
                    Timber.d("The file is a file ${f.absolutePath} - path is: $path")
                    val entry = ZipEntry(path)
                    entry.time = f.lastModified()
                    entry.isDirectory
                    entry.size = f.length()
                    zipOut.putNextEntry(entry)
                    while (true) {
                        val readBytes = origin.read(data)
                        if (readBytes == -1) {
                            break
                        }
                        zipOut.write(data, 0, readBytes)
                    }
                }
            }
        }
    }
}


fun List<File>.remove(): Boolean {
    return map {
        if (it.isDirectory) {
            it.deleteRecursively()
        } else {
            it.delete()
        }
    }.all { it }
}


fun AppCompatEditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            afterTextChanged.invoke(s.toString())
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }
    })
}