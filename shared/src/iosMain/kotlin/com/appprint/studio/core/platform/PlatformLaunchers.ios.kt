package com.appprint.studio.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberImagePickerLauncher(
    maxItems: Int,
    onPicked: (List<String>) -> Unit,
): () -> Unit = remember(onPicked) {
    { onPicked(emptyList()) } // PHPickerViewController is wired when the iosApp module is built on macOS
}

@Composable
actual fun rememberCameraCaptureLauncher(
    onCaptured: (String?) -> Unit,
): () -> Unit = remember(onCaptured) {
    { onCaptured(null) }
}

@Composable
actual fun rememberNotificationPermissionRequester(): () -> Unit = remember { { } }
