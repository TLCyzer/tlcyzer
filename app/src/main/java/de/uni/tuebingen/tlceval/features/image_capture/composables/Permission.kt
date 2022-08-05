package de.uni.tuebingen.tlceval.features.image_capture.composables

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Rationale(
    onDoNotShowRationale: () -> Unit,
    onRequestPermission: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = "TLCyzer",
            style = MaterialTheme.typography.h1,
            color = MaterialTheme.colors.primary,
            modifier = Modifier
                .padding(horizontal = 32.dp, vertical = 16.dp)
                .weight(1f, false)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(2f, true)
                .padding(32.dp), verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "The camera is important for this app. Please grant the permission.",
                color = MaterialTheme.colors.onBackground
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onRequestPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Request permission")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onDoNotShowRationale,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Don't show again")
            }
        }
        Spacer(modifier = Modifier.weight(1f, true))
    }

}

@Composable
fun NeverAskAgainScreen(navigateToSettingsScreen: () -> Unit, navigateToGallery: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = "TLCyzer",
            style = MaterialTheme.typography.h1,
            color = MaterialTheme.colors.primary,
            modifier = Modifier
                .padding(horizontal = 32.dp, vertical = 16.dp)
                .weight(1f, false)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(2f, true)
                .padding(32.dp), verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Without access to the camera the App cannot be used to process TLC plates",
                color = MaterialTheme.colors.onBackground
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = navigateToGallery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Go to Gallery")
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = navigateToSettingsScreen,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Open Settings")
            }
        }
        Spacer(modifier = Modifier.weight(1f, true))
    }
}

@Composable
fun PermissionDenied(
    navigateToSettingsScreen: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = "TLCyzer",
            style = MaterialTheme.typography.h1,
            color = MaterialTheme.colors.primary,
            modifier = Modifier
                .padding(horizontal = 32.dp, vertical = 16.dp)
                .weight(1f, false)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(2f, true)
                .padding(32.dp), verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Camera permission denied. Without the camera we cannot capture TLC plates.",
                color = MaterialTheme.colors.onBackground
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = navigateToSettingsScreen,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Open Settings")
            }
        }
        Spacer(modifier = Modifier.weight(1f, true))
    }
}