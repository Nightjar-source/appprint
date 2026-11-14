package com.appprint.studio.ui.platform

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // no system Back to map on the desktop target — panels/sheets own dismissal semantics
}
