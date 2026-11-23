package com.appprint.studio.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.document
import org.w3c.dom.DragEvent
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.files.File

/** Last active image picker — lets a plain drag-and-drop reuse the same callback without a common API. */
private var dropTarget: ((List<File>) -> Unit)? = null
private var dropInstalled = false

private fun installDropTarget() {
    if (dropInstalled) return
    dropInstalled = true
    document.addEventListener("dragover", { it.preventDefault() })
    document.addEventListener("drop", { event ->
        event.preventDefault()
        val files = (event as? DragEvent)?.dataTransfer?.files ?: return@addEventListener
        val dropped = (0 until files.length).mapNotNull { files.item(it) }
        dropTarget?.invoke(dropped)
    })
}

/** Registers browser Files and hands logical `web://` handles to the shared ViewModel. */
private fun registerPicked(files: List<File>, maxItems: Int, onPicked: (List<String>) -> Unit) {
    val picked = files.take(maxItems)
    if (picked.isEmpty()) {
        onPicked(emptyList())
        return
    }
    val handles = arrayOfNulls<String>(picked.size)
    var remaining = picked.size
    picked.forEachIndexed { index, file ->
        WebFileRegistry.register(file) { entry ->
            handles[index] = entry.handle
            remaining--
            if (remaining == 0) onPicked(handles.filterNotNull())
        }
    }
}

private fun fileInput(
    accept: String,
    maxItems: Int,
    capture: String? = null,
    onFiles: (List<String>) -> Unit,
): () -> Unit {
    installDropTarget()
    val input = document.createElement("input") as HTMLInputElement
    input.type = "file"
    input.accept = accept
    input.multiple = maxItems > 1
    input.style.display = "none"
    if (capture != null) input.setAttribute("capture", capture)
    document.body?.appendChild(input)

    dropTarget = { dropped -> registerPicked(dropped, maxItems, onFiles) }

    var onChange: ((Event) -> Unit)? = null
    return {
        onChange?.let { input.removeEventListener("change", it) }
        onChange = { _: Event ->
            val files = input.files
            val picked = if (files == null) emptyList() else (0 until files.length).mapNotNull { files.item(it) }
            registerPicked(picked, maxItems, onFiles)
            // Allow selecting the same file again after a cancel or a repeated import.
            input.value = ""
        }
        input.addEventListener("change", onChange!!)
        input.click()
    }
}

@Composable
actual fun rememberImagePickerLauncher(
    maxItems: Int,
    onPicked: (List<String>) -> Unit,
): () -> Unit = remember(onPicked, maxItems) {
    fileInput("image/*", maxOf(1, maxItems), onFiles = onPicked)
}

@Composable
actual fun rememberCameraCaptureLauncher(
    onCaptured: (String?) -> Unit,
): () -> Unit = remember(onCaptured) {
    fileInput("image/*", 1, capture = "environment") { handles ->
        onCaptured(handles.firstOrNull())
    }
}

@Composable
actual fun rememberNotificationPermissionRequester(): () -> Unit = remember { { } }
