package com.appprint.studio.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decodes [source] (private storage path) into a Compose ImageBitmap for display,
 * subsampled to something reasonable. Returns null while loading / on failure
 * (callers render the placeholder art instead). Platform-specific.
 */
@Composable
expect fun rememberCardBitmap(source: String?): ImageBitmap?
