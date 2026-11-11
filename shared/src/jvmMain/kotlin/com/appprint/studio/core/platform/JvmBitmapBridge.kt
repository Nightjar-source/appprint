package com.appprint.studio.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
actual fun rememberCardBitmap(source: String?): ImageBitmap? {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = source) {
        value = source?.let { path ->
            withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = File(path).readBytes()
                    org.jetbrains.skia.Image.makeFromEncoded(bytes).toBitmap().asComposeImageBitmap()
                }.getOrNull()
            }
        }
    }
    return bitmap
}
