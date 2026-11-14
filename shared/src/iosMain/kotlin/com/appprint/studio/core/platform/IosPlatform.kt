package com.appprint.studio.core.platform

import com.appprint.studio.core.model.AppTheme
import com.appprint.studio.core.model.CardItem
import com.appprint.studio.core.model.SheetSettings
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory

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

    actual fun importImage(source: String): ImportedImage? = null // placeholders — wire iosApp on macOS

    actual fun displayNameOf(source: String): String? = null

    actual fun imageAsDataUri(path: String): String = ""

    actual fun exportPdf(cards: List<CardItem>, settings: SheetSettings, pageCount: Int): ByteArray? = null

    actual fun exportPng(page: Int, cards: List<CardItem>, settings: SheetSettings): ByteArray? = null

    actual fun printPdf(
        pdfBytes: ByteArray,
        pageCount: Int,
        sheetWidthMm: Float,
        sheetHeightMm: Float,
        jobName: String,
    ): Boolean = false

    actual fun shareFile(path: String, mimeType: String, subject: String?): Boolean = false

    actual fun writeExport(fileName: String, bytes: ByteArray): String? = null

    actual fun autoCropImage(path: String, id: Long): String? = null

    actual fun showExportNotification(title: String, body: String, path: String?) = Unit
}
