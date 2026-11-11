package com.appprint.studio.core.platform

import androidx.compose.runtime.Composable

/**
 * Platform launchers for the system image picker and the camera.
 * Android wires real Activity result contracts; Desktop and iOS provide
 * equivalents or graceful no-ops. Returns a launch function the UI can call.
 */
@Composable
expect fun rememberImagePickerLauncher(
    maxItems: Int,
    onPicked: (List<String>) -> Unit,
): () -> Unit

@Composable
expect fun rememberCameraCaptureLauncher(
    onCaptured: (String?) -> Unit,
): () -> Unit

/**
 * Requests POST_NOTIFICATIONS on Android 13+ (no-op elsewhere).
 * Invoked by the UI right before a download/print action that triggers a notification.
 */
@Composable
expect fun rememberNotificationPermissionRequester(): () -> Unit
