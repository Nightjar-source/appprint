package com.appprint.studio.core.platform

import kotlinx.browser.document
import kotlinx.browser.window
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.toByteArray
import org.khronos.webgl.toInt8Array
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.File
import org.w3c.files.FileReader

/**
 * Web-only logical file registry.
 *
 * Browsers do not expose filesystem paths. Picker/drop/camera results are registered here and the
 * returned `web://…` handle is passed through ViewModels and session snapshots in place of a path.
 * The registry keeps the live [File], an object URL for preview/print, and — once the asynchronous
 * FileReader callbacks complete — the bytes and a real `data:` URI for standalone HTML export.
 */
internal class WebFileEntry(
    val handle: String,
    val file: File?,
    val name: String,
    val mime: String,
    val objectUrl: String,
    var bytes: ByteArray? = null,
    var dataUri: String? = null,
    var image: HTMLImageElement? = null,
    var widthPx: Int = 0,
    var heightPx: Int = 0,
)

internal object WebFileRegistry {

    private var nextId = 1
    private val entries = LinkedHashMap<String, WebFileEntry>()

    private fun storageKey(handle: String) = "tabati:img:" + handle
    private fun metaKey(handle: String) = "tabati:imgmeta:" + handle

    /** Restores an imported browser file after a page reload from localStorage. */
    private fun restore(handle: String): WebFileEntry? {
        val uri = try { window.localStorage.getItem(storageKey(handle)) } catch (_: Throwable) { null }
            ?.takeIf { it.startsWith("data:") } ?: return null
        val meta = try { window.localStorage.getItem(metaKey(handle)) } catch (_: Throwable) { null }
        val parts = meta?.split("x")
        val entry = WebFileEntry(
            handle = handle,
            file = null,
            name = handle.substringAfterLast('/', "image"),
            mime = mimeFromDataUri(uri),
            objectUrl = uri,
            bytes = bytesFromDataUri(uri),
            dataUri = uri,
            widthPx = parts?.getOrNull(0)?.toIntOrNull() ?: 0,
            heightPx = parts?.getOrNull(1)?.toIntOrNull() ?: 0,
        )
        val img = document.createElement("img") as HTMLImageElement
        img.onload = { entry.widthPx = img.naturalWidth; entry.heightPx = img.naturalHeight }
        img.src = uri
        entry.image = img
        entries[handle] = entry
        return entry
    }

    /** Registers a real browser [File] and calls [onReady] once bytes + data URI + decoded image are cached. */
    fun register(file: File, onReady: (WebFileEntry) -> Unit): WebFileEntry {
        val handle = "web://file/${nextId++}/${file.name}"
        val entry = WebFileEntry(
            handle = handle,
            file = file,
            name = file.name,
            mime = file.type.ifBlank { mimeForName(file.name) },
            objectUrl = URL.createObjectURL(file),
        )
        entries[handle] = entry

        var remaining = 3
        fun done() {
            remaining--
            if (remaining == 0) onReady(entry)
        }

        val bytesReader = FileReader()
        bytesReader.onload = {
            val buffer = bytesReader.result as? ArrayBuffer
            if (buffer != null) {
                entry.bytes = Int8Array(buffer, 0, buffer.byteLength).toByteArray()
            }
            done()
        }
        bytesReader.onerror = { done() }
        bytesReader.readAsArrayBuffer(file)

        val uriReader = FileReader()
        uriReader.onload = {
            val value = uriReader.result?.toString()
            if (value != null && value.startsWith("data:")) {
                entry.dataUri = value
                try { window.localStorage.setItem(storageKey(handle), value) } catch (_: Throwable) {}
            }
            done()
        }
        uriReader.onerror = { done() }
        uriReader.readAsDataURL(file)

        val element = document.createElement("img") as HTMLImageElement
        element.onload = {
            entry.widthPx = element.naturalWidth
            entry.heightPx = element.naturalHeight
            entry.image = element
            try { window.localStorage.setItem(metaKey(handle), "${entry.widthPx}x${entry.heightPx}") } catch (_: Throwable) {}
            done()
        }
        element.addEventListener("error", { done() })
        // Decoding the object URL keeps the HTMLImageElement usable for canvas crops/exports.
        element.src = entry.objectUrl

        return entry
    }

    /** Registers bytes produced in-browser (exports, canvas crops). */
    fun registerBytes(name: String, mime: String, bytes: ByteArray): WebFileEntry {
        val handle = "web://export/${nextId++}/$name"
        val entry = WebFileEntry(
            handle = handle,
            file = null,
            name = name,
            mime = mime,
            objectUrl = URL.createObjectURL(blobFrom(bytes.toInt8Array(), mime)),
            bytes = bytes,
        )
        entries[handle] = entry
        return entry
    }

