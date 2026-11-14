package com.appprint.studio.core.export

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import com.appprint.studio.core.image.EnhanceMath
import com.appprint.studio.core.model.CardItem
import com.appprint.studio.core.model.FrameStyle
import com.appprint.studio.core.model.SheetBackground
import com.appprint.studio.core.model.SheetSettings
import kotlin.math.max
import kotlin.math.min
import java.io.File

/**
 * Android sheet renderer — draws one page onto a Bitmap at 300 DPI.
 * Port of the design's `renderSheetToCanvas` (360° freeRotation + right-angle rotation aware):
 *   bg → margin guides → sheet frame → cards (image or Iraqi sample art) → card frame + cut guides.
 */
object SheetRenderer {

    private const val DPI = 300
    private const val PX_PER_MM = DPI / 25.4f

    fun renderPage(page: Int, cards: List<CardItem>, settings: SheetSettings): Bitmap {
        val wPx = max(1, (settings.sheetWidthMm * PX_PER_MM).toInt())
        val hPx = max(1, (settings.sheetHeightMm * PX_PER_MM).toInt())
        val bitmap = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val frame = settings.frame

        // background
        when (frame.background) {
            SheetBackground.WHITE -> canvas.drawColor(Color.WHITE)
            SheetBackground.OFFWHITE -> canvas.drawColor(Color.parseColor("#FDFAF1"))
            SheetBackground.TRANSPARENT -> canvas.drawColor(Color.TRANSPARENT)
        }

        // margin cutting guides (dashed red) — print-shop reference, light
        if (frame.guides) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = Color.argb(140, 179, 38, 56)
                strokeWidth = 4.5f
                pathEffect = android.graphics.DashPathEffect(floatArrayOf(22f, 22f), 0f)
            }
            canvas.drawRect(
                settings.marginMm * PX_PER_MM,
                settings.marginMm * PX_PER_MM,
                (settings.sheetWidthMm - settings.marginMm) * PX_PER_MM,
                (settings.sheetHeightMm - settings.marginMm) * PX_PER_MM,
                p,
            )
        }

        // sheet frame (border of the whole page)
        if (frame.sheet && frame.thickness > 0f) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = frame.colorHex.toInt()
                strokeWidth = max(2f, frame.thickness * 4f)
                frame.style.let { style ->
                    pathEffect = when (style) {
                        FrameStyle.SOLID -> null
                        FrameStyle.DASHED -> android.graphics.DashPathEffect(floatArrayOf(22f, 16f), 0f)
                        FrameStyle.DOTTED -> android.graphics.DashPathEffect(floatArrayOf(4f, 14f), 0f)
                    }
                }
            }
            canvas.drawRect(2f, 2f, wPx - 2f, hPx - 2f, p)
        }

        // cards
        val pageCards = cards.filter { it.page == page }
        for (card in pageCards) {
            val cw = card.widthMm * PX_PER_MM
            val ch = card.heightMm * PX_PER_MM
            val cx = (card.xMm + card.widthMm / 2f) * PX_PER_MM
            val cy = (card.yMm + card.heightMm / 2f) * PX_PER_MM
            val isRight = card.rotation % 180 != 0
            val innerW = if (isRight) ch else cw
            val innerH = if (isRight) cw else ch
            val totalAngle = card.freeRotation + card.enhance.tilt

            canvas.save()
            canvas.translate(cx, cy)
            canvas.rotate(totalAngle)
            canvas.save()
            canvas.rotate(card.rotation.toFloat())
            canvas.translate(-innerW / 2f, -innerH / 2f)

            val imageSource = card.enhancedImageUri?.takeIf { it.isNotEmpty() } ?: card.imageUri
            var drewImage = false
            if (imageSource != null) {
                drewImage = drawEnhancedImage(canvas, imageSource, innerW, innerH, card.enhance)
            }
            if (!drewImage) drawSampleArt(canvas, card, innerW, innerH)
            canvas.restore()

            // card frame
            if (frame.card && card.border && frame.thickness > 0f) {
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = frame.colorHex.toInt()
                    strokeWidth = max(2f, frame.thickness * 4f)
                    pathEffect = when (frame.style) {
                        FrameStyle.SOLID -> null
                        FrameStyle.DASHED -> android.graphics.DashPathEffect(floatArrayOf(18f, 14f), 0f)
                        FrameStyle.DOTTED -> android.graphics.DashPathEffect(floatArrayOf(4f, 12f), 0f)
                    }
                }
                canvas.drawRect(-cw / 2f, -ch / 2f, cw / 2f, ch / 2f, p)
            }

            // per-card cut guide (red dashed, beyond artwork)
            if (frame.guides && card.guides) {
                val bleed = 1.2f * PX_PER_MM
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = Color.argb(184, 179, 38, 56)
                    strokeWidth = 3.5f
                    pathEffect = android.graphics.DashPathEffect(floatArrayOf(14f, 14f), 0f)
                }
                canvas.drawRect(-cw / 2f - bleed, -ch / 2f - bleed, cw / 2f + bleed, ch / 2f + bleed, p)
            }
            canvas.restore()
        }
        return bitmap
    }

    private fun drawEnhancedImage(canvas: Canvas, path: String, innerW: Float, innerH: Float, enhance: com.appprint.studio.core.model.EnhanceSettings): Boolean {
        val file = File(path.removePrefix("file://"))
        if (!file.exists()) return false
        var bmp = decodeSampled(file.absolutePath, maxDimensionPx = 1600) ?: return false
        if (!enhance.isIdentity) {
            val enhanced = enhanceBitmap(bmp, enhance)
            bmp.recycle()
            bmp = enhanced
        }
        val scale = max(innerW / bmp.width, innerH / bmp.height)
        val sw = innerW / scale
        val sh = innerH / scale
        val src = Rect(
            ((bmp.width - sw) / 2f).toInt().coerceAtLeast(0),
            ((bmp.height - sh) / 2f).toInt().coerceAtLeast(0),
            (bmp.width / 2f + sw / 2f).toInt().coerceAtMost(bmp.width),
            (bmp.height / 2f + sh / 2f).toInt().coerceAtMost(bmp.height),
        )
        val dst = Rect(0, 0, innerW.toInt(), innerH.toInt())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bmp, src, dst, paint)
        bmp.recycle()
        return true
    }

    private fun enhanceBitmap(source: Bitmap, enhance: com.appprint.studio.core.model.EnhanceSettings): Bitmap {
        val argb = IntArray(source.width * source.height)
        source.getPixels(argb, 0, source.width, 0, 0, source.width, source.height)
        EnhanceMath.applyToPixels(argb, source.width, source.height, enhance)
        return Bitmap.createBitmap(argb, source.width, source.height, Bitmap.Config.ARGB_8888)
    }

    private fun drawSampleArt(canvas: Canvas, card: CardItem, innerW: Float, innerH: Float) {
        val isPassport = card.typeKey == "passport"
        val bg = if (isPassport) Color.parseColor("#17365A") else Color.parseColor("#FDFAF1")
        canvas.drawColor(bg)
        val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#006C47") }
        canvas.drawRect(0f, 0f, innerW, 12f, accent)

        // Iraqi flag band for the national card style
        val flagPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val flagH = innerH * 0.30f
        val bandH = flagH / 3f
        flagPaint.color = Color.parseColor("#CE1126")
        canvas.drawRect(0f, innerH - flagH, innerW, innerH - flagH + bandH, flagPaint)
        flagPaint.color = Color.parseColor("#FFFFFF")
        canvas.drawRect(0f, innerH - flagH + bandH, innerW, innerH - flagH + bandH * 2f, flagPaint)
        flagPaint.color = Color.parseColor("#007A3D")
        canvas.drawRect(0f, innerH - flagH + bandH * 2f, innerW, innerH, flagPaint)

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isPassport) Color.WHITE else Color.parseColor("#1C1C1C")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = min(innerH * 0.16f, innerW / max(1, card.name.length) * 1.9f)
        }
        val labelY = innerH * 0.42f
        canvas.drawText(card.name, innerW / 2f, labelY, namePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isPassport) Color.argb(200, 255, 255, 255) else Color.argb(190, 28, 28, 28)
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            textSize = min(innerH * 0.065f, innerW / 26f)
        }
        canvas.drawText("OFFICIAL ID DOCUMENT • وثيقة رسمية", innerW / 2f, labelY + innerH * 0.15f, subPaint)
    }

    private fun decodeSampled(path: String, maxDimensionPx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > maxDimensionPx) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return runCatching { BitmapFactory.decodeFile(path, opts) }.getOrNull()
    }
}
