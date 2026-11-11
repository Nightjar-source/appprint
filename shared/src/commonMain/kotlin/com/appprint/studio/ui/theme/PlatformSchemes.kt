package com.appprint.studio.ui.theme

import androidx.compose.runtime.Composable

/**
 * Platform-provided dynamic color schemes (Material You / Dynamic Color).
 * Android 12+ returns (lightScheme, darkScheme) built from the user's wallpaper palette;
 * every M3 role pair is tonally derived so on* colors always contrast their backgrounds.
 * Other targets return null → callers fall back to the brand schemes.
 */
@Composable
expect fun rememberPlatformColorSchemes(): Pair<androidx.compose.material3.ColorScheme, androidx.compose.material3.ColorScheme>?
