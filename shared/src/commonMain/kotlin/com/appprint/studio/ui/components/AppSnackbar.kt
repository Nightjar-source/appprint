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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import com.appprint.studio.ui.theme.Motion
import kotlinx.coroutines.launch
import kotlin.math.abs

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
    val drag = remember { Animatable(0f) }
    val dragScope = rememberCoroutineScope()
    val dragX = drag.value
    val dismissThreshold = with(density) { 56.dp.toPx() }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        progress.animateTo(
            targetValue = if (visible) 1f else 0f,
            animationSpec = if (visible) Motion.fast() else Motion.effects(),
        )
    }
    val floatProgress = progress.value
    Row(
        modifier = modifier
            .testTag("app-toast")
            .alpha(floatProgress)
            // Dynamic-Island style: expands horizontally, settles vertically and lifts in.
            // translationX is in raw pixels (unlike the RTL-aware Modifier.offset), which
            // fixes the reversed swipe direction in Arabic.
            .graphicsLayer {
                translationX = dragX
                scaleX = 0.62f + 0.38f * floatProgress
                scaleY = 0.78f + 0.22f * floatProgress
                translationY = (1f - floatProgress) * -10f
            }
            .widthIn(max = 360.dp)
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
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragScope.launch { drag.snapTo(drag.value + amount.x) }
                                },
                                onDragEnd = {
                                    if (abs(drag.value) >= dismissThreshold) {
                                        dismiss?.invoke()
                                    } else {
                                        // Snap back with a spring — never a hard jump.
                                        dragScope.launch { drag.animateTo(0f, animationSpec = Motion.default()) }
                                    }
                                },
                                onDragCancel = {
                                    dragScope.launch { drag.animateTo(0f, animationSpec = Motion.default()) }
                                },
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
