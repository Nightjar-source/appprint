package com.appprint.studio.core.platform

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Loads a private-file card image downsampled for preview (~1200px). Null on failure. */
@Composable
actual fun rememberCardBitmap(imageUri: String): ImageBitmap? {
    return produceState<ImageBitmap?>(initialValue = null, imageUri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val file = File(imageUri.removePrefix("file://"))
                if (!file.exists()) return@withContext null

                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                if (bounds.outWidth <= 0) return@withContext null

                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1200) sample *= 2
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                BitmapFactory.decodeFile(file.absolutePath, opts)?.asImageBitmap()
            }.getOrNull()
        }
    }.value
}
