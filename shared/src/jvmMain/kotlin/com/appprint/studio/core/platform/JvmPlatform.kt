package com.appprint.studio.core.platform

import com.appprint.studio.core.model.AppTheme
import com.appprint.studio.core.model.CardItem
import com.appprint.studio.core.model.SheetSettings
import java.awt.Desktop
import java.io.File
import java.util.Base64
import java.util.prefs.Preferences

actual object PlatformActions {

    private const val PREF_NODE = "tabati"
    private const val KEY_THEME = "theme"

    actual fun loadTheme(): AppTheme {
        val raw = runCatching {
            Preferences.userRoot().node(PREF_NODE).get(KEY_THEME, AppTheme.SYSTEM.name)
        }.getOrNull()
        return runCatching { AppTheme.valueOf(raw!!) }.getOrDefault(AppTheme.SYSTEM)
    }

    actual fun saveTheme(theme: AppTheme) {
        runCatching { Preferences.userRoot().node(PREF_NODE).put(KEY_THEME, theme.name) }
    }

    actual fun privateDir(name: String): String {
        val base = File(System.getProperty("user.home"), ".tabati/$name")
        base.mkdirs()
        return base.absolutePath
    }

    actual fun importImage(source: String): ImportedImage? {
        return runCatching {
            val src = File(source.removePrefix("file://"))
            if (!src.exists()) return null
            val dir = File(privateDir("imports"))
            val stamp = System.currentTimeMillis()
            val dest = File(dir, "import-$stamp.jpg")
            src.copyTo(dest, overwrite = true)
            val bounds = javax.imageio.ImageIO.read(dest)
            if (bounds == null) null else ImportedImage(dest.absolutePath, src.nameWithoutExtension, bounds.width, bounds.height, cropped = false)
        }.getOrNull()
    }

    actual fun displayNameOf(source: String): String? =
        File(source.removePrefix("file://")).nameWithoutExtension.takeIf { it.isNotBlank() }

    actual fun imageAsDataUri(path: String): String {
        return runCatching {
            val bytes = File(path.removePrefix("file://")).readBytes()
            if (bytes.isEmpty()) return ""
            val mime = when {
                path.endsWith(".png", ignoreCase = true) -> "image/png"
                path.endsWith(".webp", ignoreCase = true) -> "image/webp"
                else -> "image/jpeg"
            }
            "data:$mime;base64," + Base64.getEncoder().encodeToString(bytes)
        }.getOrElse { "" }
    }

    actual fun downloadToDownloads(fileName: String, bytes: ByteArray): Boolean {
        // Desktop verification build — save next to the user's real Downloads folder.
        return runCatching {
            val downloads = File(System.getProperty("user.home"), "Downloads")
            val dir = if (downloads.exists() && downloads.isDirectory) downloads else File(privateDir("downloads"))
            val file = File(dir, fileName.replace(Regex("[\\\\/:*?\"<>|]"), "_"))
            file.writeBytes(bytes)
            true
        }.getOrDefault(false)
    }

    actual fun exportPdf(cards: List<CardItem>, settings: SheetSettings, pageCount: Int): ByteArray? {
        // Verification-target renderer: simple but functional — enough for local PDF smoke checks.
        return runCatching {
            val pages = (0 until pageCount).map { page ->
                val image = renderSheet(page, cards, settings)
                val out = java.io.ByteArrayOutputStream()
                javax.imageio.ImageIO.write(image, "jpg", out)
                com.appprint.studio.core.pdf.PdfBuilder.Page(
                    jpegBytes = out.toByteArray(),
                    pixelWidth = image.width,
                    pixelHeight = image.height,
                    widthPt = settings.sheetWidthMm * com.appprint.studio.core.pdf.PdfBuilder.MM_TO_PT,
                    heightPt = settings.sheetHeightMm * com.appprint.studio.core.pdf.PdfBuilder.MM_TO_PT,
                )
            }
            com.appprint.studio.core.pdf.PdfBuilder.build(pages, "tabati-print")
        }.getOrNull()
    }

    actual fun exportPng(page: Int, cards: List<CardItem>, settings: SheetSettings): ByteArray? {
        return runCatching {
            val image = renderSheet(page, cards, settings)
            val out = java.io.ByteArrayOutputStream()
            javax.imageio.ImageIO.write(image, "png", out)
            out.toByteArray()
        }.getOrNull()
    }

    private fun renderSheet(page: Int, cards: List<CardItem>, settings: SheetSettings): java.awt.image.BufferedImage {
        val dpi = 150
        val pxPerMm = dpi / 25.4f
        val w = maxOf(1, (settings.sheetWidthMm * pxPerMm).toInt())
        val h = maxOf(1, (settings.sheetHeightMm * pxPerMm).toInt())
        val image = java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.color = when (settings.frame.background) {
            com.appprint.studio.core.model.SheetBackground.WHITE -> java.awt.Color.WHITE
            com.appprint.studio.core.model.SheetBackground.OFFWHITE -> java.awt.Color(0xFDFAF1)
            com.appprint.studio.core.model.SheetBackground.TRANSPARENT -> java.awt.Color.WHITE
        }
        g.fillRect(0, 0, w, h)
        if (settings.frame.guides) {
            g.color = java.awt.Color(179, 38, 56, 120)
            g.stroke = java.awt.BasicStroke(3f, java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_MITER, 10f, floatArrayOf(22f, 22f), 0f)
            g.drawRect(
                (settings.marginMm * pxPerMm).toInt(),
                (settings.marginMm * pxPerMm).toInt(),
                ((settings.sheetWidthMm - settings.marginMm * 2) * pxPerMm).toInt(),
                ((settings.sheetHeightMm - settings.marginMm * 2) * pxPerMm).toInt(),
            )
        }
        for (card in cards.filter { it.page == page }) {
            val x = (card.xMm * pxPerMm).toInt()
            val y = (card.yMm * pxPerMm).toInt()
            val cw = (card.widthMm * pxPerMm).toInt()
            val ch = (card.heightMm * pxPerMm).toInt()
            g.color = java.awt.Color(0xFDFAF1)
            g.fillRoundRect(x, y, cw, ch, 12, 12)
            g.color = java.awt.Color(0x006C47)
            g.stroke = java.awt.BasicStroke(2f)
            g.drawRoundRect(x, y, cw, ch, 12, 12)
            g.color = java.awt.Color(0x161D19)
            g.font = g.font.deriveFont(java.awt.Font.BOLD, (ch * 0.16f).coerceIn(8f, 64f))
            val fm = g.fontMetrics
            g.drawString(card.name, x + (cw - fm.stringWidth(card.name)) / 2, y + ch / 2)
        }
        g.dispose()
        return image
    }

    actual fun printPdf(
        pdfBytes: ByteArray,
        pageCount: Int,
        sheetWidthMm: Float,
        sheetHeightMm: Float,
        jobName: String,
    ): Boolean {
        // Desktop verification build: no system print service — save next to exports instead.
        return runCatching {
            val file = File(privateDir("exports"), "$jobName.pdf")
            file.writeBytes(pdfBytes)
            true
        }.getOrDefault(false)
    }

    actual fun shareFile(path: String, mimeType: String, subject: String?): Boolean {
        return runCatching {
            val file = File(path)
            if (!file.exists()) return false
            if (Desktop.isDesktopSupported()) {
                runCatching { Desktop.getDesktop().open(file) }
                true
            } else false
        }.getOrDefault(false)
    }

    actual fun writeExport(fileName: String, bytes: ByteArray): String? {
        return runCatching {
            val dir = File(privateDir("exports"))
            val file = File(dir, fileName)
            file.writeBytes(bytes)
            file.absolutePath
        }.getOrNull()
    }

    actual fun autoCropImage(path: String, id: Long): String? = null // not supported on desktop verification target

    actual fun showExportNotification(title: String, body: String, path: String?) {
        // Desktop: no system notification service wired — log for the verification build.
        println("[tabati] $title — $body (${path ?: "no file"})")
    }
}
