package com.appprint.studio.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.padding
import com.appprint.studio.core.model.CardType
import com.appprint.studio.ui.components.Chrome
import com.appprint.studio.ui.components.EdgeFadeRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.appprint.studio.ui.strings.Str

/** Compose density-independent dp per millimetre (96 dp/inch). */
private const val DP_PER_MM = 3.7795276f

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
                        .width(290.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(top = Chrome.topReserve()),
                )
                Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
            }

            Column(Modifier.weight(1f).fillMaxHeight()) {
                if (state.cards.isEmpty()) {
                    StudioToolbar(
                        state = state,
                        onAddImages = pickImages,
                        onCamera = onCamera,
                        onSamples = viewModel::addSamples,
                        onArrange = viewModel::arrangeSmart,
                        onSortSize = viewModel::sortBySize,
                        onClear = { showClearConfirm = true },
                        onSharePng = { viewModel.sharePng(state.currentPage) },
                        onOpenPaperSettings = if (wide) null else ({ showPaperSheet = true }),
                        modifier = Modifier.padding(top = Chrome.topReserve()),
                    )
                    EmptyState(
                        onUpload = pickImages,
                        onSamples = viewModel::addSamples,
                        onAddSample = viewModel::addSampleOfType,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                } else {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        SheetCanvas(
                            state = state,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize(),
                        )
                        StudioToolbar(
                            state = state,
                            onAddImages = pickImages,
                            onCamera = onCamera,
                            onSamples = viewModel::addSamples,
                            onArrange = viewModel::arrangeSmart,
                            onSortSize = viewModel::sortBySize,
                            onClear = { showClearConfirm = true },
                            onSharePng = { viewModel.sharePng(state.currentPage) },
                            onOpenPaperSettings = if (wide) null else ({ showPaperSheet = true }),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = Chrome.topReserve()),
                        )
                    }
                    SheetPageBar(
                        state = state,
                        onSelectPage = viewModel::currentPage,
                        modifier = Modifier.padding(bottom = Chrome.bottomReserve()),
                    )
                }
            }

            if (wide || medium) {
                Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                InspectorPanel(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier
                        .width(if (wide) 320.dp else 300.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(top = Chrome.topReserve()),
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
            title = { Text(Str.clear_confirm_title) },
            text = { Text(Str.clear_confirm_body) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll()
                    showClearConfirm = false
                }) { Text(Str.delete, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text(Str.cancel) }
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
    onSortSize: () -> Unit,
    onClear: () -> Unit,
    onSharePng: () -> Unit,
    onOpenPaperSettings: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    // Edge-to-edge scroll strip — the scaffold melts into the strip on both screen edges
    EdgeFadeRow(edgeColor = MaterialTheme.colorScheme.background, modifier = modifier) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item {
                FilledTonalButton(onClick = onAddImages, shape = RoundedCornerShape(percent = 50)) {
                    Icon(AppIcons.Upload, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text(Str.btn_add_images, maxLines = 1)
                }
            }
            item {
                FilledTonalButton(onClick = onCamera, shape = RoundedCornerShape(percent = 50)) {
                    Icon(AppIcons.Camera, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text(Str.btn_take_photo, maxLines = 1)
                }
            }
            item {
                FilledTonalButton(onClick = onSamples, shape = RoundedCornerShape(percent = 50)) {
                    Icon(AppIcons.Sparkle, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text(Str.btn_samples, maxLines = 1)
                }
            }
            item {
                FilledTonalButton(onClick = onArrange, enabled = state.cards.isNotEmpty(), shape = RoundedCornerShape(percent = 50)) {
                    Icon(AppIcons.Sparkle, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text(Str.btn_arrange, maxLines = 1)
                }
            }
            item {
                FilledTonalButton(onClick = onSortSize, enabled = state.cards.isNotEmpty(), shape = RoundedCornerShape(percent = 50)) {
                    Icon(AppIcons.Sort, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text(Str.btn_sort_size, maxLines = 1)
                }
            }
            item {
                FilledTonalButton(onClick = onSharePng, enabled = state.cards.isNotEmpty(), shape = RoundedCornerShape(percent = 50)) {
                    Icon(AppIcons.Image, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text(Str.btn_save_png, maxLines = 1)
                }
            }
            item {
                TextButton(onClick = onClear, enabled = state.cards.isNotEmpty()) {
                    Text(Str.btn_clear_all, color = MaterialTheme.colorScheme.error, maxLines = 1)
                }
            }
            if (onOpenPaperSettings != null) {
                item {
                    IconButton(onClick = onOpenPaperSettings) {
                        Icon(AppIcons.Menu, contentDescription = Str.paper_title)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------ page bar

@Composable
private fun SheetPageBar(state: StudioUiState, onSelectPage: (Int) -> Unit, modifier: Modifier = Modifier) {
    if (state.pageCount <= 1) return
    EdgeFadeRow(edgeColor = MaterialTheme.colorScheme.background, modifier = modifier) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items((0 until state.pageCount).toList(), key = { it }) { page ->
                AssistChip(
                    onClick = { onSelectPage(page) },
                    label = { Text("ورقة ${page + 1}") },
                    leadingIcon = if (state.currentPage == page) {
                        { Icon(AppIcons.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

// ------------------------------------------------------------ empty state

@Composable
private fun EmptyState(
    onUpload: () -> Unit,
    onSamples: () -> Unit,
    onAddSample: (CardType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        FlagCardGlyph(sizeDp = 84.dp)
        Text(
            text = Str.empty_title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(top = 14.dp, start = 24.dp, end = 24.dp),
        )
        Text(
            text = Str.empty_subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, start = 32.dp, end = 32.dp),
        )
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onUpload, shape = RoundedCornerShape(percent = 50)) {
                Icon(AppIcons.Upload, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(Str.empty_upload)
            }
            OutlinedButton(onClick = onSamples, shape = RoundedCornerShape(percent = 50)) {
                Icon(AppIcons.Sparkle, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(Str.empty_cta)
            }
        }

        // edge-to-edge samples strip — official Iraqi card types with true measurements
        Text(
            text = Str.sample_strip_title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 22.dp, start = 24.dp, end = 24.dp),
        )
        Spacer(Modifier.size(8.dp))
        EdgeFadeRow(edgeColor = MaterialTheme.colorScheme.background) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items(CardType.PRINTABLE.filter { it != CardType.CUSTOM }, key = { it.key }) { type ->
                    SampleCardTile(type = type, onClick = { onAddSample(type) })
                }
            }
        }
    }
}

@Composable
private fun SampleCardTile(type: CardType, onClick: () -> Unit) {
    val isPassport = type == CardType.PASSPORT
    Column(
        modifier = Modifier
            .width(112.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // mini official-card preview — same artwork language as the canvas samples
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .shadow(6.dp, RoundedCornerShape(8.dp))
                .size(92.dp, 58.dp)
                .background(if (isPassport) Color(0xFF17365A) else Color(0xFFFDFAF1)),
        ) {
            if (isPassport) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(type.shortAr, color = Color(0xFFD9A51F), fontWeight = FontWeight.Black, fontSize = 13.sp)
                    Text("PASSPORT", color = Color(0xFFD9A51F), fontSize = 6.sp, letterSpacing = 2.sp)
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth().weight(0.42f)) {
                        Box(Modifier.weight(0.34f).fillMaxSize().background(Color(0xFFC8102E)))
                        Box(Modifier.weight(0.32f).fillMaxSize().background(Color(0xFFFDFAF1)), contentAlignment = Alignment.Center) {
                            Text("الله أكبر", color = Color(0xFF007A3D), fontWeight = FontWeight.Black, fontSize = 6.sp, maxLines = 1)
                        }
                        Box(Modifier.weight(0.34f).fillMaxSize().background(Color(0xFF007A3D)))
                    }
                    Box(Modifier.weight(0.58f).fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(type.shortAr, color = Color(0xFF1C1C1C), fontWeight = FontWeight.Black, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        }
        Text(
            text = type.labelAr,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = type.sizeNoteAr.substringBefore('—').trim(),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

// ------------------------------------------------------------ canvas

@Composable
private fun SheetCanvas(state: StudioUiState, viewModel: StudioViewModel, modifier: Modifier = Modifier) {
    val settings = state.settings
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var panX by rememberSaveable { mutableFloatStateOf(0f) }
    var panY by rememberSaveable { mutableFloatStateOf(0f) }
    // gestures live on the WHOLE canvas area → coordinates map 1:1 to the finger
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

        // full-bleed pan/zoom surface — any finger anywhere moves the paper
        Box(
            Modifier
                .fillMaxSize()
                .transformable(transformable)
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        zoom = 1f
                        panX = 0f
                        panY = 0f
                    })
                },
        )

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
                        .pointerInput(Unit) {
                            detectTapGestures(onDoubleTap = {
                                zoom = 1f
                                panX = 0f
                                panY = 0f
                            })
                        }
                        .clickable { viewModel.selectCard(null) },
                ) {
                    if (settings.showGuides) {
                        Canvas(Modifier.fillMaxSize()) {
                            val m = settings.marginMm * DP_PER_MM * density
                            val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
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
                            text = "ورقة ${state.currentPage + 1}",
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
                    FrameStyle.DASHED -> PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
                    FrameStyle.DOTTED -> PathEffect.dashPathEffect(floatArrayOf(1.5.dp.toPx(), 5.dp.toPx()))
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
                val dash = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
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
