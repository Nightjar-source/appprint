package com.appprint.studio.ui.theme

import androidx.compose.runtime.Composable

/** Platform dynamic color schemes — Android 12+ only; null elsewhere (brand fallback). */
@Composable
expect fun rememberPlatformColorSchemes(): Pair<androidx.compose.material3.ColorScheme, androidx.compose.material3.ColorScheme>?
