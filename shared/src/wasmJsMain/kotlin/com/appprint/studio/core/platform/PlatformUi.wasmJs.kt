package com.appprint.studio.core.platform

import com.appprint.studio.core.model.AppTheme

/** Web has no OS status bar — no-op. */
actual object PlatformUi {
    actual fun syncSystemBars(dark: Boolean) = Unit
}
