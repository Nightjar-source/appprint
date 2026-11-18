package com.appprint.studio.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
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

    androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(
            modifier = modifier
                .background(canvasColor)
                .clipToBounds()
                // background tap clears the selection (empty area); cards consume their own taps first
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { viewModel.selectCard(null) },
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            when (state.zoomMode) {
                                ZoomMode.FIT -> {
                                    viewModel.setZoomMode(ZoomMode.MANUAL)
                                    viewModel.setManualZoom(115f)
                                }
                                else -> {
                                    val next = state.manualZoomPercent + 15f
                                    if (next > 200f) viewModel.zoomFit()
                                    else viewModel.setManualZoom(next)
                                }
                            }
                        })
                },
        ) {
            val availW = maxWidth.value
            val availH = maxHeight.value
            val optimal = minOf(
                (availW / paperWdp).coerceAtLeast(0.2f) * 0.97f,
                ((availH - 8f) / paperHdp).coerceAtLeast(0.2f),
                1.35f,
            ).coerceAtLeast(0.2f)
            fit = optimal
            val zoom = if (state.zoomMode == ZoomMode.FIT) optimal else optimal * (state.manualZoomPercent / 100f)
            val vScroll = rememberScrollState()
            val hScroll = rememberScrollState()

            // HTML: canvas is flat md-canvas — no dot grid, no pattern.
            // overflow-auto: the canvas scrolls; the sheet is pre-shrunk by zoom.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(vScroll)
                    .horizontalScroll(hScroll),
                contentAlignment = Alignment.Center,
            ) {
                // layout is PRE-SHRUNK by zoom — no graphicsLayer scale anywhere, so the
                // layout is exactly the visual rect and every child lines up by construction
                Box(
                    modifier = Modifier
                        .size((contentW * zoom).dp, (contentH * zoom).dp),
                ) {
                    Box(
                        modifier = Modifier
                            .offset(x = (rulerTotalDp * zoom).dp, y = (rulerTotalDp * zoom).dp)
                            .size((paperWdp * zoom).dp, (paperHdp * zoom).dp)
                            // design: 0 24px 56px -14px rgba(0,0,0,.36) + 0 6px 18px -4px rgba(0,0,0,.22) + 1px hairline ring
                            .shadow((24f * zoom).dp, androidx.compose.ui.graphics.RectangleShape, clip = false, ambientColor = Color(0x5C000000), spotColor = Color(0x5C000000))
                            .shadow((6f * zoom).dp, androidx.compose.ui.graphics.RectangleShape, clip = false, ambientColor = Color(0x38000000), spotColor = Color(0x38000000))
                            .clipToBounds()
                            .background(sheetBackgroundColor(settings)),
                    ) {
                        if (settings.frame.background == com.appprint.studio.core.model.SheetBackground.TRANSPARENT) {
                            // checkerboard under the transparent paper — like the design
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // design: repeating-conic-gradient 8mm × 8mm
                                val cell = (8f * DP_PER_MM * zoom).dp.toPx()
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
                                val m = settings.marginMm * DP_PER_MM * density.density * zoom
                                drawRect(
                                    color = Color(0xFFB32638).copy(alpha = 0.55f),
                                    topLeft = Offset(m, m),
                                    size = Size((size.width - 2 * m).coerceAtLeast(0f), (size.height - 2 * m).coerceAtLeast(0f)),
                                    style = Stroke(
                                        width = 1f.coerceAtLeast(1.dp.toPx() * zoom),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f * zoom, 8f * zoom), 0f),
                                    ),
                                )
                            }
                        }
                        if (settings.showGrid) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val step = 10f * DP_PER_MM * density.density * zoom
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
                                    com.appprint.studio.core.model.FrameStyle.DASHED -> PathEffect.dashPathEffect(floatArrayOf(16f * zoom, 12f * zoom), 0f)
                                    com.appprint.studio.core.model.FrameStyle.DOTTED -> PathEffect.dashPathEffect(floatArrayOf(1.5f * zoom, 8f * zoom), 0f)
                                }
                                drawRect(
                                    color = Color(settings.frame.colorHex),
                                    style = Stroke(width = (settings.frame.thickness * 1.6f * zoom).coerceAtLeast(1.6f), pathEffect = dash),
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
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = (8f * zoom).coerceAtLeast(6f).sp),
                            fontWeight = FontWeight.Bold,
                            color = Color.Black.copy(alpha = 0.4f),
                            modifier = Modifier.align(Alignment.TopEnd).padding(top = (8f * zoom).dp, end = (10f * zoom).dp),
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
                                dpPerMm = DP_PER_MM * zoom,
                            )
                        }
                    }
                    // rulers drawn on top like the design's ui-overlay: horizontal above the paper, vertical to its left
                    if (settings.frame.ruler) {
                        val rulerBg = androidx.compose.ui.graphics.Color.Transparent
                        RulerBar(
                            vertical = false,
                            lengthMm = settings.sheetWidthMm,
                            stripDp = paperWdp * zoom,
                            background = rulerBg,
                            scale = zoom,
                            modifier = Modifier.offset(x = (24f * zoom).dp, y = (2f * zoom).dp),
                        )
                        // design: "صفحة n / m" at the right end, inside the ruler strip row
                        Text(
                            text = "صفحة ${state.activePage + 1} / ${state.pageCount}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = (8f * zoom).coerceAtLeast(6f).sp),
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-2f * zoom).dp, y = (2f * zoom).dp),
                        )
                        RulerBar(
                            vertical = true,
                            lengthMm = settings.sheetHeightMm,
                            stripDp = paperHdp * zoom,
                            background = rulerBg,
                            scale = zoom,
                            modifier = Modifier.offset(y = (24f * zoom).dp),
                        )
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
    stripDp: Float,
    background: Color,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    val tick = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
    if (vertical) {
        Box(modifier = modifier.width((20f * scale).dp).height(stripDp.dp).background(background)) {
            var mm = 0
            while (mm <= lengthMm) {
                val big = mm % 50 == 0
                Text(
                    text = "$mm",
                    fontSize = (if (big) 9f else 8f).let { it * scale }.coerceAtLeast(5f).sp,
                    fontWeight = if (big) FontWeight.Black else FontWeight.Bold,
                    color = tick,
                    softWrap = false,
                    maxLines = 1,
                    modifier = Modifier
                        .offset(y = (mm * DP_PER_MM * scale).dp, x = (2f * scale).dp),
                )
                mm += 10
            }
        }
    } else {
        // no fixed height constraint — a 9dp cap clipped every Text to invisibility
        Box(modifier = modifier.width(stripDp.dp).background(background)) {
            var mm = 0
            while (mm <= lengthMm) {
                val big = mm % 50 == 0
                Text(
                    text = "$mm",
                    fontSize = (if (big) 11f else 10f).let { it * scale }.coerceAtLeast(7f).sp,
                    fontWeight = if (big) FontWeight.Black else FontWeight.Bold,
                    color = tick,
                    softWrap = false,
                    maxLines = 1,
                    modifier = Modifier
                        .offset(x = (mm * DP_PER_MM * scale).dp, y = (1f * scale).dp),
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
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 240.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val galleryInteraction = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(sheetGreen)
                        .pressScale(galleryInteraction)
                        .clickable(interactionSource = galleryInteraction, indication = null, onClick = onPickGallery)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.Collections,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "رفع صور البطاقات",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                    )
                }
                val cameraInteraction = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color.White)
                        .clickable(interactionSource = cameraInteraction, indication = null, onClick = onCaptureCamera)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = Color(0xFF262626),
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "الكاميرا",
                        color = Color(0xFF262626),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                }
            }
            Text(
                text = "أو جرب بأمثلة تعليمية",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = sheetGreen,
                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                modifier = Modifier.clickable(onClick = onAddDemoCards),
            )
        }
    }
}
