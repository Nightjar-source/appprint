package com.appprint.studio.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appprint.studio.core.image.EnhanceMath
import com.appprint.studio.core.model.CardItem
import com.appprint.studio.core.platform.rememberCardBitmap
import com.appprint.studio.ui.TabatiViewModel
import com.appprint.studio.ui.TabatiUiState
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val DP_PER_MM = 3.7795276f

/**
 * One card on the sheet — ported from the HTML `card-item`:
 *
 *  - the outer box is the card's *footprint* (`w × h`, already swapped for 90°/270°) and
 *    rotates ONLY by `freeRotation + enhance.tilt` (HTML line 2672);
 *  - the inner content keeps the physical photo size and rotates by the right-angle
 *    `rotation` (HTML lines 2690–2699) — never both on the same layer;
 *  - pointer deltas arrive in the rotated local frame, so move/resize convert them back
 *    to screen space before touching millimetres — this is what keeps the drag 1:1;
 *  - rotation is computed in window coordinates from the card centre captured at
 *    pointer-down, exactly like the HTML `atan2(clientY - centerY, clientX - centerX)`.
 */
@Composable
internal fun CardOnSheet(
    card: CardItem,
    state: TabatiUiState,
    viewModel: TabatiViewModel,
    dpPerMm: Float,
    zoom: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val selected = card.id == state.selectedCardId
    val currentCard by rememberUpdatedState(card)
    // Unrotated layout coordinates of the card box — used for the screen-space rotate maths.
    var cardCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    // Coordinates of the pointer-input node itself. Pointer positions are LOCAL to a node
    // that MOVES with the card, so deltas must be mapped to window space before accumulating.
    var pointerCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    // HTML: transform rotate(card.freeRotation + (compareBefore ? 0 : enhance.tilt))
    val outerAngle = card.freeRotation + if (state.compareBefore) 0f else card.enhance.tilt

    Box(
        modifier = modifier
            .testTag("card-${card.id}")
            .offset {
                IntOffset(
                    (card.xMm * dpPerMm * density.density).roundToInt(),
                    (card.yMm * dpPerMm * density.density).roundToInt(),
                )
            }
            .size((card.widthMm * dpPerMm).dp, (card.heightMm * dpPerMm).dp)
            .then(
                if (selected) {
                    // design .card-item.is-selected: box-shadow 0 14px 32px rgba(0,108,71,.32)
                    Modifier.shadow(
                        16.dp,
                        RectangleShape,
                        clip = false,
                        ambientColor = Color(0xFF006C47).copy(alpha = 0.32f),
                        spotColor = Color(0xFF006C47).copy(alpha = 0.32f),
                    )
                } else {
                    Modifier
                },
            )
            // Coordinates BEFORE the rotation layer = stable, unrotated card rect in window space.
            .onGloballyPositioned { cardCoordinates = it }
            .graphicsLayer { rotationZ = outerAngle }
            .onGloballyPositioned { pointerCoordinates = it }
            // ONE unified per-card gesture set. Tap selects (on press, like HTML), long-press
            // opens the context sheet, drag moves. Both handlers live on this card only, so a
            // selected card's neighbours can never receive its gestures.
            .pointerInput(card.id) {
                // ONE gesture set per card: press selects (like HTML), long-press opens the
                // context sheet, every move is applied 1:1 in millimetres. Keeping all three
                // in a single pointerInput avoids detectors stealing each other's events.
                awaitPointerEventScope {
                    var down = awaitPointerEvent(PointerEventPass.Initial)
                        .changes.firstOrNull { it.pressed && !it.previousPressed }
                    while (down == null) {
                        down = awaitPointerEvent(PointerEventPass.Initial)
                            .changes.firstOrNull { it.pressed && !it.previousPressed }
                    }
                    val pointerId = down.id
                    val startX = currentCard.xMm
                    val startY = currentCard.yMm
                    var previous = pointerCoordinates?.localToWindow(down.position) ?: down.position
                    var movedX = 0f
                    var movedY = 0f
                    var moved = false
                    var longPressSent = false
                    while (true) {
                        // withTimeoutOrNull is a member of AwaitPointerEventScope, so it is
                        // legal inside its restricted suspension scope.
                        val event = withTimeoutOrNull(if (longPressSent) Long.MAX_VALUE else 500L) {
                            awaitPointerEvent(PointerEventPass.Main)
                        }
                        if (event == null) {
                            if (!moved && !longPressSent) {
                                longPressSent = true
                                viewModel.openLongPressSheet(card.id)
                            }
                            continue
                        }
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed) break
                        val screen = pointerCoordinates?.localToWindow(change.position) ?: change.position
                        val delta = screen - previous
                        previous = screen
                        if (delta.x != 0f || delta.y != 0f) moved = true
                        // Window space is already screen-aligned — no inverse rotation needed.
                        // px-per-mm includes the sheet's CSS-style zoom layer.
                        val scale = DP_PER_MM * zoom * density.density
                        movedX += delta.x / scale
                        movedY += delta.y / scale
                        change.consume()
                        viewModel.moveCard(currentCard.id, startX + movedX, startY + movedY)
                    }
                    if (!moved) viewModel.selectCard(card.id)
                }
            },
    ) {
        val isRightAngle = card.rotation % 180 != 0
        val innerW = if (isRightAngle) card.heightMm else card.widthMm
        val innerH = if (isRightAngle) card.widthMm else card.heightMm

        // Inner content — physical photo size, rotated only by the right-angle rotation.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size((innerW * dpPerMm).dp, (innerH * dpPerMm).dp)
                .graphicsLayer { rotationZ = card.rotation.toFloat() }
                .clip(androidx.compose.ui.graphics.RectangleShape),
        ) {
            val bitmap = card.imageUri?.let { rememberCardBitmap(it) }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = card.name,
                    contentScale = ContentScale.Crop,
                    colorFilter = enhanceColorFilterOrNull(card.enhance, state.compareBefore),
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                SampleCardArt(card = card)
            }
        }

        if (selected) {
            // design: outline 2.5px primary + outline-offset 2px — drawn OUTSIDE the card
            // bounds (a plain Box child would be coerced to the parent's fixed constraints).
            val selColor = MaterialTheme.colorScheme.primary
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gap = 2.dp.toPx()
                val stroke = 2.5.dp.toPx()
                val inset = gap + stroke / 2f
                drawRect(
                    color = selColor,
                    topLeft = Offset(-inset, -inset),
                    size = Size(size.width + inset * 2, size.height + inset * 2),
                    style = Stroke(width = stroke),
                )
            }
        }

        // design: card badges — size+angle / auto-fix / enhanced
        com.appprint.studio.ui.components.CardBadges(card = card, modifier = Modifier.fillMaxSize())

        // card frame (the printed border on the physical card)
        if (state.settings.frame.card && card.border && state.settings.frame.colorHex != 0L && state.settings.frame.thickness > 0f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val frame = state.settings.frame
                val dash = when (frame.style) {
                    com.appprint.studio.core.model.FrameStyle.SOLID -> null
                    com.appprint.studio.core.model.FrameStyle.DASHED -> PathEffect.dashPathEffect(floatArrayOf(9f, 7f), 0f)
                    com.appprint.studio.core.model.FrameStyle.DOTTED -> PathEffect.dashPathEffect(floatArrayOf(1.5f, 6f), 0f)
                }
                drawRect(
                    color = Color(frame.colorHex),
                    style = Stroke(width = (frame.thickness * 1.9f).coerceAtLeast(1.4f), pathEffect = dash),
                )
            }
        }
        // per-card cut guide (red dashed, 1.2 mm beyond the card edge)
        if (state.settings.frame.guides && card.guides) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val bleed = 1.2f * dpPerMm * density.density
                drawRect(
                    color = Color(0xFFB32638).copy(alpha = 0.75f),
                    topLeft = Offset(-bleed, -bleed),
                    size = androidx.compose.ui.geometry.Size(size.width + bleed * 2, size.height + bleed * 2),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
                    ),
                )
            }
        }

        if (selected) {
            // rotate handle — floats 36dp above the card with a 2dp × 8dp stem (design: -top-9 + stem)
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-36).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RotateHandle(
                    card = card,
                    viewModel = viewModel,
                    dpPerMm = dpPerMm,
                    cardCoordinates = { cardCoordinates },
                )
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(8.dp)
                        .background(MaterialTheme.colorScheme.secondary),
                )
            }
            ResizeHandle(
                card = card,
                viewModel = viewModel,
                dpPerMm = dpPerMm,
                compareBefore = state.compareBefore,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 8.dp, y = 8.dp),
            )
        }
    }
}

