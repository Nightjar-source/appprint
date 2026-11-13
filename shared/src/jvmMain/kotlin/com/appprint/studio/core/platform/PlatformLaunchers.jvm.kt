package com.appprint.studio.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun rememberImagePickerLauncher(
    maxItems: Int,
    onPicked: (List<String>) -> Unit,
): () -> Unit = remember(onPicked) {
    {
        val dialog = FileDialog(null as Frame?, "اختر صوراً للبطاقات", FileDialog.LOAD)
        dialog.isMultipleMode = maxItems > 1
        dialog.file = "*.jpg;*.jpeg;*.png;*.bmp;*.webp"
        dialog.isVisible = true
        val files = dialog.files?.toList().orEmpty()
        onPicked(files.map { it.absolutePath })
    }
}

@Composable
actual fun rememberCameraCaptureLauncher(
    onCaptured: (String?) -> Unit,
): () -> Unit = remember(onCaptured) {
    {
        // No camera capture on Desktop — the file picker above is the capture path here.
        onCaptured(null)
    }
}

@Composable
actual fun rememberNotificationPermissionRequester(): () -> Unit = remember { { } }