    /** Registers a `data:` / `blob:` / http(s) URL that arrived without going through the picker. */
    fun registerExternal(url: String, mime: String): WebFileEntry {
        val existing = entries.values.firstOrNull { it.objectUrl == url }
        if (existing != null) return existing
        val name = when {
            url.startsWith("data:") -> "embedded." + mime.substringAfter("/", "bin").substringBefore(';')
            else -> url.substringAfterLast('/').substringBefore('?').ifBlank { "external" }
        }
        val handle = "web://external/${nextId++}/$name"
        val entry = WebFileEntry(
            handle = handle,
            file = null,
            name = name,
            mime = mime,
            objectUrl = url,
            bytes = bytesFromDataUri(url),
            dataUri = url.takeIf { it.startsWith("data:") },
        )
        entries[handle] = entry
        return entry
    }

    /** Registers a canvas-produced data URI (used by auto-crop). */
    fun registerData(dataUri: String, name: String, width: Int, height: Int): WebFileEntry {
        val handle = "web://data/${nextId++}/$name"
        val entry = WebFileEntry(
            handle = handle,
            file = null,
            name = name,
            mime = mimeFromDataUri(dataUri),
            objectUrl = dataUri,
            bytes = bytesFromDataUri(dataUri),
            dataUri = dataUri,
            widthPx = width,
            heightPx = height,
        )
        entries[handle] = entry
        return entry
    }

    fun find(handle: String): WebFileEntry? = entries[handle] ?: restore(handle)

    /** Resolves a handle, or lazily registers a raw browser URL. */
    fun resolve(source: String, mime: String): WebFileEntry? {
        val direct = entries[source]
        if (direct != null) return direct
        return when {
            source.startsWith("data:") || source.startsWith("blob:") ||
                source.startsWith("http://") || source.startsWith("https://") ->
                registerExternal(source, mime)
            else -> null
        }
    }

    /** Reads bytes asynchronously when they were not cached yet. */
    fun ensureBytes(entry: WebFileEntry, onReady: (ByteArray?) -> Unit) {
        entry.bytes?.let { onReady(it); return }
        val file = entry.file
        if (file == null) {
            onReady(bytesFromDataUri(entry.objectUrl))
            return
        }
        val reader = FileReader()
        reader.onload = {
            val buffer = reader.result as? ArrayBuffer
            val bytes = if (buffer != null) Int8Array(buffer, 0, buffer.byteLength).toByteArray() else null
            if (bytes != null) entry.bytes = bytes
            onReady(bytes)
        }
        reader.onerror = { onReady(null) }
        reader.readAsArrayBuffer(file)
    }

    /** Base64-encodes bytes into a `data:` URI using the browser's native btoa. */
    fun dataUriOf(entry: WebFileEntry): String {
        val bytes = entry.bytes ?: return entry.objectUrl
        return "data:${entry.mime};base64,${base64Of(bytes)}"
    }

    fun base64Of(bytes: ByteArray): String {
        val binary = StringBuilder(bytes.size)
        for (byte in bytes) binary.append((byte.toInt() and 0xFF).toChar())
        return window.btoa(binary.toString())
    }

    fun bytesFromDataUri(dataUri: String): ByteArray? {
        if (!dataUri.startsWith("data:")) return null
        val comma = dataUri.indexOf(',')
        if (comma < 0 || !dataUri.substring(0, comma).contains("base64")) return null
        return try {
            val binary = window.atob(dataUri.substring(comma + 1))
            ByteArray(binary.length) { binary[it].code.toByte() }
        } catch (_: Throwable) {
            null
        }
    }

    fun mimeFromDataUri(dataUri: String): String {
        val end = dataUri.indexOf(',').takeIf { it > 5 } ?: return "application/octet-stream"
        return dataUri.substring(5, end).substringBefore(';').ifBlank { "application/octet-stream" }
    }

    fun mimeForName(name: String): String = when {
        name.endsWith(".png", ignoreCase = true) -> "image/png"
        name.endsWith(".webp", ignoreCase = true) -> "image/webp"
        name.endsWith(".jpg", ignoreCase = true) || name.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
        name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
        name.endsWith(".html", ignoreCase = true) || name.endsWith(".htm", ignoreCase = true) -> "text/html"
        name.endsWith(".txt", ignoreCase = true) -> "text/plain"
        else -> "application/octet-stream"
    }

    internal fun blobFor(bytes: ByteArray, mime: String): Blob = blobFrom(bytes.toInt8Array(), mime)
}

private fun blobFrom(bytes: Int8Array, mime: String): Blob = js("new Blob([bytes], { type: mime })")
