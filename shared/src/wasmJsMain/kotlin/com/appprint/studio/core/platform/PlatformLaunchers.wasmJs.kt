package com.appprint.studio.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.window
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.dom.createEvent

private fun fileInput(accept: String, multiple: Boolean, onFiles: (List<String>) -> Unit): () -> Unit {
    val input = document.createElement("input") as HTMLInputElement
    input.type = "file"
    input.accept = accept
    input.multiple = multiple
    input.style.display = "none"
    document.body?.appendChild(input)
    var onChange: ((Event) -> Unit)? = null
    return {
        onChange?.let { input.removeEventListener("change", it) }
        onChange = { _: Event ->
            val picked = input.files?.let { files ->
                (0 until files.length).map { files[it].name }
            }.orEmpty()
            onFiles(picked)
            input.value = ""
        }
        input.addEventListener("change", onChange!!)
        input.click()
    }
}

@Composable
actual fun rememberImagePickerLauncher(maxItems: Int, onPicked: (List<String>) -> Unit): () -> Unit =
    remember(onPicked) { fileInput("image/*", maxItems > 1, onPicked) }

@Composable
actual fun rememberCameraCaptureLauncher(onCaptured: (String?) -> Unit): () -> Unit =
    remember(onCaptured) { { onCaptured(null) } }

@Composable
actual fun rememberNotificationPermissionRequester(): () -> Unit = remember { { } }

private val document: org.w3c.dom.Document get() = kotlinx.browser.document
