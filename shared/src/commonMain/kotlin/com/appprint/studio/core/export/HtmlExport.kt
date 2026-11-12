package com.appprint.studio.core.export

/**
 * Builds a genuine, standalone print-ready HTML document embedding every sheet
 * as a base64 JPEG — opens anywhere (browser/reader), prints at exactly 100%
 * with zero drift, mirroring «ستوديو الطباعة.html.html»'s print flow.
 * UI-free: no platform/AppKit/Android APIs.
 */
object HtmlExport {

    /** One rendered sheet to embed in the document. */
    data class EmbeddedPage(
        val jpegBytes: ByteArray,
        val widthPt: Float,
        val heightPt: Float,
    )

    /** Base64 with zero platform dependencies (stdlib-only lookup table). */
    fun base64Encode(data: ByteArray): String {
        val table = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val out = StringBuilder(data.size * 4 / 3 + 4)
        var i = 0
        while (i < data.size) {
            val b0 = data[i].toInt() and 0xFF
            val b1 = if (i + 1 < data.size) data[i + 1].toInt() and 0xFF else 0
            val b2 = if (i + 2 < data.size) data[i + 2].toInt() and 0xFF else 0
            val triple = (b0 shl 16) or (b1 shl 8) or b2
            out.append(table[(triple shr 18) and 0x3F])
            out.append(table[(triple shr 12) and 0x3F])
            out.append(if (i + 1 < data.size) table[(triple shr 6) and 0x3F] else '=')
            out.append(if (i + 2 < data.size) table[triple and 0x3F] else '=')
            i += 3
        }
        return out.toString()
    }

    fun buildPrintDocument(pages: List<EmbeddedPage>, widthMm: Float, heightMm: Float, title: String): ByteArray {
        val sb = StringBuilder()
        sb.append("<!DOCTYPE html>\n<html lang=\"ar\" dir=\"rtl\">\n<head>\n<meta charset=\"utf-8\" />\n")
        sb.append("<title>").append(escapeHtml(title)).append("</title>\n")
        sb.append("<style>\n")
        sb.append("@page { size: ${widthMm}mm ${heightMm}mm; margin: 0; }\n")
        sb.append("* { box-sizing: border-box; }\n")
        sb.append("body { margin: 0; background: #fff; font-family: system-ui, sans-serif; }\n")
        sb.append(".sheet { width: ${widthMm}mm; height: ${heightMm}mm; position: relative; overflow: hidden; background: #fff; page-break-after: always; break-after: page; }\n")
        sb.append(".sheet:last-child { page-break-after: auto; break-after: auto; }\n")
        sb.append(".sheet img { width: 100%; height: 100%; object-fit: fill; display: block; }\n")
        sb.append(".info { padding: 6px 14px; font-size: 12px; color: #555; border-bottom: 1px solid #eee; font-family: system-ui, sans-serif; }\n")
        sb.append("@media print { .info { display: none; } }\n")
        sb.append("</style>\n</head>\n<body>\n")
        sb.append("<div class=\"info\">مطبعة البطاقات العراقية — اطبع بقياس 100% بدون تكبير/تصغير واضغط Ctrl+P للطباعة مباشرة</div>\n")
        pages.forEachIndexed { index, page ->
            val data = base64Encode(page.jpegBytes)
            sb.append("<section class=\"sheet\"><img alt=\"ورقة ${index + 1}\" src=\"data:image/jpeg;base64,")
            sb.append(data)
            sb.append("\"/></section>\n")
        }
        sb.append("<script>window.addEventListener('load', function(){ window.print(); });</script>\n")
        sb.append("</body>\n</html>\n")
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun escapeHtml(s: String): String = buildString(s.length) {
        s.forEach { c ->
            when (c) {
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '&' -> append("&amp;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(c)
            }
        }
    }
}
