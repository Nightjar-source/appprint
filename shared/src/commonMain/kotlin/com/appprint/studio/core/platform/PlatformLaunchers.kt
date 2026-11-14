package com.appprint.studio.core.platform

import androidx.compose.runtime.Composable

/** Composable launchers implemented per platform (camera capture / gallery picker / notifications). */

/** Photo Picker launcher — returns a lambda that opens the picker. */
@Composable
expect fun rememberImagePickerLauncher(
    maxItems: Int,
    onPicked: (List<String>) -> Unit,
): () -> Unit

/** Camera capture launcher — returns a lambda that launches the camera. */
@Composable
expect fun rememberCameraCaptureLauncher(
    onCaptured: (String?) -> Unit,
): () -> Unit

/** Runtime permission (POST_NOTIFICATIONS on Android 13+) — returns a requester lambda. */
@Composable
expect fun rememberNotificationPermissionRequester(): () -> Unit
