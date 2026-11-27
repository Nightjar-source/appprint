package com.appprint.studio.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import kotlinx.coroutines.delay

/**
 * Web actual: imported browser files are cached as real bytes by [WebFileRegistry].
 * Decode them into a Compose ImageBitmap so the ORIGINAL photo is shown on the sheet,
 * not a predefined placeholder.
 */
@Composable
actual fun rememberCardBitmap(imageUri: String): ImageBitmap? = produceState<ImageBitmap?>(initialValue = null, imageUri) {
    while (true) {
        val bytes = WebFileRegistry.find(imageUri)?.bytes
        if (bytes != null) {
            value = runCatching { bytes.decodeToImageBitmap() }.getOrNull()
            break
        }
        delay(50)
    }
}.value
