@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.appprint.studio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appprint.studio.ui.StudioSection
import com.appprint.studio.ui.TabatiViewModel
import com.appprint.studio.ui.components.Chrome
import com.appprint.studio.ui.platform.PlatformBackHandler
import com.appprint.studio.ui.strings.Str

/**
 * The Studio — the canvas fills the screen; sections open as a side panel on
 * medium+ or a bottom drawer on compact, exactly like the design.
 */
@Composable
fun StudioScreen(
    viewModel: TabatiViewModel,
    onImportMenu: () -> Unit,
    onReplaceImage: (Long) -> Unit,
    onPickGallery: () -> Unit = onImportMenu,
    onCaptureCamera: () -> Unit = onImportMenu,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isCompact = Chrome.isCompact()
    val isExpanded = Chrome.isExpanded()

    var deletePageIdx by remember { mutableStateOf<Int?>(null) }
    var swapFromId by remember { mutableStateOf<Long?>(null) }
    var clearAllOpen by remember { mutableStateOf(false) }

    // Back: close overlays → editing section → deselect. Never exits directly.
    val overlayOpen = state.longPressSheetCardId != null || swapFromId != null || deletePageIdx != null || clearAllOpen
    PlatformBackHandler(
        enabled = overlayOpen || state.section != StudioSection.SHEET || state.selectedCardId != null,
    ) {
        when {
            overlayOpen -> { /* sheets dismiss via their own buttons */ }
            state.section != StudioSection.SHEET -> viewModel.setSection(StudioSection.SHEET)
            state.selectedCardId != null -> viewModel.selectCard(null)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = Chrome.topReserve(), bottom = Chrome.bottomReserve()),
        ) {
            // -------------------------------------------------- workspace
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    SheetCanvas(
                        state = state,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize(),
                        onPickGallery = onPickGallery,
                        onCaptureCamera = onCaptureCamera,
                        onAddDemoCards = viewModel::addSamples,
                    )
                }
                if (state.cards.isNotEmpty()) {
                    SheetPageBar(
                        state = state,
                        onSelectPage = viewModel::currentPage,
                        onAddSheet = viewModel::addSheet,
                        onRequestDelete = { page -> deletePageIdx = page },
                        onArrange = viewModel::arrangeSmart,
                        onResetAll = { clearAllOpen = true },
                    )
                }
            }

            // ----------------------------------------- side panel (medium+)
            AnimatedVisibility(
                visible = !isCompact && state.section != StudioSection.SHEET,
                enter = fadeIn(tween(220)) + expandHorizontally(expandFrom = Alignment.End, animationSpec = tween(220)),
                exit = fadeOut(tween(160)) + shrinkHorizontally(shrinkTowards = Alignment.End, animationSpec = tween(160)),
                modifier = Modifier.fillMaxHeight(),
            ) {
                Box(
                    modifier = Modifier
                        .width((if (isExpanded) 400 else 360).dp)
                        .fillMaxHeight(),
                ) {
                    SectionPanel(
                        state = state,
                        viewModel = viewModel,
                        onReplaceImage = onReplaceImage,
                        onClearAllRequest = { clearAllOpen = true },
                        onRequestSwap = { id -> swapFromId = id },
                    )
                }
            }
        }

        // --------------------------------------- bottom drawer (compact)
        AnimatedVisibility(
            visible = isCompact && state.section != StudioSection.SHEET,
            enter = fadeIn(tween(180)) + expandVertically(expandFrom = Alignment.Bottom, animationSpec = tween(220)),
            exit = fadeOut(tween(120)) + shrinkVertically(shrinkTowards = Alignment.Bottom, animationSpec = tween(160)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            SurfaceDrawer(
                state = state,
                viewModel = viewModel,
                onReplaceImage = onReplaceImage,
                onClearAllRequest = { clearAllOpen = true },
                onRequestSwap = { id -> swapFromId = id },
            )
        }

        // --------------------------------------- long-press quick sheet
        val longPressedId = state.longPressSheetCardId
        if (longPressedId != null) {
            com.appprint.studio.ui.components.QuickActionsSheet(
                state = state,
                viewModel = viewModel,
                onReplaceImage = onReplaceImage,
                onRequestSwap = { swapFromId = longPressedId },
                onDismiss = viewModel::dismissLongPressSheet,
            )
        }

        // ------------------------------------------ swap picker sheet
        val swapId = swapFromId
        if (swapId != null) {
            SwapPickerSheet(
                fromId = swapId,
                state = state,
                onSwap = { targetId ->
                    viewModel.swapCardsPositions(swapId, targetId)
                    swapFromId = null
                },
                onDismiss = { swapFromId = null },
            )
        }

        // ------------------------------------------ delete page dialog
        deletePageIdx?.let { idx ->
            val count = state.cards.count { it.page == idx }
            HtmlStyleDialog(
                title = Str.delete_page_title.format(idx + 1),
                onDismiss = { deletePageIdx = null },
            ) {
                Text(
                    text = Str.delete_page_body.format(count),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HtmlDialogButton(
                    container = MaterialTheme.colorScheme.primary,
                    content = MaterialTheme.colorScheme.onPrimary,
                    label = Str.delete_page_keep,
                    onClick = { viewModel.deletePageKeepCards(idx); deletePageIdx = null },
                )
                HtmlDialogButton(
                    container = MaterialTheme.colorScheme.tertiary,
                    content = MaterialTheme.colorScheme.onTertiary,
                    label = Str.delete_page_cards,
                    onClick = { viewModel.deletePageWithCards(idx); deletePageIdx = null },
                )
            }
        }

        // ------------------------------------------ clear all dialog
        if (clearAllOpen) {
            HtmlStyleDialog(
                title = Str.clear_all_title,
                onDismiss = { clearAllOpen = false },
            ) {
                Text(
                    text = Str.clear_all_body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HtmlDialogButton(
                        container = MaterialTheme.colorScheme.surfaceContainerHighest,
                        content = MaterialTheme.colorScheme.onSurface,
                        label = Str.clear_all_cancel,
                        onClick = { clearAllOpen = false },
                        modifier = Modifier.weight(1f),
                    )
                    HtmlDialogButton(
                        container = MaterialTheme.colorScheme.tertiary,
                        content = MaterialTheme.colorScheme.onTertiary,
                        label = Str.clear_all_confirm,
                        onClick = { viewModel.clearAll(); clearAllOpen = false },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** Section dispatcher shared by the side panel and the compact bottom drawer. */
@Composable
private fun SectionPanel(
    state: com.appprint.studio.ui.TabatiUiState,
    viewModel: TabatiViewModel,
    onReplaceImage: (Long) -> Unit,
    onClearAllRequest: () -> Unit,
    onRequestSwap: (Long) -> Unit,
) {
    when (state.section) {
        StudioSection.PAPER -> PaperPanel(state = state, viewModel = viewModel)
        StudioSection.CARDS -> CardsPanel(state = state, viewModel = viewModel, onReplaceImage = onReplaceImage, onClearAllRequest = onClearAllRequest, onRequestSwap = onRequestSwap)
        StudioSection.ENHANCE -> EnhancePanel(state = state, viewModel = viewModel)
        StudioSection.BORDERS -> BordersPanel(state = state, viewModel = viewModel)
        StudioSection.GUIDE -> GuidePanel(state = state, viewModel = viewModel)
        StudioSection.SHEET -> Unit
    }
}

/** Compact bottom drawer hosting the same panels. */
@Composable
private fun SurfaceDrawer(
    state: com.appprint.studio.ui.TabatiUiState,
    viewModel: TabatiViewModel,
    onReplaceImage: (Long) -> Unit,
    onClearAllRequest: () -> Unit,
    onRequestSwap: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(440.dp)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(scheme.surfaceContainerLow),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 10.dp, bottom = 4.dp)
                .size(width = 40.dp, height = 4.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(scheme.outlineVariant),
        )
        Box(Modifier.fillMaxWidth().fillMaxHeight()) {
            SectionPanel(
                state = state,
                viewModel = viewModel,
                onReplaceImage = onReplaceImage,
                onClearAllRequest = onClearAllRequest,
                onRequestSwap = onRequestSwap,
            )
        }
    }
}

/** Floating page tabs + quick actions — glass pill row above the canvas, exactly the design. */
@Composable
private fun SheetPageBar(
    state: com.appprint.studio.ui.TabatiUiState,
    onSelectPage: (Int) -> Unit,
    onAddSheet: () -> Unit,
    onRequestDelete: (Int) -> Unit,
    onArrange: () -> Unit,
    onResetAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 2.dp, end = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        // glass pill wrapping everything, like the design's floating container
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(scheme.surfaceContainerLow.copy(alpha = 0.92f))
                .border(1.dp, scheme.outlineVariant, RoundedCornerShape(percent = 50))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // page tabs
            LazyRow(
                modifier = Modifier.widthIn(max = 180.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items((0 until state.pageCount).toList(), key = { it }) { page ->
                    val selected = state.activePage == page
                    val cardsOnPage = state.cards.count { it.page == page }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(if (selected) scheme.primary else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable { onSelectPage(page) }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "ورقة ${page + 1} (${cardsOnPage})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (selected) scheme.onPrimary else scheme.onSurface,
                            maxLines = 1,
                        )
                        if (state.pageCount > 1) {
                            IconButton(
                                onClick = { onRequestDelete(page) },
                                modifier = Modifier.size(16.dp),
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = null, tint = if (selected) scheme.onPrimary else scheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }

            // add sheet — secondary-container circle with outline, like the design
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(scheme.secondaryContainer.copy(alpha = 0.92f))
                    .border(1.dp, scheme.outlineVariant, RoundedCornerShape(percent = 50))
                    .clickable(onClick = onAddSheet)
                    .semantics { role = Role.Button },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, contentDescription = Str.add_sheet, tint = scheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
            }

            // ترتيب ذكي — smart arrange (primary-container accent)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(scheme.primaryContainer)
                    .clickable(onClick = onArrange)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .semantics { role = Role.Button },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = scheme.onPrimaryContainer, modifier = Modifier.size(14.dp))
                Text("ترتيب ذكي", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = scheme.onPrimaryContainer)
            }

            // البدء من جديد — tertiary-container accent, resets workspace
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(scheme.tertiaryContainer)
                    .clickable(onClick = onResetAll)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .semantics { role = Role.Button },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Filled.RotateRight, contentDescription = null, tint = scheme.onTertiaryContainer, modifier = Modifier.size(14.dp))
                Text("البدء من جديد", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = scheme.onTertiaryContainer)
            }
        }
    }
}

/** Swap picker sheet — every OTHER card, name + page, tap to exchange placements. */
@Composable
private fun SwapPickerSheet(
    fromId: Long,
    state: com.appprint.studio.ui.TabatiUiState,
    onSwap: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(
                Str.swap_with,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
            )
            androidx.compose.foundation.layout.Spacer(Modifier.size(10.dp))
            state.cards.filter { it.id != fromId }.forEach { card ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = { onSwap(card.id) })
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${card.name} — ورقة ${card.page + 1} • ${card.widthMm}×${card.heightMm}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Filled.SwapHoriz, contentDescription = null)
                }
            }
        }
    }
}

/** Design-style modal dialog: dimmed scrim + rounded high-surface panel + close X + title. */
@Composable
private fun HtmlStyleDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val scrim = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f))
            .clickable(interactionSource = scrim, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 44.dp)
                .widthIn(max = 340.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(scheme.surfaceContainerHigh)
                .border(1.dp, scheme.outlineVariant, RoundedCornerShape(28.dp))
                .padding(16.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { /* consume taps — scrim stays closed */ },
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "إغلاق")
                }
            }
            content()
        }
    }
}

/** Full-width rounded-full dialog button in the design's colors. */
@Composable
private fun HtmlDialogButton(
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(percent = 50))
            .background(container)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Black,
            color = content,
            maxLines = 1,
        )
    }
}


