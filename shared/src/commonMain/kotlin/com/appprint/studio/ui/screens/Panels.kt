@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.appprint.studio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appprint.studio.core.model.ArrangeMode
import com.appprint.studio.core.model.CardGuides
import com.appprint.studio.core.model.CardItem
import com.appprint.studio.core.model.CardType
import com.appprint.studio.core.model.EnhanceMode
import com.appprint.studio.core.model.EnhanceSettings
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
import com.appprint.studio.ui.strings.Str

/**
 * Editing panels — the Design's five tabs rendered as a scrollable column.
 * Every control calls straight into [TabatiViewModel] — no duplicated state.
 */

private const val PANEL_TITLE_PAD = 12

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
                TextButton(onClick = viewModel::arrangeSmart) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer6()
                    Text(Str.arrange_smart, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
            trailing?.invoke()
            Spacer6()
            TextButton(
                onClick = { viewModel.setSection(com.appprint.studio.ui.StudioSection.SHEET) },
                modifier = Modifier.height(32.dp),
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
                .padding(horizontal = PANEL_TITLE_PAD.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
private fun Spacer6() {
    androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
}

// ---------------------------------------------------------------- paper

@Composable
fun PaperPanel(state: TabatiUiState, viewModel: TabatiViewModel) {
    val settings = state.settings
    PanelScaffold(title = Str.section_paper, state = state, viewModel = viewModel) {
        PanelSectionHeader(Str.paper_size)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PaperPreset.PRESETS.forEach { preset ->
                PaperSizeChip(
                    label = preset.label,
                    sub = if (preset == PaperPreset.CUSTOM) Str.custom_paper else "${preset.widthMm.toInt()}×${preset.heightMm.toInt()}",
                    selected = settings.presetKey == preset.key,
                    onClick = { viewModel.setPaperPreset(preset.key) },
                )
            }
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
            PanelChoiceChip(Str.portrait, selected = settings.orientation == Orientation.PORTRAIT, onClick = { viewModel.setOrientation(Orientation.PORTRAIT) })
            PanelChoiceChip(Str.landscape, selected = settings.orientation == Orientation.LANDSCAPE, onClick = { viewModel.setOrientation(Orientation.LANDSCAPE) })
        }

        PanelSliderRow(
            label = Str.margins,
            value = settings.marginMm,
            valueRange = 0f..30f,
            onValueChange = viewModel::setMargin,
            valueText = "${settings.marginMm.toInt()} مم",
            steps = 29,
        )
        PanelSliderRow(
            label = Str.gap,
            value = settings.gapMm,
            valueRange = 0f..20f,
            onValueChange = viewModel::setGap,
            valueText = "${settings.gapMm.toInt()} مم",
            steps = 19,
        )

        PanelSectionHeader(Str.layout_mode)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PanelChoiceChip(Str.mode_comfortable, selected = settings.mode == ArrangeMode.COMFORTABLE, onClick = { viewModel.setMode(ArrangeMode.COMFORTABLE) })
            PanelChoiceChip(Str.mode_compact, selected = settings.mode == ArrangeMode.COMPACT, onClick = { viewModel.setMode(ArrangeMode.COMPACT) })
            PanelChoiceChip(Str.mode_grid, selected = settings.mode == ArrangeMode.GRID, onClick = { viewModel.setMode(ArrangeMode.GRID) })
        }

        PanelToggleRow(Str.group_smart, checked = settings.groupSmart, onCheckedChange = viewModel::setGroupSmart)
        PanelToggleRow(Str.auto_arrange, checked = settings.autoArrangeOnAdd, onCheckedChange = viewModel::setAutoArrange)
        PanelToggleRow(Str.multi_page, checked = settings.allowMultiPage, onCheckedChange = viewModel::setAllowMultiPage)
    }
}

@Composable
private fun PaperSizeChip(label: String, sub: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .width(102.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) scheme.primary else scheme.surfaceContainerHigh)
            .border(1.dp, if (selected) scheme.primary else scheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = if (selected) scheme.onPrimary else scheme.onSurface)
        Text(sub, style = MaterialTheme.typography.labelSmall, color = if (selected) scheme.onPrimary.copy(alpha = 0.85f) else scheme.onSurfaceVariant)
    }
}

