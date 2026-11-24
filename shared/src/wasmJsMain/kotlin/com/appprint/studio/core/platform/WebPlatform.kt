package com.appprint.studio.core.platform

import com.appprint.studio.core.export.HtmlExport
import com.appprint.studio.core.model.AppTheme
import com.appprint.studio.core.pdf.PdfBuilder
import com.appprint.studio.core.model.CardItem
import com.appprint.studio.core.model.SheetBackground
import com.appprint.studio.core.model.SheetSettings
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8ClampedArray
import org.khronos.webgl.toInt8Array
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLIFrameElement
import org.w3c.dom.get
import org.w3c.dom.set
import org.w3c.files.File
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Browser bridge for the PWA.
 *
 * There is no filesystem: picker/drop results and generated exports live in [WebFileRegistry] and
 * are addressed by logical `web://…` handles. `exportPdf()` now builds a REAL PDF with the shared
 * dependency-free [PdfBuilder]: each sheet is rasterised to a JPEG on a canvas, then wrapped into a
 * DCTDecode PDF with an A4/A5/Letter MediaBox. `printPdf()` still uses printable HTML via @page for
 * the browser print dialog, because browsers cannot reliably print an embedded PDF blob.
 */
actual object PlatformActions {

    private const val K_THEME = "tabati.theme"
    private const val K_SNAPSHOT = "tabati.ui_state"

    /** Last printable HTML used by the browser print dialog (separate from the real PDF bytes). */
    private var printableHtml: String? = null

    // ------------------------------------------------------------- theme / session

    actual fun loadTheme(): AppTheme =
        localStorage[K_THEME]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() } ?: AppTheme.SYSTEM

    actual fun saveTheme(theme: AppTheme) {
        localStorage[K_THEME] = theme.name
    }

    actual fun saveUiSnapshot(json: String) {
        localStorage[K_SNAPSHOT] = json
    }

    actual fun loadUiSnapshot(): String? = localStorage[K_SNAPSHOT]

    actual fun privateDir(name: String): String = "web://$name"

    // ------------------------------------------------------------- import

    actual fun importImage(source: String): ImportedImage? {
        val original = WebFileRegistry.find(source) ?: return null
        // Match Android: trim the scanner bed border at import time when the decoded image allows it.
        val final = autoCropEntry(original, original.widthPx.toLong()) ?: original
        if (final.widthPx <= 0 || final.heightPx <= 0) return null
        return ImportedImage(
            privatePath = final.handle,
            displayName = final.name.substringBeforeLast('.', final.name),
            widthPx = final.widthPx,
            heightPx = final.heightPx,
            cropped = final !== original,
        )
    }

    actual fun displayNameOf(source: String): String? {
        val entry = WebFileRegistry.find(source)
        val raw = entry?.name
            ?: source.substringAfterLast('/').substringBefore('?').takeIf { it.isNotBlank() }
            ?: return null
        return raw.substringBeforeLast('.', raw).takeIf { it.isNotBlank() }
    }

    actual fun imageAsDataUri(path: String): String {
        val entry = WebFileRegistry.find(path)
            ?: return path.takeIf { it.startsWith("data:") || it.startsWith("blob:") || it.startsWith("http") } ?: ""
        // The picker caches a real data URI; fall back to the object URL while FileReader is pending.
        return entry.dataUri ?: WebFileRegistry.dataUriOf(entry)
    }

    // ------------------------------------------------------------- export

    /** Real PDF bytes (DCTDecode/JPEG pages) — same generator contract as Android. */
    actual fun exportPdf(cards: List<CardItem>, settings: SheetSettings, pageCount: Int): ByteArray? = runCatching {
        // Keep printable HTML available for the browser print button only.
        runCatching {
            printableHtml = HtmlExport.build(
                pages = (0 until pageCount).map { page -> cards.filter { it.page == page } },
                paperWidthMm = settings.sheetWidthMm,
                paperHeightMm = settings.sheetHeightMm,
                marginMm = settings.marginMm,
                frame = settings.frame,
                imageSource = { card -> card.imageUri?.let { imageAsDataUri(it) } ?: "" },
            ).decodeToString()
        }

        val dpi = 150
        val pxPerMm = dpi.toDouble() / 25.4
        val widthPx = max(1, (settings.sheetWidthMm.toDouble() * pxPerMm).toInt())
        val heightPx = max(1, (settings.sheetHeightMm.toDouble() * pxPerMm).toInt())
        val pdfPages = (0 until pageCount).map { page ->
            PdfBuilder.Page(
                jpegBytes = renderSheet(page, cards, settings, mime = "image/jpeg", quality = 0.92),
                pixelWidth = widthPx,
                pixelHeight = heightPx,
                widthPt = settings.sheetWidthMm * PdfBuilder.MM_TO_PT,
                heightPt = settings.sheetHeightMm * PdfBuilder.MM_TO_PT,
            )
        }
        PdfBuilder.build(pdfPages, title = "tabati-print")
    }.getOrNull()

    actual fun exportPng(page: Int, cards: List<CardItem>, settings: SheetSettings): ByteArray? =
        runCatching { renderSheet(page, cards, settings) }.getOrNull()

    actual fun printPdf(
        pdfBytes: ByteArray,
        pageCount: Int,
        sheetWidthMm: Float,
        sheetHeightMm: Float,
        jobName: String,
    ): Boolean {
        val html = printableHtml ?: pdfBytes.toHtmlOrNull()
        return printHtml(html)
    }

    actual fun downloadToDownloads(fileName: String, bytes: ByteArray): Boolean = runCatching {
        val name = safeExportName(fileName, bytes)
        val entry = WebFileRegistry.registerBytes(name, WebFileRegistry.mimeForName(name), bytes)
        triggerDownload(entry.objectUrl, name)
        true
    }.getOrDefault(false)

    actual fun writeExport(fileName: String, bytes: ByteArray): String? = runCatching {
        val name = safeExportName(fileName, bytes)
        WebFileRegistry.registerBytes(name, WebFileRegistry.mimeForName(name), bytes).handle
    }.getOrNull()

    actual fun shareFile(path: String, mimeType: String, subject: String?): Boolean {
        val entry = WebFileRegistry.resolve(path, mimeType) ?: return false
        val bytes = entry.bytes ?: return false
        return try {
            val jsFile = jsFileFrom(bytes.toInt8Array(), entry.name, entry.mime)
            if (navigatorShare(jsFile, subject ?: entry.name)) {
                true
            } else {
                triggerDownload(entry.objectUrl, entry.name)
                true
            }
        } catch (_: Throwable) {
            // Web Share needs a user gesture and secure context; a plain download is the fallback.
            triggerDownload(entry.objectUrl, entry.name)
            true
        }
    }

    actual fun previewFile(path: String, mimeType: String): Boolean = runCatching {
        val entry = WebFileRegistry.resolve(path, mimeType) ?: return false
        window.open(entry.objectUrl, "_blank", "") != null
    }.getOrDefault(false)

    actual fun autoCropImage(path: String, id: Long): String? =
        WebFileRegistry.find(path)?.let { autoCropEntry(it, id) }?.handle

    // ------------------------------------------------------------- notifications

    actual fun showExportNotification(title: String, body: String, path: String?) {
        val toast = document.createElement("div")
        toast.setAttribute("role", "status")
        toast.setAttribute("aria-live", "polite")
        toast.setAttribute(
            "style",
            "position:fixed;right:16px;bottom:16px;z-index:2147483647;max-width:340px;" +
                "padding:12px 16px;border-radius:12px;background:#161D19;color:#fff;" +
                "font:600 14px system-ui,sans-serif;box-shadow:0 8px 26px rgba(0,0,0,.35);",
        )
        toast.textContent = if (path.isNullOrBlank()) "$title — $body" else "$title — $body ($path)"
        document.body?.appendChild(toast)
        window.setTimeout({
            try {
                document.body?.removeChild(toast)
            } catch (_: Throwable) {
                // already removed
            }
            null
        }, 4500)
    }

    actual fun exportNotificationPermissionGranted(): Boolean = true
}

