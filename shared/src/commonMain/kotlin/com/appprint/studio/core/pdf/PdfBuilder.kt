package com.appprint.studio.core.pdf

/**
 * Minimal, dependency-free PDF writer — one JPEG image per page, exactly the
 * structure produced by the design's `buildNativePdf`:
 *  - MediaBox in points (1 mm = 2.83464567 pt)
 *  - 300 DPI DCTDecode images
 * Every platform rasterises sheets itself and hands us JPEG bytes.
 */
object PdfBuilder {

    const val MM_TO_PT: Float = 2.83464567f

    data class Page(
        val jpegBytes: ByteArray,
        val pixelWidth: Int,
        val pixelHeight: Int,
        val widthPt: Float,
        val heightPt: Float,
    ) {
        override fun equals(other: Any?): Boolean = this === other
        override fun hashCode(): Int = System.identityHashCode(this)
    }

    fun build(pages: List<Page>, title: String = "tabati-print"): ByteArray {
        val buffer = ByteArrayBuilder()

        fun pushString(s: String) = buffer.write(s.toByteArray(Charsets.US_ASCII))

        buffer.write(byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34, 0x0A, 0x25, 0xE2.toByte(), 0xE3.toByte(), 0xCF.toByte(), 0xD3.toByte(), 0x0A))

        val totalObjects = 2 + pages.size * 3
        val offsets = IntArray(totalObjects + 1)
        var objectNumber = 0

        fun reserveObject(): Int = ++objectNumber

        val catalogNum = reserveObject()
        val pagesNum = reserveObject()
        data class Obj(val num: Int, val head: String, val body: ByteArray? = null, val tail: String? = null)
        val objects = ArrayList<Obj>(totalObjects)
        objects.add(Obj(catalogNum, "$catalogNum 0 obj\n<< /Type /Catalog /Pages $pagesNum 0 R >>\nendobj\n"))
        val kids = (0 until pages.size).joinToString(" ") { idx -> "${3 + idx * 3} 0 R" }
        objects.add(Obj(pagesNum, "$pagesNum 0 obj\n<< /Type /Pages /Kids [$kids] /Count ${pages.size} >>\nendobj\n"))

        pages.forEachIndexed { idx, page ->
            val pageObj = 3 + idx * 3
            val imageObj = 4 + idx * 3
            val contentObj = 5 + idx * 3
            reserveObject(); reserveObject(); reserveObject()

            val stream = "q ${format(page.widthPt)} 0 0 ${format(page.heightPt)} 0 0 cm /Im$idx Do Q".toByteArray(Charsets.US_ASCII)

            objects.add(
                Obj(
                    pageObj,
                    "$pageObj 0 obj\n<< /Type /Page /Parent $pagesNum 0 R /MediaBox [0 0 ${format(page.widthPt)} ${format(page.heightPt)}] /Resources << /XObject << /Im$idx $imageObj 0 R >> /ProcSet [/PDF /ImageC] >> /Contents $contentObj 0 R >>\nendobj\n",
                ),
            )
            objects.add(
                Obj(
                    imageObj,
                    "$imageObj 0 obj\n<< /Type /XObject /Subtype /Image /Width ${page.pixelWidth} /Height ${page.pixelHeight} /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${page.jpegBytes.size} >>\nstream\n",
                    page.jpegBytes,
                    "\nendstream\nendobj\n",
                ),
            )
            objects.add(
                Obj(
                    contentObj,
                    "$contentObj 0 obj\n<< /Length ${stream.size} >>\nstream\n",
                    stream,
                    "\nendstream\nendobj\n",
                ),
            )
        }

        objects.sortBy { it.num }
        for (obj in objects) {
            offsets[obj.num] = buffer.size
            pushString(obj.head)
            if (obj.body != null) buffer.write(obj.body)
            if (obj.tail != null) pushString(obj.tail)
        }

        val xrefPosition = buffer.size
        val xrefBuilder = StringBuilder()
        xrefBuilder.append("xref\n0 ${totalObjects + 1}\n")
        xrefBuilder.append("0000000000 65535 f \n")
        for (i in 1..totalObjects) {
            xrefBuilder.append(pad10(offsets[i]) + " 00000 n \n")
        }
        xrefBuilder.append("trailer\n<< /Size ${totalObjects + 1} /Root $catalogNum 0 R >>\nstartxref\n$xrefPosition\n%%EOF")
        pushString(xrefBuilder.toString())
        return buffer.toByteArray()
    }

    private fun format(value: Float): String {
        val v = (value * 100f).toInt() / 100f
        return v.toString().substringBefore('.') + "." +
            (v.toString().substringAfter('.', "0") + "0").padEnd(2, '0').take(2)
    }

    private fun pad10(value: Int): String = value.toString().padStart(10, '0')

    private class ByteArrayBuilder {
        private var bytes = ByteArray(8192)
        var size: Int = 0
            private set

        fun write(chunk: ByteArray) {
            ensure(bytes.size + chunk.size)
            System.arraycopy(chunk, 0, bytes, size, chunk.size)
            size += chunk.size
        }

        private fun ensure(min: Int) {
            if (bytes.size < min) {
                val grown = ByteArray((bytes.size * 2).coerceAtLeast(min))
                System.arraycopy(bytes, 0, grown, 0, bytes.size)
                bytes = grown
            }
        }

        fun toByteArray(): ByteArray = bytes.copyOf(size)
    }
}
