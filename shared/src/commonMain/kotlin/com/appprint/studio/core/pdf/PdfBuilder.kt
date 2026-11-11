package com.appprint.studio.core.pdf

/**
 * Minimal, dependency-free PDF writer — one image (JPEG/DCTDecode) per page.
 * Produces byte-identical layout guarantees across targets because every platform
 * rasterizes sheets itself and hands us JPEG page bitmaps.
 * (Mirrors the hand-rolled generator in the original HTML design tool.)
 */
object PdfBuilder {

    data class Page(
        val jpegBytes: ByteArray,
        val pixelWidth: Int,
        val pixelHeight: Int,
        val widthPt: Float,
        val heightPt: Float,
    ) {
        override fun equals(other: Any?): Boolean = other === this
        override fun hashCode(): Int = System.identityHashCode(this)
    }

    fun build(pages: List<Page>, title: String = "iraqi-cards-print"): ByteArray {
        val buffer = ByteArrayBuilder()

        fun pushString(s: String) = buffer.write(s.toByteArray(Charsets.US_ASCII))

        val offsets = IntArray(4 + pages.size * 3)
        var objNum = 0

        pushString("%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n")

        fun markObject(num: Int) {
            if (num < offsets.size) offsets[num] = buffer.size
        }

        // Object 1 — Catalog
        markObject(1)
        pushString("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")

        // Object 2 — Pages
        markObject(2)
        val kids = pages.indices.joinToString(" ") { (3 + it * 3).toString() + " 0 R" }
        pushString("2 0 obj\n<< /Type /Pages /Kids [$kids] /Count ${pages.size} >>\nendobj\n")

        fun stream(bytes: ByteArray, dict: String) {
            pushString("<< $dict /Length ${bytes.size} >>\nstream\n")
            buffer.write(bytes)
            pushString("\nendstream\nendobj\n")
        }

        pages.forEachIndexed { index, page ->
            val pageObj = 3 + index * 3
            val imageObj = pageObj + 1
            val contentObj = pageObj + 2

            val content = "q\n${fmt(page.widthPt)} 0 0 ${fmt(page.heightPt)} 0 0 cm\n/Im$index Do\nQ\n".toByteArray(Charsets.US_ASCII)

            // Page object
            markObject(pageObj)
            pushString(
                "$pageObj 0 obj\n" +
                    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${fmt(page.widthPt)} ${fmt(page.heightPt)}] " +
                    "/Resources << /XObject << /Im$index $imageObj 0 R >> /ProcSet [/PDF /ImageC] >> " +
                    "/Contents $contentObj 0 R >>\nendobj\n"
            )

            // Image XObject
            markObject(imageObj)
            pushString("$imageObj 0 obj\n")
            stream(
                page.jpegBytes,
                "/Type /XObject /Subtype /Image /Width ${page.pixelWidth} /Height ${page.pixelHeight} " +
                    "/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode",
            )

            // Contents stream
            markObject(contentObj)
            pushString("$contentObj 0 obj\n")
            stream(content, "")
        }

        val xrefStart = buffer.size
        val objectCount = 3 + pages.size * 3
        pushString("xref\n0 ${objectCount + 1}\n")
        pushString("0000000000 65535 f \n")
        for (num in 1..objectCount) {
            val offset = if (num < offsets.size) offsets[num] else 0
            pushString(offset.toString().padStart(10, '0') + " 00000 n \n")
        }
        pushString(
            "trailer\n<< /Size ${objectCount + 1} /Root 1 0 R " +
                "/Info << /Title (${escapePdfString(title)}) /Producer (AppPrint Studio) >> >>\n" +
                "startxref\n$xrefStart\n%%EOF"
        )
        return buffer.toByteArray()
    }

    private fun fmt(v: Float): String {
        val rounded = kotlin.math.round(v * 100f) / 100f
        return if (rounded == rounded.toInt().toFloat()) {
            rounded.toInt().toString()
        } else {
            rounded.toString().trimEnd('0').trimEnd('.')
        }
    }

    private fun escapePdfString(value: String): String =
        value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")

    private class ByteArrayBuilder {
        private var bytes = ByteArray(16 * 1024)
        var size = 0
            private set

        fun write(chunk: ByteArray) {
            ensure(size + chunk.size)
            chunk.copyInto(bytes, size)
            size += chunk.size
        }

        private fun ensure(capacity: Int) {
            if (capacity <= bytes.size) return
            var newSize = bytes.size * 2
            while (newSize < capacity) newSize *= 2
            bytes = bytes.copyOf(newSize)
        }

        fun toByteArray(): ByteArray = bytes.copyOf(size)
    }
}
