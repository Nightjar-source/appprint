package com.appprint.studio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.appprint.studio.core.model.ArrangeMode
import com.appprint.studio.core.model.FrameStyle
import com.appprint.studio.core.model.PaperPreset
import com.appprint.studio.core.model.SheetBackground
import com.appprint.studio.core.model.CardType
import com.appprint.studio.ui.StudioUiState
import com.appprint.studio.ui.StudioViewModel
import com.appprint.studio.ui.components.AppIcons
import com.appprint.studio.ui.components.ColorDot
import com.appprint.studio.ui.components.LabeledSlider
import com.appprint.studio.ui.components.SectionHeader
import com.appprint.studio.ui.components.StudioChip
import com.appprint.studio.ui.components.ToggleRow
import org.jetbrains.compose.resources.stringResource
import com.appprint.studio.shared.resources.Res

// ---------------------------------------------------------------- paper panel

@Composable
fun PaperSettingsPanel(state: StudioUiState, viewModel: StudioViewModel, modifier: Modifier = Modifier) {
    val s = state.settings
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        SectionHeader(stringResource(Res.string.paper_title))

        // Paper size chips
        Column(Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
            Text(
                stringResource(Res.string.paper_size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier.padding(top = 6.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PaperPreset.entries.forEach { preset ->
                    StudioChip(
                        label = preset.label,
                        selected = s.presetKey == preset.key,
                        onClick = { viewModel.setPaperPreset(preset.key) },
                    )
                }
                StudioChip(
                    label = stringResource(Res.string.paper_custom),
                    selected = s.isCustom,
                    onClick = { viewModel.setPaperPreset(null) },
                )
            }
        }
        if (s.isCustom) {
            LabeledSlider(
                label = "العرض (مم)",
                value = s.customWidthMm,
                valueRange = 50f..500f,
                onValueChange = { v -> viewModel.setCustomPaper(v, s.customHeightMm) },
                valueText = s.customWidthMm.toInt().toString(),
            )
            LabeledSlider(
                label = "الطول (مم)",
                value = s.customHeightMm,
                valueRange = 50f..500f,
                onValueChange = { v -> viewModel.setCustomPaper(s.customWidthMm, v) },
                valueText = s.customHeightMm.toInt().toString(),
            )
        }

        // Orientation
        Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text(
                stringResource(Res.string.orientation),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StudioChip(stringResource(Res.string.orientation_portrait), !s.landscape, { viewModel.setLandscape(false) })
                StudioChip(stringResource(Res.string.orientation_landscape), s.landscape, { viewModel.setLandscape(true) })
            }
        }

        LabeledSlider(
            label = stringResource(Res.string.margins),
            value = s.marginMm,
            valueRange = 0f..25f,
            onValueChange = viewModel::setMargin,
            valueText = "${s.marginMm.toInt()}",
        )
        LabeledSlider(
            label = stringResource(Res.string.gap),
            value = s.gapMm,
            valueRange = 2f..20f,
            onValueChange = viewModel::setGap,
            valueText = "${s.gapMm.toInt()}",
        )

        // Arrangement mode
        Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text(
                stringResource(Res.string.mode),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StudioChip(stringResource(Res.string.mode_grid), s.mode == ArrangeMode.GRID, { viewModel.setMode(ArrangeMode.GRID) })
                StudioChip(stringResource(Res.string.mode_comfy), s.mode == ArrangeMode.COMFORTABLE, { viewModel.setMode(ArrangeMode.COMFORTABLE) })
            }
            if (s.mode == ArrangeMode.COMFORTABLE) {
                Text(
                    stringResource(Res.string.gap_comfy_min),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        ToggleRow(
            title = stringResource(Res.string.group_smart),
            subtitle = "الأكبر أولاً، صفوف مدمجة داخل الهوامش",
            checked = s.smartGroup,
            onCheckedChange = viewModel::setSmartGroup,
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Background
        Column(Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
            Text(
                stringResource(Res.string.background),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    SheetBackground.WHITE to Res.string.bg_white,
                    SheetBackground.OFFWHITE to Res.string.bg_offwhite,
                    SheetBackground.TRANSPARENT to Res.string.bg_transparent,
                ).forEach { (bg, labelRes) ->
                    StudioChip(stringResource(labelRes), s.background == bg, { viewModel.setBackground(bg) })
                }
            }
        }

        // Guides & frames
        ToggleRow(
            title = stringResource(Res.string.guides),
            subtitle = "تظهر بخط متقطع أحمر — تُحذف عند الطباعة الفعلية من PDF",
            checked = s.showGuides,
            onCheckedChange = viewModel::setShowGuides,
        )
        ToggleRow(
            title = stringResource(Res.string.frames),
            subtitle = null,
            checked = s.showFrames,
            onCheckedChange = viewModel::setShowFrames,
        )

        if (s.showFrames) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
                Text(
                    stringResource(Res.string.frame_style),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        FrameStyle.SOLID to Res.string.frame_solid,
                        FrameStyle.DASHED to Res.string.frame_dashed,
                        FrameStyle.DOTTED to Res.string.frame_dotted,
                    ).forEach { (style, labelRes) ->
                        StudioChip(stringResource(labelRes), s.frameStyle == style, { viewModel.setFrameStyle(style) })
                    }
                }
                Text(
                    stringResource(Res.string.frame_color),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(0xFF151816L, 0xFFC8102EL, 0xFF007A3DL, 0xFFD9A51FL, 0xFF57534AL).forEach { hex ->
                        ColorDot(hex = hex, selected = s.frameColorHex == hex, onClick = { viewModel.setFrameColor(hex) })
                    }
                }
            }
            LabeledSlider(
                label = stringResource(Res.string.frame_width),
                value = s.frameWidthDp,
                valueRange = 0.5f..4f,
                onValueChange = viewModel::setFrameWidth,
                valueText = String.format("%.1f", s.frameWidthDp),
            )
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Arrange actions
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = viewModel::arrangeSmart, enabled = state.cards.isNotEmpty(), modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.btn_arrange), maxLines = 1)
            }
            FilledTonalButton(onClick = viewModel::sortBySize, enabled = state.cards.isNotEmpty(), modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.btn_sort_size), maxLines = 1)
            }
        }
    }
}

