package com.appprint.studio.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Translucent capsule scroll indicator drawn *behind* the content.
 * It never reserves layout space — the design's fake "scroll gutters" were just padding.
 */
@Composable
fun Modifier.overlayVerticalScrollbar(
    state: ScrollState,
    width: Dp = 3.dp,
): Modifier {
    val barColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
    return drawBehind {
        if (state.maxValue <= 0) return@drawBehind
        val viewport = size.height
        val content = state.maxValue + viewport
        val minThumb = 40.dp.toPx().coerceAtMost(viewport)
        val thumbH = (viewport * viewport / content).coerceAtLeast(minThumb)
        val inset = 2.dp.toPx()
        val barW = width.toPx().coerceAtMost(size.width / 4f)
        val y = (state.value.toFloat() / state.maxValue.toFloat()) * (viewport - thumbH)
        drawRoundRect(
            color = barColor,
            topLeft = Offset(size.width - barW - inset, y),
            size = Size(barW, thumbH),
            cornerRadius = CornerRadius(barW / 2f),
        )
    }
}
