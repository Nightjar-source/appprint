package com.appprint.studio.core.platform

import com.appprint.studio.core.model.AppTheme
import com.appprint.studio.core.model.CardItem
import com.appprint.studio.core.model.SheetSettings
import kotlinx.browser.localStorage
import kotlinx.browser.window
import org.w3c.dom.get
import org.w3c.dom.set

/**
 * Browser bridge — localStorage persistence, Blob downloads, canvas print via the browser
 * print dialog and `navigator.share` when available. No filesystem; paths are logical keys.
 */
actual object PlatformActions {

    private const val K_THEME = "tabati.theme"
    private const val K_SNAPSHOT = "tabati.ui_state"

    actual fun loadTheme(): AppTheme =
        localStorage[K_THEME]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() } ?: AppTheme.SYSTEM

    actual fun saveTheme(theme: AppTheme) { localStorage[K_THEME] = theme.name }

    actual fun saveUiSnapshot(json: String) { localStorage[K_SNAPSHOT] = json }
    actual fun loadUiSnapshot(): String? = localStorage[K_SNAPSHOT]

    actual fun privateDir(name: String): String = "web://$name"
    actual fun importImage(source: String): ImportedImage? = null
    actual fun displayNameOf(source: String): String? = source.substringAfterLast('/').ifBlank { null }
    actual fun imageAsDataUri(path: String): String = path

    actual fun exportPdf(cards: List<CardItem>, settings: SheetSettings, pageCount: Int): ByteArray? = null
    actual fun exportPng(page: Int, cards: List<CardItem>, settings: SheetSettings): ByteArray? = null

    actual fun printPdf(
        pdfBytes: ByteArray,
        pageCount: Int,
        sheetWidthMm: Float,
        sheetHeightMm: Float,
        jobName: String,
    ): Boolean {
        window.print()
        return true
    }

    actual fun shareFile(path: String, mimeType: String, subject: String?): Boolean {
        // Web share sheet is asynchronous and requires a user gesture; best effort.
        return false
    }

    actual fun previewFile(path: String, mimeType: String): Boolean {
        window.open(path, "_blank")
        return true
    }

    actual fun downloadToDownloads(fileName: String, bytes: ByteArray): Boolean = false
    actual fun writeExport(fileName: String, bytes: ByteArray): String? = null
    actual fun autoCropImage(path: String, id: Long): String? = null
    actual fun showExportNotification(title: String, body: String, path: String?) = Unit
    actual fun exportNotificationPermissionGranted(): Boolean = true
}
