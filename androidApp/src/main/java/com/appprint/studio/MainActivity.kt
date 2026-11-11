package com.appprint.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.appprint.studio.core.platform.AndroidPlatform
import com.appprint.studio.ui.AppRoot

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        AndroidPlatform.set(applicationContext)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AppRoot()
        }
    }
}
