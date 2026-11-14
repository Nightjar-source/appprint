package com.appprint.studio.core.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Android bitmap helpers: sampled decode + border-contrast edge auto-crop
 * (scanner-style). Port of the design's auto-crop algorithm.
 */
object Enhance {

    fun decodeSampled(path: String, maxDimensionPx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > maxDimensionPx) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return runCatching { BitmapFactory.decodeFile(path, opts) }.getOrNull()
    }

    /**
     * Detects and removes a near-uniform border (scanner bed background) by sampling
     * the outermost rows/cols as background, then shrinking until content coverage
     * exceeds ~12% of a row/col. Returns the cropped bitmap, or the original when
     * no border is found.
     */
    fun cropToContent(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        if (w < 32 || h < 32) return source
        val argb = IntArray(w * h)
        source.getPixels(argb, 0, w, 0, 0, w, h)

        // sample background from the four edges
        var sumR = 0L; var sumG = 0L; var sumB = 0L; var samples = 0
        val stepX = max(1, w / 60)
        val stepY = max(1, h / 60)
        for (x in 0 until w step stepX) {
            val a = argb[x]; val b = argb[(h - 1) * w + x]
            sumR += (a shr 16) and 0xFF; sumG += (a shr 8) and 0xFF; sumB += a and 0xFF; samples++
            sumR += (b shr 16) and 0xFF; sumG += (b shr 8) and 0xFF; sumB += b and 0xFF; samples++
        }
        for (y in 0 until h step stepY) {
            val a = argb[y * w]; val b = argb[y * w + (w - 1)]
            sumR += (a shr 16) and 0xFF; sumG += (a shr 8) and 0xFF; sumB += a and 0xFF; samples++
            sumR += (b shr 16) and 0xFF; sumG += (b shr 8) and 0xFF; sumB += b and 0xFF; samples++
        }
        val bgR = sumR.toFloat() / samples
        val bgG = sumG.toFloat() / samples
        val bgB = sumB.toFloat() / samples

        fun isBgPixel(idx: Int): Boolean {
            val dr = ((argb[idx] shr 16) and 0xFF) - bgR
            val dg = ((argb[idx] shr 8) and 0xFF) - bgG
            val db = (argb[idx] and 0xFF) - bgB
            return sqrt(dr * dr + dg * dg + db * db) < 44f
        }

        fun rowBgRatio(y: Int): Float {
            var bg = 0; var total = 0
            for (x in 0 until w step 3) { total++; if (isBgPixel(y * w + x)) bg++ }
            return bg.toFloat() / total
        }

        fun colBgRatio(x: Int): Float {
            var bg = 0; var total = 0
            for (y in 0 until h step 3) { total++; if (isBgPixel(y * w + x)) bg++ }
            return bg.toFloat() / total
        }

        var top = 0
        var bottom = h - 1
        var left = 0
        var right = w - 1
        while (top < h - 12 && rowBgRatio(top) > 0.88f) top++
        while (bottom > top + 12 && rowBgRatio(bottom) > 0.88f) bottom--
        while (left < w - 12 && colBgRatio(left) > 0.88f) left++
        while (right > left + 12 && colBgRatio(right) > 0.88f) right--

        val cropW = right - left + 1
        val cropH = bottom - top + 1
        val sourceArea = w.toFloat() * h
        val cropArea = cropW.toFloat() * cropH
        // nothing meaningful to crop, or we'd lose the card entirely — keep original
        if (cropW < w * 0.35f || cropH < h * 0.35f || cropArea > sourceArea * 0.97f) {
            return source
        }
        return Bitmap.createBitmap(source, left, top, cropW, cropH)
    }
}
