package com.appprint.studio.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.dashPathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.stringResource
import com.appprint.studio.shared.resources.Res
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt
import com.appprint.studio.core.image.PixelOps
import com.appprint.studio.core.model.CardItem
import com.appprint.studio.core.model.FrameStyle
import com.appprint.studio.core.model.SheetBackground
import com.appprint.studio.core.platform.rememberCardBitmap
import com.appprint.studio.core.platform.rememberCameraCaptureLauncher
import com.appprint.studio.core.platform.rememberImagePickerLauncher
import com.appprint.studio.ui.StudioUiState
import com.appprint.studio.ui.StudioViewModel
import com.appprint.studio.ui.components.AppIcons
import com.appprint.studio.ui.components.FlagCardGlyph
import com.appprint.studio.ui.components.SampleCardArt
import com.appprint.studio.ui.components.TagChip

/** Compose density-independent dp per millimetre (96 dp/inch). */
private const val DP_PER_MM = 3.7795276f

@Composable
fun StudioScreen(viewModel: StudioViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showClearConfirm by rememberSaveable { mutableStateOf(false) }
    var showPaperSheet by rememberSaveable { mutableStateOf(false) }
    var showInspectorSheet by rememberSaveable { mutableStateOf(false) }

    val pickImages = rememberImagePickerLauncher(maxItems = 100) { uris -> viewModel.importImages(uris) }
    val onCamera = rememberCameraCaptureLauncher { path ->
        if (path != null) viewModel.importCapture(path) else viewModel.notifyCameraUnavailable()
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 1150.dp
        val medium = !wide && maxWidth >= 640.dp
        val phone = !wide && !medium

        // On phones, tapping a card slides the inspector sheet up
        LaunchedEffect(state.selectedId, phone) {
            if (phone && state.selectedId != null) showInspectorSheet = true
        }

        Row(Modifier.fillMaxSize()) {
            if (wide) {
                PaperSettingsPanel(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface),
                )
                HorizontalDivider()
            }

            Column(Modifier.weight(1f).fillMaxHeight()) {
                StudioToolbar(
                    state = state,
                    onAddImages = pickImages,
                    onCamera = onCamera,
                    onSamples = viewModel::addSamples,
                    onArrange = viewModel::arrangeSmart,
                    onClear = { showClearConfirm = true },
                    onSharePng = { viewModel.sharePng(state.currentPage) },
                    onOpenPaperSettings = if (wide) null else ({ showPaperSheet = true }),
                )
                if (state.cards.isEmpty()) {
                    EmptyState(
                        onUpload = pickImages,
                        onSamples = viewModel::addSamples,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                } else {
                    SheetCanvas(
                        state = state,
                        viewModel = viewModel,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                    SheetPageBar(
                        state = state,
                        onSelectPage = viewModel::currentPage,
                    )
                }
            }

            if (wide || medium) {
                HorizontalDivider()
                InspectorPanel(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier
                        .width(if (wide) 330.dp else 300.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface),
                )
            }
        }

        if (showPaperSheet && !wide) {
            ModalBottomSheet(onDismissRequest = { showPaperSheet = false }) {
                PaperSettingsPanel(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
                )
            }
        }
        if (showInspectorSheet && phone) {
            ModalBottomSheet(onDismissRequest = { showInspectorSheet = false }) {
                InspectorPanel(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
                )
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(Res.string.clear_confirm_title)) },
            text = { Text(stringResource(Res.string.clear_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll()
                    showClearConfirm = false
                }) { Text(stringResource(Res.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text(stringResource(Res.string.cancel)) }
            },
        )
    }
}

// ------------------------------------------------------------ toolbar

@Composable
private fun StudioToolbar(
    state: StudioUiState,
    onAddImages: () -> Unit,
    onCamera: () -> Unit,
    onSamples: () -> Unit,
    onArrange: () -> Unit,
    onClear: () -> Unit,
    onSharePng: () -> Unit,
    onOpenPaperSettings: (() -> Unit)?,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item {
            FilledTonalButton(onClick = onAddImages) {
                Icon(AppIcons.Upload, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text(stringResource(Res.string.btn_add_images), maxLines = 1)
            }
        }
        item {
            FilledTonalButton(onClick = onCamera) {
                Icon(AppIcons.Camera, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text(stringResource(Res.string.btn_take_photo), maxLines = 1)
            }
        }
        item {
            FilledTonalButton(onClick = onSamples) {
                Icon(AppIcons.Sparkle, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text(stringResource(Res.string.btn_samples), maxLines = 1)
            }
        }
        item {
            FilledTonalButton(onClick = onArrange, enabled = state.cards.isNotEmpty()) {
                Icon(androidx.compose.material.icons.Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text(stringResource(Res.string.btn_arrange), maxLines = 1)
            }
        }
        item {
            FilledTonalButton(onClick = onSharePng, enabled = state.cards.isNotEmpty()) {
                Icon(AppIcons.Image, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text(stringResource(Res.string.btn_save_png), maxLines = 1)
            }
        }
        item {
            TextButton(onClick = onClear, enabled = state.cards.isNotEmpty()) {
                Text(stringResource(Res.string.btn_clear_all), color = MaterialTheme.colorScheme.error, maxLines = 1)
            }
        }
        if (onOpenPaperSettings != null) {
            item {
                IconButton(onClick = onOpenPaperSettings) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Menu, contentDescription = stringResource(Res.string.paper_title))
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
}

// ------------------------------------------------------------ page bar

@Composable
private fun SheetPageBar(state: StudioUiState, onSelectPage: (Int) -> Unit) {
    if (state.pageCount <= 1) return
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items((0 until state.pageCount).toList(), key = { it }) { page ->
            AssistChip(
                onClick = { onSelectPage(page) },
                label = { Text(stringResource(Res.string.sheet_tab, page + 1)) },
                leadingIcon = if (state.currentPage == page) {
                    { Icon(androidx.compose.material.icons.Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else {
                    null
                },
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
}

// ------------------------------------------------------------ empty state

@Composable
private fun EmptyState(onUpload: () -> Unit, onSamples: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        FlagCardGlyph(sizeDp = 92.dp)
        Text(
            text = stringResource(Res.string.empty_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(top = 18.dp, start = 24.dp, end = 24.dp),
        )
        Text(
            text = stringResource(Res.string.empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp),
        )
        Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onUpload) {
                Icon(AppIcons.Upload, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(Res.string.empty_upload))
            }
            OutlinedButton(onClick = onSamples) {
                Icon(AppIcons.Sparkle, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(Res.string.empty_cta))
            }
        }
    }
}

// ------------------------------------------------------------ canvas

@Composable
private fun SheetCanvas(state: StudioUiState, viewModel: StudioViewModel, modifier: Modifier = Modifier) {
    val settings = state.settings
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var panX by rememberSaveable { mutableFloatStateOf(0f) }
    var panY by rememberSaveable { mutableFloatStateOf(0f) }
    val transformable = rememberTransformableState { zoomChange, panChange, _ ->
        zoom = (zoom * zoomChange).coerceIn(1f, 4.5f)
        panX += panChange.x
        panY += panChange.y
    }

    BoxWithConstraints(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .clipToBounds(),
    ) {
        val sheetWdp = settings.sheetWidthMm * DP_PER_MM
        val sheetHdp = settings.sheetHeightMm * DP_PER_MM
        val fit = minOf(
            (maxWidth.value - 20f) / sheetWdp,
            (maxHeight.value - 20f) / sheetHdp,
        ).coerceIn(0.25f, 2.2f)
        val effective = fit * zoom

        // blueprint dot grid backdrop
        Canvas(Modifier.fillMaxSize()) {
            val step = 26.dp.toPx()
            val dot = 1.1.dp.toPx()
            val color = Color(0xFF8F8C82).copy(alpha = 0.35f)
            var x = step / 2
            while (x < size.width) {
                var y = step / 2
                while (y < size.height) {
                    drawCircle(color, radius = dot, center = Offset(x, y))
                    y += step
                }
                x += step
            }
        }

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(sheetWdp.dp, sheetHdp.dp)
                    .transformable(transformable)
                    .graphicsLayer {
                        scaleX = effective
                        scaleY = effective
                        translationX = panX
                        translationY = panY
                    },
            ) {
                val paperColor = when (settings.background) {
                    SheetBackground.WHITE -> Color.White
                    SheetBackground.OFFWHITE -> Color(0xFFFDFAF1)
                    SheetBackground.TRANSPARENT -> Color(0x66FFFFFF)
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .shadow(18.dp, RoundedCornerShape(3.dp))
                        .clip(RoundedCornerShape(3.dp))
                        .background(paperColor)
                        .clickable { viewModel.selectCard(null) },
                ) {
                    if (settings.showGuides) {
                        Canvas(Modifier.fillMaxSize()) {
                            val m = settings.marginMm * DP_PER_MM * density
                            val dash = dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
                            drawRect(
                                color = Color(0xFFC8102E).copy(alpha = 0.5f),
                                topLeft = Offset(m, m),
                                size = Size(size.width - 2 * m, size.height - 2 * m),
                                style = Stroke(width = 1.dp.toPx(), pathEffect = dash),
                            )
                        }
                    }
                    state.cardsOnPage.forEach { card ->
                        CardOnSheet(
                            card = card,
                            density = DP_PER_MM,
                            scale = effective,
                            selected = card.id == state.selectedId,
                            showGuides = settings.showGuides,
                            frameWidthDp = settings.frameWidthDp,
                            frameColorHex = settings.frameColorHex,
                            frameStyle = settings.frameStyle,
                            showFrames = settings.showFrames,
                            onSelect = viewModel::selectCard,
                            onMove = viewModel::moveCard,
                        )
                    }
                    if (state.pageCount > 1) {
                        TagChip(
                            text = stringResource(Res.string.sheet_tab, state.currentPage + 1),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardOnSheet(
    card: CardItem,
    density: Float,
    scale: Float,
    selected: Boolean,
    showGuides: Boolean,
    frameWidthDp: Float,
    frameColorHex: Long,
    frameStyle: FrameStyle,
    showFrames: Boolean,
    onSelect: (Long) -> Unit,
    onMove: (Long, Float, Float) -> Unit,
) {
    val currentCard by rememberUpdatedState(card)

    Box(
        Modifier
            .offset { IntOffset((card.xMm * density).roundToInt(), (card.yMm * density).roundToInt()) }
            .size((card.effectiveWidth * density).dp, (card.effectiveHeight * density).dp)
            .pointerInput(card.id, scale) {
                detectDragGestures { _, dragAmount ->
                    val cur = currentCard
                    onMove(
                        cur.id,
                        cur.xMm + dragAmount.x / (density * scale),
                        cur.yMm + dragAmount.y / (density * scale),
                    )
                }
            }
            .clickable { onSelect(card.id) },
    ) {
        val contentW = card.widthMm * density
        val contentH = card.heightMm * density
        Box(
            Modifier
                .align(Alignment.Center)
                .size(contentW.dp, contentH.dp)
                .graphicsLayer { rotationZ = if (card.rotation == 90) 90f else 0f }
                .clip(RoundedCornerShape(6.dp)),
        ) {
            if (card.imageUri != null) {
                CardImage(card = card)
            } else {
                SampleCardArt(card = card, modifier = Modifier.fillMaxSize())
            }
        }

        if (showFrames && card.border) {
            Canvas(Modifier.fillMaxSize()) {
                val dash = when (frameStyle) {
                    FrameStyle.SOLID -> null
                    FrameStyle.DASHED -> dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
                    FrameStyle.DOTTED -> dashPathEffect(floatArrayOf(1.5.dp.toPx(), 5.dp.toPx()))
                }
                drawRoundRect(
                    color = Color(frameColorHex),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                    style = Stroke(width = frameWidthDp.dp.toPx(), pathEffect = dash),
                )
            }
        }
        if (showGuides && card.guides) {
            Canvas(Modifier.fillMaxSize()) {
                val dash = dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
                val outside = 1.2f * density
                drawRect(
                    color = Color(0xFFC8102E).copy(alpha = 0.7f),
                    topLeft = Offset(-outside, -outside),
                    size = Size(size.width + 2 * outside, size.height + 2 * outside),
                    style = Stroke(width = 1.dp.toPx(), pathEffect = dash),
                )
            }
        }
        if (selected) {
            Box(
                Modifier
                    .fillMaxSize()
                    .border(2.dp, Color(0xFF00914A), RoundedCornerShape(6.dp)),
            )
            TagChip(
                text = "${card.type.shortAr} · ${card.effectiveWidth.toInt()}×${card.effectiveHeight.toInt()}",
                modifier = Modifier.align(Alignment.TopStart).padding(3.dp),
            )
        }
    }
}

@Composable
private fun CardImage(card: CardItem) {
    val bitmap = rememberCardBitmap(card.imageUri)
    val colorMatrix = remember(card.enhance) { androidx.compose.ui.graphics.ColorMatrix(PixelOps.buildEnhanceMatrix(card.enhance)) }
    if (bitmap == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh))
    } else {
        androidx.compose.foundation.Image(
            bitmap = bitmap,
            contentDescription = card.name,
            contentScale = ContentScale.Crop,
            colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(colorMatrix),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
