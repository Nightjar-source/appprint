package com.appprint.studio.ui.platform

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Browser Back navigates away — panels own their own dismissal on web.
}
