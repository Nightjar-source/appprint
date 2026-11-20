@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.appprint.studio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appprint.studio.ui.StudioSection
import com.appprint.studio.ui.TabatiViewModel
import com.appprint.studio.ui.components.Chrome
import com.appprint.studio.ui.components.pressScale
import com.appprint.studio.ui.platform.PlatformBackHandler
import com.appprint.studio.ui.strings.Str
import com.appprint.studio.ui.theme.Motion

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
    val showRail = Chrome.railVisible()
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
        // True edge-to-edge: the paper canvas fills the entire screen (no reserved band).
        // The floating bars are 100% transparent glass with no color blocks, so the sheet
        // legitimately extends to every screen edge underneath them — exactly like the HTML.
        Row(modifier = Modifier.fillMaxSize()) {
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
                    if (state.cards.isNotEmpty()) {
                        // floating page bar — sits BELOW the floating top bar + buttons
                        // (status-bar inset + top-bar height + gap), like the design.
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = Chrome.topReserve() + 12.dp),
                        ) {
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
                }
            }

            // ----------------------------------------- side panel (medium+)
            AnimatedVisibility(
                visible = showRail && state.section != StudioSection.SHEET,
                enter = fadeIn(Motion.effects()) + expandHorizontally(expandFrom = Alignment.End, animationSpec = spring(dampingRatio = 0.72f, stiffness = 460f)),
                exit = fadeOut(Motion.fast()) + shrinkHorizontally(shrinkTowards = Alignment.End, animationSpec = spring(dampingRatio = 0.78f, stiffness = 460f)),
                modifier = Modifier.fillMaxHeight(),
            ) {
                Box(
                    modifier = Modifier
                        .width((if (isExpanded) 410 else 370).dp)
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
            visible = !showRail && state.section != StudioSection.SHEET,
            enter = fadeIn(Motion.fast()) + expandVertically(expandFrom = Alignment.Bottom, animationSpec = spring(dampingRatio = 0.84f, stiffness = 240f)),
            exit = fadeOut(Motion.fast()) + shrinkVertically(shrinkTowards = Alignment.Bottom, animationSpec = spring(dampingRatio = 0.78f, stiffness = 460f)),
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
            com.appprint.studio.ui.components.HtmlDialog(maxWidth = 384.dp, onDismiss = { deletePageIdx = null }) {
                com.appprint.studio.ui.components.HtmlDialogHeader(Str.delete_page_title.format(idx + 1), { deletePageIdx = null })
                Text(
                    text = Str.delete_page_body.format(count),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp,
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
            com.appprint.studio.ui.components.HtmlDialog(maxWidth = 320.dp, onDismiss = { clearAllOpen = false }) {
                com.appprint.studio.ui.components.HtmlDialogHeader(Str.clear_all_title, { clearAllOpen = false })
                Text(
                    text = Str.clear_all_body,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HtmlDialogButton(
                        container = MaterialTheme.colorScheme.surfaceContainerHighest,
                        content = MaterialTheme.colorScheme.onSurface,
                        label = Str.clear_all_cancel,
                        onClick = { clearAllOpen = false },
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.ExtraBold,
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
    // Translucent glass drawer (no solid "dry block") — uses the same bright-top tint as the
    // floating pills so the sheet shows through softly instead of being covered by a slab.
    val drawerBrush = androidx.compose.ui.graphics.Brush.verticalGradient(
        colors = listOf(
            scheme.surfaceContainerLow.copy(alpha = 0.78f),
            scheme.surfaceContainerLow.copy(alpha = 0.88f),
        ),
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.48f)
            .background(drawerBrush),
    ) {
        // hairline top border only — the design's drawer has borderTop: 1px outline-variant
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(scheme.outlineVariant),
        )
        Box(Modifier.fillMaxWidth().fillMaxHeight().padding(bottom = Chrome.bottomReserve())) {
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

/** Floating page tabs + quick actions — individual translucent pills, exactly the design (no outer container). */
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
    LazyRow(
        modifier = modifier
            .padding(start = 12.dp, end = 12.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // M3 Expressive page tabs — real FilterChips, selected state from the color scheme.
        items((0 until state.pageCount).toList(), key = { it }) { page ->
            val selected = state.activePage == page
            val cardsOnPage = state.cards.count { it.page == page }
            FilterChip(
                selected = selected,
                onClick = { onSelectPage(page) },
                label = {
                    Text(
                        text = "ورقة ${page + 1} (${cardsOnPage})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        fontSize = 11.sp,
                        maxLines = 1,
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Description,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                    )
                },
                trailingIcon = if (state.pageCount > 1) {
                    {
                        // Plain 18dp hit area, like the HTML's pl-2/pr-1 close button.
                        // (IconButton would expand to the 48dp minimum touch target and
                        // stretch the 36dp chip.)
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .clickable { onRequestDelete(page) }
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "حذف الورقة"
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(13.dp))
                        }
                    }
                } else null,
                shape = RoundedCornerShape(percent = 50),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = scheme.surfaceContainerHigh,
                    labelColor = scheme.onSurface,
                    selectedContainerColor = scheme.primary,
                    selectedLabelColor = scheme.onPrimary,
                ),
                modifier = Modifier.height(36.dp),
            )
        }

        // M3 filled-tonal icon button — add sheet.
        item(key = "add-sheet") {
            FilledTonalIconButton(
                onClick = onAddSheet,
                shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = scheme.secondaryContainer,
                    contentColor = scheme.onSecondaryContainer,
                ),
                modifier = Modifier.size(36.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = Str.add_sheet, modifier = Modifier.size(18.dp))
            }
        }

        // M3 filled-tonal button — smart arrange.
        item(key = "arrange") {
            FilledTonalButton(
                onClick = onArrange,
                shape = RoundedCornerShape(percent = 50),
                contentPadding = PaddingValues(horizontal = 14.dp),
                modifier = Modifier.height(36.dp),
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(5.dp))
                Text("ترتيب ذكي", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            }
        }

        // M3 filled-tonal button (tertiary) — reset the workspace.
        item(key = "reset-all") {
            FilledTonalButton(
                onClick = onResetAll,
                shape = RoundedCornerShape(percent = 50),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = scheme.tertiaryContainer,
                    contentColor = scheme.onTertiaryContainer,
                ),
                contentPadding = PaddingValues(horizontal = 14.dp),
                modifier = Modifier.height(36.dp),
            ) {
                Icon(Icons.Filled.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(5.dp))
                Text("البدء من جديد", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            }
        }
    }
}

/** Swap picker dialog — every OTHER card, name + page + size, tap to exchange placements. */
@Composable
private fun SwapPickerSheet(
    fromId: Long,
    state: com.appprint.studio.ui.TabatiUiState,
    onSwap: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    com.appprint.studio.ui.components.HtmlDialog(maxWidth = 384.dp, onDismiss = onDismiss) {
        com.appprint.studio.ui.components.HtmlDialogHeader(Str.swap_with, onDismiss)
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .heightIn(max = 240.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            state.cards.filter { it.id != fromId }.forEach { card ->
                val swapInteraction = remember(card.id) { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressScale(swapInteraction)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable(interactionSource = swapInteraction, indication = null) { onSwap(card.id) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = card.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                    Text(
                        text = "ورقة ${card.page + 1} • ${formatDim(card.widthMm)}×${formatDim(card.heightMm)}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
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
    fontWeight: FontWeight = FontWeight.Black,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(interaction)
            .fillMaxWidth()
            .clip(RoundedCornerShape(percent = 50))
            .background(container)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = fontWeight,
            color = content,
            maxLines = 1,
        )
    }
}

private fun formatDim(v: Float): String =
    "%.1f".format(v).trimEnd('0').trimEnd('.')


