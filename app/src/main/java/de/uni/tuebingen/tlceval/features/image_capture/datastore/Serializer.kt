package de.uni.tuebingen.tlceval.features.image_capture.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import de.uni.tuebingen.tlceval.CaptureSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream

object CaptureSettingsSerializer : Serializer<CaptureSettings> {
    override val defaultValue: CaptureSettings = CaptureSettings.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): CaptureSettings {
        return withContext(Dispatchers.IO) {
            runCatching {
                CaptureSettings.parseFrom(input)
            }.onFailure { Timber.e(it, "Couldn't read capture settings!") }
                .getOrDefault(defaultValue)
        }
    }

    override suspend fun writeTo(t: CaptureSettings, output: OutputStream) {
        withContext(Dispatchers.IO) {
            runCatching {
                t.writeTo(output)
            }.onFailure {
                Timber.e(it, "Couldn't write capture settings!")
            }
        }
    }
}

val Context.captureSettingsDataStore: DataStore<CaptureSettings> by dataStore(
    fileName = "capture_settings.pb",
    serializer = CaptureSettingsSerializer
)