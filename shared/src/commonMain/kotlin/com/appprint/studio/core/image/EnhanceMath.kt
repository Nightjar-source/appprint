package com.appprint.studio.core.image

import com.appprint.studio.core.model.EnhanceMode
import com.appprint.studio.core.model.EnhanceSettings
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Pure-math evaluation of the scanner enhance pipeline — the exact formulas
 * from the design's `processCardPixels`, platform-free so both the Android
 * renderer (export at 300 DPI) and the JVM/desktop target can apply them.
 *
 * The SAME math drives the on-screen ColorMatrix preview (via [colorMatrixValues])
 * and the per-pixel export pass (via [applyToPixels]).
 */
object EnhanceMath {

    /** Average scanner white-lift used by the `shadow` toggle. */
    fun shadowLift(luma: Float): Float =
        if (luma > 145f) ((luma - 145f) / 110f) * 52f else 0f

    /** Contrast factor exactly as the design derives it (contrast −100 → ±255 range). */
    fun contrastFactor(contrastPercent: Float): Float {
        val c = (contrastPercent - 100f) * 2.55f
        return (259f * (c + 255f)) / (255f * (259f - c))
    }

    /** Combined saturation scale relative to luma (1 = unchanged). */
    fun saturationFactor(saturationPercent: Float): Float = saturationPercent / 100f

    /** Combined brightness scale (shadow lift multiplies in when enabled). */
    fun brightnessFactor(brightnessPercent: Float, shadowLiftOn: Boolean): Float =
        (brightnessPercent / 100f) * (if (shadowLiftOn) 1.14f else 1.0f)

    /**
     * 4×5 ColorMatrix approximation of the pipeline for the *preview* path
     * (brightness/contrast/saturation in linear space + shadow-lift bias).
     * The export path applies the exact pixel math below; preview stays fast & GPU-friendly.
     */
    fun colorMatrixValues(settings: EnhanceSettings): FloatArray {
        val m = FloatArray(20)
        val b = brightnessFactor(settings.brightness, settings.shadowLift)
        val cf = contrastFactor(settings.contrast)
        val sf = saturationFactor(settings.saturation)

        val rs = 0.299f; val gs = 0.587f; val bs = 0.114f

        fun row(r: Float, g: Float, bl: Float, cOffset: Float, dst: Int) {
            m[dst] = r * cf * b
            m[dst + 1] = g * cf * b
            m[dst + 2] = bl * cf * b
            m[dst + 4] = cOffset * cf
        }

        when (settings.mode) {
            EnhanceMode.COLOR -> {
                // mix saturation towards luma with factor sf
                val satR = rs * (1 - sf); val satG = gs * (1 - sf); val satB = bs * (1 - sf)
                row(satR + sf, satG, satB, 128f * (1f - cf), 0)
                row(satR, satG + sf, satB, 128f * (1f - cf), 5)
                row(satR, satG, satB + sf, 128f * (1f - cf), 10)
                m[18] = 1f
            }
            EnhanceMode.GRAY -> {
                row(rs * cf * b, gs * cf * b, bs * cf * b, 128f * (1f - cf), 0)
                row(rs * cf * b, gs * cf * b, bs * cf * b, 128f * (1f - cf), 5)
                row(rs * cf * b, gs * cf * b, bs * cf * b, 128f * (1f - cf), 10)
                m[18] = 1f
            }
            EnhanceMode.BW -> {
                // hard threshold look: amplify contrast strongly, gray goes white
                val k = cf * 1.55f * b * 1.08f
                row(rs * k, gs * k, bs * k, 128f * (1f - k) + 22f, 0)
                row(rs * k, gs * k, bs * k, 128f * (1f - k) + 22f, 5)
                row(rs * k, gs * k, bs * k, 128f * (1f - k) + 22f, 10)
                m[18] = 1f
            }
        }
        return m
    }

    /**
     * Exact per-pixel pipeline, in-place on an ARGB int array (row-major, [stride] = width).
     * [sharpnessPercent] > 100 applies the design's 1-ring unsharp mask pass (requires full image).
     */
    fun applyToPixels(
        pixels: IntArray,
        width: Int,
        height: Int,
        settings: EnhanceSettings,
    ) {
        val brightF = brightnessFactor(settings.brightness, settings.shadowLift)
        val cf = contrastFactor(settings.contrast)
        val sf = saturationFactor(settings.saturation)
        val grayish = settings.mode != EnhanceMode.COLOR

        for (i in pixels.indices) {
            var r = (pixels[i] shr 16) and 0xFF
            var g = (pixels[i] shr 8) and 0xFF
            var b = pixels[i] and 0xFF

            if (settings.shadowLift) {
                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                val lift = shadowLift(lum)
                r += lift.toInt(); g += lift.toInt(); b += lift.toInt()
            }

            r = (r * brightF).toInt()
            g = (g * brightF).toInt()
            b = (b * brightF).toInt()

            r = (cf * (r - 128f) + 128f).toInt()
            g = (cf * (g - 128f) + 128f).toInt()
            b = (cf * (b - 128f) + 128f).toInt()

            if (grayish) {
                var lum = (0.299f * r + 0.587f * g + 0.114f * b)
                if (settings.mode == EnhanceMode.BW) {
                    lum = if (lum > 140f) min(255f, lum * 1.2f + 22f) else max(0f, lum * 0.7f - 15f)
                }
                r = lum.toInt(); g = lum.toInt(); b = lum.toInt()
            } else if (abs(sf - 1f) > 0.001f) {
                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                r = (lum + (r - lum) * sf).toInt()
                g = (lum + (g - lum) * sf).toInt()
                b = (lum + (b - lum) * sf).toInt()
            }

            r = r.coerceIn(0, 255); g = g.coerceIn(0, 255); b = b.coerceIn(0, 255)
            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        val sharp = settings.sharpness
        if (sharp > 100f && width > 4 && height > 4) {
            val amount = ((sharp - 100f) / 80f) * 0.72f
            val copy = pixels.copyOf()
            for (y in 1 until height - 1) {
                val row = y * width
                for (x in 1 until width - 1) {
                    val idx = row + x
                    for (chShift in intArrayOf(16, 8, 0)) {
                        val center = (copy[idx] shr chShift) and 0xFF
                        val neighbors =
                            ((copy[idx - width] shr chShift) and 0xFF) +
                                ((copy[idx + width] shr chShift) and 0xFF) +
                                ((copy[idx - 1] shr chShift) and 0xFF) +
                                ((copy[idx + 1] shr chShift) and 0xFF)
                        val out = (center + amount * (4 * center - neighbors)).toInt().coerceIn(0, 255)
                        val mask = (0xFF shl chShift).inv()
                        pixels[idx] = (pixels[idx] and mask) or (out shl chShift)
                    }
                }
            }
        }
    }
}
