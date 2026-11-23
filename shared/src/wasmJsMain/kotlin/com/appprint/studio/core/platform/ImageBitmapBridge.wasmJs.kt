package com.appprint.studio.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Compose's Kotlin/Wasm `ImageBitmap` construction API is not exposed by the verified public
 * surface, so on-screen previews keep the shared placeholder. Browser-side canvas crops and PNG
 * export decode the real HTML `img` elements cached by [WebFileRegistry] instead.
 */
@Composable
actual fun rememberCardBitmap(imageUri: String): ImageBitmap? = remember(imageUri) { null }
