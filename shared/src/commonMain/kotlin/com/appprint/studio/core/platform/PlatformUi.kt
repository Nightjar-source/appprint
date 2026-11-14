package com.appprint.studio.core.platform

/**
 * System-bar appearance control — edge-to-edge requires the status/navigation bar
 * icon colors to match the app surface (light icons on dark canvas, dark icons on light).
 */
expect object PlatformUi {
    /**
     * Sets appearance of status + navigation bar icons for the given darkness.
     * @param dark true when the app is in dark mode (icons must be light-colored).
     */
    fun syncSystemBars(dark: Boolean)
}
