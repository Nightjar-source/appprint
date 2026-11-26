package com.appprint.studio

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.appprint.studio.core.platform.AndroidPlatform
import com.appprint.studio.ui.AppRoot
import com.appprint.studio.ui.strings.Str
import com.google.android.gms.ads.MobileAds
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Native splash covers the first frame instantly, then hands over to the full-window
        // adaptive Compose splash in AppRoot (together ≈3s). Keeping this at 3s would hide it.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // The platform splash is dismissed automatically as soon as the first content frame
        // is drawn. Never pin it with setKeepOnScreenCondition { true } — that leaves the
        // app stuck on a permanent blank screen.
        AndroidPlatform.set(applicationContext)
        AndroidPlatform.setWindow(window)
        // AdMob — Google Mobile Ads SDK init (TEST App ID until the real AdMob publisher unit
        // is issued; real ad unit ids live in com.appprint.studio.Admob).
        MobileAds.initialize(this) { _ -> }
        // Compose-First: initialize Jetpack DataStore (with legacy SharedPreferences migration)
        // before any UI reads settings — the in-memory mirror then serves 0-cost sync reads.
        com.appprint.studio.core.platform.AppDataStore.init(applicationContext)
        // Must run after super.onCreate so the window/decor view exists; before setContent.
        enableEdgeToEdge()

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
            // AdMob: test banner slot + test interstitial on PDF save / share.
            val adController = remember { AdMobController(this) }
            AppRoot(
                adBanner = { modifier -> AdMobBanner(modifier) },
                showInterstitial = { onClosed -> adController.show(onClosed) },
            )
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
