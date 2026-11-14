package com.appprint.studio.core.platform

import androidx.core.view.WindowCompat

actual object PlatformUi {

    actual fun syncSystemBars(dark: Boolean) {
        // Edge-to-edge: bars stay transparent; only the icon colors adapt to the
        // current surface (light icons on dark canvas, dark icons on light).
        // WindowInsetsControllerCompat works on every supported API level.
        val window = AndroidPlatform.currentWindow() ?: return
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
    }
}
