package com.appprint.studio.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import com.appprint.studio.ui.theme.Motion
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Island-style toast — the design's `.m3-float-enter` pill:
 * on-surface pill, outlineVariant border, soft shadow, check inside a primary-container
 * circle (morphing star-style loader while busy).
 *
 * Show/hide are fluid like the design's fluid float (scale 0.72 + lift → settle) and the
 * M3 Expressive Island expand/collapse — driven by the spring engine, no frame-by-frame
 * manual work.
 */
@Composable
fun AppSnackbar(
    message: String,
    busy: Boolean = false,
    visible: Boolean = true,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val dismiss by rememberUpdatedState(onDismiss)
    var dragX by remember { mutableFloatStateOf(0f) }
    val dismissThreshold = with(density) { 56.dp.toPx() }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        progress.animateTo(
            targetValue = if (visible) 1f else 0f,
            animationSpec = if (visible) Motion.default() else Motion.effects(),
        )
    }
    val floatProgress = progress.value
    // design fluidFloatEnter: scale(0.72) translateY(-10px) → scale(1.04) translateY(1px) → scale(1) translateY(0)
    val floatScale = 0.72f + 0.32f * floatProgress
    Row(
        modifier = modifier
            .testTag("app-toast")
            .offset { IntOffset(dragX.roundToInt(), 0) }
            .alpha(floatProgress)
            .graphicsLayer {
                scaleX = floatScale
                scaleY = floatScale
                translationY = (1f - floatProgress) * -10f
            }
            .height(36.dp)
            .shadow(10.dp, RoundedCornerShape(percent = 50), clip = false)
            .clip(RoundedCornerShape(percent = 50))
            .background(scheme.onSurface)
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(percent = 50))
            .then(
                if (dismiss != null) {
                    Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                        ) { dismiss?.invoke() }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { change, drag ->
                                    change.consume()
                                    dragX += drag.x
                                },
                                onDragEnd = {
                                    if (abs(dragX) >= dismissThreshold) dismiss?.invoke() else dragX = 0f
                                },
                                onDragCancel = { dragX = 0f },
                            )
                        }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(scheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (busy) {
                CircularProgressIndicator(
                    color = scheme.onPrimaryContainer,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = scheme.onPrimaryContainer,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
        Text(
            text = message,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold),
            color = scheme.surface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
