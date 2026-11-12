package com.appprint.studio.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import com.appprint.studio.core.model.CardItem
import com.appprint.studio.core.model.CardType
import com.appprint.studio.core.model.EnhanceSettings
import java.io.File
import java.io.FileInputStream

/**
 * Android image pipeline: sampled decoding, white-border auto-crop,
 * 3×3 sharpen. Heavy lifting is shared with other platforms via PixelOps.
 */
object Enhance {

    fun colorFilterFor(e: EnhanceSettings): ColorMatrixColorFilter? =
        if (e.isIdentity) null else ColorMatrixColorFilter(ColorMatrix(PixelOps.buildEnhanceMatrix(e)))

    /** Decode a bitmap from a content:// Uri string or a file path; sub-sampled to [maxDimensionPx]. */
    fun decodeSampled(context: Context, source: String, maxDimensionPx: Int): Bitmap? {
        return runCatching {
            val resolver = context.contentResolver
            val uri = runCatching { Uri.parse(source) }.getOrNull()

            fun stream() = when {
                uri != null && (source.startsWith("content:") || source.startsWith("file:")) ->
                    resolver.openInputStream(uri)
                else -> File(source).takeIf { it.exists() }?.let { FileInputStream(it) }
            }

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            stream()?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0) return null

            var sample = 1
            while (bounds.outWidth / sample > maxDimensionPx || bounds.outHeight / sample > maxDimensionPx) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = stream()?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
            return applyExifRotation(bitmap, context, source, uri)
        }.getOrNull()
    }

    /**
     * Rotate the decoded bitmap to its EXIF orientation — without this,
     * camera photos print/preview sideways or upside-down.
     */
    private fun applyExifRotation(
        bitmap: Bitmap,
        context: Context,
        source: String,
        uri: android.net.Uri?,
    ): Bitmap {
        return runCatching {
            val orientation = when {
                uri != null && (source.startsWith("content:") || source.startsWith("file:")) ->
                    context.contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
                        ?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                else -> File(source).takeIf { it.exists() }?.let { ExifInterface(it) }
                        ?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: return bitmap

            val matrix = android.graphics.Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.postScale(-1f, 1f)
                }
                else -> return bitmap
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also { rotated ->
                if (rotated !== bitmap) bitmap.recycle()
            }
        }.getOrDefault(bitmap)
    }

    /** Bounding box of non-near-white content, via shared pixel kernels. */
    fun findContentBounds(bitmap: Bitmap, tolerance: Int = 242): Rect? {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val b = PixelOps.contentBounds(pixels, w, h, tolerance) ?: return null
        return Rect(b.left, b.top, b.right, b.bottom)
    }

    /** Crop white scanner edges. Returns cropped copy, or the [source] itself when unchanged. */
    fun cropToContent(bitmap: Bitmap, tolerance: Int = 242): Bitmap {
        val bounds = findContentBounds(bitmap, tolerance) ?: return bitmap
        if (bounds.width() >= bitmap.width * 0.98 && bounds.height() >= bitmap.height * 0.98) return bitmap
        if (bounds.width() < 16 || bounds.height() < 16) return bitmap
        return Bitmap.createBitmap(bitmap, bounds.left, bounds.top, bounds.width(), bounds.height())
    }

    /** 3×3 unsharp mask via shared kernel. */
    fun sharpen(source: Bitmap, amount: Float): Bitmap {
        if (amount <= 0.01f) return source
        val w = source.width
        val h = source.height
        val input = IntArray(w * h)
        source.getPixels(input, 0, w, 0, 0, w, h)
        val output = PixelOps.sharpen(input, w, h, amount)
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(output, 0, w, 0, 0, w, h)
        return result
    }

    /** Full enhance pipeline: matrix filter then sharpen. */
    fun applyEnhance(source: Bitmap, e: EnhanceSettings): Bitmap {
        var working = source
        val filter = colorFilterFor(e)
        if (filter != null) {
            val canvasBitmap = Bitmap.createBitmap(working.width, working.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(canvasBitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = filter }
            canvas.drawBitmap(working, 0f, 0f, paint)
            working = canvasBitmap
        }
        if (e.sharpness > 0) {
            working = sharpen(working, e.sharpness / 100f)
        }
        return working
    }

    /** Draws the «sample card» print artwork into [canvas] (used by the 300-DPI sheet renderer). */
    fun drawSampleCard(canvas: Canvas, widthPx: Float, heightPx: Float, card: CardItem) {
        val w = widthPx
        val h = heightPx
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val isPassport = card.type == CardType.PASSPORT
        val bgColor = if (isPassport) Color.parseColor("#17365A") else Color.parseColor("#FDFAF1")
        val radius = h * 0.075f

        paint.style = Paint.Style.FILL
        paint.color = bgColor
        val body = android.graphics.RectF(0f, 0f, w, h)
        canvas.drawRoundRect(body, radius, radius, paint)

        if (isPassport) {
            paint.color = Color.parseColor("#D9A51F")
            canvas.drawCircle(w / 2f, h * 0.34f, h * 0.11f, paint)
            paint.color = Color.parseColor("#17365A")
            canvas.drawCircle(w / 2f, h * 0.34f, h * 0.088f, paint)
            paint.color = Color.parseColor("#D9A51F")
            canvas.drawCircle(w / 2f, h * 0.34f, h * 0.062f, paint)
            paint.color = Color.parseColor("#D9A51F")
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = h * 0.085f
            paint.isFakeBoldText = true
            canvas.drawText("جمهورية العراق", w / 2f, h * 0.60f, paint)
            paint.isFakeBoldText = false
            paint.textSize = h * 0.052f
            paint.letterSpacing = 0.08f
            canvas.drawText("REPUBLIC OF IRAQ", w / 2f, h * 0.70f, paint)
            paint.textSize = h * 0.075f
            paint.isFakeBoldText = true
            paint.letterSpacing = 0.18f
            canvas.drawText("PASSPORT", w / 2f, h * 0.84f, paint)
            paint.letterSpacing = 0f
            return
        }

        val stripeH = h * 0.115f
        val stripeTop = h * 0.055f
        paint.color = Color.parseColor("#C8102E")
        canvas.drawRect(w * 0.06f, stripeTop, w * 0.373f, stripeTop + stripeH, paint)
        paint.color = Color.parseColor("#007A3D")
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = stripeH * 0.52f
        paint.isFakeBoldText = true
        canvas.drawText("الله أكبر", w * 0.493f, stripeTop + stripeH * 0.74f, paint)
        paint.color = Color.parseColor("#007A3D")
        canvas.drawRect(w * 0.613f, stripeTop, w * 0.94f, stripeTop + stripeH, paint)

        paint.color = Color.parseColor("#1C1C1C")
        paint.isFakeBoldText = true
        paint.textSize = h * 0.115f
        canvas.drawText(card.name, w / 2f, h * 0.52f, paint)

        paint.isFakeBoldText = false
        paint.textSize = h * 0.062f
        paint.color = Color.parseColor("#4B453A")
        canvas.drawText("REPUBLIC OF IRAQ • جمهورية العراق", w / 2f, h * 0.64f, paint)

        paint.isFakeBoldText = true
        paint.textSize = h * 0.075f
        paint.color = Color.parseColor("#0D5B3A")
        canvas.drawText("IRAQ • العراق", w / 2f, h * 0.80f, paint)

        paint.isFakeBoldText = false
        paint.textSize = h * 0.045f
        paint.color = Color.parseColor("#B0AFA6")
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("SAMPLE", w * 0.055f, h * 0.955f, paint)
    }
}