// ----------------------------------------------------------------- browser helpers

private fun triggerDownload(url: String, fileName: String) {
    val anchor = document.createElement("a") as org.w3c.dom.HTMLAnchorElement
    anchor.href = url
    anchor.download = fileName
    anchor.setAttribute("style", "display:none")
    document.body?.appendChild(anchor)
    anchor.click()
    window.setTimeout({
        try {
            document.body?.removeChild(anchor)
        } catch (_: Throwable) {
            // already removed
        }
        null
    }, 2000)
}

private fun safeExportName(fileName: String, bytes: ByteArray): String {
    val base = if (isHtmlBytes(bytes) && fileName.endsWith(".pdf", ignoreCase = true)) {
        fileName.substring(0, fileName.length - 4) + ".html"
    } else fileName
    return buildString(base.length) {
        for (ch in base) append(if (ch in "\\/:*?\"<>|") '_' else ch)
    }
}

private fun isHtmlBytes(bytes: ByteArray): Boolean {
    val head = bytes.copyOfRange(0, min(bytes.size, 256))
    return try {
        head.decodeToString().trimStart().startsWith("<!doctype html", ignoreCase = true)
    } catch (_: Throwable) {
        false
    }
}

private fun ByteArray.toHtmlOrNull(): String? = try {
    decodeToString().takeIf { it.contains("<!doctype html", ignoreCase = true) }
} catch (_: Throwable) {
    null
}

