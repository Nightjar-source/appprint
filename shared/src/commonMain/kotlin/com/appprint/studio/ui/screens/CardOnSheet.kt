package com.appprint.studio.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appprint.studio.core.image.EnhanceMath
import com.appprint.studio.core.model.CardItem
import com.appprint.studio.core.platform.rememberCardBitmap
import com.appprint.studio.ui.TabatiViewModel
import com.appprint.studio.ui.TabatiUiState
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * One card on the sheet — positioned by absolute offset, rotated by
 * rotation (right-angle) + freeRotation (magnetic 360°), with drag,
 * rotate-handle and resize-handle gestures.
 */
@Composable
internal fun CardOnSheet(
    card: CardItem,
    state: TabatiUiState,
    viewModel: TabatiViewModel,
    dpPerMm: Float,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val selected = card.id == state.selectedCardId
    val currentCard by rememberUpdatedState(card)
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    (card.xMm * dpPerMm * density.density).roundToInt(),
                    (card.yMm * dpPerMm * density.density).roundToInt(),
                )
            }
            .size((card.widthMm * dpPerMm).dp, (card.heightMm * dpPerMm).dp)
            .graphicsLayer { rotationZ = card.totalRotation }
            // ONE unified pointer handler. Competing pointerInput/tap/drag modifiers on the
            // same box swallow each other's events — which broke selection and made drag
            // feel reversed. Tap selects, long-press opens the sheet, drag moves, all in
            // a single detector that awaits each gesture in sequence.
            .pointerInput(card.id) {
                detectTapGestures(
                    onTap = { viewModel.selectCard(card.id) },
                    onLongPress = { viewModel.openLongPressSheet(card.id) },
                )
            }
            .pointerInput(card.id) {
                detectDragGestures(
                    onDragStart = { viewModel.selectCard(card.id) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val c = currentCard
                        val s = state.settings
                        val bound = if (s.frame.guides) s.marginMm else 0f
                        val maxX = (s.sheetWidthMm - bound - c.widthMm).coerceAtLeast(bound)
                        val maxY = (s.sheetHeightMm - bound - c.heightMm).coerceAtLeast(bound)
                        viewModel.moveCard(
                            c.id,
                            (c.xMm + dragAmount.x / (dpPerMm * density.density)).coerceIn(bound, maxX),
                            (c.yMm + dragAmount.y / (dpPerMm * density.density)).coerceIn(bound, maxY),
                        )
                    },
                )
            },
    ) {
        val isRightAngle = card.rotation % 180 != 0
        val innerW = if (isRightAngle) card.heightMm else card.widthMm
        val innerH = if (isRightAngle) card.widthMm else card.heightMm

        // inner content — rotated 90°/270° for right-angle cards
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
            // selection ring (design: 2.5px primary outline outside the card + green glow)
            val selColor = MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .size(((card.widthMm * dpPerMm) + 5f).dp, ((card.heightMm * dpPerMm) + 5f).dp)
                    .offset(x = (-2.5).dp, y = (-2.5).dp)
                    .border(2.5.dp, selColor, androidx.compose.ui.graphics.RectangleShape)
                    .shadow(
                        16.dp,
                        androidx.compose.ui.graphics.RectangleShape,
                        clip = false,
                        ambientColor = Color(0xFF006C47).copy(alpha = 0.32f),
                        spotColor = Color(0xFF006C47).copy(alpha = 0.32f),
                    ),
            )
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
                    topLeft = androidx.compose.ui.geometry.Offset(-bleed, -bleed),
                    size = androidx.compose.ui.geometry.Size(size.width + bleed * 2, size.height + bleed * 2),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
                    ),
                )
            }
        }

        if (selected) {
            // rotate handle — floats 36dp above the card with a 2dp × 8dp stem to the card (design: -top-9 + stem)
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
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 8.dp, y = 8.dp),
            )
        }
    }
}

/**
 * Circular drag handle that rotates the card around its center.
 * The pointer is projected onto the card's own box each event — the angle
 * falls out of atan2(dy, dx) with 45° magnetic snapping.
 */
@Composable
private fun RotateHandle(
    card: CardItem,
    viewModel: TabatiViewModel,
    dpPerMm: Float,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val currentCard by rememberUpdatedState(card)
    val scheme = MaterialTheme.colorScheme
    var liveAngle by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
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
                .pointerInput(card.id) {
                    detectDragGestures(
                        onDragStart = { _start -> /* keep last */ },
                        onDragEnd = { liveAngle = null },
                        onDragCancel = { liveAngle = null },
                        onDrag = { change, _ ->
                            change.consume()
                            val c = currentCard
                            // handle sits at the card's top-center (offset -36dp): recover card-box coords
                            val handleLeft = (c.widthMm * dpPerMm / 2f - 16f) * density.density
                            val handleTop = -36f * density.density
                            val px = handleLeft + change.position.x
                            val py = handleTop + change.position.y
                            val centerX = c.widthMm * dpPerMm / 2f * density.density
                            val centerY = c.heightMm * dpPerMm / 2f * density.density
                            val raw = Math.toDegrees(atan2((py - centerY).toDouble(), (px - centerX).toDouble())).toFloat() + 90f
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
                detectDragGestures(onDrag = { change, dragAmount ->
                    change.consume()
                    val c = currentCard
                    viewModel.resizeCard(
                        c.id,
                        c.widthMm + dragAmount.x / (dpPerMm * density.density),
                        c.heightMm + dragAmount.y / (dpPerMm * density.density),
                    )
                })
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
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
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
