package com.appprint.studio.core.export

import com.appprint.studio.core.image.PixelOps
import com.appprint.studio.core.model.CardItem
import com.appprint.studio.core.model.FrameStyle
import com.appprint.studio.core.model.SheetBackground
import com.appprint.studio.core.model.SheetSettings
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * Desktop JVM sheet renderer — AWT/ImageIO port of the Android renderer.
 * NOTE: sample artwork uses Latin marks on desktop (AWT does not shape Arabic);
 * the on-device (Android) renderer and the Compose preview draw full Arabic text.
 */
object JvmSheetRenderer {

    private const val DPI = 300
    private const val MM_TO_PT = 72f / 25.4f
    private const val PX_PER_MM = DPI / 25.4f

    data class RenderedPage(
        val jpegBytes: ByteArray,
        val pixelWidth: Int,
        val pixelHeight: Int,
        val widthPt: Float,
        val heightPt: Float,
    )

    fun renderPage(page: Int, cards: List<CardItem>, settings: SheetSettings): BufferedImage {
        val pxPerMm = PX_PER_MM
        val width = (settings.sheetWidthMm * pxPerMm).toInt().coerceAtLeast(1)
        val height = (settings.sheetHeightMm * pxPerMm).toInt().coerceAtLeast(1)
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)

            when (settings.background) {
                SheetBackground.WHITE -> { g.color = Color.WHITE; g.fillRect(0, 0, width, height) }
                SheetBackground.OFFWHITE -> { g.color = Color(0xFDFAF1); g.fillRect(0, 0, width, height) }
                SheetBackground.TRANSPARENT -> Unit
            }

            cards.filter { it.page == page }.sortedBy { it.id }.forEach { card ->
                drawCard(g, card, settings, pxPerMm)
            }