private fun printHtml(html: String?): Boolean {
    if (html.isNullOrBlank()) {
        window.print()
        return true
    }
    val frame = document.createElement("iframe") as HTMLIFrameElement
    frame.setAttribute("style", "position:fixed;right:0;bottom:0;width:0;height:0;border:0")
    frame.srcdoc = html

    var printed = false
    fun doPrint() {
        if (printed) return
        printed = true
        val target = frame.contentWindow
        if (target != null) {
            target.focus()
            target.print()
        } else {
            window.print()
        }
        window.setTimeout({
            try {
                document.body?.removeChild(frame)
            } catch (_: Throwable) {
                // already removed
            }
            null
        }, 1500)
    }

    frame.onload = { doPrint() }
    document.body?.appendChild(frame)
    // Safari occasionally skips the srcdoc load event for zero-sized frames.
    window.setTimeout({ doPrint(); null }, 1200)
    return true
}

// ----------------------------------------------------------------- canvas export / crop

private fun renderSheet(
    page: Int,
    cards: List<CardItem>,
    settings: SheetSettings,
    mime: String = "image/png",
    quality: Double? = null,
): ByteArray {
    val dpi = 150
    val pxPerMm = dpi.toDouble() / 25.4
    val width = max(1, (settings.sheetWidthMm.toDouble() * pxPerMm).toInt())
    val height = max(1, (settings.sheetHeightMm.toDouble() * pxPerMm).toInt())
    val canvas = document.createElement("canvas") as HTMLCanvasElement
    canvas.width = width
    canvas.height = height
    val ctx = canvas.getContext("2d") as? CanvasRenderingContext2D
        ?: error("2D canvas context unavailable")

    if (settings.frame.background == SheetBackground.TRANSPARENT) {
        ctx.clearRect(0.0, 0.0, width.toDouble(), height.toDouble())
    } else {
        val bg = if (settings.frame.background == SheetBackground.OFFWHITE) "#fdfaf1" else "#ffffff"
        setFill(ctx, bg)
        ctx.fillRect(0.0, 0.0, width.toDouble(), height.toDouble())
    }

    if (settings.frame.guides) {
        val m = settings.marginMm.toDouble() * pxPerMm
        setStroke(ctx, "rgba(179,38,56,.55)")
        ctx.lineWidth = 1.0
        ctx.strokeRect(
            m, m,
            max(0.0, width.toDouble() - m * 2),
            max(0.0, height.toDouble() - m * 2),
        )
    }

    for (card in cards.filter { it.page == page }) {
        val x = card.xMm.toDouble() * pxPerMm
        val y = card.yMm.toDouble() * pxPerMm
        val cw = max(1.0, card.widthMm.toDouble() * pxPerMm)
        val ch = max(1.0, card.heightMm.toDouble() * pxPerMm)

        ctx.save()
        ctx.translate(x + cw / 2, y + ch / 2)
        ctx.rotate(card.totalRotation.toDouble() * PI / 180.0)
        ctx.translate(-cw / 2, -ch / 2)

        setFill(ctx, if (card.typeKey == "passport") "#17365a" else "#fdfaf1")
        ctx.fillRect(0.0, 0.0, cw, ch)

        val image = card.imageUri?.let { WebFileRegistry.find(it)?.image }
        if (image != null && image.naturalWidth > 0 && image.naturalHeight > 0) {
            val naturalW = image.naturalWidth.toDouble()
            val naturalH = image.naturalHeight.toDouble()
            val scale = max(cw / naturalW, ch / naturalH)
            val sw = cw / scale
            val sh = ch / scale
            val sx = (naturalW - sw) / 2.0
            val sy = (naturalH - sh) / 2.0
            ctx.drawImage(image, sx, sy, sw, sh, 0.0, 0.0, cw, ch)
        } else {
            val fontSize = (ch * 0.16).coerceIn(8.0, 64.0)
            ctx.font = "700 ${fontSize.toInt()}px system-ui, sans-serif"
            setFill(ctx, if (card.typeKey == "passport") "#ffffff" else "#161d19")
            val textWidth = ctx.measureText(card.name).width
            ctx.fillText(card.name, (cw - textWidth) / 2, ch * 0.56, cw)
        }

        if (settings.frame.card && card.border && settings.frame.thickness > 0f && settings.frame.colorHex != 0L) {
            setStroke(ctx, colorHexCss(settings.frame.colorHex))
            ctx.lineWidth = max(1.0, settings.frame.thickness.toDouble())
            ctx.strokeRect(0.0, 0.0, cw, ch)
        }
        ctx.restore()
    }

    val dataUri = canvas.toDataURL(mime, quality)
    return WebFileRegistry.bytesFromDataUri(dataUri) ?: error("Canvas encoding failed ($mime)")
}

