package com.appprint.studio.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.appprint.studio.ui.platform.PlatformBackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.alpha
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
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
import com.appprint.studio.ui.StudioUiState
import com.appprint.studio.ui.StudioViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.appprint.studio.ui.components.IraqiLogo
import com.appprint.studio.ui.components.Chrome
import com.appprint.studio.ui.components.AppConfirmDialog
import com.appprint.studio.ui.components.CanvasViewState
import com.appprint.studio.ui.components.pressScale
import com.appprint.studio.ui.theme.Motion
import com.appprint.studio.ui.theme.contrastingTextColor
import com.appprint.studio.ui.components.SampleCardArt
import com.appprint.studio.ui.strings.Str

/** Compose density-independent dp per millimetre (96 dp/inch). */
private const val DP_PER_MM = 3.7795276f

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    viewModel: StudioViewModel,
    canvasView: CanvasViewState,
    onAddImages: () -> Unit,
    onCamera: () -> Unit,
    onClearRequest: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showClearConfirm by rememberSaveable { mutableStateOf(false) }
    var showPaperSheet by rememberSaveable { mutableStateOf(false) }
    var showInspectorSheet by rememberSaveable { mutableStateOf(false) }
    var toolbarHidden by rememberSaveable { mutableStateOf(false) }
    var showDeletePage by rememberSaveable { mutableStateOf<Int?>(null) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 1000.dp
        val medium = !wide && maxWidth >= 640.dp
        val phone = !wide && !medium

        // On phones, tapping a card slides the inspector sheet up
        LaunchedEffect(state.selectedId, phone) {
            if (phone && state.selectedId != null) showInspectorSheet = true
        }

        // the top-bar arrow button opens the selected card's controls on every size class
        LaunchedEffect(state.cardPanelRequest) {
            if (state.cardPanelRequest > 0 && state.selectedId != null) showInspectorSheet = true
        }

        // Back: close dialogs/sheets first, then deselect — never exits directly
        val overlayOpen = showClearConfirm || showDeletePage != null || showInspectorSheet || showPaperSheet
        PlatformBackHandler(enabled = overlayOpen || state.selectedId != null) {
            when {
                showClearConfirm -> showClearConfirm = false
                showDeletePage != null -> showDeletePage = null
                showInspectorSheet -> showInspectorSheet = false
                showPaperSheet -> showPaperSheet = false
                state.selectedId != null -> viewModel.selectCard(null)
            }
        }

        Row(Modifier.fillMaxSize().padding(top = Chrome.topReserve(), bottom = Chrome.bottomReserve())) {
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
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        EmptyState(
                            onUpload = onAddImages,
                            onSamples = viewModel::addSamples,
                            onAddSample = viewModel::addSampleOfType,
                            modifier = Modifier.fillMaxSize(),
                        )
                        FloatingStrip(
                            visible = phone && !toolbarHidden && state.selectedId == null,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(start = 10.dp, end = 10.dp, top = 8.dp),
                        ) {
                            EditorToolbar(
                                state = state,
                                viewModel = viewModel,
                                onAddImages = onAddImages,
                                onCamera = onCamera,
                                onSamples = viewModel::addSamples,
                                onArrange = viewModel::arrangeSmart,
                                onSortSize = viewModel::sortBySize,
                                onClear = onClearRequest,
                                onOpenPaperSettings = if (wide) null else ({ showPaperSheet = true }),
                                onHide = { toolbarHidden = true },
                                compact = phone,
                            )
                        }
                        FloatingStrip(
                            visible = phone && toolbarHidden && state.selectedId == null,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(start = 10.dp, end = 10.dp, top = 8.dp),
                        ) {
                            ToolbarHiddenBar(onShow = { toolbarHidden = false })
                        }
                    }
                } else {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        SheetCanvas(
                            state = state,
                            viewModel = viewModel,
                            canvasView = canvasView,
                            modifier = Modifier.fillMaxSize(),
                        )
                        FloatingStrip(
                            visible = phone && !toolbarHidden && state.selectedId == null,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(start = 10.dp, end = 10.dp, top = 8.dp),
                        ) {
                            EditorToolbar(
                                state = state,
                                viewModel = viewModel,
                                onAddImages = onAddImages,
                                onCamera = onCamera,
                                onSamples = viewModel::addSamples,
                                onArrange = viewModel::arrangeSmart,
                                onSortSize = viewModel::sortBySize,
                                onClear = onClearRequest,
                                onOpenPaperSettings = if (wide) null else ({ showPaperSheet = true }),
                                onHide = { toolbarHidden = true },
                                compact = phone,
                            )
                        }
                        FloatingStrip(
                            visible = phone && toolbarHidden && state.selectedId == null,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(start = 10.dp, end = 10.dp, top = 8.dp),
                        ) {
                            ToolbarHiddenBar(onShow = { toolbarHidden = false })
                        }
                    }
                    SheetPageBar(
                        state = state,
                        onSelectPage = viewModel::currentPage,
                        onAddSheet = viewModel::addSheet,
                        onRequestDelete = { showDeletePage = state.currentPage },
                        modifier = Modifier.padding(bottom = 2.dp, top = 2.dp),
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
        if (showInspectorSheet && !wide) {
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
        AppConfirmDialog(
            icon = Icons.Filled.DeleteSweep,
            title = Str.clear_confirm_title,
            body = Str.clear_confirm_body,
            primaryLabel = Str.btn_clear_all,
            onPrimary = {
                viewModel.clearAll()
                showClearConfirm = false
            },
            cancelLabel = Str.cancel,
            onCancel = { showClearConfirm = false },
            onDismiss = { showClearConfirm = false },
        )
    }
    if (showDeletePage != null) {
        val page = showDeletePage!!
        val count = state.cards.count { it.page == page }
        AppConfirmDialog(
            icon = Icons.Filled.DeleteOutline,
            title = Str.delete_page_title,
            body = "الورقة رقم ${page + 1} تحوي $count بطاقة. احذف الورقة فقط وستُوزَّع البطاقات على باقي الأوراق، أو احذفها مع بطاقاتها.",
            primaryLabel = Str.delete_page_all,
            onPrimary = {
                viewModel.deleteSheet(page, deleteCards = true)
                showDeletePage = null
            },
            secondaryLabel = Str.delete_page_keep,
            onSecondary = {
                viewModel.deleteSheet(page, deleteCards = false)
                showDeletePage = null
            },
            cancelLabel = Str.cancel,
            onCancel = { showDeletePage = null },
            onDismiss = { showDeletePage = null },
        )
    }
}


