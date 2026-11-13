package com.appprint.studio

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView

/**
 * Temporary placeholder — the app is being rebuilt from scratch from the new
 * design (HTML → Compose Multiplatform). Replace with the real
 * ComponentActivity + setContent entry once the new UI lands in :shared.
 */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 22f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#151816"))
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            gravity = android.view.Gravity.CENTER
        })
    }
}
