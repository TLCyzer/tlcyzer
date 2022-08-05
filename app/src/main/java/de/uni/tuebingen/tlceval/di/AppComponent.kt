package de.uni.tuebingen.tlceval.di

import android.content.Context
import androidx.room.Room
import de.uni.tuebingen.tlceval.R
import de.uni.tuebingen.tlceval.data.database.CaptureDatabase
import de.uni.tuebingen.tlceval.features.detail.DetailModel
import de.uni.tuebingen.tlceval.features.detail.DetailViewModel
import de.uni.tuebingen.tlceval.features.gallery.GalleryModel
import de.uni.tuebingen.tlceval.features.gallery.GalleryViewModel
import de.uni.tuebingen.tlceval.features.gallery.datastore.gallerySettingsDataStore
import de.uni.tuebingen.tlceval.features.image_capture.CaptureModel
import de.uni.tuebingen.tlceval.features.image_capture.CaptureViewModel
import de.uni.tuebingen.tlceval.features.image_capture.datastore.captureSettingsDataStore
import de.uni.tuebingen.tlceval.features.processor.crop.CropModel
import de.uni.tuebingen.tlceval.features.processor.crop.CropViewModel
import de.uni.tuebingen.tlceval.features.processor.spot.BlobModel
import de.uni.tuebingen.tlceval.features.processor.spot.BlobViewModel
import de.uni.tuebingen.tlceval.ni.TlcProcessor
import kotlinx.coroutines.*
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.io.File

const val TLC_PROCESSOR_SCOPE = "TLCProcessor"

val applicationModule = module {
    single<Timber.Tree> { DebugTree() }

    single<File> {
        val appContext: Context = get()
        val mediaDir = File(
            appContext.getExternalFilesDir(null),
            appContext.resources.getString(R.string.app_name)
        ).apply { mkdirs() }

        return@single if (mediaDir.exists())
            mediaDir else appContext.filesDir
    }

    single {
        val appContext: Context = get()
        return@single Room.databaseBuilder(
            appContext,
            CaptureDatabase::class.java, "capture-database"
        ).build()
    }

    scope(named(TLC_PROCESSOR_SCOPE)) {
        scoped { (path: String) ->
            TlcProcessor(path)
        }
    }
}

val captureModule = module {
    single {
        val db: CaptureDatabase = get()
        CaptureModel(db.captureDao())
    }
    single {
        val appContext: Context = get()
        appContext.captureSettingsDataStore
    }
    viewModel {
        CaptureViewModel(get(), get(), get())
    }
}

val galleryModule = module {
    single {
        val db: CaptureDatabase = get()
        GalleryModel(get(), db.captureDao())
    }
    single {
        val appContext: Context = get()
        appContext.gallerySettingsDataStore
    }
    viewModel { GalleryViewModel(get(), get()) }
}

val cropModule = module {
    factory { (processing_timestamp: Long) ->
        val db: CaptureDatabase = get()
        CropModel(processing_timestamp, db.captureDao(), db.rectDao())
    }
    viewModel { (processing_timestamp: Long) ->
        CropViewModel(
            get { parametersOf(processing_timestamp) }
        )
    }
}
val blobModule = module {
    factory { (processing_timestamp: Long) ->
        val db: CaptureDatabase = get()
        BlobModel(processing_timestamp, db.captureDao(), db.spotDao())
    }
    viewModel { (processing_timestamp: Long) ->
        BlobViewModel(get { parametersOf(processing_timestamp) })
    }
}

val detailModule = module {
    factory { (processing_timestamp: Long) ->
        val db: CaptureDatabase = get()
        DetailModel(processing_timestamp, db.captureDao())
    }
    viewModel { (processing_timestamp: Long) ->
        DetailViewModel(get {
            parametersOf(
                processing_timestamp
            )
        })
    }
}