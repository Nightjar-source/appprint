package com.appprint.studio.ui.platform

import androidx.compose.runtime.Composable

/** Platform-specific system Back dispatch (predictive back on Android, no-op elsewhere). */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
