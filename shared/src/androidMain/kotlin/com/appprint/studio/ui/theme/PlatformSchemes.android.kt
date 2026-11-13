package com.appprint.studio.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import android.os.Build

@Composable
actual fun rememberPlatformColorSchemes(): Pair<ColorScheme, ColorScheme>? {
    val context = LocalContext.current
    return remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicLightColorScheme(context) to dynamicDarkColorScheme(context)
        } else {
            null
        }
    }
}
