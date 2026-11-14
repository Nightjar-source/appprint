package com.appprint.studio.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** Material You on Android 12+; null everywhere else (brand palettes are used). */
@Composable
actual fun rememberPlatformColorSchemes(): Pair<ColorScheme, ColorScheme>? {
    val context = LocalContext.current
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    return dynamicLightColorScheme(context) to dynamicDarkColorScheme(context)
}
