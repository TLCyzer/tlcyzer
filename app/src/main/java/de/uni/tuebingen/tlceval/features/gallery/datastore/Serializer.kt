package de.uni.tuebingen.tlceval.features.gallery.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import de.uni.tuebingen.tlceval.GallerySettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream

object GallerySettingsSerializer : Serializer<GallerySettings> {
    override val defaultValue: GallerySettings = GallerySettings.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): GallerySettings {
        return withContext(Dispatchers.IO) {
            runCatching {
                GallerySettings.parseFrom(input)
            }.onFailure { Timber.e(it, "Couldn't read gallery settings!") }
                .getOrDefault(defaultValue)
        }
    }

    override suspend fun writeTo(t: GallerySettings, output: OutputStream) {
        withContext(Dispatchers.IO) {
            runCatching {
                t.writeTo(output)
            }.onFailure {
                Timber.e(it, "Couldn't write gallery settings!")
            }
        }
    }
}

val Context.gallerySettingsDataStore: DataStore<GallerySettings> by dataStore(
    fileName = "gallery_settings.pb",
    serializer = GallerySettingsSerializer
)