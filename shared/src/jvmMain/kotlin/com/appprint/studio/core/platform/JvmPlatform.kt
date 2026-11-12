package com.appprint.studio.core.platform

import com.appprint.studio.core.export.HtmlExport
import com.appprint.studio.core.export.JvmSheetRenderer
import com.appprint.studio.core.model.AppTheme
import com.appprint.studio.core.model.CardItem
import com.appprint.studio.core.model.SheetBackground
import com.appprint.studio.core.model.SheetSettings
import com.appprint.studio.core.pdf.PdfBuilder
import java.awt.Desktop
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.prefs.Preferences
import javax.imageio.ImageIO
import javax.imageio.stream.ImageInputStream
import javax.print.Doc
import javax.print.DocFlavor
import javax.print.PrintServiceLookup
import javax.print.SimpleDoc
import javax.print.attribute.HashPrintRequestAttributeSet
import javax.print.attribute.standard.MediaSize
import javax.print.attribute.standard.OrientationRequested

actual object PlatformActions {

    private const val PREF_NODE = "appprint"
    private const val KEY_THEME = "theme"

    // ------------------------------------------------------------- theme

    actual fun loadTheme(): AppTheme {
        val raw = Preferences.userRoot().node(PREF_NODE).get(KEY_THEME, AppTheme.SYSTEM.name)
        return runCatching { AppTheme.valueOf(raw) }.getOrDefault(AppTheme.SYSTEM)
    }

    actual fun saveTheme(theme: AppTheme) {
        Preferences.userRoot().node(PREF_NODE).put(KEY_THEME, theme.name)
    }

    // ------------------------------------------------------------- filesystem

    actual fun privateDir(name: String): String {
        val dir = File(File(System.getProperty("user.home"), ".appprint"), name).apply { mkdirs() }
        return dir.absolutePath
    }

    actual fun writeExport(fileName: String, bytes: ByteArray): String? {
        return runCatching {
            val file = File(privateDir("exports"), fileName)
            FileOutputStream(file).use { it.write(bytes) }
            file.absolutePath
        }.getOrNull()
    }

    // ------------------------------------------------------------- import

    actual fun displayNameOf(source: String): String? =
        File(source.removePrefix("file://")).nameWithoutExtension.takeIf { it.isNotBlank() }

    actual fun importImage(source: String): ImportedImage? {
        return runCatching {
            val cleanPath = source.removePrefix("file://")
            val input = File(cleanPath).takeIf { it.exists() } ?: return null
            val dir = File(privateDir("imports"))
            val dest = File(dir, "import-${System.currentTimeMillis()}.jpg")
            Files.copy(input.toPath(), dest.toPath())

            // bounds without full decode (jpeg/png/bmp/gif)
            var width = -1
            var height = -1
            runCatching {
                val stream: ImageInputStream = ImageIO.createImageInputStream(dest)
                val readers = ImageIO.getImageReaders(stream)
                if (readers.hasNext()) {
                    val reader = readers.next()
                    reader.input = stream
                    width = reader.getWidth(0)
                    height = reader.getHeight(0)
                    reader.dispose()
                    stream.close()
                }
            }
            if (width <= 0 || height <= 0) return null
            val originalW = width
            val originalH = height

            // auto-crop white scanner borders in place
            var cropped = false
            runCatching {
                val img = ImageIO.read(dest) ?: return@runCatching
                val px = IntArray(img.width * img.height)
                img.getRGB(0, 0, img.width, img.height, px, 0, img.width)
                val bounds = com.appprint.studio.core.image.PixelOps.contentBounds(px, img.width, img.height)
                if (bounds != null &&
                    (bounds.width < img.width * 0.98 || bounds.height < img.height * 0.98) &&
                    bounds.width >= 16 && bounds.height >= 16
                ) {
                    val sub = img.getSubimage(bounds.left, bounds.top, bounds.width, bounds.height)
                    ImageIO.write(sub, "jpg", dest)
                    cropped = true
                    width = bounds.width
                    height = bounds.height
                }
            }

            ImportedImage(
                privatePath = dest.absolutePath,
                displayName = displayNameOf(source) ?: "card-${System.currentTimeMillis()}",
                widthPx = if (cropped) width else originalW,
                heightPx = if (cropped) height else originalH,
                cropped = cropped,
            )
        }.getOrNull()
    }

    // ------------------------------------------------------------- export

    actual fun exportHtml(cards: List<CardItem>, settings: SheetSettings, pageCount: Int): ByteArray? {
        return runCatching {
            val pages = (0 until pageCount).map { page ->
                val rendered = JvmSheetRenderer.renderPageJpeg(page, cards, settings)
                HtmlExport.EmbeddedPage(
                    jpegBytes = rendered.jpegBytes,
                    widthPt = rendered.widthPt,
                    heightPt = rendered.heightPt,
                )
            }
            HtmlExport.buildPrintDocument(pages, settings.sheetWidthMm, settings.sheetHeightMm, title = "iraqi-cards-print")
        }.getOrNull()
    }

    actual fun exportPdf(cards: List<CardItem>, settings: SheetSettings, pageCount: Int): ByteArray? {
        return runCatching {
            val pages = (0 until pageCount).map { page ->
                val rendered = JvmSheetRenderer.renderPageJpeg(page, cards, settings)
                PdfBuilder.Page(
                    jpegBytes = rendered.jpegBytes,
                    pixelWidth = rendered.pixelWidth,
                    pixelHeight = rendered.pixelHeight,
                    widthPt = rendered.widthPt,
                    heightPt = rendered.heightPt,
                )
            }
            PdfBuilder.build(pages, title = "iraqi-cards-print")
        }.getOrNull()
    }

    actual fun exportPng(page: Int, cards: List<CardItem>, settings: SheetSettings): ByteArray? {
        return runCatching {
            val image = JvmSheetRenderer.renderPage(page, cards, settings)
            val stream = ByteArrayOutputStream()
            ImageIO.write(image, "png", stream)
            stream.toByteArray()
        }.getOrNull()
    }

    actual fun printPdf(
        pdfBytes: ByteArray,
        pageCount: Int,
        sheetWidthMm: Float,
        sheetHeightMm: Float,
        jobName: String,
    ): Boolean {
        // write a temp copy; PDF printers are consumed by service name when available,
        // otherwise the system viewer handles "print" from the OS dialog.
        val tmp = File.createTempFile(jobName, ".pdf").apply { deleteOnExit() }
        FileOutputStream(tmp).use { it.write(pdfBytes) }

        val attrs = HashPrintRequestAttributeSet().apply {
            add(OrientationRequested.PORTRAIT)
            add(MediaSize(sheetWidthMm / 25.4f, sheetHeightMm / 25.4f, MediaSize.MM))
        }
        val services = runCatching {
            PrintServiceLookup.lookupPrintServices(DocFlavor.INPUT_STREAM.PDF, attrs)
        }.getOrDefault(emptyArray())

        return runCatching {
            if (services.isNotEmpty()) {
                val service = services.first()
                val doc: Doc = SimpleDoc(FileInputStream(tmp), DocFlavor.INPUT_STREAM.PDF, null)
                service.createPrintJob().print(doc, attrs)
                true
            } else if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(tmp)
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    actual fun shareFile(path: String, mimeType: String, subject: String?): Boolean {
        val file = File(path)
        if (!file.exists()) return false
        return runCatching {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file)
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    actual fun showExportNotification(title: String, body: String, path: String?) {
        // Desktop: best-effort console line; the file is saved next to ~/.appprint anyway.
        println("[AppPrint] $title — $body" + (path?.let { " ($it)" } ?: ""))
    }
}
