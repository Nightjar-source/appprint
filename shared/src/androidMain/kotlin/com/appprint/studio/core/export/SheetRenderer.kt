package com.appprint.studio.core.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.DashPathEffect
import android.graphics.RectF
import com.appprint.studio.core.image.Enhance
import com.appprint.studio.core.model.CardItem
import com.appprint.studio.core.model.FrameStyle
import com.appprint.studio.core.model.SheetBackground
import com.appprint.studio.core.model.SheetSettings
import java.io.ByteArrayOutputStream

/**
 * Renders sheets at true print resolution (300 DPI) on Android Canvas.
 * Sheet pages are handed to the shared [com.appprint.studio.core.pdf.PdfBuilder]
 * as 300-DPI JPEGs — identical bytes on every export path.
 */
object SheetRenderer {
    const val DPI = 300
    private const val MM_TO_PT = 72f / 25.4f
    private const val PX_PER_MM = DPI / 25.4f

    data class RenderedPage(
        val jpegBytes: ByteArray,
        val pixelWidth: Int,
        val pixelHeight: Int,
        val widthPt: Float,
        val heightPt: Float,
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun renderPageBitmap(context: Context, page: Int, cards: List<CardItem>, settings: SheetSettings): Bitmap {
        val pxPerMm = PX_PER_MM
        val sheetW = settings.sheetWidthMm
        val sheetH = settings.sheetHeightMm
        val bitmap = Bitmap.createBitmap(
            (sheetW * pxPerMm).toInt().coerceAtLeast(1),
            (sheetH * pxPerMm).toInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)

        when (settings.background) {
            SheetBackground.WHITE -> canvas.drawColor(Color.WHITE)
            SheetBackground.OFFWHITE -> canvas.drawColor(Color.parseColor("#FDFAF1"))
            SheetBackground.TRANSPARENT -> Unit
        }

        val pageCards = cards.filter { it.page == page }.sortedBy { it.id }
        pageCards.forEach { drawCard(context, canvas, it, settings, pxPerMm) }

        if (settings.showGuides) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.4f * pxPerMm
            paint.color = Color.argb(128, 200, 16, 46)
            paint.pathEffect = DashPathEffect(floatArrayOf(3f * pxPerMm, 2f * pxPerMm), 0f)
            val m = settings.marginMm * pxPerMm
            canvas.drawRect(m, m, bitmap.width - m, bitmap.height - m, paint)
            paint.pathEffect = null
        }
        return bitmap
    }

    fun renderPageJpeg(context: Context, page: Int, cards: List<CardItem>, settings: SheetSettings): RenderedPage {
        val bitmap = renderPageBitmap(context, page, cards, settings)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
        val result = RenderedPage(
            jpegBytes = stream.toByteArray(),
            pixelWidth = bitmap.width,
            pixelHeight = bitmap.height,
            widthPt = settings.sheetWidthMm * MM_TO_PT,
            heightPt = settings.sheetHeightMm * MM_TO_PT,
        )
        bitmap.recycle()
        return result
    }

    private fun drawCard(
        context: Context,
        canvas: Canvas,
        card: CardItem,
        settings: SheetSettings,
        pxPerMm: Float,
    ) {
        val wPx = card.effectiveWidth * pxPerMm
        val hPx = card.effectiveHeight * pxPerMm
        val xPx = card.xMm * pxPerMm
        val yPx = card.yMm * pxPerMm
        val radius = hPx * 0.075f

        canvas.save()
        canvas.translate(xPx + wPx / 2f, yPx + hPx / 2f)
        canvas.rotate(card.rotation.toFloat())
        val localW = card.widthMm * pxPerMm
        val localH = card.heightMm * pxPerMm
        canvas.translate(-localW / 2f, -localH / 2f)

        val destination = RectF(0f, 0f, localW, localH)
        val image = card.imageUri?.let {
            Enhance.decodeSampled(context, it, maxDimensionPx = maxOf(localW, localH).toInt().coerceAtLeast(64))
        }
        if (image != null) {
            val enhanced = Enhance.applyEnhance(image, card.enhance)
            canvas.save()
            val clip = Path().apply { addRoundRect(destination, radius, radius, Path.Direction.CW) }
            canvas.clipPath(clip)
            val src = centerCropRect(enhanced.width, enhanced.height, localW / localH)
            canvas.drawBitmap(enhanced, src, destination, Paint(Paint.ANTI_ALIAS_FLAG))
            canvas.restore()
            if (enhanced !== image) enhanced.recycle()
            image.recycle()
        } else {
            Enhance.drawSampleCard(canvas, localW, localH, card)
        }

        if (settings.showFrames && card.border) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth =
                (settings.frameWidthDp * 0.2646f * pxPerMm).coerceAtLeast(0.3f * pxPerMm)
            paint.color = settings.frameColorHex.toInt()
            paint.pathEffect = when (settings.frameStyle) {
                FrameStyle.SOLID -> null
                FrameStyle.DASHED -> DashPathEffect(floatArrayOf(2.2f * pxPerMm, 1.6f * pxPerMm), 0f)
                FrameStyle.DOTTED -> DashPathEffect(floatArrayOf(0.5f * pxPerMm, 1.3f * pxPerMm), 0f)
            }
            val inset = paint.strokeWidth / 2f
            canvas.drawRoundRect(
                RectF(inset, inset, localW - inset, localH - inset),
                radius, radius, paint,
            )
            paint.pathEffect = null
        }

        if (settings.showGuides && card.guides) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.4f * pxPerMm
            paint.color = Color.argb(178, 200, 16, 46)
            paint.pathEffect = DashPathEffect(floatArrayOf(2.6f * pxPerMm, 1.8f * pxPerMm), 0f)
            val outside = 1.2f * pxPerMm
            canvas.drawRect(-outside, -outside, localW + outside, localH + outside, paint)
            paint.pathEffect = null
        }

        canvas.restore()
    }

    private fun centerCropRect(srcW: Int, srcH: Int, targetAspect: Float): android.graphics.Rect {
        val srcAspect = srcW.toFloat() / srcH.toFloat()
        return if (srcAspect > targetAspect) {
            val newW = (srcH * targetAspect).toInt().coerceAtLeast(1)
            val left = (srcW - newW) / 2
            android.graphics.Rect(left, 0, left + newW, srcH)
        } else {
            val newH = (srcW / targetAspect).toInt().coerceAtLeast(1)
            val top = (srcH - newH) / 2
            android.graphics.Rect(0, top, srcW, top + newH)
        }
    }
}