/**
 * A strip that floats above the canvas and slides/fades in and out together.
 * Declared outside the column scope so the plain (non-scoped) AnimatedVisibility overload is used.
 */
@Composable
private fun FloatingStrip(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(Motion.effects()) + slideInVertically(Motion.spatial()) { -it },
        exit = fadeOut(Motion.effectsFast()) + slideOutVertically(Motion.effectsFast()) { -it },
        modifier = modifier,
    ) {
        content()
    }
}

// ------------------------------------------------------------ editor toolbar


@Composable
private fun EditorToolbar(
    state: StudioUiState,
    viewModel: StudioViewModel,
    onAddImages: () -> Unit,
    onCamera: () -> Unit,
    onSamples: () -> Unit,
    onArrange: () -> Unit,
    onSortSize: () -> Unit,
    onClear: () -> Unit,
    onOpenPaperSettings: (() -> Unit)?,
    onHide: () -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    var controlsOpen by rememberSaveable { mutableStateOf(false) }
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val indication = LocalIndication.current

    Column(
        modifier = modifier
            .widthIn(max = 520.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(scheme.surfaceContainerHigh.copy(alpha = 0.92f))
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.45f), RoundedCornerShape(percent = 50)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, top = 5.dp, end = 4.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // primary action — the only accented item in the strip
                item {
                    Row(
                        modifier = Modifier
                            .pressScale(interaction)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(scheme.primary)
                            .clickable(
                                interactionSource = interaction,
                                indication = indication,
                                onClick = onAddImages,
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Filled.FileUpload,
                            contentDescription = null,
                            tint = contrastingTextColor(scheme.primary),
                            modifier = Modifier.size(17.dp),
                        )
                        Text(
                            Str.btn_add_images,
                            style = MaterialTheme.typography.labelMedium,
                            color = contrastingTextColor(scheme.primary),
                            maxLines = 1,
                        )
                    }
                }
                item { StripIcon(Icons.Filled.PhotoCamera, Str.btn_take_photo, onCamera) }
                item { StripIcon(Icons.Filled.AutoAwesome, Str.btn_samples, onSamples) }
                item {
                    StripDivider()
                }
                item {
                    StripIcon(
                        icon = Icons.Filled.AutoFixHigh,
                        label = Str.btn_arrange,
                        onClick = onArrange,
                        enabled = state.cards.isNotEmpty(),
                    )
                }
                item {
                    StripIcon(
                        icon = Icons.Filled.SwapVert,
                        label = Str.btn_sort_size,
                        onClick = onSortSize,
                        enabled = state.cards.isNotEmpty(),
                    )
                }
                if (onOpenPaperSettings != null) {
                    item { StripIcon(Icons.Filled.Article, Str.paper_title, onOpenPaperSettings) }
                }
                item {
                    StripIcon(
                        icon = Icons.Filled.DeleteSweep,
                        label = Str.btn_clear_all,
                        onClick = onClear,
                        enabled = state.cards.isNotEmpty(),
                        tint = scheme.error,
                    )
                }
                item {
                    // control toggle — opens the quick-controls row
                    StripIcon(
                        icon = Icons.Filled.Tune,
                        label = Str.quick_controls,
                        onClick = { controlsOpen = !controlsOpen },
                        selected = controlsOpen,
                    )
                }
            }
            IconButton(onClick = onHide) {
                Icon(
                    Icons.Filled.ExpandLess,
                    contentDescription = Str.hide_toolbar,
                    tint = scheme.onSurfaceVariant,
                )
            }
        }

        // quick controls — the extra row the control button opens
        AnimatedVisibility(
            visible = controlsOpen,
            enter = fadeIn(Motion.effects()) + expandVertically(Motion.spatial()),
            exit = fadeOut(Motion.effectsFast()) + shrinkVertically(Motion.effectsFast()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuickToggle(
                    label = Str.orientation,
                    value = if (state.settings.landscape) Str.orientation_landscape else Str.orientation_portrait,
                    onClick = { viewModel.setLandscape(!state.settings.landscape) },
                )
                QuickToggle(
                    label = Str.mode,
                    value = when (state.settings.mode) {
                        com.appprint.studio.core.model.ArrangeMode.GRID -> Str.mode_grid
                        com.appprint.studio.core.model.ArrangeMode.COMFORTABLE -> Str.mode_comfy
                        com.appprint.studio.core.model.ArrangeMode.COMPACT -> Str.mode_compact
                    },
                    onClick = {
                        val next = when (state.settings.mode) {
                            com.appprint.studio.core.model.ArrangeMode.GRID -> com.appprint.studio.core.model.ArrangeMode.COMFORTABLE
                            com.appprint.studio.core.model.ArrangeMode.COMFORTABLE -> com.appprint.studio.core.model.ArrangeMode.COMPACT
                            com.appprint.studio.core.model.ArrangeMode.COMPACT -> com.appprint.studio.core.model.ArrangeMode.GRID
                        }
                        viewModel.setMode(next)
                    },
                )
                QuickToggle(
                    label = Str.showFrames,
                    value = if (state.settings.showFrames) Str.on else Str.off,
                    onClick = { viewModel.setShowFrames(!state.settings.showFrames) },
                )
                QuickToggle(
                    label = Str.showGuides,
                    value = if (state.settings.showGuides) Str.on else Str.off,
                    onClick = { viewModel.setShowGuides(!state.settings.showGuides) },
                )
            }
        }
    }
}

