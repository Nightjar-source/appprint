package com.appprint.studio.core.platform

import com.appprint.studio.core.model.AppTheme
import com.appprint.studio.core.model.CardItem
import com.appprint.studio.core.model.SheetSettings

/**
 * Platform bridge — every expect here is implemented per target.
 * Android: full implementation (camera, print framework, FileProvider sharing).
 * Desktop JVM: full implementation (AWT dialogs, javax.print, Skia rasterization).
 * iOS: targets declared; actuals fail gracefully until the iosApp is wired on macOS.
 */

/** Result of copying a user-picked/captured image into private storage. */
data class ImportedImage(
    val privatePath: String,
    val displayName: String,
    val widthPx: Int,
    val heightPx: Int,
    val cropped: Boolean,
)

expect object PlatformActions {

    // --- settings persistence (the «مظهر الأداة» radio from the design) ---
    fun loadTheme(): AppTheme
    fun saveTheme(theme: AppTheme)

    /** Private directory inside the app sandbox; created on demand. Returns absolute path. */
    fun privateDir(name: String): String

    /**
     * Copies [source] (content Uri string / file path / capture path) into private storage,
     * auto-cropping white scanner borders. Returns null on failure.
     */
    fun importImage(source: String): ImportedImage?

    /** Display name of a picked/captured source, best effort. */
    fun displayNameOf(source: String): String?

    /** Renders every sheet at 300 DPI into a single PDF byte array. */
    fun exportPdf(cards: List<CardItem>, settings: SheetSettings, pageCount: Int): ByteArray?

    /** Renders one sheet at 300 DPI into PNG bytes. */
    fun exportPng(page: Int, cards: List<CardItem>, settings: SheetSettings): ByteArray?

    /** Sends the PDF to the system print framework. Returns true when accepted. */
    fun printPdf(pdfBytes: ByteArray, pageCount: Int, sheetWidthMm: Float, sheetHeightMm: Float, jobName: String): Boolean

    /** Shares a file from private storage via the platform share flow. */
    fun shareFile(path: String, mimeType: String, subject: String?): Boolean

    /** Writes bytes to the private exports directory; returns the absolute path or null. */
    fun writeExport(fileName: String, bytes: ByteArray): String?

    /** System notification: download/print finished. No-op where unsupported. */
    fun showExportNotification(title: String, body: String, path: String?)
}
