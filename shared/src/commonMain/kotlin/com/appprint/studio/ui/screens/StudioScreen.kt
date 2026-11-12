package com.appprint.studio.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.appprint.studio.ui.platform.PlatformBackHandler
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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt
import com.appprint.studio.core.image.PixelOps
import com.appprint.studio.core.model.CardItem
import com.appprint.studio.core.model.CardType
import com.appprint.studio.core.model.FrameStyle
import com.appprint.studio.core.model.SheetBackground
import com.appprint.studio.core.platform.rememberCardBitmap
import com.appprint.studio.core.platform.rememberCameraCaptureLauncher
import com.appprint.studio.core.platform.rememberImagePickerLauncher
import com.appprint.studio.ui.StudioUiState
import com.appprint.studio.ui.StudioViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.appprint.studio.ui.components.IraqiLogo
import com.appprint.studio.ui.components.SampleCardArt
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
    var toolbarHidden by rememberSaveable { mutableStateOf(false) }
    var showDeletePage by rememberSaveable { mutableStateOf<Int?>(null) }

    val pickImages = rememberImagePickerLauncher(maxItems = 100) { uris -> viewModel.importImages(uris) }
    val onCamera = rememberCameraCaptureLauncher { path ->
        if (path != null) viewModel.importCapture(path) else viewModel.notifyCameraUnavailable()
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 1000.dp
        val medium = !wide && maxWidth >= 640.dp
        val phone = !wide && !medium

        // On phones, tapping a card slides the inspector sheet up
        LaunchedEffect(state.selectedId, phone) {
            if (phone && state.selectedId != null) showInspectorSheet = true
        }

        // Back: close dialogs/sheets first, then deselect — never exits directly
        PlatformBackHandler(enabled = true) {
            when {
                showClearConfirm -> showClearConfirm = false
                showDeletePage != null -> showDeletePage = null
                showInspectorSheet -> showInspectorSheet = false
                showPaperSheet -> showPaperSheet = false
                state.selectedId != null -> viewModel.selectCard(null)
            }
        }

        Row(Modifier.fillMaxSize()) {
            if (wide) {
                PaperSettingsPanel(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier
                        .width(290.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                )
                Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
            }

            Column(Modifier.weight(1f).fillMaxHeight()) {
                if (state.cards.isEmpty()) {
                    if (!toolbarHidden) {
                        EditorToolbar(
                            state = state,
                            onAddImages = pickImages,
                            onCamera = onCamera,
                            onSamples = viewModel::addSamples,
                            onArrange = viewModel::arrangeSmart,
                            onSortSize = viewModel::sortBySize,
                            onClear = { showClearConfirm = true },
                            onSharePng = { viewModel.sharePng(state.currentPage) },
                            onOpenPaperSettings = if (wide) null else ({ showPaperSheet = true }),
                            onHide = { toolbarHidden = true },
                            compact = phone,
                        )
                    } else {
                        ToolbarHiddenBar { toolbarHidden = false }
                    }
                    EmptyState(
                        onUpload = pickImages,
                        onSamples = viewModel::addSamples,
                        onAddSample = viewModel::addSampleOfType,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                } else {
                    if (!toolbarHidden) {
                        EditorToolbar(
                            state = state,
                            onAddImages = pickImages,
                            onCamera = onCamera,
                            onSamples = viewModel::addSamples,
                            onArrange = viewModel::arrangeSmart,
                            onSortSize = viewModel::sortBySize,
                            onClear = { showClearConfirm = true },
                            onSharePng = { viewModel.sharePng(state.currentPage) },
                            onOpenPaperSettings = if (wide) null else ({ showPaperSheet = true }),
                            onHide = { toolbarHidden = true },
                            compact = phone,
                        )
                    } else {
                        ToolbarHiddenBar { toolbarHidden = false }
                    }
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        SheetCanvas(
                            state = state,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    SheetPageBar(
                        state = state,
                        onSelectPage = viewModel::currentPage,
                        onAddSheet = viewModel::addSheet,
                        onRequestDelete = { showDeletePage = state.currentPage },
                        modifier = Modifier.padding(bottom = 6.dp, top = 2.dp),
                    )
                }
            }

            if (wide) {
                Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                InspectorPanel(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .background(MaterialTheme.colorScheme.surfaceContainer),
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
    if (showDeletePage != null) {
        val page = showDeletePage!!
        val count = state.cards.count { it.page == page }
        AlertDialog(
            onDismissRequest = { showDeletePage = null },
            title = { Text(Str.delete_page_title) },
            text = {
                Text(
                    "هذه الورقة رقم ${page + 1} فيها $count بطاقة.\n${Str.delete_page_body}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextButton(onClick = {
                        viewModel.deleteSheet(page, deleteCards = false)
                        showDeletePage = null
                    }) { Text(Str.delete_page_keep) }
                    TextButton(onClick = {
                        viewModel.deleteSheet(page, deleteCards = true)
                        showDeletePage = null
                    }) { Text(Str.delete_page_all, color = MaterialTheme.colorScheme.error) }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePage = null }) { Text(Str.cancel) }
            },
        )
    }
}

// ------------------------------------------------------------ editor toolbar

@Composable
private fun EditorToolbar(
    state: StudioUiState,
    onAddImages: () -> Unit,
    onCamera: () -> Unit,
    onSamples: () -> Unit,
    onArrange: () -> Unit,
    onSortSize: () -> Unit,
    onClear: () -> Unit,
    onSharePng: () -> Unit,
    onOpenPaperSettings: (() -> Unit)?,
    onHide: () -> Unit,
    compact: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item {
                FilledTonalButton(onClick = onAddImages, shape = RoundedCornerShape(percent = 50)) {
                    Icon(Icons.Filled.FileUpload, contentDescription = Str.btn_add_images, modifier = Modifier.size(18.dp))
                    if (!compact) {
                        Spacer(Modifier.size(6.dp))
                        Text(Str.btn_add_images, maxLines = 1)
                    }
                }
            }
            item {
                FilledTonalButton(onClick = onCamera, shape = RoundedCornerShape(percent = 50)) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = Str.btn_take_photo, modifier = Modifier.size(18.dp))
                    if (!compact) {
                        Spacer(Modifier.size(6.dp))
                        Text(Str.btn_take_photo, maxLines = 1)
                    }
                }
            }
            item {
                FilledTonalButton(onClick = onSamples, shape = RoundedCornerShape(percent = 50)) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = Str.btn_samples, modifier = Modifier.size(18.dp))
                    if (!compact) {
                        Spacer(Modifier.size(6.dp))
                        Text(Str.btn_samples, maxLines = 1)
                    }
                }
            }
            item {
                FilledTonalButton(onClick = onArrange, enabled = state.cards.isNotEmpty(), shape = RoundedCornerShape(percent = 50)) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = Str.btn_arrange, modifier = Modifier.size(18.dp))
                    if (!compact) {
                        Spacer(Modifier.size(6.dp))
                        Text(Str.btn_arrange, maxLines = 1)
                    }
                }
            }
            item {
                FilledTonalButton(onClick = onSortSize, enabled = state.cards.isNotEmpty(), shape = RoundedCornerShape(percent = 50)) {
                    Icon(Icons.Filled.SwapVert, contentDescription = Str.btn_sort_size, modifier = Modifier.size(18.dp))
                    if (!compact) {
                        Spacer(Modifier.size(6.dp))
                        Text(Str.btn_sort_size, maxLines = 1)
                    }
                }
            }
            item {
                FilledTonalButton(onClick = onSharePng, enabled = state.cards.isNotEmpty(), shape = RoundedCornerShape(percent = 50)) {
                    Icon(Icons.Filled.Image, contentDescription = Str.btn_save_png, modifier = Modifier.size(18.dp))
                    if (!compact) {
                        Spacer(Modifier.size(6.dp))
                        Text(Str.btn_save_png, maxLines = 1)
                    }
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
                        Icon(Icons.Filled.Menu, contentDescription = Str.paper_title)
                    }
                }
            }
        }
        IconButton(onClick = onHide) {
            Icon(Icons.Filled.ExpandLess, contentDescription = "إخفاء شريط الأدوات", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Slim bar shown when the toolbar is hidden — one tap brings it back. */
@Composable
private fun ToolbarHiddenBar(onShow: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 8.dp, end = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onShow) {
            Icon(Icons.Filled.ExpandMore, contentDescription = "إظهار شريط الأدوات", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ------------------------------------------------------------ page bar

@Composable
private fun SheetPageBar(
    state: StudioUiState,
    onSelectPage: (Int) -> Unit,
    onAddSheet: () -> Unit,
    onRequestDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.pageCount > 1) {
            items((0 until state.pageCount).toList(), key = { it }) { page ->
                AssistChip(
                    onClick = { onSelectPage(page) },
                    label = { Text("ورقة ${page + 1} (${state.cards.count { it.page == page }})") },
                    leadingIcon = if (state.currentPage == page) {
                        { Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else {
                        null
                    },
                )
            }
            item {
                IconButton(onClick = onRequestDelete) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = Str.delete_sheet, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(21.dp))
                }
            }
        }
        item {
            IconButton(onClick = onAddSheet) {
                Icon(Icons.Filled.Add, contentDescription = Str.add_sheet, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
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
        IraqiLogo(size = 84.dp)
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
                Icon(Icons.Filled.FileUpload, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(Str.empty_upload)
            }
            OutlinedButton(onClick = onSamples, shape = RoundedCornerShape(percent = 50)) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(Str.empty_cta)
            }
        }

        // official Iraqi card samples strip — edge to edge
        Text(
            text = Str.sample_strip_title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 18.dp, start = 24.dp, end = 24.dp),
        )
        Spacer(Modifier.size(8.dp))
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(CardType.PRINTABLE.filter { it != CardType.CUSTOM }, key = { it.key }) { type ->
                SampleCardTile(type = type, onClick = { onAddSample(type) })
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
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .shadow(4.dp, RoundedCornerShape(8.dp))
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
    val densityPx = LocalDensity.current.density
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var panX by rememberSaveable { mutableFloatStateOf(0f) }
    var panY by rememberSaveable { mutableFloatStateOf(0f) }
    // gestures live on the WHOLE canvas area -> 1:1 finger-follow
    val transformable = rememberTransformableState { zoomChange, panChange, _ ->
        zoom = (zoom * zoomChange).coerceIn(1f, 4.5f)
        panX += panChange.x
        panY += panChange.y
    }

    // Gestures behave identically in RTL — pin the canvas to LTR coordinates.
    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
        BoxWithConstraints(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .clipToBounds(),
        ) {
            val sheetWdp = settings.sheetWidthMm * DP_PER_MM
            val sheetHdp = settings.sheetHeightMm * DP_PER_MM
            val fit = minOf(
                (maxWidth.value - 24f) / sheetWdp,
                (maxHeight.value - 24f) / sheetHdp,
            ).coerceIn(0.25f, 2.4f)
            val effective = fit * zoom

            Box(
                Modifier
                    .fillMaxSize()
                    .transformable(transformable)
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = {
                            zoom = when {
                                zoom < 1.6f -> 1.75f
                                zoom < 2.4f -> 2.5f
                                else -> 1f
                            }
                            if (zoom == 1f) {
                                panX = 0f
                                panY = 0f
                            }
                        })
                    },
            )

            // light blueprint backdrop (adapts to theme)
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
                // the sheet itself
                Box(
                    Modifier
                        .fillMaxSize()
                        .shadow(16.dp, RoundedCornerShape(3.dp))
                        .clip(RoundedCornerShape(3.dp))
                        .background(paperColor)
                        .pointerInput(Unit) {
                            detectTapGestures(onDoubleTap = {
                                zoom = when {
                                    zoom < 1.6f -> 1.75f
                                    zoom < 2.4f -> 2.5f
                                    else -> 1f
                                }
                                if (zoom == 1f) {
                                    panX = 0f
                                    panY = 0f
                                }
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
                    // cards on this page
                    state.cardsOnPage.forEach { card ->
                        CardOnSheet(
                            card = card,
                            densityPx = densityPx,
                            scale = effective,
                            selected = card.id == state.selectedId,
                            compareBefore = state.compareBefore,
                            showGuides = settings.showGuides,
                            frameWidthDp = settings.frameWidthDp,
                            frameColorHex = settings.frameColorHex,
                            frameStyle = settings.frameStyle,
                            showFrames = settings.showFrames,
                            onSelect = viewModel::selectCard,
                            onMove = viewModel::moveCard,
                        )
                    }
                }
            }
            // floating zoom controls — always visible on the sheet
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ZoomButton(icon = Icons.Filled.ZoomIn, contentDescription = Str.zoom_in) {
                    zoom = (zoom * 1.33f).coerceIn(1f, 4.5f)
                }
                ZoomButton(icon = Icons.Filled.ZoomOut, contentDescription = Str.zoom_out) {
                    zoom = (zoom / 1.33f).coerceIn(1f, 4.5f)
                }
                ZoomButton(icon = Icons.Filled.Restore, contentDescription = Str.reset_view) {
                    zoom = 1f
                    panX = 0f
                    panY = 0f
                }
            }
            }
        }
    }
}

@Composable
private fun ZoomButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .shadow(8.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun CardOnSheet(
    card: CardItem,
    densityPx: Float,
    scale: Float,
    selected: Boolean,
    compareBefore: Boolean,
    showGuides: Boolean,
    frameWidthDp: Float,
    frameColorHex: Long,
    frameStyle: FrameStyle,
    showFrames: Boolean,
    onSelect: (Long) -> Unit,
    onMove: (Long, Float, Float) -> Unit,
) {
    val currentCard by rememberUpdatedState(card)
    val mmToPx = DP_PER_MM * densityPx

    Box(
        Modifier
            .offset { IntOffset((card.xMm * mmToPx).roundToInt(), (card.yMm * mmToPx).roundToInt()) }
            .size((card.effectiveWidth * DP_PER_MM).dp, (card.effectiveHeight * DP_PER_MM).dp)
            .pointerInput(card.id, scale) {
                detectDragGestures { _, dragAmount ->
                    val cur = currentCard
                    onMove(
                        cur.id,
                        cur.xMm + dragAmount.x / (mmToPx * scale),
                        cur.yMm + dragAmount.y / (mmToPx * scale),
                    )
                }
            }
            .clickable { onSelect(card.id) },
    ) {
        val contentW = card.widthMm * DP_PER_MM
        val contentH = card.heightMm * DP_PER_MM
        Box(
            Modifier
                .align(Alignment.Center)
                .size(contentW.dp, contentH.dp)
                .graphicsLayer { rotationZ = card.rotation.toFloat() }
                .clip(RoundedCornerShape(6.dp)),
        ) {
            if (card.imageUri != null) {
                CardImage(card = card, compareBefore = compareBefore)
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
                val outside = 1.2f * DP_PER_MM * densityPx
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
                    .border(2.dp, Color(0xFF007A3D), RoundedCornerShape(6.dp)),
            )
        }
    }
}

@Composable
private fun CardImage(card: CardItem, compareBefore: Boolean) {
    val bitmap = rememberCardBitmap(card.imageUri)
    val colorMatrix = remember(card.enhance, compareBefore) {
        if (compareBefore) null
        else androidx.compose.ui.graphics.ColorMatrix(PixelOps.buildEnhanceMatrix(card.enhance))
    }
    if (bitmap == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh))
    } else {
        androidx.compose.foundation.Image(
            bitmap = bitmap,
            contentDescription = card.name,
            contentScale = ContentScale.Crop,
            colorFilter = colorMatrix?.let { androidx.compose.ui.graphics.ColorFilter.colorMatrix(it) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
