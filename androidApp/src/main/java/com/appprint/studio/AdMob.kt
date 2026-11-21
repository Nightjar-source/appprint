package com.appprint.studio

import android.content.Context
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

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

    fun initialize(context: Context) {
        MobileAds.initialize(context) { _ -> }
    }

    /** Creates a test adaptive/inline banner AdView — wrap in an AndroidView to host it. */
    fun createTestBanner(context: Context, widthDp: Int = 320): AdView = AdView(context).apply {
        adUnitId = TEST_BANNER_AD_UNIT_ID
        setAdSize(AdSize.BANNER)
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
    }
}
