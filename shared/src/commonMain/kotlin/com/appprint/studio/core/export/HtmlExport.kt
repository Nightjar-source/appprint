package com.appprint.studio.core.export

import com.appprint.studio.core.model.CardItem
import com.appprint.studio.core.model.FrameStyle
import com.appprint.studio.core.model.SheetBackground
import com.appprint.studio.core.model.SheetFrame

/**
 * Standalone, print-ready HTML export — direct port of the design's
 * `buildPrintableHtml`. Images are embedded as data/file URIs resolved by [imageSource].
 * The generated file prints 1:1 with `@page { size: <w>mm <h>mm; margin: 0 }`.
 */
object HtmlExport {

    private fun escapeHtml(s: String): String = buildString(s.length + 16) {
        for (ch in s) {
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                else -> append(ch)
            }
        }
    }

    private fun frameBorderCss(frame: SheetFrame): String {
        if (!frame.sheet || frame.thickness <= 0f) return ""
        val styleName = when (frame.style) {
            FrameStyle.SOLID -> "solid"
            FrameStyle.DASHED -> "dashed"
            FrameStyle.DOTTED -> "dotted"
        }
        return "border:${frame.thickness.toInt().coerceAtLeast(1)}px $styleName #%06x;".format(frame.colorHex and 0xFFFFFF)
    }

    fun build(
        pages: List<List<CardItem>>,
        paperWidthMm: Float,
        paperHeightMm: Float,
        marginMm: Float,
        frame: SheetFrame,
        imageSource: (CardItem) -> String,
    ): ByteArray {
        val sheets = pages.mapIndexed { _, pageCards ->
            val items = pageCards.joinToString("") { card -> cardHtml(card, frame, imageSource) }
            val marginGuide = if (frame.guides) {
                val w = (paperWidthMm - marginMm * 2).coerceAtLeast(0f)
                val h = (paperHeightMm - marginMm * 2).coerceAtLeast(0f)
                "<div style=\"position:absolute;left:${marginMm}mm;top:${marginMm}mm;width:${w}mm;height:${h}mm;border:0.4mm dashed rgba(179,38,56,.5)\"></div>"
            } else ""
            val bg = when (frame.background) {
                SheetBackground.WHITE -> "#ffffff"
                SheetBackground.OFFWHITE -> "#fdfaf1"
                SheetBackground.TRANSPARENT -> "transparent"
            }
            "<section class=\"sheet\" style=\"background:$bg;${frameBorderCss(frame)}\">$marginGuide$items</section>"
        }.joinToString("\n")

        val html = """
<!doctype html><html lang="ar" dir="rtl"><head><meta charset="utf-8"><title>طبعتي — ملف الطباعة</title><style>@page{size:${paperWidthMm}mm ${paperHeightMm}mm;margin:0}*{box-sizing:border-box}body{margin:0;background:#fff}.sheet{width:${paperWidthMm}mm;height:${paperHeightMm}mm;position:relative;overflow:hidden}.sheet:not(:last-child){page-break-after:always;break-after:page}.card{position:absolute;overflow:hidden}@media print{body{margin:0}}</style></head><body>$sheets</body></html>
        """.trimIndent()
        return html.toByteArray(Charsets.UTF_8)
    }

    private fun cardHtml(card: CardItem, frame: SheetFrame, imageSource: (CardItem) -> String): String {
        val isRot90 = card.rotation % 180 != 0
        val innerW = if (isRot90) card.heightMm else card.widthMm
        val innerH = if (isRot90) card.widthMm else card.heightMm
        val totalAngle = card.freeRotation + card.enhance.tilt
        val src = imageSource(card)
        val border = if (frame.card && card.border && frame.thickness > 0f) {
            val styleName = when (frame.style) {
                FrameStyle.SOLID -> "solid"
                FrameStyle.DASHED -> "dashed"
                FrameStyle.DOTTED -> "dotted"
            }
            "border:${frame.thickness.toInt().coerceAtLeast(1)}px $styleName #%06x;".format(frame.colorHex and 0xFFFFFF)
        } else ""
        val guide = if (frame.guides && card.guides) {
            "<div style=\"position:absolute;inset:-1.2mm;border:0.4mm dashed rgba(179,38,56,.7)\"></div>"
        } else ""
        val innerContent = if (src.isNotEmpty()) {
            "<img src=\"${escapeHtml(src)}\" alt=\"${escapeHtml(card.name)}\" style=\"width:100%;height:100%;object-fit:cover;display:block\">"
        } else {
            val bg = if (card.typeKey == "passport") "#17365A" else "#FDFAF1"
            val fg = if (card.typeKey == "passport") "#ffffff" else "#1c1c1c"
            "<div style=\"width:100%;height:100%;background:$bg;color:$fg;padding:3mm;text-align:center\"><div style=\"height:1.2mm;background:#006C47\"></div><div style=\"font-weight:900;font-size:11pt;margin-top:4mm\">${escapeHtml(card.name)}</div><div style=\"font-size:7pt;opacity:.75;margin-top:2mm\">OFFICIAL ID DOCUMENT</div></div>"
        }
        val body = "<div style=\"position:absolute;left:50%;top:50%;width:${innerW}mm;height:${innerH}mm;transform:translate(-50%,-50%) rotate(${card.rotation}deg);overflow:hidden\">$innerContent</div>"
        return "<div class=\"card\" style=\"left:${card.xMm}mm;top:${card.yMm}mm;width:${card.widthMm}mm;height:${card.heightMm}mm;transform:rotate(${totalAngle}deg);$border\">$body$guide</div>"
    }
}