/** Port of the scanner-border trim used on Android; returns a new registered crop or null. */
private fun autoCropEntry(entry: WebFileEntry, id: Long): WebFileEntry? {
    val image = entry.image ?: return null
    val w = entry.widthPx
    val h = entry.heightPx
    if (w < 32 || h < 32) return null

    val source = document.createElement("canvas") as HTMLCanvasElement
    source.width = w
    source.height = h
    val ctx = source.getContext("2d") as? CanvasRenderingContext2D ?: return null
    ctx.drawImage(image, 0.0, 0.0, w.toDouble(), h.toDouble())
    val pixels = ctx.getImageData(0.0, 0.0, w.toDouble(), h.toDouble()).data as Uint8ClampedArray

    var sumR = 0.0
    var sumG = 0.0
    var sumB = 0.0
    var samples = 0
    fun addBackground(index: Int) {
        val offset = index * 4
        sumR += (byteAt(pixels, offset) and 0xFF).toDouble()
        sumG += (byteAt(pixels, offset + 1) and 0xFF).toDouble()
        sumB += (byteAt(pixels, offset + 2) and 0xFF).toDouble()
        samples++
    }
    val stepX = max(1, w / 60)
    val stepY = max(1, h / 60)
    for (x in 0 until w step stepX) {
        addBackground(x)
        addBackground((h - 1) * w + x)
    }
    for (y in 0 until h step stepY) {
        addBackground(y * w)
        addBackground(y * w + w - 1)
    }
    if (samples == 0) return null
    val bgR = sumR / samples.toDouble()
    val bgG = sumG / samples.toDouble()
    val bgB = sumB / samples.toDouble()

    fun isBackground(index: Int): Boolean {
        val offset = index * 4
        val dr = (byteAt(pixels, offset) and 0xFF).toDouble() - bgR
        val dg = (byteAt(pixels, offset + 1) and 0xFF).toDouble() - bgG
        val db = (byteAt(pixels, offset + 2) and 0xFF).toDouble() - bgB
        return sqrt(dr * dr + dg * dg + db * db) < 44.0
    }
    fun rowRatio(y: Int): Float {
        var bg = 0
        var total = 0
        for (x in 0 until w step 3) {
            total++
            if (isBackground(y * w + x)) bg++
        }
        return if (total == 0) 0f else bg.toFloat() / total.toFloat()
    }
    fun colRatio(x: Int): Float {
        var bg = 0
        var total = 0
        for (y in 0 until h step 3) {
            total++
            if (isBackground(y * w + x)) bg++
        }
        return if (total == 0) 0f else bg.toFloat() / total.toFloat()
    }

    var top = 0
    var bottom = h - 1
    var left = 0
    var right = w - 1
    while (top < h - 12 && rowRatio(top) > 0.88f) top++
    while (bottom > top + 12 && rowRatio(bottom) > 0.88f) bottom--
    while (left < w - 12 && colRatio(left) > 0.88f) left++
    while (right > left + 12 && colRatio(right) > 0.88f) right--

    val cropW = right - left + 1
    val cropH = bottom - top + 1
    val sourceArea = w.toFloat() * h.toFloat()
    if (
        cropW.toFloat() < w.toFloat() * 0.35f ||
        cropH.toFloat() < h.toFloat() * 0.35f ||
        cropW.toFloat() * cropH.toFloat() > sourceArea * 0.97f
    ) return null

    val out = document.createElement("canvas") as HTMLCanvasElement
    out.width = cropW
    out.height = cropH
    val outCtx = out.getContext("2d") as? CanvasRenderingContext2D ?: return null
    outCtx.drawImage(
        source,
        left.toDouble(), top.toDouble(), cropW.toDouble(), cropH.toDouble(),
        0.0, 0.0, cropW.toDouble(), cropH.toDouble(),
    )
    val dataUri = out.toDataURL("image/png", null)
    val name = entry.name.substringBeforeLast('.', entry.name) + "-crop-$id.png"
    return WebFileRegistry.registerData(dataUri, name, cropW, cropH)
}

/** kotlinx-browser's Uint8ClampedArray has no typed get operator in this release. */
private fun byteAt(pixels: Uint8ClampedArray, index: Int): Int = js("pixels[index]")

private fun colorHexCss(argb: Long): String =
    "#" + (argb and 0xFFFFFFL).toString(16).padStart(6, '0')

private fun setFill(ctx: CanvasRenderingContext2D, color: String): String = js("(ctx.fillStyle = color)")
private fun setStroke(ctx: CanvasRenderingContext2D, color: String): String = js("(ctx.strokeStyle = color)")

private fun jsFileFrom(bytes: Int8Array, name: String, mime: String): File =
    js("new File([bytes], name, { type: mime })")

/** Web Share API is not typed in kotlinx-browser; guarded by try/catch at the call site. */
private fun navigatorShare(file: File, title: String): Boolean =
    js("(navigator.share({ files: [file], title: title }).catch(function () {}), true)")
