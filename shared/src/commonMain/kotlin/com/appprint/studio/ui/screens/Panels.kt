@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.appprint.studio.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.DriveEta
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appprint.studio.core.arrange.SheetArranger
import com.appprint.studio.core.model.ArrangeMode
import com.appprint.studio.core.model.CardGuides
import com.appprint.studio.core.model.CardItem
import com.appprint.studio.core.model.CardType
import com.appprint.studio.core.model.EnhanceMode
import com.appprint.studio.core.model.FrameStyle
import com.appprint.studio.core.model.Orientation
import com.appprint.studio.core.model.PaperPreset
import com.appprint.studio.core.model.SheetBackground
import com.appprint.studio.ui.TabatiViewModel
import com.appprint.studio.ui.TabatiUiState
import com.appprint.studio.ui.components.PanelChoiceChip
import com.appprint.studio.ui.components.PanelSectionHeader
import com.appprint.studio.ui.components.PanelSliderRow
import com.appprint.studio.ui.components.PanelToggleRow
import com.appprint.studio.ui.components.pressScale
import com.appprint.studio.ui.strings.Str

/**
 * Editing panels — the design's five tabs, ported literally from the HTML.
 */

@Composable
private fun PanelScaffold(
    title: String,
    state: TabatiUiState,
    viewModel: TabatiViewModel,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .background(scheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
            )
            if (state.cards.isNotEmpty()) {
                TextButton(
                    onClick = viewModel::arrangeSmart,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        containerColor = scheme.secondaryContainer,
                        contentColor = scheme.onSecondaryContainer,
                    ),
                    shape = RoundedCornerShape(percent = 50),
                    modifier = Modifier.height(32.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer6()
                    Text(Str.arrange_smart, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
            trailing?.invoke()
            Spacer6()
            TextButton(
                onClick = { viewModel.setSection(com.appprint.studio.ui.StudioSection.SHEET) },
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary,
                ),
                shape = RoundedCornerShape(percent = 50),
                modifier = Modifier.height(32.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            ) {
                Text(Str.done, fontWeight = FontWeight.Bold)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(scheme.outlineVariant.copy(alpha = 0.6f)),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun Spacer6() {
    androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
}

/**
 * Material adaptive grid: column count derives from the ACTUAL available width so cards
 * reflow on phones, tablets and folded screens instead of being pinned to 2/3 columns.
 */
@Composable
private fun <T> ResponsiveGrid(
    items: List<T>,
    minCellWidth: androidx.compose.ui.unit.Dp = 150.dp,
    horizontalGap: androidx.compose.ui.unit.Dp = 8.dp,
    verticalGap: androidx.compose.ui.unit.Dp = 8.dp,
    itemContent: @Composable (T) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val available = maxWidth
        val columns = (available / (minCellWidth + horizontalGap)).toInt().coerceIn(1, 4)
        val cellWidth = (available - horizontalGap * (columns - 1)) / columns
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(horizontalGap),
            verticalArrangement = Arrangement.spacedBy(verticalGap),
        ) {
            items.forEach { item ->
                Box(Modifier.width(cellWidth)) { itemContent(item) }
            }
        }
    }
}

@Composable
private fun Spacer8() {
    androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
}

/** Container filled with surfaceContainer + rounded-2xl — the design's inner section box. */
@Composable
private fun SectionBox(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surfaceContainer)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

// ---------------------------------------------------------------- paper

@Composable
fun PaperPanel(state: TabatiUiState, viewModel: TabatiViewModel) {
    val settings = state.settings
    val clusters = state.cards
        .groupBy { SheetArranger.sizeKey(it) }
        .entries
        .sortedByDescending { it.value.size }

    PanelScaffold(title = Str.section_paper, state = state, viewModel = viewModel) {
        PanelSectionHeader(Str.paper_size)
        ResponsiveGrid(items = PaperPreset.PRESETS, minCellWidth = 104.dp) { preset ->
            PaperSizeChip(
                preset = preset,
                sub = if (preset == PaperPreset.CUSTOM) Str.custom_sub else "${preset.widthMm.toInt()}×${preset.heightMm.toInt()}",
                selected = settings.presetKey == preset.key,
                onClick = { viewModel.setPaperPreset(preset.key) },
            )
        }

        if (settings.presetKey == PaperPreset.CUSTOM.key) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = settings.customWidthMm.toInt().toString(),
                    onValueChange = { v -> v.toIntOrNull()?.let { viewModel.setCustomPaper(it.toFloat().coerceIn(50f, 500f), settings.customHeightMm) } },
                    label = { Text(Str.custom_width) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = settings.customHeightMm.toInt().toString(),
                    onValueChange = { v -> v.toIntOrNull()?.let { viewModel.setCustomPaper(settings.customWidthMm, it.toFloat().coerceIn(50f, 500f)) } },
                    label = { Text(Str.custom_height) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        PanelSectionHeader("${Str.orientation} (${settings.sheetWidthMm.toInt()} × ${settings.sheetHeightMm.toInt()} مم)")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PanelChoiceChip(
                Str.portrait,
                selected = settings.orientation == Orientation.PORTRAIT,
                onClick = { viewModel.setOrientation(Orientation.PORTRAIT) },
                modifier = Modifier.weight(1f),
                accent = true,
            )
            PanelChoiceChip(
                Str.landscape,
                selected = settings.orientation == Orientation.LANDSCAPE,
                onClick = { viewModel.setOrientation(Orientation.LANDSCAPE) },
                modifier = Modifier.weight(1f),
                accent = true,
            )
        }

        SectionBox {
            PanelSliderRow(
                label = Str.margins,
                value = settings.marginMm,
                valueRange = 0f..25f,
                onValueChange = viewModel::setMargin,
                valueText = "${settings.marginMm.toInt()} مم",
                steps = 24,
            )
            PanelSliderRow(
                label = Str.gap,
                value = settings.gapMm,
                valueRange = 2f..10f,
                onValueChange = viewModel::setGap,
                valueText = "${settings.gapMm.toInt()} مم",
                steps = 7,
            )
        }

        SectionBox {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    Str.smart_tools,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        if (state.pageCount > 1) Str.pages_many.format(state.pageCount) else Str.pages_one,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = viewModel::arrangeSmart,
                    shape = RoundedCornerShape(percent = 50),
                    modifier = Modifier.weight(1f).height(44.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer6()
                    Text(Str.arrange_auto, fontWeight = FontWeight.Black, maxLines = 1)
                }
                Button(
                    onClick = viewModel::sortBySize,
                    shape = RoundedCornerShape(percent = 50),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                    modifier = Modifier.weight(1f).height(44.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Icon(Icons.Filled.Sort, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer6()
                    Text(Str.arrange_by_size, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                }
            }
            OutlinedButton(
                onClick = viewModel::regroupSmart,
                shape = RoundedCornerShape(percent = 50),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().height(40.dp),
            ) {
                Icon(Icons.Filled.Dashboard, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer6()
                Text(Str.regroup_smart, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            if (settings.groupSmart && clusters.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    clusters.forEach { (key, items) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(SheetArranger.clusterColor(key))),
                            )
                            Spacer8()
                            Text(
                                Str.cluster_line.format("$key مم", items.size),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        PanelSectionHeader(Str.layout_mode)
        val modeItems = listOf(
            Triple(Str.mode_compact, Str.mode_compact_sub, ArrangeMode.COMPACT),
            Triple(Str.mode_comfortable, Str.mode_comfortable_sub, ArrangeMode.COMFORTABLE),
            Triple(Str.mode_grid, Str.mode_grid_sub, ArrangeMode.GRID),
        )
        ResponsiveGrid(items = modeItems, minCellWidth = 110.dp) { (label, sub, mode) ->
            ModeChip(label, sub, selected = settings.mode == mode, onClick = { viewModel.setMode(mode) }, modifier = Modifier.fillMaxWidth())
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PanelToggleRow(Str.auto_arrange, checked = settings.autoArrangeOnAdd, onCheckedChange = viewModel::setAutoArrange)
            PanelToggleRow(Str.multi_page, checked = settings.allowMultiPage, onCheckedChange = viewModel::setAllowMultiPage)
            PanelToggleRow(Str.group_smart, checked = settings.groupSmart, onCheckedChange = viewModel::setGroupSmart)
            PanelToggleRow(Str.show_grid, checked = settings.showGrid, onCheckedChange = viewModel::setShowGrid)
        }

        // Quick experiment section — only for an empty workspace; a working user doesn't need demos.
        if (state.cards.isEmpty()) {
            val outlineColor = MaterialTheme.colorScheme.outlineVariant
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawRoundRect(
                        color = outlineColor,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
                        ),
                    )
                }
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(Str.experiment_title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                Text(
                    Str.experiment_desc,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = viewModel::addSamples,
                    shape = RoundedCornerShape(percent = 50),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                ) {
                    Text(Str.demo_button, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                }
            }
        }
    }
}
}

@Composable
private fun ModeChip(
    label: String,
    sub: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) scheme.primary else scheme.surfaceContainerHigh)
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = if (selected) scheme.onPrimary else scheme.onSurface,
        )
        Text(
            sub,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = if (selected) scheme.onPrimary.copy(alpha = 0.85f) else scheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PaperSizeChip(preset: PaperPreset, sub: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val icon = when (preset) {
        PaperPreset.A4 -> Icons.Filled.Description
        PaperPreset.A5 -> Icons.Filled.MenuBook
        PaperPreset.A3 -> Icons.Filled.PhotoSizeSelectLarge
        PaperPreset.LETTER -> Icons.Filled.Article
        PaperPreset.LEGAL -> Icons.Filled.Gavel
        PaperPreset.CUSTOM -> Icons.Filled.Tune
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) scheme.primary else scheme.surfaceContainerHigh)
            .border(1.dp, if (selected) scheme.primary else scheme.outlineVariant, RoundedCornerShape(18.dp))
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (selected) scheme.onPrimary.copy(alpha = 0.18f) else scheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) scheme.onPrimary else scheme.primary,
                modifier = Modifier.size(15.dp),
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 3.dp, y = (-3).dp)
                        .size(13.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(scheme.onPrimary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(9.dp))
                }
            }
        }
        androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
        Text(
            preset.label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = if (selected) scheme.onPrimary else scheme.onSurface,
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Text(
            sub,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.5.sp,
            color = if (selected) scheme.onPrimary.copy(alpha = 0.85f) else scheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

// ---------------------------------------------------------------- cards

@Composable
fun CardsPanel(
    state: TabatiUiState,
    viewModel: TabatiViewModel,
    onReplaceImage: (Long) -> Unit,
    onClearAllRequest: () -> Unit = {},
    onRequestSwap: (Long) -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val selected = state.selectedCard
    var pendingType by remember { mutableStateOf(CardType.NATIONAL) }
    val currentType = selected?.type ?: pendingType

    PanelScaffold(title = Str.section_cards, state = state, viewModel = viewModel) {
        PanelSectionHeader(Str.card_type_header)
        ResponsiveGrid(items = CardType.PRINTABLE, minCellWidth = 150.dp) { type ->
            TypeGridChip(
                type = type,
                active = currentType == type,
                onClick = {
                    pendingType = type
                    selected?.let { sel -> viewModel.applyCardType(sel.id, type) }
                },
            )
        }
        if (currentType == CardType.CUSTOM && selected != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = selected.widthMm.toInt().toString(),
                    onValueChange = { v ->
                        v.toIntOrNull()?.let { mm ->
                            viewModel.resizeCard(selected.id, mm.toFloat().coerceIn(20f, 500f), selected.heightMm)
                        }
                    },
                    label = { Text(Str.card_dims_w) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = selected.heightMm.toInt().toString(),
                    onValueChange = { v ->
                        v.toIntOrNull()?.let { mm ->
                            viewModel.resizeCard(selected.id, selected.widthMm, mm.toFloat().coerceIn(20f, 500f))
                        }
                    },
                    label = { Text(Str.card_dims_h) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // ── بطاقاتك على الورقة (n) — كل البطاقات مع تعديل مباشر ───────────
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                Str.your_cards.format(state.cards.size),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
            )
            if (state.cards.isNotEmpty()) {
                TextButton(
                    onClick = onClearAllRequest,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        containerColor = scheme.tertiaryContainer,
                        contentColor = scheme.onTertiaryContainer,
                    ),
                    shape = RoundedCornerShape(percent = 50),
                    modifier = Modifier.height(30.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                ) {
                    Text(Str.clear_all, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        if (state.cards.isEmpty()) {
            Text(
                Str.empty_title,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        } else {
            state.cards.forEach { card ->
                CardEditorRow(
                    card = card,
                    state = state,
                    selected = card.id == state.selectedCardId,
                    onSelect = { viewModel.selectCard(card.id) },
                    viewModel = viewModel,
                    onReplaceImage = onReplaceImage,
                    onRequestSwap = onRequestSwap,
                )
            }
        }
    }
}

/** One editable card block in the list — exactly the design's card editor card. */
@Composable
private fun CardEditorRow(
    card: CardItem,
    state: TabatiUiState,
    selected: Boolean,
    onSelect: () -> Unit,
    viewModel: TabatiViewModel,
    onReplaceImage: (Long) -> Unit,
    onRequestSwap: (Long) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val totalDeg = SheetArranger.totalRotation(card).toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) scheme.surfaceContainerHighest else scheme.surfaceContainer)
            .border(1.5.dp, if (selected) scheme.primary else scheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onSelect)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = card.name,
                onValueChange = { viewModel.renameCard(card.id, it) },
                singleLine = true,
                textStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                modifier = Modifier.weight(1f),
            )
            Spacer8()
            if (state.pageCount > 1) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(scheme.secondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        "ورقة ${card.page + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSecondaryContainer,
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(scheme.secondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        "ورقة 1",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSecondaryContainer,
                    )
                }
            }
        }

        if (state.pageCount > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (0 until state.pageCount).forEach { page ->
                    val isCurrent = card.page == page
                    Text(
                        text = "${page + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isCurrent) scheme.onPrimary else scheme.onSurface,
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(if (isCurrent) scheme.primary else scheme.surfaceContainerHigh)
                            .clickable { viewModel.moveCardToPage(card.id, page) }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }

        if (card.detectedLabel.isNotEmpty()) {
            Text(
                card.detectedLabel,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = formatDim(card.widthMm),
                onValueChange = { v -> v.toFloatOrNull()?.let { viewModel.resizeCard(card.id, it.coerceIn(20f, 500f), card.heightMm) } },
                label = { Text(Str.card_dims_w) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = formatDim(card.heightMm),
                onValueChange = { v -> v.toFloatOrNull()?.let { viewModel.resizeCard(card.id, card.widthMm, it.coerceIn(20f, 500f)) } },
                label = { Text(Str.card_dims_h) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }

        // 360° — slider + quick angle chips
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.surfaceContainerLow)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Autorenew, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer6()
                Text(
                    Str.rotate_free,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(scheme.secondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        "${totalDeg}°",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = scheme.onSecondaryContainer,
                    )
                }
            }
            Slider(
                value = totalDeg.toFloat(),
                onValueChange = { v -> viewModel.setCardAngle360(card.id, v, magneticSnap = false) },
                valueRange = 0f..360f,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                listOf(0f, 45f, 90f, 180f, 270f).forEach { deg ->
                    val isActive = totalDeg == deg.toInt()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(if (isActive) scheme.primary else scheme.surfaceContainerHigh)
                            .clickable { viewModel.setCardAngle360(card.id, deg, magneticSnap = false) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${deg.toInt()}°",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isActive) scheme.onPrimary else scheme.onSurface,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(scheme.surfaceContainerHigh)
                        .clickable { viewModel.setCardAngle360(card.id, (totalDeg + 15f) % 360f, magneticSnap = false) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = Str.plus_15,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }

        // border / guides toggles + replace / swap / copy / delete
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MiniTogglePill(
                label = "${Str.frame_on_card}: ${if (card.border) Str.enabled else Str.disabled}",
                onClick = { viewModel.updateCard(card.id) { it.copy(border = !it.border) } },
            )
            MiniTogglePill(
                label = "${Str.guides_on_card}: ${if (card.guides) Str.enabled else Str.disabled}",
                onClick = { viewModel.updateCard(card.id) { it.copy(guides = !it.guides) } },
            )
            PanelIconPill(Icons.Filled.Image, Str.replace_image) { onReplaceImage(card.id) }
            if (state.cards.size > 1) {
                PanelIconPill(Icons.Filled.SwapHoriz, Str.swap, accent = true) { onRequestSwap(card.id) }
            }
            PanelIconPill(Icons.Filled.ContentCopy, Str.duplicate) { viewModel.duplicateCard(card.id) }
            PanelIconPill(Icons.Filled.Delete, Str.remove, danger = true) { viewModel.deleteCard(card.id) }
        }
    }
}

private fun formatDim(v: Float): String =
    "%.1f".format(v).trimEnd('0').trimEnd('.')

@Composable
private fun MiniTogglePill(label: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = scheme.onSurface,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(scheme.surfaceContainerLow)
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun PanelIconPill(
    icon: ImageVector,
    label: String,
    accent: Boolean = false,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val bg = when {
        danger -> scheme.tertiaryContainer
        accent -> scheme.secondaryContainer
        else -> scheme.surfaceContainerLow
    }
    val fg = when {
        danger -> scheme.onTertiaryContainer
        accent -> scheme.onSecondaryContainer
        else -> scheme.onSurface
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(bg)
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = fg, maxLines = 1)
    }
}

@Composable
private fun TypeGridChip(type: CardType, active: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val iconVector = when (type) {
        CardType.NATIONAL -> Icons.Filled.Person
        CardType.PASSPORT -> Icons.Filled.TravelExplore
        CardType.RESIDENCE -> Icons.Filled.Home
        CardType.RATION -> Icons.Filled.ShoppingCart
        CardType.DRIVING -> Icons.Filled.DriveEta
        CardType.VOTER -> Icons.Filled.HowToVote
        CardType.STUDENT -> Icons.Filled.School
        CardType.CUSTOM -> Icons.Filled.Tune
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (active) scheme.primaryContainer else scheme.surfaceContainerHigh)
            .border(
                width = if (active) 1.5.dp else 1.dp,
                color = if (active) scheme.primary else scheme.outlineVariant,
                shape = RoundedCornerShape(18.dp),
            )
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(type.brandColorHex)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(iconVector, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Text(
                type.labelAr,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (active) scheme.onPrimaryContainer else scheme.onSurface,
                maxLines = 2,
                lineHeight = 15.sp,
                modifier = Modifier.weight(1f),
            )
        }
        androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
        Text(
            text = "${type.widthMm} × ${type.heightMm} مم",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

// ---------------------------------------------------------------- enhance

@Composable
fun EnhancePanel(state: TabatiUiState, viewModel: TabatiViewModel) {
    val scheme = MaterialTheme.colorScheme
    val selected = state.selectedCard
    val imageCount = state.cards.count { it.imageUri != null }

    PanelScaffold(title = Str.section_enhance, state = state, viewModel = viewModel) {
        if (state.cards.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PanelChoiceChip(
                    Str.enhance_scope_one,
                    selected = state.enhanceScope == com.appprint.studio.ui.EnhanceScope.SINGLE,
                    onClick = { viewModel.setEnhanceScope(com.appprint.studio.ui.EnhanceScope.SINGLE) },
                    modifier = Modifier.weight(1f),
                )
                PanelChoiceChip(
                    Str.enhance_scope_all.format(imageCount),
                    selected = state.enhanceScope == com.appprint.studio.ui.EnhanceScope.ALL,
                    onClick = { viewModel.setEnhanceScope(com.appprint.studio.ui.EnhanceScope.ALL) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // pending bulk-confirm banner (تأكيد / إلغاء) — like the design's inline row
        state.pendingBulkPatch?.let {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(scheme.secondaryContainer)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    Str.bulk_question.format(imageCount),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = scheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = Str.confirm,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = scheme.onPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(scheme.primary)
                        .clickable { viewModel.confirmPendingBulkEnhance() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
                Text(
                    text = Str.cancel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(scheme.surfaceContainerLow)
                        .clickable { viewModel.dismissPendingBulk() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        when {
            selected == null -> {
                Text(
                    Str.enhance_not_selected,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp),
                )
                return@PanelScaffold
            }
            selected.imageUri == null -> {
                Text(
                    Str.enhance_no_image,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp),
                )
                return@PanelScaffold
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ActionPill(
                icon = Icons.Filled.AutoAwesome,
                label = Str.enhance_auto,
                container = scheme.primary,
                onContainer = scheme.onPrimary,
                onClick = {
                    if (state.enhanceScope == com.appprint.studio.ui.EnhanceScope.ALL && imageCount > 1) {
                        viewModel.requestPendingBulkEnhance(com.appprint.studio.core.model.EnhanceSettings.AUTO)
                    } else {
                        viewModel.autoEnhanceSelected()
                    }
                },
            )
            ActionPill(
                icon = Icons.Filled.Crop,
                label = Str.auto_crop,
                container = scheme.secondaryContainer,
                onContainer = scheme.onSecondaryContainer,
                enabled = !state.processing,
                onClick = viewModel::autoCropSelected,
            )
            ActionPill(
                label = if (state.compareBefore) Str.compare_after else Str.compare_before,
                container = if (state.compareBefore) scheme.primary else scheme.surfaceContainerHighest,
                onContainer = if (state.compareBefore) scheme.onPrimary else scheme.onSurface,
                onClick = viewModel::toggleCompareBefore,
            )
            if (state.cards.size > 1) {
                ActionPill(
                    icon = Icons.Filled.ContentCopy,
                    label = Str.copy_enhance_all,
                    container = scheme.surfaceContainerHighest,
                    onContainer = scheme.onSurface,
                    onClick = { viewModel.requestPendingBulkEnhance(selected.enhance) },
                )
                ActionPill(
                    label = Str.reset,
                    container = scheme.tertiaryContainer,
                    onContainer = scheme.onTertiaryContainer,
                    onClick = viewModel::resetAllEnhance,
                )
            }
        }

        PanelToggleRow(
            title = Str.shadow_lift,
            checked = selected.enhance.shadowLift,
            onCheckedChange = { v -> viewModel.updateSelectedEnhance { it.copy(shadowLift = v) } },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PanelChoiceChip(Str.mode_color, selected = selected.enhance.mode == EnhanceMode.COLOR, onClick = { viewModel.updateSelectedEnhance { it.copy(mode = EnhanceMode.COLOR) } }, modifier = Modifier.weight(1f))
            PanelChoiceChip(Str.mode_bw, selected = selected.enhance.mode == EnhanceMode.BW, onClick = { viewModel.updateSelectedEnhance { it.copy(mode = EnhanceMode.BW) } }, modifier = Modifier.weight(1f))
            PanelChoiceChip(Str.mode_gray, selected = selected.enhance.mode == EnhanceMode.GRAY, onClick = { viewModel.updateSelectedEnhance { it.copy(mode = EnhanceMode.GRAY) } }, modifier = Modifier.weight(1f))
        }

        SectionBox {
            PanelSliderRow(Str.brightness, selected.enhance.brightness, 50f..180f, { v -> viewModel.updateSelectedEnhance { it.copy(brightness = v) } }, "${selected.enhance.brightness.toInt()}%", steps = 129)
            PanelSliderRow(Str.contrast, selected.enhance.contrast, 50f..180f, { v -> viewModel.updateSelectedEnhance { it.copy(contrast = v) } }, "${selected.enhance.contrast.toInt()}%", steps = 129)
            PanelSliderRow(Str.saturation, selected.enhance.saturation, 50f..180f, { v -> viewModel.updateSelectedEnhance { it.copy(saturation = v) } }, "${selected.enhance.saturation.toInt()}%", steps = 129)
            PanelSliderRow(Str.sharpness, selected.enhance.sharpness, 50f..180f, { v -> viewModel.updateSelectedEnhance { it.copy(sharpness = v) } }, "${selected.enhance.sharpness.toInt()}%", steps = 129)
            PanelSliderRow(Str.tilt, selected.enhance.tilt, -15f..15f, { v -> viewModel.setTiltSelected(v) }, "${selected.enhance.tilt}°", steps = 59)
        }

        Text(
            text = Str.reset_this_card,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(percent = 50))
                .border(1.dp, scheme.outlineVariant, RoundedCornerShape(percent = 50))
                .clickable { viewModel.resetSelectedEnhanceOriginal() }
                .padding(vertical = 10.dp),
        )
    }
}

@Composable
private fun ActionPill(
    label: String,
    container: Color,
    onContainer: Color,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(container.copy(alpha = if (enabled) 1f else 0.4f))
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        icon?.let { Icon(it, contentDescription = null, tint = onContainer, modifier = Modifier.size(15.dp)) }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = onContainer,
            maxLines = 1,
        )
    }
}

// ---------------------------------------------------------------- borders

@Composable
fun BordersPanel(state: TabatiUiState, viewModel: TabatiViewModel) {
    val scheme = MaterialTheme.colorScheme
    val frame = state.settings.frame
    val allOff = frame.allFramesOff

    PanelScaffold(title = Str.section_borders, state = state, viewModel = viewModel) {
        // master switch — full-width primary pill with visibility icon
        Button(
            onClick = viewModel::toggleAllFrames,
            shape = RoundedCornerShape(percent = 50),
            modifier = Modifier.fillMaxWidth().height(46.dp),
        ) {
            Icon(
                if (allOff) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer6()
            Text(
                if (allOff) Str.show_all_frames else Str.hide_all_frames,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FrameToggleRow(Str.frame_sheet, frame.sheet) { v -> viewModel.setFrame { it.copy(sheet = v) } }
            FrameToggleRow(Str.frame_card, frame.card) { v -> viewModel.setFrame { it.copy(card = v) } }
            FrameToggleRow(Str.frame_guides, frame.guides) { v -> viewModel.setFrame { it.copy(guides = v) } }
            FrameToggleRow(Str.frame_ruler, frame.ruler) { v -> viewModel.setFrame { it.copy(ruler = v) } }
        }

        PanelSectionHeader(Str.frame_bg)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BgSwatch(Str.white, selected = frame.background == SheetBackground.WHITE, swatch = Color.White) {
                viewModel.setFrameBackground(SheetBackground.WHITE)
            }
            BgSwatch(Str.offwhite, selected = frame.background == SheetBackground.OFFWHITE, swatch = Color(0xFFFDFAF1)) {
                viewModel.setFrameBackground(SheetBackground.OFFWHITE)
            }
            BgSwatch(Str.transparent, selected = frame.background == SheetBackground.TRANSPARENT, swatch = null) {
                viewModel.setFrameBackground(SheetBackground.TRANSPARENT)
            }
        }

        SectionBox {
            PanelSliderRow(
                label = Str.thickness,
                value = frame.thickness,
                valueRange = 0f..3f,
                onValueChange = viewModel::setFrameThickness,
                valueText = "${"%.1f".format(frame.thickness)} px",
                steps = 5,
            )
        }

        PanelSectionHeader(Str.frame_color)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FrameColorDot(Str.color_black, hex = 0xFF111111L, selected = frame.colorHex == 0xFF111111L) { viewModel.setFrameColor(0xFF111111L) }
            FrameColorDot(Str.color_gray, hex = 0xFF888888L, selected = frame.colorHex == 0xFF888888L) { viewModel.setFrameColor(0xFF888888L) }
            FrameColorDot(Str.color_red, hex = 0xFFC8102EL, selected = frame.colorHex == 0xFFC8102EL) { viewModel.setFrameColor(0xFFC8102EL) }
            FrameColorDot(Str.color_none, hex = null, selected = frame.colorHex == 0L) { viewModel.setFrameColor(0L) }
        }

        PanelSectionHeader(Str.frame_style)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PanelChoiceChip(Str.solid, selected = frame.style == FrameStyle.SOLID, onClick = { viewModel.setFrameStyle(FrameStyle.SOLID) }, modifier = Modifier.weight(1f))
            PanelChoiceChip(Str.dashed, selected = frame.style == FrameStyle.DASHED, onClick = { viewModel.setFrameStyle(FrameStyle.DASHED) }, modifier = Modifier.weight(1f))
            PanelChoiceChip(Str.dotted, selected = frame.style == FrameStyle.DOTTED, onClick = { viewModel.setFrameStyle(FrameStyle.DOTTED) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun FrameToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surfaceContainer)
            .clickable(interactionSource = interaction, indication = null, onClick = { onCheckedChange(!checked) })
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun BgSwatch(label: String, selected: Boolean, swatch: Color?, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .width(104.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) scheme.primary else scheme.surfaceContainerHigh)
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 28.dp, height = 18.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, scheme.outlineVariant, RoundedCornerShape(4.dp)),
        ) {
            if (swatch != null) {
                Box(Modifier.fillMaxSize().background(swatch))
            } else {
                // checkerboard swatch — transparent paper
                Canvas(Modifier.fillMaxSize()) {
                    val cell = 6.dp.toPx()
                    for (r in 0..2) {
                        for (c in 0..4) {
                            if ((r + c) % 2 == 0) {
                                drawRect(Color(0xFFCCCCCC), topLeft = androidx.compose.ui.geometry.Offset(c * cell, r * cell), size = androidx.compose.ui.geometry.Size(cell, cell))
                            }
                        }
                    }
                }
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (selected) scheme.onPrimary else scheme.onSurface,
        )
    }
}

@Composable
private fun FrameColorDot(label: String, hex: Long?, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) scheme.primary else scheme.surfaceContainerHigh)
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .border(1.dp, scheme.outline, CircleShape)
                .then(if (hex != null) Modifier.background(Color(hex)) else Modifier.background(Color.Transparent)),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) scheme.onPrimary else scheme.onSurface,
            maxLines = 1,
        )
    }
}

// ---------------------------------------------------------------- guide

@Composable
fun GuidePanel(state: TabatiUiState, viewModel: TabatiViewModel) {
    val scheme = MaterialTheme.colorScheme
    val selectedCard = state.selectedCard
    var selectedType by remember(selectedCard?.id) {
        mutableStateOf(selectedCard?.type ?: CardType.NATIONAL)
    }
    val activeType = selectedType
    val guide = CardGuides[activeType] ?: CardGuides.getValue(CardType.NATIONAL)

    PanelScaffold(title = Str.guide_title, state = state, viewModel = viewModel) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CardType.PRINTABLE.forEach { type ->
                val isActive = type == activeType
                Text(
                    text = type.shortAr,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isActive) scheme.onPrimary else scheme.onSurface,
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(if (isActive) scheme.primary else scheme.surfaceContainerHigh)
                        .border(1.dp, if (isActive) scheme.primary else scheme.outlineVariant, RoundedCornerShape(percent = 50))
                        .clickable { selectedType = type }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }

        // official size card — secondaryContainer, apply-to-selected action
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.secondaryContainer)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    Str.official_size,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSecondaryContainer.copy(alpha = 0.75f),
                    modifier = Modifier.weight(1f),
                )
                if (selectedCard != null && selectedCard.type != activeType) {
                    Text(
                        text = Str.apply_on_selected,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = scheme.onPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(scheme.primary)
                            .clickable { viewModel.applyCardType(selectedCard.id, activeType) }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
            Text(
                text = activeType.labelAr,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = scheme.onSecondaryContainer,
            )
            Text(
                text = guide.sizeNoteAr,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.ExtraBold,
                color = scheme.onSecondaryContainer,
            )
        }

        // use / issuer / tips
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.surfaceContainer)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GuideLabelValue("${Str.guide_use}:", guide.useAr)
            GuideLabelValue("${Str.guide_issuer}:", guide.issuerAr)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${Str.guide_tips}:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
                guide.tipsAr.forEach { tip -> GuideBullet(tip) }
                GuideBullet(Str.dpi_line)
            }
        }

        // print checklist
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.surfaceContainer)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                Str.checklist_title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Black,
                color = Color(0xFF006C47),
            )
            Text(Str.checklist_1, style = MaterialTheme.typography.bodySmall, color = scheme.onSurface)
            Text(Str.checklist_2, style = MaterialTheme.typography.bodySmall, color = scheme.onSurface)
            Text(Str.checklist_3, style = MaterialTheme.typography.bodySmall, color = scheme.onSurface)
        }
    }
}

@Composable
private fun GuideLabelValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 20.sp,
        )
    }
}

@Composable
private fun GuideBullet(text: String) {
    Row(modifier = Modifier.padding(start = 4.dp)) {
        Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
        Spacer8()
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp,
        )
    }
}
