package com.appprint.studio.core.platform

import com.appprint.studio.core.model.AppTheme
import com.appprint.studio.core.model.CardItem
import com.appprint.studio.core.model.SheetSettings

/**
 * Platform bridge — every expect here has exactly one actual per shipped target.
 * Android: full implementation (Photo Picker, camera, print framework, FileProvider).
 * Desktop JVM: full implementation (AWT dialogs, javax.print) — used for verification builds.
 * iOS: declared; graceful no-ops until the iosApp is wired on macOS.
 * Fully offline everywhere: NO network capability is declared or used.
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

    // --- theme persistence (the design's «تلقائي/فاتح/داكن» radio) ---
    fun loadTheme(): AppTheme
    fun saveTheme(theme: AppTheme)

    /** Private directory inside the app sandbox; created on demand. Absolute path. */
    fun privateDir(name: String): String

    /** Copies [source] (content URI / file path) into private storage, auto-cropping white scanner borders. */
    fun importImage(source: String): ImportedImage?

    /** Display name of a picked/captured source, best effort. */
    fun displayNameOf(source: String): String?

    /** Reads a private image file and returns `data:image/…;base64,…` for standalone HTML export. */
    fun imageAsDataUri(path: String): String

    /** Renders every sheet at 300 DPI into a single PDF byte array. Null on failure. */
    fun exportPdf(cards: List<CardItem>, settings: SheetSettings, pageCount: Int): ByteArray?

    /** Renders one sheet at 300 DPI into PNG bytes. */
    fun exportPng(page: Int, cards: List<CardItem>, settings: SheetSettings): ByteArray?

    /** Sends the PDF to the system print framework. True when accepted by the spooler. */
    fun printPdf(pdfBytes: ByteArray, pageCount: Int, sheetWidthMm: Float, sheetHeightMm: Float, jobName: String): Boolean

    /** Shares a file from private storage via the platform share flow (FileProvider on Android). */
    fun shareFile(path: String, mimeType: String, subject: String?): Boolean

    /** Writes bytes to the private exports directory; absolute path or null. */
    fun writeExport(fileName: String, bytes: ByteArray): String?

    /** Auto-crop white scanner borders of a private image. Returns new path, or null when clean/unsupported. */
    fun autoCropImage(path: String, id: Long): String?

    /** System notification: export finished. No-op where unsupported. */
    fun showExportNotification(title: String, body: String, path: String?)
}