            if (settings.showGuides) {
                g.color = Color(200, 16, 46, 128)
                g.stroke = BasicStroke(0.4f * pxPerMm, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, floatArrayOf(3f * pxPerMm, 2f * pxPerMm), 0f)
                val m = (settings.marginMm * pxPerMm).toInt()
                g.drawRect(m, m, width - 2 * m, height - 2 * m)
            }
        } finally {
            g.dispose()
        }
        return image
    }

    fun renderPageJpeg(page: Int, cards: List<CardItem>, settings: SheetSettings): RenderedPage {
        val image = renderPage(page, cards, settings)
        val stream = ByteArrayOutputStream()
        ImageIO.write(flattenForJpeg(image), "jpg", stream)
        return RenderedPage(
            jpegBytes = stream.toByteArray(),
            pixelWidth = image.width,
            pixelHeight = image.height,
            widthPt = settings.sheetWidthMm * MM_TO_PT,
            heightPt = settings.sheetHeightMm * MM_TO_PT,
        )
    }

    private fun flattenForJpeg(image: BufferedImage): BufferedImage {
        val flat = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
        val g = flat.createGraphics()
        g.color = Color.WHITE
        g.fillRect(0, 0, flat.width, flat.height)
        g.drawImage(image, 0, 0, null)
        g.dispose()
        return flat
    }

    private fun drawCard(g: Graphics2D, card: CardItem, settings: SheetSettings, pxPerMm: Float) {
        val wPx = card.effectiveWidth * pxPerMm
        val hPx = card.effectiveHeight * pxPerMm
        val localW = card.widthMm * pxPerMm
        val localH = card.heightMm * pxPerMm

        val saved = g.transform
        g.translate((card.xMm * pxPerMm + wPx / 2f).toDouble(), (card.yMm * pxPerMm + hPx / 2f).toDouble())
        g.rotate(Math.toRadians(card.rotation.toDouble()))
        g.translate((-localW / 2f).toDouble(), (-localH / 2f).toDouble())

        val destination = java.awt.geom.RoundRectangle2D.Double(0.0, 0.0, localW.toDouble(), localH.toDouble(), hPx * 0.15, hPx * 0.15)

        val source = card.imageUri?.let { path ->
            runCatching { ImageIO.read(File(path)) }.getOrNull()
        }
        if (source != null) {
            val enhanced = applyEnhanceAwt(source, card.enhance)
            val clip = g.clip
            g.clip(destination)
            val srcAspect = enhanced.width.toFloat() / enhanced.height
            val dstAspect = (localW / localH)
            val (sx, sy, sw, sh) = if (srcAspect > dstAspect) {
                val newW = (enhanced.height * dstAspect).toInt().coerceAtLeast(1)
                intArrayOf((enhanced.width - newW) / 2, 0, newW, enhanced.height)
            } else {
                val newH = (enhanced.width / dstAspect).toInt().coerceAtLeast(1)
                intArrayOf(0, (enhanced.height - newH) / 2, enhanced.width, newH)
            }
            g.drawImage(
                enhanced,
                0, 0, localW.toInt(), localH.toInt(),
                sx, sy, sx + sw, sy + sh,
                null,
            )
            g.clip = clip
        } else {
            drawSampleArt(g, card, localW, localH)
        }

        if (settings.showFrames && card.border) {
            g.color = Color(settings.frameColorHex.toInt(), true)
            g.stroke = BasicStroke(
                (settings.frameWidthDp * 0.2646f * pxPerMm).coerceAtLeast(0.3f * pxPerMm),
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER,
                1f,
                if (settings.frameStyle == FrameStyle.SOLID) null else floatArrayOf(2.2f * pxPerMm, 1.6f * pxPerMm),
                0f,
            )
            if (settings.frameStyle == FrameStyle.DOTTED) {
                g.stroke = BasicStroke(
                    (settings.frameWidthDp * 0.2646f * pxPerMm).coerceAtLeast(0.3f * pxPerMm),
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_MITER,
                    1f,
                    floatArrayOf(0.5f * pxPerMm, 1.3f * pxPerMm),
                    0f,
                )
            }
            g.draw(destination)
        }

        if (settings.showGuides && card.guides) {
            g.color = Color(200, 16, 46, 178)
            g.stroke = BasicStroke(0.4f * pxPerMm, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, floatArrayOf(2.6f * pxPerMm, 1.8f * pxPerMm), 0f)
            val outside = 1.2f * pxPerMm
            g.drawRect(-outside.toInt(), -outside.toInt(), (localW + 2 * outside).toInt(), (localH + 2 * outside).toInt())
        }

        g.transform = saved
    }

    private fun drawSampleArt(g: Graphics2D, card: CardItem, w: Float, h: Float) {
        val isPassport = card.type == com.appprint.studio.core.model.CardType.PASSPORT
        g.color = if (isPassport) Color(0x17365A) else Color(0xFDFAF1)
        g.fillRoundRect(0, 0, w.toInt(), h.toInt(), (h * 0.15f).toInt(), (h * 0.15f).toInt())

        if (isPassport) {
            g.color = Color(0xD9A51F)
            g.fillOval((w / 2 - h * 0.11f).toInt(), (h * 0.23f).toInt(), (h * 0.22f).toInt(), (h * 0.22f).toInt())
            g.font = Font("SansSerif", Font.BOLD, (h * 0.085f).toInt())
            g.drawString("REPUBLIC OF IRAQ", w / 2 - g.fontMetrics.stringWidth("REPUBLIC OF IRAQ") / 2f, h * 0.66f)
            g.font = Font("SansSerif", Font.BOLD, (h * 0.075f).toInt())
            val label = "PASSPORT"
            g.drawString(label, w / 2 - g.fontMetrics.stringWidth(label) / 2f, h * 0.84f)
            return
        }

        val stripeTop = h * 0.055f
        val stripeH = h * 0.115f
        g.color = Color(0xC8102E)
        g.fillRect((w * 0.06f).toInt(), stripeTop.toInt(), (w * 0.313f).toInt(), stripeH.toInt())
        g.color = Color(0x007A3D)
        g.fillRect((w * 0.613f).toInt(), stripeTop.toInt(), (w * 0.327f).toInt(), stripeH.toInt())

        g.color = Color(0x1C1C1C)
        g.font = Font("SansSerif", Font.BOLD, (h * 0.115f).toInt())
        val name = card.name.take(24)
        g.drawString(name, w / 2 - g.fontMetrics.stringWidth(name) / 2f, h * 0.52f)
        g.color = Color(0x4B453A)
        g.font = Font("SansSerif", Font.PLAIN, (h * 0.062f).toInt())
        val line = "REPUBLIC OF IRAQ"
        g.drawString(line, w / 2 - g.fontMetrics.stringWidth(line) / 2f, h * 0.64f)
        g.color = Color(0x0D5B3A)
        g.font = Font("SansSerif", Font.BOLD, (h * 0.075f).toInt())
        val iraq = "IRAQ"
        g.drawString(iraq, w / 2 - g.fontMetrics.stringWidth(iraq) / 2f, h * 0.80f)

        g.color = Color(0xB0AFA6)
        g.font = Font("SansSerif", Font.PLAIN, (h * 0.045f).toInt())
        g.drawString("SAMPLE", w * 0.055f, h * 0.955f)
    }

    private fun applyEnhanceAwt(source: BufferedImage, e: com.appprint.studio.core.model.EnhanceSettings): BufferedImage {
        val matrix = PixelOps.buildEnhanceMatrix(e)
        val w = source.width
        val h = source.height
        val input = IntArray(w * h)
        source.getRGB(0, 0, w, h, input, 0, w)

        // color matrix pass (inline: out = M * in, clamped)
        if (!e.isIdentity) {
            val tmp = IntArray(input.size)
            for (i in input.indices) {
                val p = input[i]
                val a = (p ushr 24) and 0xFF
                val r = (p ushr 16) and 0xFF
                val gch = (p ushr 8) and 0xFF
                val b = p and 0xFF
                fun ch(row: Int): Int {
                    val v = matrix[row * 5 + 0] * r +
                        matrix[row * 5 + 1] * gch +
                        matrix[row * 5 + 2] * b +
                        matrix[row * 5 + 4]
                    return v.toInt().coerceIn(0, 255)
                }
                val rr = ch(0); val gg = ch(1); val bb = ch(2)
                tmp[i] = (a shl 24) or (rr shl 16) or (gg shl 8) or bb
            }
            input.copyInto(tmp, 0, 0, input.size)
        }

        val sharpened = if (e.sharpness > 0) PixelOps.sharpen(input, w, h, e.sharpness / 100f) else input

        val out = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        out.setRGB(0, 0, w, h, sharpened, 0, w)
        return out
    }
}
