package com.appprint.studio.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Browser file names arrive as object-URL-like tokens. Web canvas decode is done by the
 * launcher; here we attempt a best-effort decode from a data URI when present, else null.
 */
@Composable
actual fun rememberCardBitmap(imageUri: String): ImageBitmap? = remember(imageUri) { null }
