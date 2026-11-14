package com.appprint.studio.ui.platform

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS actuals are placeholders — wire the edge-swipe in iosApp on macOS
}
