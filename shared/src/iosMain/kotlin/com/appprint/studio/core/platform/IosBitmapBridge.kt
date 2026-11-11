package com.appprint.studio.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

@Composable
actual fun rememberCardBitmap(source: String?): ImageBitmap? = null // falls back to sample artwork
