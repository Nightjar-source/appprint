package com.appprint.studio.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/** Loads a private-file card image as an ImageBitmap (downsampled) — Compose preview only. */
@Composable
expect fun rememberCardBitmap(imageUri: String): ImageBitmap?