/**
 * Circular drag handle that rotates the card around its centre.
 * The angle is computed in WINDOW space (like HTML `clientX/clientY`), so a snap that
 * swaps the footprint cannot make the angle jump mid-gesture.
 */
@Composable
private fun RotateHandle(
    card: CardItem,
    viewModel: TabatiViewModel,
    dpPerMm: Float,
    cardCoordinates: () -> LayoutCoordinates?,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val currentCard by rememberUpdatedState(card)
    val scheme = MaterialTheme.colorScheme
    var liveAngle by remember { mutableStateOf<String?>(null) }
    var handleCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var startCentre by remember { mutableStateOf<Offset?>(null) }

    Box(contentAlignment = Alignment.Center) {
        liveAngle?.let { ang ->
            Text(
                text = ang,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-18).dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MaterialTheme.colorScheme.onSurface)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        Box(
            modifier = modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(scheme.secondary)
                .onGloballyPositioned { handleCoordinates = it }
                .pointerInput(card.id) {
                    detectDragGestures(
                        onDragStart = {
                            val coords = cardCoordinates()
                            startCentre = coords?.let {
                                it.localToWindow(Offset(it.size.width / 2f, it.size.height / 2f))
                            }
                        },
                        onDragEnd = { liveAngle = null; startCentre = null },
                        onDragCancel = { liveAngle = null; startCentre = null },
                        onDrag = { change, _ ->
                            change.consume()
                            val c = currentCard
                            val centre = startCentre
                            val handle = handleCoordinates
                            val raw = if (centre != null && handle != null) {
                                // localToWindow applies the card's free rotation for us.
                                val pointer = handle.localToWindow(change.position)
                                (
                                    atan2(
                                        (pointer.y - centre.y).toDouble(),
                                        (pointer.x - centre.x).toDouble(),
                                    ) * 180.0 / PI
                                    ).toFloat() + 90f
                            } else {
                                // Fallback (first frame / no coordinates yet).
                                val handleLeft = (c.widthMm * dpPerMm / 2f - 16f) * density.density
                                val handleTop = -36f * density.density
                                val px = handleLeft + change.position.x
                                val py = handleTop + change.position.y
                                val centerX = c.widthMm * dpPerMm / 2f * density.density
                                val centerY = c.heightMm * dpPerMm / 2f * density.density
                                (
                                    atan2((py - centerY).toDouble(), (px - centerX).toDouble()) * 180.0 / PI
                                    ).toFloat() + 90f
                            }
                            val applied = viewModel.setCardAngle360(c.id, raw, magneticSnap = true)
                            liveAngle = "${applied.toInt()}°"
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Autorenew,
                contentDescription = null,
                tint = scheme.onSecondary,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

/** Corner drag handle — resizes in millimetres (min 20 mm). */
@Composable
private fun ResizeHandle(
    card: CardItem,
    viewModel: TabatiViewModel,
    dpPerMm: Float,
    compareBefore: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val currentCard by rememberUpdatedState(card)
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(scheme.primary)
            .pointerInput(card.id) {
                var startW = 0f
                var startH = 0f
                var grownW = 0f
                var grownH = 0f
                detectDragGestures(
                    onDragStart = {
                        startW = currentCard.widthMm
                        startH = currentCard.heightMm
                        grownW = 0f
                        grownH = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val c = currentCard
                        val angle = c.freeRotation + if (compareBefore) 0f else c.enhance.tilt
                        val rad = angle * PI / 180.0
                        val cosA = cos(rad).toFloat()
                        val sinA = sin(rad).toFloat()
                        val dx = dragAmount.x * cosA - dragAmount.y * sinA
                        val dy = dragAmount.x * sinA + dragAmount.y * cosA
                        val scale = dpPerMm * density.density
                        grownW += dx / scale
                        grownH += dy / scale
                        viewModel.resizeCard(
                            c.id,
                            startW + grownW,
                            startH + grownH,
                        )
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.SwapHoriz,
            contentDescription = null,
            tint = scheme.onPrimary,
            modifier = Modifier
                .size(16.dp)
                .graphicsLayer(rotationZ = 45f),
        )
    }
}

/** ColorMatrix approximation of the scanner pipeline — fast preview only (export does the real pass). */
private fun enhanceColorFilterOrNull(
    enhance: com.appprint.studio.core.model.EnhanceSettings,
    compareBefore: Boolean,
): ColorFilter? {
    if (compareBefore || enhance.isIdentity) return null
    val m = EnhanceMath.colorMatrixValues(enhance)
    return ColorFilter.colorMatrix(ColorMatrix(m))
}

/** Sample-art card — exactly the design's placeholder: green top bar, name, OFFICIAL caption, photo box + 1:1 tag. */
@Composable
private fun SampleCardArt(card: CardItem) {
    val isPassport = card.typeKey == "passport"
    val bg = if (isPassport) Color(0xFF17365A) else Color(0xFFFDFAF1)
    val fg = if (isPassport) Color.White else Color(0xFF1C1C1C)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Color(0xFF006C47)),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(11.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = card.name,
                    color = fg,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    maxLines = 2,
                    lineHeight = 13.sp,
                )
                Text(
                    text = "OFFICIAL ID DOCUMENT • وثيقة رسمية",
                    color = fg.copy(alpha = 0.75f),
                    fontSize = 7.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 42.dp, height = 31.dp)
                        .border(1.dp, fg.copy(alpha = 0.4f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "صورة",
                        color = fg.copy(alpha = 0.55f),
                        fontSize = 6.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = "1:1",
                    color = fg,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
