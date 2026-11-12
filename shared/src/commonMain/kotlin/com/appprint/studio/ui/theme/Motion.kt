package com.appprint.studio.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring

/**
 * Motion tokens — Material 3 Expressive motion theming expressed as Compose specs.
 *
 * Two families, exactly as the M3 spec splits them:
 *  - SPATIAL: anything that moves, grows or travels on screen. Physical spring with a
 *    slight expressive overshoot, so a sheet or a pill reads as mass, not as a tween.
 *  - EFFECTS: colour, opacity, elevation and tint. Spring with dampingRatio = 1 — a
 *    crisp, non-overshooting settle, because a bouncing colour looks like a glitch.
 *
 * Both families honour the platform "remove animations" setting automatically, since
 * Compose drives every spec through the system MotionDurationScale.
 */
object Motion {
    /** Default spatial motion — 420 stiffness / 0.82 damping ≈ 260ms settle. */
    fun <T> spatial(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.82f, stiffness = 420f)

    /** Fast spatial — press feedback, selection pops. Nothing over 160ms. */
    fun <T> spatialFast(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.9f, stiffness = 900f)

    /** Slow spatial — large surfaces: sheets, panels, page swaps. */
    fun <T> spatialSlow(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.78f, stiffness = 220f)

    /** Default effects motion — colour, alpha, tint (no overshoot). */
    fun <T> effects(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 1f, stiffness = 1600f)

    /** Fast effects — hover/press tints that must feel instant. */
    fun <T> effectsFast(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 1f, stiffness = 2800f)

    /** Emphasized deceleration — entrances that must arrive and stay. */
    val EmphasizedDecel: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Emphasized acceleration — exits that must leave quickly. */
    val EmphasizedAccel: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Emphasized standard — on-screen morphs without overshoot. */
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}
