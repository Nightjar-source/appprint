package com.appprint.studio.core.platform

import android.content.Context

/**
 * Process-wide holder for the Application context.
 * Set once by MainActivity before any platform call (export, share, notification).
 * Using the Application context avoids leaking Activity instances into the UI layer.
 */
object AndroidPlatform {
    lateinit var appContext: Context
        private set

    fun set(context: Context) {
        if (!::appContext.isInitialized) {
            appContext = context.applicationContext
        }
    }

    val isSet: Boolean get() = ::appContext.isInitialized
}
