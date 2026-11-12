package com.appprint.studio.core.platform

import com.appprint.studio.core.model.AppTheme
import com.appprint.studio.core.model.CardItem
import com.appprint.studio.core.model.SheetSettings
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite

/**
 * iOS actuals — the module compiles for iOS targets, but the app is not shipped for iOS yet.
 * Behaviour is intentionally graceful: imports/exports/printing are unavailable,
 * samples and the whole UI work, nothing crashes.
 *
 * To enable iOS: implement render/print with UIGraphicsBeginPDFContext /
 * UIPrintInteractionController and picking with PHPickerViewController, then add an
 * iosApp module (see README / kmp-wizard conventions).
 */
actual object PlatformActions {

    actual fun loadTheme(): AppTheme = AppTheme.SYSTEM
    actual fun saveTheme(theme: AppTheme) = Unit

    actual fun privateDir(name: String): String {
        val base = NSTemporaryDirectory().removeSuffix("/")
        val dir = "$base/$name"
        NSFileManager.defaultManager.createDirectoryAtPath(
            path = dir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        return dir
    }

    actual fun importImage(source: String): ImportedImage? {
        println("[AppPrint] iOS import is not wired yet — build the iosApp on macOS")
        return null
    }

    actual fun displayNameOf(source: String): String? =
        source.substringAfterLast('/').substringBeforeLast('.').takeIf { it.isNotBlank() }

    actual fun exportHtml(cards: List<CardItem>, settings: SheetSettings, pageCount: Int): ByteArray? {
        println("[AppPrint] iOS HTML export is not wired yet")
        return null
    }

    actual fun exportPdf(cards: List<CardItem>, settings: SheetSettings, pageCount: Int): ByteArray? {
        println("[AppPrint] iOS PDF export is not wired yet")
        return null
    }

    actual fun exportPng(page: Int, cards: List<CardItem>, settings: SheetSettings): ByteArray? {
        println("[AppPrint] iOS PNG export is not wired yet")
        return null
    }

    actual fun printPdf(
        pdfBytes: ByteArray,
        pageCount: Int,
        sheetWidthMm: Float,
        sheetHeightMm: Float,
        jobName: String,
    ): Boolean {
        println("[AppPrint] iOS printing is not wired yet")
        return false
    }

    actual fun shareFile(path: String, mimeType: String, subject: String?): Boolean = false

    actual fun writeExport(fileName: String, bytes: ByteArray): String? {
        val path = privateDir("exports") + "/" + fileName
        val fp = fopen(path, "wb") ?: return null
        val written = fwrite(bytes.refTo(0), 1u, bytes.size.toULong(), fp)
        fclose(fp)
        return if (written == bytes.size.toULong()) path else null
    }

    actual fun showExportNotification(title: String, body: String, path: String?) {
        println("[AppPrint] $title — $body")
    }
}
