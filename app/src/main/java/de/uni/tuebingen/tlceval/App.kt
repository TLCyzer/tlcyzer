package de.uni.tuebingen.tlceval

import android.app.Application
import de.uni.tuebingen.tlceval.di.*
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin
import timber.log.Timber
import java.io.File


class App : Application(), KoinComponent {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@App)
            modules(
                listOf(
                    applicationModule,
                    captureModule,
                    galleryModule,
                    cropModule,
                    blobModule,
                    detailModule
                )
            )
        }

        val tree: Timber.Tree = get()
        Timber.plant(tree)

        try {
            System.loadLibrary("tlc_jni")
            Timber.d("tlc lib loaded!")
        } catch (e: UnsatisfiedLinkError) {
            Timber.e("Load libary ERROR: $e")
            return
        }
    }

    companion object {

        fun addNoMedia(toHide: File) {
            val noMediaFile = File(toHide, ".nomedia")
            if (!noMediaFile.exists()) {
                noMediaFile.createNewFile()
            }
        }
    }
}