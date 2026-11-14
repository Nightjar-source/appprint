package com.appprint.studio.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.appprint.studio.ui.components.AppConfirmDialog
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
    onAddImages: () -> Unit,
    onCamera: () -> Unit,
    onReplaceImage: (Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isCompact = Chrome.isCompact()
    val isExpanded = Chrome.isExpanded()

    var deletePageIdx by remember { mutableStateOf<Int?>(null) }
    var swapFromId by remember { mutableStateOf<Long?>(null) }

    // Back: close overlays → editing section → deselect. Never exits directly.
    val overlayOpen = state.longPressSheetCardId != null || swapFromId != null || deletePageIdx != null
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
                if (state.cards.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        EmptyState(
                            state = state,
                            onAddImages = onAddImages,
                            onSamples = viewModel::addSamples,
                            onAddSampleOfType = viewModel::addSampleOfType,
                        )
                    }
                } else {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        SheetCanvas(state = state, viewModel = viewModel, modifier = Modifier.fillMaxSize())
                    }
                    SheetPageBar(
                        state = state,
                        onSelectPage = viewModel::currentPage,
                        onAddSheet = viewModel::addSheet,
                        onRequestDelete = { deletePageIdx = state.activePage },
                    )
                }
            }

            // ----------------------------------------- side panel (medium+)
            if (!isCompact && state.section != StudioSection.SHEET) {
                Box(
                    modifier = Modifier
                        .width((if (isExpanded) 400 else 360).dp)
                        .fillMaxHeight(),
                ) {
                    SectionPanel(state = state, viewModel = viewModel, onReplaceImage = onReplaceImage)
                }
            }
        }

        // --------------------------------------- bottom drawer (compact)
        if (isCompact && state.section != StudioSection.SHEET) {
            SurfaceDrawer(
                state = state,
                viewModel = viewModel,
                onReplaceImage = onReplaceImage,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        // --------------------------------------- long-press quick sheet
        val longPressedId = state.longPressSheetCardId
        if (longPressedId != null) {
            QuickActionsSheet(
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
            AppConfirmDialog(
                icon = Icons.Filled.DeleteOutline,
                title = Str.delete_page_title,
                body = "الورقة رقم ${idx + 1} تحوي $count بطاقة. احذفها مع بطاقاتها، أو انقل البطاقات إلى الأوراق الباقية.",
                primaryLabel = Str.delete_page_cards,
                onPrimary = { viewModel.deletePageWithCards(idx); deletePageIdx = null },
                secondaryLabel = Str.delete_page_keep,
                onSecondary = { viewModel.deletePageKeepCards(idx); deletePageIdx = null },
                onCancel = { deletePageIdx = null },
                onDismiss = { deletePageIdx = null },
            )
        }

        // ---------------------------- bulk enhance confirmation dialog
        if (state.pendingBulkPatch != null) {
            AppConfirmDialog(
                icon = Icons.Filled.AutoFixHigh,
                title = "تطبيق التحسين على كل البطاقات؟",
                body = "سيُطبَّق هذا التحسين على كل البطاقات التي تحتوي صورة — لا يمكن التراجع.",
                primaryLabel = "تطبيق على الكل",
                onPrimary = viewModel::confirmPendingBulkEnhance,
                onCancel = viewModel::dismissPendingBulk,
                onDismiss = viewModel::dismissPendingBulk,
            )
        }
    }
}

/** Section dispatcher shared by the side panel and the compact bottom drawer. */
@Composable
private fun SectionPanel(
    state: com.appprint.studio.ui.TabatiUiState,
    viewModel: TabatiViewModel,
    onReplaceImage: (Long) -> Unit,
) {
    when (state.section) {
        StudioSection.PAPER -> PaperPanel(state = state, viewModel = viewModel)
        StudioSection.CARDS -> CardsPanel(state = state, viewModel = viewModel, onReplaceImage = onReplaceImage)
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
            SectionPanel(state = state, viewModel = viewModel, onReplaceImage = onReplaceImage)
        }
    }
}

/** Floating page strip — flat labels + hairline underline on the active sheet, + button at the side. */
@Composable
private fun SheetPageBar(
    state: com.appprint.studio.ui.TabatiUiState,
    onSelectPage: (Int) -> Unit,
    onAddSheet: () -> Unit,
    onRequestDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 14.dp, top = 2.dp, end = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items((0 until state.pageCount).toList(), key = { it }) { page ->
                val selected = state.activePage == page
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .clickable { onSelectPage(page) }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "ورقة ${page + 1} · ${state.cards.count { it.page == page }}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.ExtraBold
                            else androidx.compose.ui.text.font.FontWeight.SemiBold,
                            color = if (selected) scheme.onSurface else scheme.onSurfaceVariant.copy(alpha = 0.72f),
                            maxLines = 1,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(width = 26.dp, height = 2.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(if (selected) scheme.primary else androidx.compose.ui.graphics.Color.Transparent),
                    )
                }
            }
        }
        if (state.pageCount > 1) {
            androidx.compose.material3.IconButton(onClick = onRequestDelete, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = Str.delete_page_title,
                    tint = scheme.error,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .size(52.dp)
                .shadow(8.dp, RoundedCornerShape(percent = 50), clip = false)
                .clip(RoundedCornerShape(percent = 50))
                .background(scheme.primary)
                .clickable(onClick = onAddSheet)
                .semantics { role = Role.Button },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = Str.add_sheet,
                tint = scheme.onPrimary,
                modifier = Modifier.size(24.dp),
            )
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
                        .clickable { onSwap(card.id) }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${card.name} — ورقة ${card.page + 1}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Filled.SwapHoriz, contentDescription = null)
                }
            }
        }
    }
}

/** Long-press quick context sheet — the design's card quick actions in a row. */
@Composable
private fun QuickActionsSheet(
    state: com.appprint.studio.ui.TabatiUiState,
    viewModel: TabatiViewModel,
    onReplaceImage: (Long) -> Unit,
    onRequestSwap: () -> Unit,
    onDismiss: () -> Unit,
) {
    val card = state.selectedCard ?: return
    val scheme = MaterialTheme.colorScheme
    val actions: List<Pair<ImageVector, String>> = listOf(
        Icons.Filled.AutoFixHigh to Str.enhance_auto,
        Icons.Filled.Crop to Str.auto_crop,
        Icons.Filled.Rotate90DegreesCw to "تدوير 90°",
        Icons.Filled.Image to Str.replace_image,
        Icons.Filled.SwapHoriz to Str.swap,
        Icons.Filled.ContentCopy to Str.duplicate,
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = scheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(
                card.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
            )
            androidx.compose.foundation.layout.Spacer(Modifier.size(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                actions.forEachIndexed { index, (icon, label) ->
                    val interaction = remember { MutableInteractionSource() }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(scheme.surfaceContainerHigh)
                            .clickable(interactionSource = interaction, indication = null) {
                                when (index) {
                                    0 -> viewModel.autoEnhanceSelected()
                                    1 -> viewModel.autoCropSelected()
                                    2 -> viewModel.setCardAngle360(card.id, (card.rotation + 90f) % 360f, magneticSnap = false)
                                    3 -> onReplaceImage(card.id)
                                    4 -> onRequestSwap()
                                    5 -> viewModel.duplicateCard(card.id)
                                }
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(icon, contentDescription = label, tint = scheme.onSurface, modifier = Modifier.size(22.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 2,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
            TextButton(onClick = { viewModel.deleteCard(card.id); onDismiss() }) {
                Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = scheme.error)
                androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
                Text(Str.remove, color = scheme.error, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        }
    }
}
