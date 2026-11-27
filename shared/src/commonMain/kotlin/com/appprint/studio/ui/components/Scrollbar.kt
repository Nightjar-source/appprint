package com.appprint.studio.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

/** Soft fog at the top/bottom of scroll areas — content fades instead of being cut. */
fun Modifier.fogEdges(
    color: Color,
    height: Dp = 18.dp,
    edge: Color = Color.Black.copy(alpha = 0.10f),
): Modifier = drawWithContent {
    drawContent()
    val h = height.toPx().coerceAtMost(size.height / 2f)
    if (h <= 0f) return@drawWithContent
    drawRect(
        brush = Brush.verticalGradient(listOf(color, Color.Transparent)),
        topLeft = Offset.Zero,
        size = Size(size.width, h),
    )
    drawRect(
        brush = Brush.verticalGradient(listOf(Color.Transparent, color)),
        topLeft = Offset(0f, size.height - h),
        size = Size(size.width, h),
    )
    // 1dp hairline at both fade boundaries so content edges read as soft, not cut.
    val line = 1.dp.toPx()
    drawRect(color = edge, topLeft = Offset.Zero, size = Size(size.width, line))
    drawRect(color = edge, topLeft = Offset(0f, size.height - line), size = Size(size.width, line))
}
