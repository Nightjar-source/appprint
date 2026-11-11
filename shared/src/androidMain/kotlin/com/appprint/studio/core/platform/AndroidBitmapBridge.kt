package com.appprint.studio.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.appprint.studio.core.image.Enhance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
actual fun rememberCardBitmap(source: String?): ImageBitmap? {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = source) {
        value = source?.let {
            withContext(Dispatchers.IO) {
                Enhance.decodeSampled(context, it, maxDimensionPx = 1600)?.asImageBitmap()
            }
        }
    }
    return bitmap
}
