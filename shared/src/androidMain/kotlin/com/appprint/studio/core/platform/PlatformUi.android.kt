package com.appprint.studio.core.platform

import android.os.Build
import androidx.core.view.WindowCompat

actual object PlatformUi {

    actual fun syncSystemBars(dark: Boolean) {
        // True edge-to-edge: status + navigation bars are 100% transparent (no scrim/contrast
        // block). contrastEnforced=false stops Android 10+ from painting a grey enforcement
        // bar behind the nav, which is exactly the "dry block" the design forbids.
        val window = AndroidPlatform.currentWindow() ?: return
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Only the icon colors adapt to the current surface.
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
    }
}