/** Neutral icon-only action inside the floating tool strip. */
@Composable
private fun StripIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    selected: Boolean = false,
    tint: androidx.compose.ui.graphics.Color? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val indication = LocalIndication.current
    val color = tint ?: scheme.onSurface
    Box(
        modifier = Modifier
            .size(38.dp)
            .pressScale(interaction, pressedScale = 0.9f)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (selected) scheme.primaryContainer else Color.Transparent)
            .clickable(
                interactionSource = interaction,
                indication = indication,
                enabled = enabled,
                onClick = onClick,
            )
            .alpha(if (enabled) 1f else 0.38f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun StripDivider() {
    Box(
        modifier = Modifier
            .height(22.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    )
}

/** Compact value chip used by the quick-controls row. */
@Composable
private fun QuickToggle(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val indication = LocalIndication.current
    Row(
        modifier = Modifier
            .pressScale(interaction, pressedScale = 0.94f)
            .clip(RoundedCornerShape(percent = 50))
            .background(scheme.surfaceContainerHighest.copy(alpha = 0.9f))
            .clickable(interactionSource = interaction, indication = indication, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = scheme.onSurface,
            maxLines = 1,
        )
    }
}

/** Slim pill shown when the tool strip is collapsed — one tap brings the whole strip back. */
@Composable
private fun ToolbarHiddenBar(onShow: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val indication = LocalIndication.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(scheme.surfaceContainerHigh.copy(alpha = 0.92f))
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.45f), RoundedCornerShape(percent = 50))
            .pressScale(interaction, pressedScale = 0.94f)
            .clickable(interactionSource = interaction, indication = indication, onClick = onShow)
            .padding(start = 12.dp, top = 6.dp, end = 14.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.ExpandMore,
            contentDescription = Str.show_toolbar,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            Str.tools_strip,
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
            maxLines = 1,
        )
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
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 2.dp, end = 4.dp, bottom = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // floating page labels — plain text, no chip chrome; the active page gets a thin underline + bold weight
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items((0 until state.pageCount).toList(), key = { it }) { page ->
                val selected = state.currentPage == page
                val onSurface = scheme.onSurface
                val variant = scheme.onSurfaceVariant
                val backdrop by animateColorAsState(
                    targetValue = if (selected) scheme.surfaceContainerHighest.copy(alpha = 0.85f) else Color.Transparent,
                    animationSpec = Motion.effects(),
                    label = "pageBackdrop",
                )
                val textColor by animateColorAsState(
                    targetValue = if (selected) onSurface else variant,
                    animationSpec = Motion.effects(),
                    label = "pageText",
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(backdrop)
                        .clickable { onSelectPage(page) }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                ) {
                    Text(
                        text = "ورقة ${page + 1} · ${state.cards.count { it.page == page }}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                    )
                }
            }
        }
        if (state.pageCount > 1) {
            IconButton(onClick = onRequestDelete) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = Str.delete_sheet,
                    tint = scheme.error,
                    modifier = Modifier.size(21.dp),
                )
            }
        }

        Box(modifier = Modifier.height(28.dp).width(1.dp).background(scheme.outlineVariant.copy(alpha = 0.6f)))

        // big, separated alone — the only accented control on the row
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(6.dp, RoundedCornerShape(percent = 50), clip = false)
                .clip(RoundedCornerShape(percent = 50))
                .background(scheme.primary)
                .clickable(onClick = onAddSheet)
                .semantics { role = androidx.compose.ui.semantics.Role.Button },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = Str.add_sheet,
                tint = contrastingTextColor(scheme.primary),
                modifier = Modifier.size(22.dp),
            )
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
private fun SheetCanvas(state: StudioUiState, viewModel: StudioViewModel, canvasView: CanvasViewState, modifier: Modifier = Modifier) {
    val settings = state.settings
    val densityPx = LocalDensity.current.density
    // gestures live on the WHOLE canvas area -> 1:1 finger-follow
    val transformable = rememberTransformableState { zoomChange, panChange, _ ->
        canvasView.zoom = (canvasView.zoom * zoomChange).coerceIn(1f, 4.5f)
        canvasView.panX += panChange.x
        canvasView.panY += panChange.y
    }

    // Gestures behave identically in RTL — pin the canvas to LTR coordinates.
    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
        BoxWithConstraints(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .clipToBounds()
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        canvasView.zoom = when {
                            canvasView.zoom < 1.6f -> 1.75f
                            canvasView.zoom < 2.4f -> 2.5f
                            else -> 1f
                        }
                        if (canvasView.zoom == 1f) {
                            canvasView.panX = 0f
                            canvasView.panY = 0f
                        }
                    })
                }
                .transformable(transformable),
        ) {
            val sheetWdp = settings.sheetWidthMm * DP_PER_MM
            val sheetHdp = settings.sheetHeightMm * DP_PER_MM
            val fit = minOf(
                (maxWidth.value - 24f) / sheetWdp,
                (maxHeight.value - 24f) / sheetHdp,
            ).coerceIn(0.25f, 2.4f)
            val zoom by animateFloatAsState(canvasView.zoom, Motion.spatialFast(), label = "zoom")
            val effective = fit * zoom

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
                        translationX = canvasView.panX
                        translationY = canvasView.panY
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
                                canvasView.zoom = when {
                                    canvasView.zoom < 1.6f -> 1.75f
                                    canvasView.zoom < 2.4f -> 2.5f
                                    else -> 1f
                                }
                                if (canvasView.zoom == 1f) {
                                    canvasView.panX = 0f
                                    canvasView.panY = 0f
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
            }
        }
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
    val selectionScale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        animationSpec = Motion.spatial(),
        label = "cardSelectionScale",
    )
    val selectionAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = Motion.effects(),
        label = "cardSelectionAlpha",
    )

    Box(
        Modifier
            .offset { IntOffset((card.xMm * mmToPx).roundToInt(), (card.yMm * mmToPx).roundToInt()) }
            .size((card.effectiveWidth * DP_PER_MM).dp, (card.effectiveHeight * DP_PER_MM).dp)
            .graphicsLayer {
                scaleX = selectionScale
                scaleY = selectionScale
            }
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
        if (selectionAlpha > 0.01f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .border(2.dp, Color(0xFF007A3D).copy(alpha = selectionAlpha), RoundedCornerShape(6.dp)),
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
