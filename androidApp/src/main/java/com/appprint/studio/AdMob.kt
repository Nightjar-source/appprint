package com.appprint.studio

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * AdMob — monetization shell for «طبعتي».
 *
 * TEST configuration only (until real AdMob units are issued):
 *  - App ID  : ca-app-pub-3940256099942544~3347511713 (Google's official Android test app)
 *  - Banner  : ca-app-pub-3940256099942544/6300978115 (Google's official test banner unit)
 *  - Interstitial : ca-app-pub-3940256099942544/6617462939 (Google's official test interstitial unit)
 *  - Rewarded : ca-app-pub-3940256099942544/6464366343 (Google's official test rewarded unit)
 *
 * The real (production) App ID / ad unit ids replace these once the publisher account is
 * whitelisted by AdMob for this application.
 */
object AdMob {
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978115"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/6617462939"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/6464366343"

    /** Banner height — exactly 48dp as required (inline adaptive, max height 48dp). */
    const val BANNER_HEIGHT_DP = 48

    fun initialize(context: Context) {
        // Force test ads on emulators in addition to the official test unit IDs below.
        MobileAds.setRequestConfiguration(
            com.google.android.gms.ads.RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                .build(),
        )
        MobileAds.initialize(context) {
            android.util.Log.d("AdMob", "SDK initialized — test unit IDs active")
        }
    }

    /** Creates a test adaptive/inline banner AdView — wrap in an AndroidView to host it. */
    fun createTestBanner(context: Context): AdView = AdView(context).apply {
        adUnitId = TEST_BANNER_AD_UNIT_ID
        // Standard test banner is the most reliable size for the official test unit.
        setAdSize(AdSize.BANNER)
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        adListener = object : AdListener() {
            override fun onAdLoaded() {
                android.util.Log.d("AdMob", "TEST BANNER LOADED — ${adSize?.height ?: BANNER_HEIGHT_DP}dp")
            }

            override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                android.util.Log.w("AdMob", "TEST BANNER FAILED: ${error.message} (code ${error.code})")
            }
        }
    }
}

/**
 * Adaptive test banner — inline adaptive, max height [AdMob.BANNER_HEIGHT_DP] (40dp < 48dp),
 * max width 320dp. Hosted through [AndroidView]; the AdView loads a test banner ad.
 */
@Composable
fun AdMobBanner(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(AdMob.BANNER_HEIGHT_DP.dp),
        factory = { ctx -> AdMob.createTestBanner(ctx).also { it.loadAd(AdRequest.Builder().build()) } },
        update = { },
    )
}

/**
 * Interstitial controller — preloads a test interstitial and shows it on demand.
 * [show] always invokes [onClosed] (dismissed, failed, or not loaded yet) so the caller's
 * action always runs exactly once.
 */
class AdMobController(private val activity: Activity) {
    private var interstitial: InterstitialAd? = null

    init {
        load()
    }

    private fun load() {
        InterstitialAd.load(
            activity,
            AdMob.TEST_INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                }
            },
        )
    }

    fun show(onClosed: () -> Unit) {
        val ad = interstitial
        if (ad == null) {
            onClosed()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                load() // preload the next one
                onClosed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitial = null
                load()
                onClosed()
            }
        }
        ad.show(activity)
    }
}
