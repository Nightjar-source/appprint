package com.appprint.studio.core.platform

import android.content.Context

/** Process-wide Application context — set once by MainActivity before any platform call. */
object AndroidPlatform {
    lateinit var appContext: Context
        private set

    @Volatile
    private var window: android.view.Window? = null

    fun set(context: Context) {
        if (!::appContext.isInitialized) {
            appContext = context.applicationContext
        }
    }

    fun setWindow(window: android.view.Window?) {
        this.window = window
    }

    fun currentWindow(): android.view.Window? = window
}
