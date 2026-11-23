package com.appprint.studio

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.appprint.studio.ui.AppRoot
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val body = document.body ?: error("No <body> element")
    ComposeViewport(body) {
        AppRoot()
    }
}
