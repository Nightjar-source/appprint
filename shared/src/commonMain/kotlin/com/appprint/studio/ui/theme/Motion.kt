package com.appprint.studio.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.Dp

/**
 * Ultra-Fluid Spring engine — design tokens from the approved HTML:
 *   Fast   : cubic-bezier(0.28, 1.38, 0.42, 1)
 *   Default: cubic-bezier(0.20, 1.24, 0.34, 1)
 *   Silk   : cubic-bezier(0.16, 1.12, 0.28, 1)
 *   Effects: cubic-bezier(0.18, 0.00, 0.12, 1.0)
 *
 * Expressive MotionScheme already covers Material components; these presets are
 * for direct manual animations (droplets, sheets, press feedback).
 */
object Motion {
    /** Damping/stiffness pairs closest to the design's cubic-bezier curves. */
    fun fast(): FiniteAnimationSpec<Float> = spring(dampingRatio = 0.72f, stiffness = 460f)

    fun default(): FiniteAnimationSpec<Float> = spring(dampingRatio = 0.78f, stiffness = 310f)

    fun silk(): FiniteAnimationSpec<Float> = spring(dampingRatio = 0.84f, stiffness = 240f)

    /** Effects curve — fast attack, soft settle. */
    fun effects(): FiniteAnimationSpec<Float> = spring(dampingRatio = 0.96f, stiffness = 380f)

    fun fastDp(): FiniteAnimationSpec<Dp> = spring(dampingRatio = 0.72f, stiffness = 460f)

    fun defaultDp(): FiniteAnimationSpec<Dp> = spring(dampingRatio = 0.78f, stiffness = 310f)

    fun silkDp(): FiniteAnimationSpec<Dp> = spring(dampingRatio = 0.84f, stiffness = 240f)

    /** Navigation pill indicator glide — slightly bouncy, per the liquid-glass reference. */
    fun navIndicator(): FiniteAnimationSpec<Float> = spring(dampingRatio = 0.55f, stiffness = 420f)
}
