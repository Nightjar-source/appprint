package com.appprint.studio.core.platform

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

@Composable
actual fun rememberImagePickerLauncher(
    maxItems: Int,
    onPicked: (List<String>) -> Unit,
): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems)
    ) { uris -> onPicked(uris.map(Uri::toString)) }

    return remember(launcher) {
        {
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickMultipleVisualMedia.ImageOnly))
        }
    }
}

@Composable
actual fun rememberCameraCaptureLauncher(
    onCaptured: (String?) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    var captureUri by remember { mutableStateOf<Uri?>(null) }

    fun newCaptureUri(): Uri {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val file = File(dir, "capture-${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = captureUri
        onCaptured(if (success && uri != null) uri.toString() else null)
        captureUri = null
    }

    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = newCaptureUri()
            captureUri = uri
            takePicture.launch(uri)
        } else {
            onCaptured(null)
        }
    }

    return remember(takePicture, requestPermission) {
        {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) {
                val uri = newCaptureUri()
                captureUri = uri
                takePicture.launch(uri)
            } else {
                requestPermission.launch(Manifest.permission.CAMERA)
            }
        }
    }
}

@Composable
actual fun rememberNotificationPermissionRequester(): () -> Unit {
    val context = LocalContext.current

    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    return remember(requestPermission) {
        {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}