// ------------------------------------------------------------- inspector panel

@Composable
fun InspectorPanel(state: StudioUiState, viewModel: StudioViewModel, modifier: Modifier = Modifier) {
    val card = state.selectedCard
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        SectionHeader(stringResource(Res.string.card_selected))

        if (card == null) {
            Text(
                text = stringResource(Res.string.no_selection),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        } else {
            // name + type + dims
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(card.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text(
                    text = "${card.type.labelAr} · ${formatMm(card.widthMm)} × ${formatMm(card.heightMm)} ${stringResource(Res.string.mm)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            // quick actions
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilledTonalButton(onClick = viewModel::duplicateSelected, modifier = Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 8.dp)) {
                    Text(stringResource(Res.string.btn_duplicate), maxLines = 1, fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp))
                }
                FilledTonalButton(onClick = viewModel::rotateSelected, modifier = Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 8.dp)) {
                    Text(stringResource(Res.string.btn_rotate), maxLines = 1, fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp))
                }
                FilledTonalButton(onClick = viewModel::deleteSelected, modifier = Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 8.dp)) {
                    Text(stringResource(Res.string.btn_delete), maxLines = 1, fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp), color = MaterialTheme.colorScheme.error)
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Enhance sliders
            SectionHeader(stringResource(Res.string.enhance_title))
            LabeledSlider(
                label = stringResource(Res.string.brightness),
                value = card.enhance.brightness.toFloat(),
                valueRange = -100f..100f,
                onValueChange = { v -> viewModel.updateEnhance { it.copy(brightness = v.toInt()) } },
                onReset = { viewModel.updateEnhance { it.copy(brightness = 0) } },
            )
            LabeledSlider(
                label = stringResource(Res.string.contrast),
                value = card.enhance.contrast.toFloat(),
                valueRange = -100f..100f,
                onValueChange = { v -> viewModel.updateEnhance { it.copy(contrast = v.toInt()) } },
                onReset = { viewModel.updateEnhance { it.copy(contrast = 0) } },
            )
            LabeledSlider(
                label = stringResource(Res.string.saturation),
                value = card.enhance.saturation.toFloat(),
                valueRange = -100f..100f,
                onValueChange = { v -> viewModel.updateEnhance { it.copy(saturation = v.toInt()) } },
                onReset = { viewModel.updateEnhance { it.copy(saturation = 0) } },
            )
            LabeledSlider(
                label = stringResource(Res.string.sharpness),
                value = card.enhance.sharpness.toFloat(),
                valueRange = 0f..100f,
                onValueChange = { v -> viewModel.updateEnhance { it.copy(sharpness = v.toInt()) } },
                onReset = { viewModel.updateEnhance { it.copy(sharpness = 0) } },
            )
            ToggleRow(
                title = stringResource(Res.string.grayscale),
                subtitle = null,
                checked = card.enhance.grayscale,
                onCheckedChange = { v -> viewModel.updateEnhance { it.copy(grayscale = v) } },
            )
            Text(
                stringResource(Res.string.enhance_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilledTonalButton(onClick = viewModel::autoEnhanceSelected, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.auto_enhance), maxLines = 1, fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp))
                }
                OutlinedButton(onClick = viewModel::resetEnhanceSelected, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.reset_enhance), maxLines = 1)
                }
            }
            TextButton(onClick = viewModel::applyEnhanceToAll, modifier = Modifier.padding(horizontal = 12.dp)) {
                Text(stringResource(Res.string.apply_all))
            }
            TextButton(onClick = viewModel::autoEnhanceAll, modifier = Modifier.padding(horizontal = 12.dp)) {
                Text(stringResource(Res.string.btn_enhance_all))
            }

            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Card facts (الشرح)
            SectionHeader("الشرح الرسمي")
            FactBlock(label = stringResource(Res.string.usage), value = card.type.useAr)
            FactBlock(label = stringResource(Res.string.issuer), value = card.type.issuerAr)
            FactBlock(label = stringResource(Res.string.official_size), value = card.type.sizeNoteAr)
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text(
                    stringResource(Res.string.print_tips_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                card.type.tipsAr.forEach { tip ->
                    Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                        Text(tip, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun FactBlock(label: String, value: String) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 1.dp))
    }
}

private fun formatMm(v: Float): String =
    if (v == v.toInt().toFloat()) v.toInt().toString() else String.format("%.1f", v)
