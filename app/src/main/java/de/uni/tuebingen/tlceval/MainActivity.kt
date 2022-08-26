package de.uni.tuebingen.tlceval

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.annotation.ExperimentalCoilApi
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import de.uni.tuebingen.tlceval.data.Capture
import de.uni.tuebingen.tlceval.data.database.CaptureDatabase
import de.uni.tuebingen.tlceval.features.detail.DetailScreen
import de.uni.tuebingen.tlceval.features.gallery.GalleryScreen
import de.uni.tuebingen.tlceval.features.image_capture.CaptureScreen
import de.uni.tuebingen.tlceval.features.processor.crop.CropScreen
import de.uni.tuebingen.tlceval.features.processor.spot.SpotScreen
import de.uni.tuebingen.tlceval.theme.AppTheme
import de.uni.tuebingen.tlceval.utils.saveSharedImage
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.File


sealed class Screen(val route: String) {
    object Camera : Screen("camera")
    object Gallery : Screen("gallery")
    object Detail : Screen("detail/{timestamp}")
    object Crop : Screen("processor/{timestamp}/crop")
    object Spot : Screen("processor/{timestamp}/spot")
}

@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
class MainActivity : ComponentActivity() {
    private val outputDir: File by inject()
    private val db: CaptureDatabase by inject()
    private val scope = MainScope()

    @OptIn(ExperimentalCoilApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when {
            intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true -> {
                Timber.d("Got single image intent")
                scope.launch { handleSendImage(intent) }
            }
            intent?.action == Intent.ACTION_SEND_MULTIPLE && intent.type?.startsWith("image/") == true -> {
                Timber.d("Got multi image intent")
                scope.launch { handleSendMultipleImages(intent) }
            }
            else -> {
                //Do nothing
            }
        }

        setContent {
            val navController = rememberNavController()

            AppTheme {
                ProvideWindowInsets {
                    // A surface container using the 'background' color from the theme
                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            navController,
                            startDestination = Screen.Camera.route,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable(Screen.Camera.route) { CaptureScreen(navController) }
                            composable(Screen.Gallery.route) { GalleryScreen(navController) }
                            composable(
                                Screen.Crop.route,
                                arguments = listOf(navArgument("timestamp") {
                                    type = NavType.LongType
                                })
                            ) {
                                CropScreen(navController, it.arguments?.getLong("timestamp"))
                            }
                            composable(
                                Screen.Spot.route,
                                arguments = listOf(navArgument("timestamp") {
                                    type = NavType.LongType
                                })
                            ) {
                                SpotScreen(navController, it.arguments?.getLong("timestamp"))
                            }
                            composable(
                                Screen.Detail.route,
                                arguments = listOf(navArgument("timestamp") {
                                    type = NavType.LongType
                                })
                            ) {
                                DetailScreen(navController, it.arguments?.getLong("timestamp"))
                            }
                        }
                    }
                }
            }


        }
    }

    private suspend fun handleSendImage(intent: Intent) {
        val parcelData = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Parcelable::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM)
        }
        (parcelData as? Uri)?.let { uri ->
            // Update UI to reflect image being shared
            val capture = saveSharedImage(this, uri, outputDir)
            if (capture != null)
                storeInDb(capture)

        }
    }

    private suspend fun handleSendMultipleImages(intent: Intent) {
        val parcelDataArray = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Parcelable::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)
        }
        parcelDataArray?.let { uris ->
            // Update UI to reflect multiple images being shared
            val captures: MutableList<Capture> = mutableListOf()
            for (uri in uris) {
                if (uri is Uri) {
                    val capture = saveSharedImage(this, uri, outputDir)
                    if (capture != null)
                        captures.add(capture)
                }
            }
            storeInDb(*captures.toTypedArray())
        }
    }

    private suspend fun storeInDb(vararg captures: Capture) {
        withContext(Dispatchers.IO) {
            db.captureDao().insertAll(*captures)
        }
    }

}
