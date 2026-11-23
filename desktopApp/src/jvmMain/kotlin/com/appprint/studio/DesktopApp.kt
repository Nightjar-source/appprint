package com.appprint.studio

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.appprint.studio.ui.AppRoot

/**
 * Native Windows/macOS/Linux entry point — Compose Desktop renders the SAME shared UI.
 * No WebView: the share module is compiled straight to the host's Skia renderer.
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "طبعتي — استوديو الطباعة الذكي 1:1",
    ) {
        AppRoot()
    }
}
