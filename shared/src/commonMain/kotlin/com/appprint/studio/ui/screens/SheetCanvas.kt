package com.appprint.studio.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.appprint.studio.core.model.AppTheme
import com.appprint.studio.ui.TabatiViewModel
import com.appprint.studio.ui.TabatiUiState
import com.appprint.studio.ui.ZoomMode

/** Compose dp per millimetre (96 dp/inch). */
private const val DP_PER_MM = 3.7795276f

/**
 * Interactive sheet — double-tap zoom, pinch/pan, cards with move/rotate/resize,
 * ruler bars, margin guides. Internally pinned to LTR (the paper is a physical object).
 */
@Composable
fun SheetCanvas(
    state: TabatiUiState,
    viewModel: TabatiViewModel,
    modifier: Modifier = Modifier,
) {
    val settings = state.settings
    val density = LocalDensity.current
    val paperWdp = settings.sheetWidthMm * DP_PER_MM
    val paperHdp = settings.sheetHeightMm * DP_PER_MM
    val rulerDp = if (settings.frame.ruler) 24f else 0f
    val contentW = paperWdp + rulerDp
    val contentH = paperHdp + rulerDp

    var panX by remember(settings.sheetWidthMm, settings.sheetHeightMm) { mutableFloatStateOf(0f) }
    var panY by remember(settings.sheetWidthMm, settings.sheetHeightMm) { mutableFloatStateOf(0f) }
    var fit by remember { mutableFloatStateOf(0.5f) }

    val darkCanvas = when (state.theme) {
        AppTheme.DARK -> true
        AppTheme.LIGHT -> false
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }
    val canvasColor = if (darkCanvas) {
        com.appprint.studio.ui.theme.TabatiCanvasDark
    } else {
        com.appprint.studio.ui.theme.TabatiCanvasLight
    }
    val dotColor = if (darkCanvas) Color(0xFF2B3230) else Color(0xFF8F8C82)

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        panX += panChange.x
        panY += panChange.y
        val basePercent = when (state.zoomMode) {
            ZoomMode.FIT -> fit * 100f
            ZoomMode.MANUAL -> state.manualZoomPercent
        }
        viewModel.setZoomMode(ZoomMode.MANUAL)
        val target = (basePercent * zoomChange / fit).coerceIn(50f, 200f)
        viewModel.setManualZoom(target)
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(
            modifier = modifier
                .background(canvasColor)
                .clipToBounds()
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        when {
                            state.zoomMode == ZoomMode.FIT -> {
                                viewModel.setZoomMode(ZoomMode.MANUAL)
                                viewModel.setManualZoom(160f)
                            }
                            state.manualZoomPercent < 130f -> viewModel.setManualZoom(220f)
                            else -> viewModel.setZoomMode(ZoomMode.FIT)
                        }
                    })
                }
                .transformable(state = transformableState),
        ) {
            val availW = maxWidth.value
            val availH = maxHeight.value
            val optimal = minOf(
                ((availW - 32f) / contentW).coerceAtLeast(0.2f),
                ((availH - 42f) / contentH).coerceAtLeast(0.2f),
                1.35f,
            ).coerceAtLeast(0.2f)
            fit = optimal
            val zoom = if (state.zoomMode == ZoomMode.FIT) optimal else optimal * (state.manualZoomPercent / 100f)

            // blueprint dot backdrop
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 26.dp.toPx()
                val dot = 1.1.dp.toPx()
                var x = step / 2
                while (x < size.width) {
                    var y = step / 2
                    while (y < size.height) {
                        drawCircle(dotColor.copy(alpha = 0.35f), radius = dot, center = Offset(x, y))
                        y += step
                    }
                    x += step
                }
            }

            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(contentW.dp, contentH.dp)
                        .graphicsLayer {
                            scaleX = zoom
                            scaleY = zoom
                            translationX = panX
                            translationY = panY
                        },
                ) {
                    // rulers: horizontal on top of the paper, vertical to its left
                    if (settings.frame.ruler) {
                        val rulerBg = if (darkCanvas) Color(0xFF161D1A) else Color(0xFFE6ECE8)
                        RulerBar(
                            vertical = false,
                            lengthMm = settings.sheetWidthMm,
                            stripDp = paperWdp.dp,
                            background = rulerBg,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = rulerDp.dp),
                        )
                        RulerBar(
                            vertical = true,
                            lengthMm = settings.sheetHeightMm,
                            stripDp = paperHdp.dp,
                            background = rulerBg,
                            modifier = Modifier
                                .align(Alignment.BottomStart),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(paperWdp.dp, paperHdp.dp)
                            .shadow(18.dp, RoundedCornerShape(3.dp))
                            .clip(RoundedCornerShape(3.dp))
                            .background(sheetBackgroundColor(settings)),
                    ) {
                        if (settings.frame.guides) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val m = settings.marginMm * DP_PER_MM * density.density
                                drawRect(
                                    color = Color(0xFFB32638).copy(alpha = 0.55f),
                                    topLeft = Offset(m, m),
                                    size = Size(size.width - 2 * m, size.height - 2 * m),
                                    style = Stroke(
                                        width = 1.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
                                    ),
                                )
                            }
                        }
                        state.cardsOnPage.forEach { card ->
                            CardOnSheet(
                                card = card,
                                state = state,
                                viewModel = viewModel,
                                dpPerMm = DP_PER_MM,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Simple ruler bar: tick every 10 mm, long tick every 50 mm. */
@Composable
private fun RulerBar(
    vertical: Boolean,
    lengthMm: Float,
    stripDp: androidx.compose.ui.unit.Dp,
    background: Color,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val tick = Color(0xFF3F4943)
    val stepPx = 10f * DP_PER_MM * density.density
    val bigStepPx = 50f * DP_PER_MM * density.density
    if (vertical) {
        Canvas(modifier = modifier.width(24.dp).height(stripDp).background(background)) {
            val lengthPx = lengthMm * DP_PER_MM * density.density
            var pos = 0f
            while (pos <= lengthPx) {
                val big = (pos % bigStepPx) < 0.5f
                val len = if (big) size.width else size.width * 0.55f
                drawLine(tick, Offset(size.width, pos), Offset(size.width - len, pos), strokeWidth = 1.4f)
                pos += stepPx
            }
            drawLine(tick, Offset(size.width - 1f, 0f), Offset(size.width - 1f, size.height), strokeWidth = 1.6f)
        }
    } else {
        Canvas(modifier = modifier.height(24.dp).width(stripDp).background(background)) {
            val lengthPx = lengthMm * DP_PER_MM * density.density
            var pos = 0f
            while (pos <= lengthPx) {
                val big = (pos % bigStepPx) < 0.5f
                val len = if (big) size.height else size.height * 0.55f
                drawLine(tick, Offset(pos, size.height), Offset(pos, size.height - len), strokeWidth = 1.4f)
                pos += stepPx
            }
            drawLine(tick, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), strokeWidth = 1.6f)
        }
    }
}

private fun sheetBackgroundColor(settings: com.appprint.studio.core.model.SheetSettings): Color =
    when (settings.frame.background) {
        com.appprint.studio.core.model.SheetBackground.WHITE -> Color.White
        com.appprint.studio.core.model.SheetBackground.OFFWHITE -> Color(0xFFFDFAF1)
        com.appprint.studio.core.model.SheetBackground.TRANSPARENT -> Color(0x66FFFFFF)
    }
