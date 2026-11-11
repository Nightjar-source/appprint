package com.appprint.studio.core.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PageRange
import android.print.PrintManager
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import com.appprint.studio.core.export.SheetRenderer
import com.appprint.studio.core.image.Enhance
import com.appprint.studio.core.model.AppTheme
import com.appprint.studio.core.model.CardItem
import com.appprint.studio.core.model.SheetSettings
import com.appprint.studio.core.pdf.PdfBuilder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

actual object PlatformActions {

    private const val PREFS = "appprint_prefs"
    private const val KEY_THEME = "theme"
    private const val EXPORTS_DIR = "exports"
    private const val IMPORTS_DIR = "imports"
    private const val NOTIF_CHANNEL_ID = "appprint_exports"

    // ------------------------------------------------------------- theme

    actual fun loadTheme(): AppTheme {
        val context = appContext() ?: return AppTheme.SYSTEM
        val raw = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .getString(KEY_THEME, AppTheme.SYSTEM.name)
        return runCatching { AppTheme.valueOf(raw!!) }.getOrDefault(AppTheme.SYSTEM)
    }

    actual fun saveTheme(theme: AppTheme) {
        val context = appContext() ?: return
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, theme.name).apply()
    }

    // ------------------------------------------------------------- filesystem

    actual fun privateDir(name: String): String {
        val context = appContext() ?: return name
        return File(context.filesDir, name).apply { mkdirs() }.absolutePath
    }

    actual fun writeExport(fileName: String, bytes: ByteArray): String? {
        return runCatching {
            val dir = File(privateDir(EXPORTS_DIR))
            val file = File(dir, fileName)
            FileOutputStream(file).use { it.write(bytes) }
            file.absolutePath
        }.getOrNull()
    }

    // ------------------------------------------------------------- import

    actual fun displayNameOf(source: String): String? {
        val context = appContext() ?: return null
        return runCatching {
            val uri = Uri.parse(source)
            if (source.startsWith("content:")) {
                context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            } else {
                File(source.removePrefix("file://")).nameWithoutExtension
            }
        }.getOrNull()
    }

    actual fun importImage(source: String): ImportedImage? {
        val context = appContext() ?: return null
        return runCatching {
            val dir = File(privateDir(IMPORTS_DIR))
            val stamp = System.currentTimeMillis()
            val dest = File(dir, "import-$stamp.jpg")

            val uri = runCatching { Uri.parse(source) }.getOrNull()
            val input = when {
                uri != null && (source.startsWith("content:") || source.startsWith("file:")) ->
                    context.contentResolver.openInputStream(uri)
                else -> File(source.removePrefix("file://")).takeIf { it.exists() }?.let { FileInputStream(it) }
            } ?: return null

            input.use { stream ->
                FileOutputStream(dest).use { output -> stream.copyTo(output) }
            }

            val name = displayNameOf(source)?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: "بطاقة-$stamp"

            // bounds first
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(dest.absolutePath, bounds)
            if (bounds.outWidth <= 0) return null

            // auto-crop white scanner borders in place
            var cropped = false
            var finalW = bounds.outWidth
            var finalH = bounds.outHeight
            Enhance.decodeSampled(context, dest.absolutePath, maxDimensionPx = 4096)?.let { decoded ->
                val croppedBitmap = Enhance.cropToContent(decoded)
                if (croppedBitmap !== decoded) {
                    FileOutputStream(dest).use { out ->
                        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                    cropped = true
                    finalW = croppedBitmap.width
                    finalH = croppedBitmap.height
                    croppedBitmap.recycle()
                }
                decoded.recycle()
            }

            ImportedImage(
                privatePath = dest.absolutePath,
                displayName = name,
                widthPx = finalW,
                heightPx = finalH,
                cropped = cropped,
            )
        }.getOrNull()
    }

    // ------------------------------------------------------------- export

    actual fun exportPdf(cards: List<CardItem>, settings: SheetSettings, pageCount: Int): ByteArray? {
        val context = appContext() ?: return null
        return runCatching {
            val pages = (0 until pageCount).map { page ->
                val rendered = SheetRenderer.renderPageJpeg(context, page, cards, settings)
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
        val context = appContext() ?: return null
        return runCatching {
            val bitmap = SheetRenderer.renderPageBitmap(context, page, cards, settings)
            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            bitmap.recycle()
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
        val context = appContext() ?: return false
        return runCatching {
            val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as? PrintManager
                ?: return false
            val tempDir = context.cacheDir
            val file = File(tempDir, "$jobName.pdf")
            FileOutputStream(file).use { it.write(pdfBytes) }

            val attrs = PrintAttributes.Builder()
                .setMediaSize(
                    PrintAttributes.MediaSize(
                        "custom_${sheetWidthMm}x$sheetHeightMm",
                        "Custom ${sheetWidthMm}x${sheetHeightMm} mm",
                        mmToMil(sheetWidthMm),
                        mmToMil(sheetHeightMm),
                    )
                )
                .setResolution(PrintAttributes.Resolution("pdf300", "300 dpi", 300, 300))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()

            printManager.print(jobName, PdfBytesPrintAdapter(file, pageCount, jobName), attrs)
            true
        }.getOrDefault(false)
    }

    actual fun shareFile(path: String, mimeType: String, subject: String?): Boolean {
        val context = appContext() ?: return false
        return runCatching {
            val file = File(path)
            if (!file.exists()) return false
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND)
                .setType(mimeType)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            if (subject != null) intent.putExtra(Intent.EXTRA_SUBJECT, subject)
            context.startActivity(Intent.createChooser(intent, subject ?: "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)
    }

    actual fun showExportNotification(title: String, body: String, path: String?) {
        val context = appContext() ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
                if (!granted) return
            }
            val manager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(NOTIF_CHANNEL_ID, "تنبيهات التصدير · مطبعة البطاقات", NotificationManager.IMPORTANCE_DEFAULT)
                manager.createNotificationChannel(channel)
            }
            val contentUri = path?.let { filePath ->
                runCatching {
                    val file = File(filePath)
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                }.getOrNull()
            }
            val mime = when (path?.substringAfterLast('.', "")?.lowercase()) {
                "png" -> "image/png"
                "jpg", "jpeg" -> "image/jpeg"
                "pdf" -> "application/pdf"
                else -> "*/*"
            }
            // ACTION_VIEW with an explicit MIME — the system resolves any capable app
            // (readers, browsers for PDFs, photo viewers for images) or asks the user to pick.
            val viewIntent = contentUri?.let {
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(it, mime)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val pendingIntent = viewIntent?.let {
                android.app.PendingIntent.getActivity(
                    context,
                    0,
                    it,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
                )
            }
            val notification = androidx.core.app.NotificationCompat.Builder(context, NOTIF_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setAutoCancel(true)
                .apply { pendingIntent?.let { setContentIntent(it) } }
                .build()
            manager.notify(1001, notification)
        } catch (_: SecurityException) {
            // notification permission denied — export still succeeds silently
        } catch (_: Throwable) {
            // notifications are best-effort; never break the export flow
        }
    }

    private fun appContext() = AndroidPlatform.takeIf { it.isSet }?.appContext

    private fun mmToMil(mm: Float): Int = (mm / 25.4f * 1000f).toInt()

    /** Print adapter that hands the already-generated PDF bytes to the print spool. */
    private class PdfBytesPrintAdapter(
        private val pdfFile: File,
        private val pageCount: Int,
        private val jobName: String,
    ) : PrintDocumentAdapter() {

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancelSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?,
        ) {
            if (cancelSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }
            val info = PrintDocumentInfo.Builder("$jobName.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(pageCount)
                .build()
            callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pages: Array<out PageRange>,
            destination: ParcelFileDescriptor,
            cancelSignal: CancellationSignal?,
            callback: WriteResultCallback,
        ) {
            runCatching {
                pdfFile.inputStream().use { input ->
                    FileOutputStream(destination.fileDescriptor).use { output -> input.copyTo(output) }
                }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            }.onFailure { callback.onWriteFailed(it.message) }
        }
    }
}