// ---------------------------------------------------------------- cards

@Composable
fun CardsPanel(state: TabatiUiState, viewModel: TabatiViewModel, onReplaceImage: (Long) -> Unit, onClearAllRequest: () -> Unit = {}) {
    val selected = state.selectedCard
    PanelScaffold(title = Str.section_cards, state = state, viewModel = viewModel) {
        if (selected == null) {
            Text(
                text = "حدد بطاقة من المعاينة لتعديلها هنا",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            OutlinedTextField(
                value = selected.name,
                onValueChange = { viewModel.renameCard(selected.id, it) },
                label = { Text("اسم البطاقة") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "${selected.widthMm.toInt()} × ${selected.heightMm.toInt()} مم — ورقة ${selected.page + 1}" +
                    selected.detectedLabel.takeIf { it.isNotEmpty() }?.let { " — $it" }.orEmpty(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            PanelSectionHeader(Str.rotate_360)
            val currentRotation = selected.totalRotation
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(0f, 45f, 90f, 135f, 180f, 270f, 315f).forEach { deg ->
                    FilterChip(
                        selected = kotlin.math.abs(currentRotation - deg) < 0.01f,
                        onClick = { viewModel.setCardAngle360(selected.id, deg, magneticSnap = false) },
                        label = { Text("${deg.toInt()}°") },
                    )
                }
            }

            PanelToggleRow("الإطار حول البطاقة", checked = selected.border, onCheckedChange = { viewModel.updateSelectedCard { it.copy(border = !it.border) } })
            PanelToggleRow("أدلة القص", checked = selected.guides, onCheckedChange = { viewModel.updateSelectedCard { it.copy(guides = !it.guides) } })

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PanelActionButton(Icons.Filled.ContentCopy, Str.duplicate) { viewModel.duplicateCard(selected.id) }
                PanelActionButton(Icons.Filled.SwapHoriz, Str.swap) { }
                PanelActionButton(Icons.Filled.Image, Str.replace_image) { onReplaceImage(selected.id) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PanelActionButton(Icons.Filled.DeleteOutline, Str.remove, danger = true) { viewModel.deleteCard(selected.id) }
            }
        }

        PanelSectionHeader("بطاقاتك على الورقة (${state.cardsOnPage.size})", trailing = {
            if (state.cards.isNotEmpty()) {
                TextButton(
                    onClick = onClearAllRequest,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                    shape = RoundedCornerShape(percent = 50),
                    modifier = Modifier.height(30.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                ) {
                    Text(
                        Str.clear_all,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        })
        state.cardsOnPage.forEach { card ->
            CardListRow(card = card, selected = card.id == state.selectedCardId, onClick = { viewModel.selectCard(card.id) })
        }
    }
}

@Composable
private fun CardListRow(card: CardItem, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) scheme.secondaryContainer else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp, 24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (card.typeKey == "passport") Color(0xFF17365A) else Color(0xFFFDFAF1))
                .border(1.dp, scheme.outlineVariant, RoundedCornerShape(4.dp)),
        )
        Spacer8()
        Text(
            text = card.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${card.widthMm.toInt()}×${card.heightMm.toInt()}",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Spacer8() {
    androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
}

@Composable
private fun PanelActionButton(
    icon: ImageVector,
    label: String,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val content = if (danger) scheme.error else scheme.onSurface
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.height(46.dp),
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        Spacer6()
        Text(label, fontWeight = FontWeight.Bold, color = content, maxLines = 1)
    }
}

// ---------------------------------------------------------------- enhance

@Composable
fun EnhancePanel(state: TabatiUiState, viewModel: TabatiViewModel) {
    val scheme = MaterialTheme.colorScheme
    val selected = state.selectedCard
    PanelScaffold(
        title = Str.section_enhance,
        state = state,
        viewModel = viewModel,
        trailing = {
            TextButton(onClick = {
                if (state.enhanceScope == com.appprint.studio.ui.EnhanceScope.ALL) {
                    viewModel.resetAllEnhance()
                } else {
                    viewModel.resetSelectedEnhance()
                }
            }) {
                Text(Str.reset, fontWeight = FontWeight.Bold)
            }
        },
    ) {
        if (selected == null) {
            Text(
                text = "حدد بطاقة من المعاينة لتحسينها هنا",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
            return@PanelScaffold
        }
        // scope
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PanelChoiceChip(Str.enhance_scope_one, selected = state.enhanceScope == com.appprint.studio.ui.EnhanceScope.SINGLE, onClick = { viewModel.setEnhanceScope(com.appprint.studio.ui.EnhanceScope.SINGLE) })
            PanelChoiceChip(Str.enhance_scope_all, selected = state.enhanceScope == com.appprint.studio.ui.EnhanceScope.ALL, onClick = { viewModel.setEnhanceScope(com.appprint.studio.ui.EnhanceScope.ALL) })
        }

        Button(
            onClick = viewModel::autoEnhanceSelected,
            shape = RoundedCornerShape(percent = 50),
            modifier = Modifier.fillMaxWidth().height(46.dp),
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null)
            Spacer6()
            Text(Str.enhance_auto, fontWeight = FontWeight.ExtraBold)
        }

        @Composable
        fun slider(label: String, value: Float, onChange: (Float) -> Unit) =
            PanelSliderRow(label, value, 50f..160f, onChange, "${value.toInt()}%", steps = 110)

        slider(Str.brightness, selected.enhance.brightness) { v -> viewModel.updateSelectedEnhance { it.copy(brightness = v) } }
        slider(Str.contrast, selected.enhance.contrast) { v -> viewModel.updateSelectedEnhance { it.copy(contrast = v) } }
        slider(Str.saturation, selected.enhance.saturation) { v -> viewModel.updateSelectedEnhance { it.copy(saturation = v) } }
        slider(Str.sharpness, selected.enhance.sharpness) { v -> viewModel.updateSelectedEnhance { it.copy(sharpness = v) } }

        PanelSectionHeader("النمط")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PanelChoiceChip(Str.mode_color, selected = selected.enhance.mode == EnhanceMode.COLOR, onClick = { viewModel.updateSelectedEnhance { it.copy(mode = EnhanceMode.COLOR) } })
            PanelChoiceChip(Str.mode_gray, selected = selected.enhance.mode == EnhanceMode.GRAY, onClick = { viewModel.updateSelectedEnhance { it.copy(mode = EnhanceMode.GRAY) } })
            PanelChoiceChip(Str.mode_bw, selected = selected.enhance.mode == EnhanceMode.BW, onClick = { viewModel.updateSelectedEnhance { it.copy(mode = EnhanceMode.BW) } })
        }

        PanelToggleRow(
            title = Str.shadow_lift,
            subtitle = Str.shadow_lift_desc,
            checked = selected.enhance.shadowLift,
            onCheckedChange = { v -> viewModel.updateSelectedEnhance { it.copy(shadowLift = v) } },
        )

        PanelToggleRow(title = Str.compare_before, checked = state.compareBefore, onCheckedChange = { viewModel.toggleCompareBefore() })
    }
}

// ---------------------------------------------------------------- borders

@Composable
fun BordersPanel(state: TabatiUiState, viewModel: TabatiViewModel) {
    val frame = state.settings.frame
    PanelScaffold(title = Str.section_borders, state = state, viewModel = viewModel) {
        PanelSectionHeader(Str.frame_selection)
        PanelToggleRow(Str.frame_sheet, checked = frame.sheet, onCheckedChange = { v -> viewModel.setFrame { it.copy(sheet = v) } })
        PanelToggleRow(Str.frame_card, checked = frame.card, onCheckedChange = { v -> viewModel.setFrame { it.copy(card = v) } })
        PanelToggleRow(Str.frame_guides, subtitle = "لا تطبع — للقص فقط", checked = frame.guides, onCheckedChange = { v -> viewModel.setFrame { it.copy(guides = v) } })
        PanelToggleRow(Str.frame_ruler, checked = frame.ruler, onCheckedChange = { v -> viewModel.setFrame { it.copy(ruler = v) } })

        TextButton(onClick = viewModel::toggleAllFrames, modifier = Modifier.fillMaxWidth()) {
            Text(if (frame.allFramesOff) Str.show_all_frames else Str.hide_all_frames, fontWeight = FontWeight.Bold)
        }

        PanelSectionHeader(Str.background)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PanelChoiceChip(Str.white, selected = frame.background == SheetBackground.WHITE, onClick = { viewModel.setFrameBackground(SheetBackground.WHITE) })
            PanelChoiceChip(Str.offwhite, selected = frame.background == SheetBackground.OFFWHITE, onClick = { viewModel.setFrameBackground(SheetBackground.OFFWHITE) })
            PanelChoiceChip(Str.transparent, selected = frame.background == SheetBackground.TRANSPARENT, onClick = { viewModel.setFrameBackground(SheetBackground.TRANSPARENT) })
        }

        PanelSliderRow(
            label = Str.thickness,
            value = frame.thickness,
            valueRange = 0.5f..5f,
            onValueChange = viewModel::setFrameThickness,
            valueText = "×${"%.1f".format(frame.thickness)}",
            steps = 9,
        )

        PanelSectionHeader(Str.frame_color)
        val colors = listOf(0xFF111111L, 0xFF006C47L, 0xFFB32638L, 0xFF16324FL, 0xFF8A5B12L, 0xFF910824L)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            colors.forEach { hex ->
                ColorDot(
                    hex = hex,
                    selected = frame.colorHex == hex,
                    onClick = { viewModel.setFrameColor(hex) },
                )
            }
        }

        PanelSectionHeader(Str.frame_style)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PanelChoiceChip(Str.solid, selected = frame.style == FrameStyle.SOLID, onClick = { viewModel.setFrameStyle(FrameStyle.SOLID) })
            PanelChoiceChip(Str.dashed, selected = frame.style == FrameStyle.DASHED, onClick = { viewModel.setFrameStyle(FrameStyle.DASHED) })
            PanelChoiceChip(Str.dotted, selected = frame.style == FrameStyle.DOTTED, onClick = { viewModel.setFrameStyle(FrameStyle.DOTTED) })
        }
    }
}

@Composable
private fun ColorDot(hex: Long, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(Color(hex))
            .border(if (selected) 2.5.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, androidx.compose.foundation.shape.CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    )
}

// ---------------------------------------------------------------- guide

@Composable
fun GuidePanel(state: TabatiUiState, viewModel: TabatiViewModel) {
    val scheme = MaterialTheme.colorScheme
    var selectedType by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(CardType.NATIONAL) }
    val guide = CardGuides[selectedType]
    PanelScaffold(title = Str.guide_title, state = state, viewModel = viewModel) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CardType.PRINTABLE.forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = { Text(type.shortAr) },
                )
            }
        }
        guide?.let { g ->
            PanelSectionHeader(g.sizeNoteAr)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(scheme.surfaceContainer)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GuideRow(Str.guide_use, g.useAr)
                GuideRow(Str.guide_issuer, g.issuerAr)
                GuideRow(Str.guide_size, g.sizeNoteAr)
                Column {
                    Text(Str.guide_tips, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                    g.tipsAr.forEach { tip ->
                        Row(modifier = Modifier.padding(top = 4.dp)) {
                            Text("•", color = scheme.primary, fontWeight = FontWeight.Black)
                            Spacer8()
                            Text(tip, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
    }
}
