package com.appprint.studio.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import com.appprint.studio.ui.theme.Motion

/**
 * Spring press feedback — the single source of touch motion in the app.
 *
 * Every tappable surface uses this instead of a bespoke scale animation, so a chip,
 * a circular app-bar button and a nav item all answer a finger with the same
 * 0.94 → 1.0 spring settle. 140ms in, spring out — feedback, not decoration.
 */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.94f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = Motion.spatialFast(),
        label = "pressScale",
    )
    return this.scale(scale)
}
