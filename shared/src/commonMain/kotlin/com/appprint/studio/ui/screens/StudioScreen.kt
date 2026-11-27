@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.appprint.studio.ui.screens

import com.appprint.studio.core.text.fmt

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.automirrored.filled.RotateRight
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
import androidx.compose.runtime.key
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
import com.appprint.studio.ui.components.overlayVerticalScrollbar
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
    // Dialog/sheet state is owned by AppRoot so a single Back handler can dismiss in order.
    deletePageIdx: Int?,
    onDeletePageIdxChange: (Int?) -> Unit,
    swapFromId: Long?,
    onSwapFromIdChange: (Long?) -> Unit,
    clearAllOpen: Boolean,
    onClearAllOpenChange: (Boolean) -> Unit,
    // AdMob banner slot — rendered above the canvas but below every dialog/sheet.
    adBanner: @Composable (Modifier) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val showRail = Chrome.railVisible()
    val isExpanded = Chrome.isExpanded()

    Box(modifier = Modifier.fillMaxSize()) {
        // True edge-to-edge: the paper canvas fills the entire screen (no reserved band).
        // The floating bars are 100% transparent glass with no color blocks, so the sheet
        // legitimately extends to every screen edge underneath them — exactly like the HTML.
        // True edge-to-edge: the adaptive canvas fills the whole screen. On medium+ and
        // foldables the editing panel FLOATS above it instead of shrinking the workspace.
        SheetCanvas(
            state = state,
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize(),
            onPickGallery = onPickGallery,
            onCaptureCamera = onCaptureCamera,
            onAddDemoCards = viewModel::addSamples,
        )
        val floatingPanelWidth = if (isExpanded) 410.dp else 370.dp
        if (state.cards.isNotEmpty()) {
            // Floating page strip — clears the rail on the start side and the floating
            // panel on the end side, while remaining fully transparent itself.
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(
                        top = Chrome.topReserve() + 12.dp,
                        end = if (showRail && state.section != StudioSection.SHEET) floatingPanelWidth else 0.dp,
                    ),
            ) {
                SheetPageBar(
                    state = state,
                    onSelectPage = viewModel::currentPage,
                    onAddSheet = viewModel::addSheet,
                    onRequestDelete = { page -> onDeletePageIdxChange(page) },
                    onArrange = viewModel::arrangeSmart,
                    onResetAll = { onClearAllOpenChange(true) },
                )
            }
        }

        // ----------------------------------------- floating side panel (medium+ / foldable)
        AnimatedVisibility(
            visible = showRail && state.section != StudioSection.SHEET,
            // Water-like M3 Expressive: low-damping spring slide + fade, no size jump.
            enter = fadeIn(Motion.effects()) + slideInHorizontally(
                animationSpec = spring(dampingRatio = 0.86f, stiffness = 280f),
                initialOffsetX = { it / 5 },
            ),
            exit = fadeOut(Motion.fast()) + slideOutHorizontally(
                animationSpec = spring(dampingRatio = 0.9f, stiffness = 340f),
                targetOffsetX = { it / 5 },
            ),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(),
        ) {
            Box(
                modifier = Modifier
                    .width(floatingPanelWidth)
                    .fillMaxHeight(),
            ) {
                SectionPanel(
                    state = state,
                    viewModel = viewModel,
                    onReplaceImage = onReplaceImage,
                    onClearAllRequest = { onClearAllOpenChange(true) },
                    onRequestSwap = { id -> onSwapFromIdChange(id) },
                )
            }
        }

        // --------------------------------------- bottom drawer (compact)
        AnimatedVisibility(
            visible = !showRail && state.section != StudioSection.SHEET,
            enter = fadeIn(Motion.effects()) + slideInVertically(
                animationSpec = spring(dampingRatio = 0.86f, stiffness = 280f),
                initialOffsetY = { it / 4 },
            ),
            exit = fadeOut(Motion.fast()) + slideOutVertically(
                animationSpec = spring(dampingRatio = 0.9f, stiffness = 340f),
                targetOffsetY = { it / 4 },
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            SurfaceDrawer(
                state = state,
                viewModel = viewModel,
                onReplaceImage = onReplaceImage,
                onClearAllRequest = { onClearAllOpenChange(true) },
                onRequestSwap = { id -> onSwapFromIdChange(id) },
            )
        }

        // --------------------------------------- AdMob banner (canvas view only)
        // Below the nav pill on compact phones, at the bottom on large screens.
        // Every dialog/sheet below is composed AFTER this, so their scrims always cover
        // the banner — it can never sit on top of a window.
        if (state.section == StudioSection.SHEET) {
            adBanner(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (showRail) 16.dp else Chrome.bottomReserve()),
            )
        }

        // --------------------------------------- long-press quick sheet
        val longPressedId = state.longPressSheetCardId
        if (longPressedId != null) {
            key(longPressedId) {
                com.appprint.studio.ui.components.QuickActionsSheet(
                    state = state,
                    viewModel = viewModel,
                    onReplaceImage = onReplaceImage,
                    onRequestSwap = { onSwapFromIdChange(longPressedId) },
                    onDismiss = viewModel::dismissLongPressSheet,
                )
            }
        }

        // ------------------------------------------ swap picker sheet
        val swapId = swapFromId
        if (swapId != null) {
            SwapPickerSheet(
                fromId = swapId,
                state = state,
                onSwap = { targetId ->
                    viewModel.swapCardsPositions(swapId, targetId)
                    onSwapFromIdChange(null)
                },
                onDismiss = { onSwapFromIdChange(null) },
            )
        }

        // ------------------------------------------ delete page dialog
        deletePageIdx?.let { idx ->
            val count = state.cards.count { it.page == idx }
            com.appprint.studio.ui.components.HtmlDialog(maxWidth = 384.dp, onDismiss = { onDeletePageIdxChange(null) }) {
                com.appprint.studio.ui.components.HtmlDialogHeader(Str.delete_page_title.fmt(idx + 1), { onDeletePageIdxChange(null) })
                Text(
                    text = Str.delete_page_body.fmt(count),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp,
                )
                HtmlDialogButton(
                    container = MaterialTheme.colorScheme.primary,
                    content = MaterialTheme.colorScheme.onPrimary,
                    label = Str.delete_page_keep,
                    onClick = { viewModel.deletePageKeepCards(idx); onDeletePageIdxChange(null) },
                )
                HtmlDialogButton(
                    container = MaterialTheme.colorScheme.tertiary,
                    content = MaterialTheme.colorScheme.onTertiary,
                    label = Str.delete_page_cards,
                    onClick = { viewModel.deletePageWithCards(idx); onDeletePageIdxChange(null) },
                )
            }
        }

        // ------------------------------------------ clear all dialog
        if (clearAllOpen) {
            com.appprint.studio.ui.components.HtmlDialog(maxWidth = 320.dp, onDismiss = { onClearAllOpenChange(false) }) {
                com.appprint.studio.ui.components.HtmlDialogHeader(Str.clear_all_title, { onClearAllOpenChange(false) })
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
                        onClick = { onClearAllOpenChange(false) },
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.ExtraBold,
                    )
                    HtmlDialogButton(
                        container = MaterialTheme.colorScheme.tertiary,
                        content = MaterialTheme.colorScheme.onTertiary,
                        label = Str.clear_all_confirm,
                        onClick = { viewModel.clearAll(); onClearAllOpenChange(false) },
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
    // On a phone the panel is a FLOATING card too: rounded on every side, with a visible
    // gap above and below it so the canvas shows through — never a slab with a hard line.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.56f)
            .padding(
                start = Chrome.EdgeInset,
                end = Chrome.EdgeInset,
                bottom = Chrome.bottomReserve(),
            ),
    ) {
        SectionPanel(
            state = state,
            viewModel = viewModel,
            onReplaceImage = onReplaceImage,
            onClearAllRequest = onClearAllRequest,
            onRequestSwap = onRequestSwap,
        )
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
            .fillMaxWidth(),
        // Start inset clears the floating side rail on large screens, but it is pure
        // transparent scroll space — no color block. Content still scrolls edge-to-edge.
        contentPadding = PaddingValues(
            start = if (Chrome.railVisible()) Chrome.railWidth() + Chrome.EdgeInset * 2 + 12.dp else 12.dp,
            end = 12.dp,
        ),
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
                    iconColor = scheme.onSurface,
                    selectedContainerColor = scheme.primary,
                    selectedLabelColor = scheme.onPrimary,
                    selectedLeadingIconColor = scheme.onPrimary,
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
                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
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
        val swapScroll = rememberScrollState()
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .heightIn(max = 240.dp)
                .verticalScroll(swapScroll)
                .overlayVerticalScrollbar(swapScroll),
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
    "%.1f".fmt(v).trimEnd('0').trimEnd('.')


