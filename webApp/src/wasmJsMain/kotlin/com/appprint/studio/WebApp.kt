package com.appprint.studio

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.appprint.studio.ui.AppRoot
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val body = document.body ?: error("No <body> element")
    ComposeViewport(body) {
        // Visible boot marker: if the document title changes, composition reached content.
        SideEffect { document.title = "طبعتي — جاهز" }
        AppRoot()
    }
}
