package com.appprint.studio.core.platform

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
    val currentOnCaptured by rememberUpdatedState(onCaptured)
    var captureFile by remember { mutableStateOf<File?>(null) }

    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { saved ->
        currentOnCaptured(if (saved) captureFile?.absolutePath else null)
    }

    fun launchCamera() {
        runCatching {
            // Headless tablets / emulators have no camera app: fail visibly instead of silently.
            val probe = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (probe.resolveActivity(context.packageManager) == null) {
                currentOnCaptured(null)
                return
            }
            val dir = File(AndroidPlatform.appContext.filesDir, "captures").apply { mkdirs() }
            val file = File(dir, "capture-${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            captureFile = file
            takePicture.launch(uri)
        }.onFailure { error ->
            android.util.Log.e("CameraCapture", "Camera launch failed", error)
            currentOnCaptured(null)
        }
    }

    // TakePicture() delegates to a system camera app and does not strictly require CAMERA,
    // but the user explicitly expects a permission prompt; request it before launching.
    val requestCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) launchCamera() else currentOnCaptured(null)
    }

    return remember(takePicture, requestCamera, context) {
        {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            } else {
                requestCamera.launch(Manifest.permission.CAMERA)
            }
            Unit
        }
    }
}

@Composable
actual fun rememberNotificationPermissionRequester(): () -> Unit {
    val context = LocalContext.current
    // Re-check the live permission on every call — a cached flag made the requester keep
    // launching the system dialog after the user had already granted it.
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { grantedNow ->
        if (grantedNow) PlatformActions.flushPendingExportNotification()
    }
    return remember(launcher, context) {
        {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                // Permission already granted — make sure nothing is stuck waiting from before.
                PlatformActions.flushPendingExportNotification()
            }
        }
    }
}
