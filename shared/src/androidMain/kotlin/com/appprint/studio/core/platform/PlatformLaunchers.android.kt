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
    // PickMultipleVisualMedia throws "Max items must be higher than 1" for a single item,
    // so single-image replacement uses the dedicated single-pick contract instead.
    if (maxItems <= 1) {
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            onPicked(listOfNotNull(uri?.toString()))
        }
        return remember(launcher) {
            { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        }
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems),
    ) { uris -> onPicked(uris.map(Uri::toString)) }

    return remember(launcher) {
        {
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
}

@Composable
actual fun rememberCameraCaptureLauncher(
    onCaptured: (String?) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    var captureUri by remember { mutableStateOf<Uri?>(null) }
    var captureFile by remember { mutableStateOf<File?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { saved ->
        if (saved) onCaptured(captureFile?.absolutePath) else onCaptured(null)
    }

    return remember(launcher) {
        {
            runCatching {
                val dir = File(AndroidPlatform.appContext.filesDir, "captures").apply { mkdirs() }
                val file = File(dir, "capture-${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                captureFile = file
                captureUri = uri
                launcher.launch(uri)
            }.onFailure { onCaptured(null) }
            Unit
        }
    }
}

@Composable
actual fun rememberNotificationPermissionRequester(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* ignored — export notification is best-effort */ }
    return remember(launcher, context) {
        {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
