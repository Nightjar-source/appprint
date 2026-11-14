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
import android.print.PrintManager
import android.provider.MediaStore
import android.util.Base64
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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

    private const val PREFS = "tabati_prefs"
    private const val KEY_THEME = "theme"
    private const val EXPORTS_DIR = "exports"
    private const val IMPORTS_DIR = "imports"
    private const val NOTIF_CHANNEL_ID = "tabati_exports"

    // ------------------------------------------------------------- theme

    actual fun loadTheme(): AppTheme {
        val context = AndroidPlatform.appContext
        val raw = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .getString(KEY_THEME, AppTheme.SYSTEM.name)
        return runCatching { AppTheme.valueOf(raw!!) }.getOrDefault(AppTheme.SYSTEM)
    }

    actual fun saveTheme(theme: AppTheme) {
        AndroidPlatform.appContext
            .getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, theme.name).apply()
    }

    // ------------------------------------------------------------- filesystem

    actual fun privateDir(name: String): String =
        File(AndroidPlatform.appContext.filesDir, name).apply { mkdirs() }.absolutePath

    actual fun writeExport(fileName: String, bytes: ByteArray): String? = runCatching {
        val dir = File(AndroidPlatform.appContext.filesDir, EXPORTS_DIR).apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { it.write(bytes) }
        file.absolutePath
    }.getOrNull()

    // ------------------------------------------------------------- import

    actual fun displayNameOf(source: String): String? {
        val context = AndroidPlatform.appContext
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
        val context = AndroidPlatform.appContext
        return runCatching {
            val dir = File(AndroidPlatform.appContext.filesDir, IMPORTS_DIR).apply { mkdirs() }
            val stamp = System.currentTimeMillis()
            val dest = File(dir, "import-$stamp.jpg")

            val uri = runCatching { Uri.parse(source) }.getOrNull()
            val input = when {
                uri != null && (source.startsWith("content:") || source.startsWith("file:")) ->
                    context.contentResolver.openInputStream(uri)
                else -> File(source.removePrefix("file://")).takeIf { it.exists() }?.let { FileInputStream(it) }
            } ?: return null

            input.use { stream -> FileOutputStream(dest).use { output -> stream.copyTo(output) } }

            val name = displayNameOf(source)?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: "بطاقة-$stamp"

            var cropped = false
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(dest.absolutePath, bounds)
            if (bounds.outWidth <= 0) return null

            Enhance.decodeSampled(dest.absolutePath, maxDimensionPx = 4096)?.let { decoded ->
                val croppedBitmap = Enhance.cropToContent(decoded)
                if (croppedBitmap !== decoded) {
                    FileOutputStream(dest).use { out ->
                        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                    cropped = true
                    croppedBitmap.recycle()
                }
                decoded.recycle()
            }

            val finalBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(dest.absolutePath, finalBounds)
            if (finalBounds.outWidth <= 0) return null

            ImportedImage(
                privatePath = dest.absolutePath,
                displayName = name,
                widthPx = finalBounds.outWidth,
                heightPx = finalBounds.outHeight,
                cropped = cropped,
            )
        }.getOrNull()
    }

    actual fun imageAsDataUri(path: String): String {
        return runCatching {
            val file = File(path.removePrefix("file://"))
            if (!file.exists()) return ""
            val bytes = file.readBytes()
            val mime = when {
                path.endsWith(".png", ignoreCase = true) -> "image/png"
                path.endsWith(".webp", ignoreCase = true) -> "image/webp"
                else -> "image/jpeg"
            }
            "data:$mime;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        }.getOrElse { "" }
    }

    // ------------------------------------------------------------- export

    actual fun exportPdf(cards: List<CardItem>, settings: SheetSettings, pageCount: Int): ByteArray? = runCatching {
        val pages = (0 until pageCount).map { page ->
            val bitmap = SheetRenderer.renderPage(page, cards, settings)
            val jpeg = java.io.ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                out.toByteArray()
            }
            bitmap.recycle()
            PdfBuilder.Page(
                jpegBytes = jpeg,
                pixelWidth = maxOf(1, (settings.sheetWidthMm * (300f / 25.4f)).toInt()),
                pixelHeight = maxOf(1, (settings.sheetHeightMm * (300f / 25.4f)).toInt()),
                widthPt = settings.sheetWidthMm * PdfBuilder.MM_TO_PT,
                heightPt = settings.sheetHeightMm * PdfBuilder.MM_TO_PT,
            )
        }
        PdfBuilder.build(pages, title = "tabati-print")
    }.getOrNull()

    actual fun exportPng(page: Int, cards: List<CardItem>, settings: SheetSettings): ByteArray? = runCatching {
        val bitmap = SheetRenderer.renderPage(page, cards, settings)
        val png = java.io.ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        }
        bitmap.recycle()
        png
    }.getOrNull()

    actual fun printPdf(
        pdfBytes: ByteArray,
        pageCount: Int,
        sheetWidthMm: Float,
        sheetHeightMm: Float,
        jobName: String,
    ): Boolean {
        val context = AndroidPlatform.appContext
        return runCatching {
            val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as? PrintManager
                ?: return false
            val file = File(context.cacheDir, "$jobName.pdf")
            FileOutputStream(file).use { it.write(pdfBytes) }

            val attrs = PrintAttributes.Builder()
                .setMediaSize(
                    PrintAttributes.MediaSize(
                        "custom_${sheetWidthMm}x$sheetHeightMm",
                        "Custom ${sheetWidthMm} x ${sheetHeightMm} mm",
                        mmToMil(sheetWidthMm),
                        mmToMil(sheetHeightMm),
                    ),
                )
                .setResolution(PrintAttributes.Resolution("pdf300", "300 dpi", 300, 300))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()

            printManager.print(jobName, PdfBytesPrintAdapter(file, pageCount, jobName), attrs)
            true
        }.getOrDefault(false)
    }

    private fun mmToMil(mm: Float): Int = (mm * 39.3701f * 1000f).toInt()

    actual fun shareFile(path: String, mimeType: String, subject: String?): Boolean {
        val context = AndroidPlatform.appContext
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

    actual fun autoCropImage(path: String, id: Long): String? = runCatching {
        val file = File(path.removePrefix("file://"))
        if (!file.exists()) return null
        val bitmap = Enhance.decodeSampled(file.absolutePath, maxDimensionPx = 4096) ?: return null
        val cropped = Enhance.cropToContent(bitmap)
        if (cropped === bitmap) {
            bitmap.recycle()
            return null
        }
        val dir = File(AndroidPlatform.appContext.filesDir, IMPORTS_DIR).apply { mkdirs() }
        val dest = File(dir, "crop-$id-${System.currentTimeMillis()}.jpg")
        FileOutputStream(dest).use { out ->
            cropped.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        bitmap.recycle(); cropped.recycle()
        dest.absolutePath
    }.getOrNull()

    // ------------------------------------------------------------- notifications

    actual fun showExportNotification(title: String, body: String, path: String?) {
        val context = AndroidPlatform.appContext
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
                if (!granted) return
            }
            val manager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(NOTIF_CHANNEL_ID, "تنبيهات التصدير · طبعتي", NotificationManager.IMPORTANCE_DEFAULT)
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
            val viewIntent = contentUri?.let {
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(it, mime)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            val pendingIntent = viewIntent?.let {
                android.app.PendingIntent.getActivity(
                    context,
                    System.currentTimeMillis().toInt(),
                    it,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
                )
            }
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                android.app.Notification.Builder(context, NOTIF_CHANNEL_ID)
            } else {
                @Suppress("DEPRECATION")
                android.app.Notification.Builder(context)
            }
            builder.setSmallIcon(android.R.drawable.ic_menu_save)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
            if (pendingIntent != null) builder.setContentIntent(pendingIntent)
            manager.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (_: Throwable) {
            // notifications are best-effort, never break export
        }
    }

    // ------------------------------------------------------------- print adapter

    private class PdfBytesPrintAdapter(
        private val file: File,
        private val pageCount: Int,
        private val jobName: String,
    ) : PrintDocumentAdapter() {

        private var pfd: ParcelFileDescriptor? = null

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes?,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?,
        ) {
            if (cancellationSignal?.isCanceled == true) {
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
            pages: Array<out android.print.PageRange>?,
            destination: ParcelFileDescriptor?,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback,
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onWriteCancelled()
                return
            }
            try {
                FileInputStream(file).use { input ->
                    java.io.FileOutputStream(destination?.fileDescriptor).use { output ->
                        input.copyTo(output)
                    }
                }
                callback.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            }
        }

        override fun onFinish() {}
    }
}
