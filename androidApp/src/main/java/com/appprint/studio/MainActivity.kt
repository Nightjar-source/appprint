package com.appprint.studio

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.appprint.studio.core.platform.AndroidPlatform
import com.appprint.studio.ui.AppRoot
import com.appprint.studio.ui.strings.Str
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdRequest
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        AndroidPlatform.set(applicationContext)
        AndroidPlatform.setWindow(window)
        // AdMob — Google Mobile Ads SDK init with a test App Request (test App ID until the
        // real AdMob unit is issued; real ad unit ids live in com.appprint.studio.Admob).
        MobileAds.initialize(this, AdRequest.Builder().build())
        // Compose-First: initialize Jetpack DataStore (with legacy SharedPreferences migration)
        // before any UI reads settings — the in-memory mirror then serves 0-cost sync reads.
        com.appprint.studio.core.platform.AppDataStore.init(applicationContext)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Android 17 memory-budget diagnostics — observability without collecting anything.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                val mgr = getSystemService(android.app.ActivityManager::class.java)
                mgr?.getHistoricalProcessExitReasons(packageName, 0, 5)?.forEach { info ->
                    val desc = info.description ?: ""
                    if (info.reason == android.app.ApplicationExitInfo.REASON_OTHER && desc.contains("MemoryLimiter", ignoreCase = true)) {
                        android.util.Log.w("TabatiMemory", "Previous session was terminated by the Android 17 memory limiter ($desc)")
                    }
                }
            }
        }

        setContent {
            AppRoot()
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Android 17 guidance: trim transient resources when leaving visible state.
        when {
            level >= android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> clearTransientCaches()
            level >= android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE -> clearTransientCaches()
            level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> clearTransientCaches()
        }
    }

    /** Delete stale export/cache files — safe: only clears our own cache directories. */
    private fun clearTransientCaches() {
        runCatching {
            val exports = File(cacheDir, "exports")
            exports.listFiles()?.forEach { file ->
                // keep recent files around for the user to open; delete older than 48h
                if (file.isFile && System.currentTimeMillis() - file.lastModified() > 48L * 60 * 60 * 1000) {
                    file.delete()
                }
            }
        }
    }
}
