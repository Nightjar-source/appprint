package com.appprint.studio.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appprint.studio.core.model.AppTheme
import com.appprint.studio.ui.TabatiUiState
import com.appprint.studio.ui.TabatiViewModel
import com.appprint.studio.ui.ZoomMode
import com.appprint.studio.ui.components.Chrome
import com.appprint.studio.ui.components.pressScale

/** Compose dp per millimetre (96 dp/inch). */
private const val DP_PER_MM = 3.7795276f

/**
 * Interactive sheet — literal port of the design's `sheet-scale-wrapper`:
 * the paper and all of its content are laid out at true 1:1 mm size and then
 * scaled by one graphics layer, exactly like CSS `transform: scale(...)`.
 * This keeps card art, fonts, badges, rulers and handles in the same proportion
 * at every zoom level. Internally pinned to LTR (a sheet is a physical object).
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
    // design: ruler strip 20px + 4px gap — the paper starts at 24px
    val rulerTotalDp = if (settings.frame.ruler) 24f else 0f
    val contentW = paperWdp + rulerTotalDp
    val contentH = paperHdp + rulerTotalDp

    val compactChrome = !Chrome.railVisible()
    val contentTopPad = Chrome.topReserve() + 46.dp
    val contentBottomPad = if (compactChrome) Chrome.bottomReserve() else 16.dp

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

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(
            modifier = modifier
                .background(canvasColor)
                .clipToBounds()
                .pointerInput(Unit) {
                    detectTapGestures(
                        // Cards consume their own tap, so this only fires on empty canvas.
                        onTap = { viewModel.selectCard(null) },
                        onDoubleTap = {
                            // Read the live state — a pointerInput block captures its
                            // composition values once and would otherwise use stale zoom.
                            val snapshot = viewModel.state.value
                            when (snapshot.zoomMode) {
                                ZoomMode.FIT -> {
                                    viewModel.setZoomMode(ZoomMode.MANUAL)
                                    viewModel.setManualZoom(115f)
                                }
                                else -> {
                                    val next = snapshot.manualZoomPercent + 15f
                                    if (next > 200f) viewModel.zoomFit()
                                    else viewModel.setManualZoom(next)
                                }
                            }
                        })
                }
                // Two-finger pinch zoom — continuous, anchored to the fit scale so 100%
                // always means "fits the screen", exactly like the zoom buttons.
                .pointerInput(Unit) {
                    awaitEachGesture {
                        // Pointer order is not guaranteed — sort by id so the two fingers
                        // we measure are always the same pair.
                        var pointers = emptyList<androidx.compose.ui.input.pointer.PointerInputChange>()
                        // Wait for the second finger; a normal one-finger tap/drag is
                        // left untouched for the canvas scroll and the cards.
                        while (pointers.size < 2) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val pressed = event.changes.filter { it.pressed }.sortedBy { it.id.value }
                            if (pressed.isEmpty()) return@awaitEachGesture
                            pointers = pressed
                        }
                        val startSpan = (pointers[1].position - pointers[0].position)
                            .getDistance()
                            .coerceAtLeast(1f)
                        val snapshot = viewModel.state.value
                        val basePercent = if (snapshot.zoomMode == ZoomMode.FIT) 100f else snapshot.manualZoomPercent
                        viewModel.setZoomMode(ZoomMode.MANUAL)
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val pressed = event.changes.filter { it.pressed }.sortedBy { it.id.value }
                            if (pressed.size < 2) {
                                // Consume the lift so the tap detector cannot turn the
                                // end of a pinch into an empty-canvas deselect.
                                event.changes.forEach { it.consume() }
                                break
                            }
                            val span = (pressed[1].position - pressed[0].position)
                                .getDistance()
                                .coerceAtLeast(1f)
                            // Multiplicative factor → every frame follows the fingers 1:1.
                            viewModel.setManualZoom(basePercent * (span / startSpan))
                            pressed.forEach { it.consume() }
                        }
                    }
                },
        ) {
            // Fit the complete physical sheet (paper + ruler) to the viewport. The few dp
            // of breathing room keep the sheet border visible without shrinking it.
            val padH = if (Chrome.isExpanded()) 108f else 116f
            val availW = (maxWidth.value - 8f).coerceAtLeast(150f)
            val availH = (maxHeight.value - padH).coerceAtLeast(170f)
            val optimal = minOf(
                availW / contentW,
                availH / contentH,
                1.35f,
            ).coerceAtLeast(0.2f)
            val fitScale = optimal.coerceIn(0.2f, 1.35f)
            val zoom = if (state.zoomMode == ZoomMode.FIT) fitScale else fitScale * (state.manualZoomPercent / 100f)
            val vScroll = rememberScrollState()
            val hScroll = rememberScrollState()

            // The sheet must never fully disappear: graphicsLayer scales the drawing but NOT
            // the scrollable layout size, so without a clamp the user can scroll far past the
            // visible (zoomed-out) paper. Clamp each axis so at least 20% of the sheet stays
            // inside the viewport in every direction.
            val scaledW = contentW * zoom
            val scaledH = contentH * zoom
            // Keep at least 20% of the SHEET itself visible (not of the viewport — the sheet
            // may be smaller than the screen when zoomed out), in both scroll directions.
            // ScrollState.value is in PIXELS, so convert the dp limit explicitly.
            val maxHScrollPx = with(density) { (0.8f * contentW * zoom).dp.toPx().toInt() }
            val maxVScrollPx = with(density) { (0.8f * contentH * zoom).dp.toPx().toInt() }
            LaunchedEffect(maxHScrollPx) {
                snapshotFlow { hScroll.value }.collect { v ->
                    if (v > maxHScrollPx) hScroll.scrollTo(maxHScrollPx)
                }
            }
            LaunchedEffect(maxVScrollPx) {
                snapshotFlow { vScroll.value }.collect { v ->
                    if (v > maxVScrollPx) vScroll.scrollTo(maxVScrollPx)
                }
            }
            // The sheet must NEVER fully disappear (design: fit keeps it inside the viewport,
            // min zoom 0.2; manual zoom stays within 50%..200% of fit). When the content fits
            // it is CENTERED (design `items-center justify-center`); when it overflows — e.g.
            // zoomed in — the scroll container owns it from a safe top-start position. Centering
            // an overflowing scroll content is what trapped the sheet (it could scroll away and
            // never come back), so the extra centering space is only applied when content fits.
            val contentHTotal = contentH + contentTopPad.value + contentBottomPad.value
            val centerExtra = if (contentHTotal <= maxHeight.value) (maxHeight.value - contentHTotal) / 2f else 0f
            val horizExtra = if (contentW <= maxWidth.value) (maxWidth.value - contentW) / 2f else 0f

            // HTML: canvas is flat md-canvas — no dot grid, no pattern.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(vScroll)
                    .horizontalScroll(hScroll),
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = horizExtra.dp,
                        end = horizExtra.dp,
                        top = contentTopPad + centerExtra.dp,
                        bottom = contentBottomPad + centerExtra.dp,
                    ),
                ) {
                    // ONE box: true paper/ruler/card layout at full mm size, scaled once by the
                    // CSS-style zoom layer. requiredSize is essential: a plain size() would be
                    // clamped to the parent's bounds and halve the sheet.
                    Box(
                        modifier = Modifier
                            .requiredSize(contentW.dp, contentH.dp)
                            .graphicsLayer {
                                scaleX = zoom
                                scaleY = zoom
                                transformOrigin = TransformOrigin(0f, 0f)
                            },
                    ) {
                        Box(
                                modifier = Modifier
                                    .testTag("sheet-paper")
                                    .offset(x = rulerTotalDp.dp, y = rulerTotalDp.dp)
                                    .size(paperWdp.dp, paperHdp.dp)
                                    // design: 0 24px 56px -14px rgba(0,0,0,.36) + 0 6px 18px -4px rgba(0,0,0,.22) + 1px hairline
                                    .shadow(24.dp, RectangleShape, clip = false, ambientColor = Color(0x5C000000), spotColor = Color(0x38000000))
                                    .shadow(6.dp, RectangleShape, clip = false, ambientColor = Color(0x38000000), spotColor = Color(0x1C000000))
                                    .clipToBounds()
                                    .background(sheetBackgroundColor(settings)),
                            ) {
                                if (settings.frame.background == com.appprint.studio.core.model.SheetBackground.TRANSPARENT) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val cell = (8f * DP_PER_MM).dp.toPx()
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
                                                width = (0.45f * DP_PER_MM * density.density).coerceAtLeast(1f),
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
                                            com.appprint.studio.core.model.FrameStyle.DASHED -> PathEffect.dashPathEffect(floatArrayOf(9f, 7f), 0f)
                                            com.appprint.studio.core.model.FrameStyle.DOTTED -> PathEffect.dashPathEffect(floatArrayOf(1.5f, 6f), 0f)
                                        }
                                        drawRect(
                                            color = Color(settings.frame.colorHex),
                                            style = Stroke(width = (settings.frame.thickness * density.density).coerceAtLeast(1f), pathEffect = dash),
                                        )
                                    }
                                }
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawRect(
                                        color = Color(0x14000000),
                                        style = Stroke(width = 1.dp.toPx()),
                                    )
                                }
                                Text(
                                    text = "${settings.presetKey} • ${formatCaptionDim(settings.sheetWidthMm)} × ${formatCaptionDim(settings.sheetHeightMm)} mm",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
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
                                state.cardsOnPage
                                    .sortedBy { if (it.id == state.selectedCardId) 1 else 0 }
                                    .forEach { card ->
                                        androidx.compose.runtime.key(card.id) {
                                            CardOnSheet(
                                                card = card,
                                                state = state,
                                                viewModel = viewModel,
                                                dpPerMm = DP_PER_MM,
                                                zoom = zoom,
                                            )
                                        }
                                    }
                            }
                            if (settings.frame.ruler) {
                                val rulerBg = Color.Transparent
                                RulerBar(
                                    vertical = false,
                                    lengthMm = settings.sheetWidthMm,
                                    stripDp = paperWdp,
                                    background = rulerBg,
                                    modifier = Modifier.offset(x = rulerTotalDp.dp),
                                )
                                // design: the page counter is the last item of the top ruler strip
                                Text(
                                    text = "صفحة ${state.activePage + 1} / ${state.pageCount}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.align(Alignment.TopEnd),
                                )
                                RulerBar(
                                    vertical = true,
                                    lengthMm = settings.sheetHeightMm,
                                    stripDp = paperHdp,
                                    background = rulerBg,
                                    modifier = Modifier.offset(y = rulerTotalDp.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

/** Millimetre ruler: one small number every 10 mm — like the HTML `ui-overlay` ruler. */@Composable
private fun RulerBar(
    vertical: Boolean,
    lengthMm: Float,
    stripDp: Float,
    background: Color,
    modifier: Modifier = Modifier,
) {
    val tick = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
    if (vertical) {
        Box(modifier = modifier.width(20.dp).height(stripDp.dp).background(background)) {
            var mm = 0
            while (mm <= lengthMm) {
                Text(
                    text = "$mm",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = tick,
                    softWrap = false,
                    maxLines = 1,
                    modifier = Modifier.offset(x = 2.dp, y = (mm * DP_PER_MM).dp),
                )
                mm += 10
            }
        }
    } else {
        Box(modifier = modifier.width(stripDp.dp).height(20.dp).background(background)) {
            var mm = 0
            while (mm <= lengthMm) {
                Text(
                    text = "$mm",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = tick,
                    softWrap = false,
                    maxLines = 1,
                    modifier = Modifier.offset(x = (mm * DP_PER_MM).dp),
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

/** Design: the empty sheet state lives inside the paper — mint icon, caption, actions, demo link. */
@Composable
private fun EmptyPaperMessage(
    modifier: Modifier = Modifier,
    onPickGallery: () -> Unit,
    onCaptureCamera: () -> Unit,
    onAddDemoCards: () -> Unit,
) {
    val sheetGreen = Color(0xFF006C47)
    Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = sheetGreen, modifier = Modifier.size(32.dp))
            }
            Text(
                text = "الورقة فارغة — ارفع صور بطاقاتك للبدء",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF262626),
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = "افتح الاستوديو لاختيار الصور أو التقطها بالكاميرا وسيتم التعرف على أبعادها الحقيقية وترتيبها تلقائياً",
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = Color(0xFF737373),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp).widthIn(max = 240.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                modifier = Modifier.padding(top = 16.dp).widthIn(max = 300.dp).fillMaxWidth(),
            ) {
                // M3 Expressive filled action — the sheet's primary empty-state affordance.
                Button(
                    onClick = onPickGallery,
                    shape = RoundedCornerShape(percent = 50),
                    colors = ButtonDefaults.buttonColors(containerColor = sheetGreen, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                    androidx.compose.foundation.layout.Spacer(Modifier.width(5.dp))
                    Text("رفع الصور", fontWeight = FontWeight.Black, fontSize = 11.sp, maxLines = 1)
                }
                // M3 outlined secondary action.
                OutlinedButton(
                    onClick = onCaptureCamera,
                    shape = RoundedCornerShape(percent = 50),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF262626),
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    androidx.compose.foundation.layout.Spacer(Modifier.width(5.dp))
                    Text("الكاميرا", fontWeight = FontWeight.Black, fontSize = 11.sp, maxLines = 1)
                }
            }
            Text(
                text = "أو جرب بأمثلة تعليمية",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = sheetGreen,
                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                modifier = Modifier.padding(top = 12.dp).clickable(onClick = onAddDemoCards),
            )
        }
    }
}
