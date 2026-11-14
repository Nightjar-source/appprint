package com.appprint.studio.core.platform

import android.content.Context

/** Process-wide Application context — set once by MainActivity before any platform call. */
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
