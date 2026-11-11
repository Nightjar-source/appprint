package com.appprint.studio.core.image

import com.appprint.studio.core.model.EnhanceSettings
import kotlin.math.min

/**
 * Platform-free pixel kernels (IntArray ARGB_8888 layout).
 * Shared by Android (android.graphics.Bitmap) and Desktop JVM (Skia/BufferedImage) actuals.
 */
object PixelOps {

    /** Row-major 4×5 color matrix (saturation → contrast → brightness), row-major FloatArray(20). */
    fun buildEnhanceMatrix(e: EnhanceSettings): FloatArray {
        val brightness = e.brightness / 100f * 80f
        val contrast = e.contrast / 100f
        val saturation = if (e.grayscale) 0f else 1f + e.saturation / 100f

        val scale = 1f + contrast
        val offset = scale * brightness + 128f * (1f - scale)

        // saturation matrix (ITU-R BT.709 luma)
        val lr = 0.2126f
        val lg = 0.7152f
        val lb = 0.0722f
        val it = 1f - saturation
        val sat = floatArrayOf(
            lr * it + saturation, lg * it, lb * it, 0f, 0f,
            lr * it, lg * it + saturation, lb * it, 0f, 0f,
            lr * it, lg * it, lb * it + saturation, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )

        // brightness + contrast matrix
        val bc = floatArrayOf(
            scale, 0f, 0f, 0f, offset,
            0f, scale, 0f, 0f, offset,
            0f, 0f, scale, 0f, offset,
            0f, 0f, 0f, 1f, 0f,
        )

        return multiply(bc, sat)
    }

    /** 4×5 × 4×5 product (row-major, as ColorMatrix expects). */
    private fun multiply(a: FloatArray, b: FloatArray): FloatArray {
        val out = FloatArray(20)
        for (row in 0 until 4) {
            for (col in 0 until 5) {
                var sum = if (col == 4) a[row * 5 + 4] else 0f
                for (k in 0 until 4) {
                    sum += a[row * 5 + k] * b[k * 5 + col]
                }
                out[row * 5 + col] = sum
            }
        }
        return out
    }

    /** Bounding box of non-(near)-white pixels; null when the image is effectively blank. */
    data class Bounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        val width: Int get() = right - left + 1
        val height: Int get() = bottom - top + 1
    }

    fun contentBounds(pixels: IntArray, width: Int, height: Int, tolerance: Int = 242, stride: Int = 2): Bounds? {
        var left = -1; var right = -1; var top = -1; var bottom = -1
        for (y in 0 until height step stride) {
            val row = y * width
            for (x in 0 until width step stride) {
                val p = pixels[row + x]
                val a = (p ushr 24) and 0xFF
                if (a < 40) continue
                val r = (p ushr 16) and 0xFF
                val g = (p ushr 8) and 0xFF
                val b = p and 0xFF
                if (r >= tolerance && g >= tolerance && b >= tolerance) continue
                if (top < 0) top = y
                bottom = y
                if (left < 0 || x < left) left = x
                if (x > right) right = x
            }
        }
        if (top < 0) return null
        val pad = (min(width, height) * 0.006f).toInt()
        return Bounds(
            (left - pad).coerceAtLeast(0),
            (top - pad).coerceAtLeast(0),
            (right + pad).coerceAtMost(width - 1),
            (bottom + pad).coerceAtMost(height - 1),
        )
    }

    /** 3×3 unsharp-mask convolution. amount in [0, 1]. Input/output ARGB IntArrays of [width]×[height]. */
    fun sharpen(input: IntArray, width: Int, height: Int, amount: Float): IntArray {
        if (amount <= 0.01f || width < 3 || height < 3) return input.copyOf()
        val a = amount.coerceIn(0f, 0.9f)
        val out = input.copyOf()
        for (y in 1 until height - 1) {
            val row = y * width
            for (x in 1 until width - 1) {
                val i = row + x
                val c = input[i]; val l = input[i - 1]; val r = input[i + 1]
                val u = input[i - width]; val d = input[i + width]
                fun ch(px: Int, shift: Int): Int = (px ushr shift) and 0xFF
                fun mix(shift: Int): Int {
                    val v = ch(c, shift) * (1f + 4f * a) -
                        a * (ch(l, shift) + ch(r, shift) + ch(u, shift) + ch(d, shift))
                    return v.toInt().coerceIn(0, 255)
                }
                val alpha = ch(c, 24)
                out[i] = (alpha shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
            }
        }
        return out
    }
}
