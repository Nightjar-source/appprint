package com.appprint.studio.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appprint.studio.core.model.AppTheme
import com.appprint.studio.ui.TabatiViewModel
import com.appprint.studio.ui.TabatiUiState
import com.appprint.studio.ui.ZoomMode
import com.appprint.studio.ui.components.pressScale

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
    onPickGallery: () -> Unit = {},
    onCaptureCamera: () -> Unit = {},
    onAddDemoCards: () -> Unit = {},
) {
    val settings = state.settings
    val density = LocalDensity.current
    val paperWdp = settings.sheetWidthMm * DP_PER_MM
    val paperHdp = settings.sheetHeightMm * DP_PER_MM
    // design: ruler strip 20dp (w-5/h-5) + 4dp gap (mr-1) — paper starts at 24dp like ml-6
    val rulerTotalDp = if (settings.frame.ruler) 24f else 0f
    val contentW = paperWdp + rulerTotalDp
    val contentH = paperHdp + rulerTotalDp

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
                    detectTapGestures(
                        onTap = { viewModel.selectCard(null) },
                        onDoubleTap = {
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
            val targetZoom = if (state.zoomMode == ZoomMode.FIT) optimal else optimal * (state.manualZoomPercent / 100f)
            // design: fluid-spring on sheet scale (cubic-bezier(0.20, 1.24, 0.34, 1))
            val zoom by androidx.compose.animation.core.animateFloatAsState(
                targetValue = targetZoom,
                animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.78f, stiffness = 320f),
                label = "sheetZoom",
            )

            // HTML: canvas is flat md-canvas — no dot grid, no pattern
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // children are positioned in unscaled dp, so ONE graphicsLayer scale shrinks
                // layout and content identically — offsets, fonts and paper all stay registered
                Box(
                    modifier = Modifier
                        .size(contentW.dp, contentH.dp)
                        .graphicsLayer {
                            scaleX = zoom
                            scaleY = zoom
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                            translationX = panX
                            translationY = panY
                        },
                ) {
                    // rulers: horizontal on top of the paper, vertical to its left
                    if (settings.frame.ruler) {
                        val rulerBg = androidx.compose.ui.graphics.Color.Transparent
                        RulerBar(
                            vertical = false,
                            lengthMm = settings.sheetWidthMm,
                            stripDp = paperWdp.dp,
                            background = rulerBg,
                            modifier = Modifier.offset(x = 24.dp, y = 2.dp),
                        )
                        // design: "صفحة n / m" on the ruler's right end
                        Text(
                            text = "صفحة ${state.activePage + 1} / ${state.pageCount}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-2).dp, y = 1.dp),
                        )
                        RulerBar(
                            vertical = true,
                            lengthMm = settings.sheetHeightMm,
                            stripDp = paperHdp.dp,
                            background = rulerBg,
                            modifier = Modifier.offset(y = 24.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .offset(x = rulerTotalDp.dp, y = rulerTotalDp.dp)
                            .size(paperWdp.dp, paperHdp.dp)
                            // design: 0 24px 56px -14px rgba(0,0,0,.36) + 0 6px 18px -4px rgba(0,0,0,.22) + 1px hairline ring
                            .shadow(30.dp, androidx.compose.ui.graphics.RectangleShape, clip = false, ambientColor = Color(0x5C000000), spotColor = Color(0x5C000000))
                            .shadow(7.dp, androidx.compose.ui.graphics.RectangleShape, clip = false, ambientColor = Color(0x38000000), spotColor = Color(0x38000000))
                            .background(sheetBackgroundColor(settings)),
                    ) {
                        if (settings.frame.background == com.appprint.studio.core.model.SheetBackground.TRANSPARENT) {
                            // checkerboard under the transparent paper — like the design
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val cell = 8.dp.toPx()
                                val cols = (size.width / cell).toInt() + 1
                                val rows = (size.height / cell).toInt() + 1
                                for (r in 0 until rows) {
                                    for (c in 0 until cols) {
                                        if ((r + c) % 2 == 0) {
                                            drawRect(
                                                color = Color(0xFFE2E2E2),
                                                topLeft = Offset(c * cell, r * cell),
                                                size = Size(cell, cell),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (settings.frame.guides) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val m = settings.marginMm * DP_PER_MM * density.density
                                drawRect(
                                    color = Color(0xFFB32638).copy(alpha = 0.55f),
                                    topLeft = Offset(m, m),
                                    size = Size((size.width - 2 * m).coerceAtLeast(0f), (size.height - 2 * m).coerceAtLeast(0f)),
                                    style = Stroke(
                                        width = 1.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
                                    ),
                                )
                            }
                        }
                        if (settings.showGrid) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val step = 10f * DP_PER_MM * density.density
                                var pos = step
                                while (pos < size.width) {
                                    drawLine(Color(0x0D000000), Offset(pos, 0f), Offset(pos, size.height), 1f)
                                    pos += step
                                }
                                pos = step
                                while (pos < size.height) {
                                    drawLine(Color(0x0D000000), Offset(0f, pos), Offset(size.width, pos), 1f)
                                    pos += step
                                }
                            }
                        }
                        if (settings.frame.sheet && settings.frame.colorHex != 0L && settings.frame.thickness > 0f) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val dash = when (settings.frame.style) {
                                    com.appprint.studio.core.model.FrameStyle.SOLID -> null
                                    com.appprint.studio.core.model.FrameStyle.DASHED -> PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                                    com.appprint.studio.core.model.FrameStyle.DOTTED -> PathEffect.dashPathEffect(floatArrayOf(1.5f, 8f), 0f)
                                }
                                drawRect(
                                    color = Color(settings.frame.colorHex),
                                    style = Stroke(width = (settings.frame.thickness * 1.6f).coerceAtLeast(1.6f), pathEffect = dash),
                                )
                            }
                        }
                        // design: 0 0 0 1px rgba(0,0,0,.08) hairline ring around the paper
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(
                                color = Color(0x14000000),
                                style = Stroke(width = 1.dp.toPx()),
                            )
                        }
                        // design: "A4 • 210 × 297 mm" caption on the paper
                        Text(
                            text = "${settings.presetKey} • ${formatCaptionDim(settings.sheetWidthMm)} × ${formatCaptionDim(settings.sheetHeightMm)} mm",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color.Black.copy(alpha = 0.4f),
                            modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 10.dp),
                        )
                        if (state.cards.isEmpty()) {
                            EmptyPaperMessage(
                                modifier = Modifier.fillMaxSize(),
                                onPickGallery = onPickGallery,
                                onCaptureCamera = onCaptureCamera,
                                onAddDemoCards = onAddDemoCards,
                            )
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

/** Ruler bar: tick every 10 mm, long tick + number every 50 mm, small numbers every 10 mm — like the design. */
@Composable
private fun RulerBar(
    vertical: Boolean,
    lengthMm: Float,
    stripDp: androidx.compose.ui.unit.Dp,
    background: Color,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val tick = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
    if (vertical) {
        Box(modifier = modifier.width(20.dp).height(stripDp).background(background)) {
            var mm = 0
            while (mm <= lengthMm) {
                val big = mm % 50 == 0
                Text(
                    text = "$mm",
                    fontSize = if (big) 9.sp else 8.sp,
                    fontWeight = if (big) FontWeight.Black else FontWeight.Bold,
                    color = tick,
                    modifier = Modifier
                        .offset(y = (mm * DP_PER_MM).dp, x = 2.dp),
                )
                mm += 10
            }
        }
    } else {
        Box(modifier = modifier.height(20.dp).width(stripDp).background(background)) {
            var mm = 0
            while (mm <= lengthMm) {
                val big = mm % 50 == 0
                Text(
                    text = "$mm",
                    fontSize = if (big) 9.sp else 8.sp,
                    fontWeight = if (big) FontWeight.Black else FontWeight.Bold,
                    color = tick,
                    modifier = Modifier
                        .offset(x = (mm * DP_PER_MM).dp, y = 2.dp),
                )
                mm += 10
            }
        }
    }
}

private fun sheetBackgroundColor(settings: com.appprint.studio.core.model.SheetSettings): Color =
    when (settings.frame.background) {
        com.appprint.studio.core.model.SheetBackground.WHITE -> Color.White
        com.appprint.studio.core.model.SheetBackground.OFFWHITE -> Color(0xFFFDFAF1)
        com.appprint.studio.core.model.SheetBackground.TRANSPARENT -> Color(0xFFFFFFFF)
    }

private fun formatCaptionDim(v: Float): String =
    "%.1f".format(v).trimEnd('0').trimEnd('.')

/** Design: empty paper state lives inside the sheet — mint icon + caption + action buttons + demo link. */
@Composable
private fun EmptyPaperMessage(
    modifier: Modifier = Modifier,
    onPickGallery: () -> Unit,
    onCaptureCamera: () -> Unit,
    onAddDemoCards: () -> Unit,
) {
    val sheetGreen = Color(0xFF006C47)
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Collections,
                    contentDescription = null,
                    tint = sheetGreen,
                    modifier = Modifier.size(32.dp),
                )
            }
            Text(
                text = "الورقة فارغة — ارفع صور بطاقاتك للبدء",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF262626),
            )
            Text(
                text = "افتح الاستوديو لاختيار الصور أو التقطها بالكاميرا وسيتم التعرف على أبعادها الحقيقية وترتيبها تلقائياً",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF737373),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.widthIn(max = 240.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val galleryInteraction = remember { MutableInteractionSource() }
                val cameraInteraction = remember { MutableInteractionSource() }
                // رفع صور البطاقات — the design's primary green pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .pressScale(galleryInteraction)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(sheetGreen)
                        .clickable(interactionSource = galleryInteraction, indication = null, onClick = onPickGallery)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.Collections,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "رفع صور البطاقات",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                    )
                }
                // الكاميرا — white pill with border
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .pressScale(cameraInteraction)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFD4D4D4), RoundedCornerShape(percent = 50))
                        .clickable(interactionSource = cameraInteraction, indication = null, onClick = onCaptureCamera)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = Color(0xFF262626),
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "الكاميرا",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF262626),
                    )
                }
            }
            Text(
                text = "أو جرب بأمثلة تعليمية",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = sheetGreen,
                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                modifier = Modifier.clickable(onClick = onAddDemoCards).padding(4.dp),
            )
        }
    }
}
