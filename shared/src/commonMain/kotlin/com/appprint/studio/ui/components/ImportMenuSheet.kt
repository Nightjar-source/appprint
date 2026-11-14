package com.appprint.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.appprint.studio.core.model.CardItem
import com.appprint.studio.core.arrange.SheetArranger
import com.appprint.studio.ui.TabatiViewModel
import com.appprint.studio.ui.strings.Str

/**
 * «إضافة بطاقات أو مستندات» — the design's import menu sheet:
 * primary action (gallery) → camera → demo cards.
 */
@Composable
fun ImportMenuSheet(
    onPickFromGallery: () -> Unit,
    onCapturePhoto: () -> Unit,
    onAddDemoCards: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = scheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "إضافة بطاقات أو مستندات",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "إغلاق")
                }
            }

            // Primary action — gallery
            ImportRow(
                icon = Icons.Filled.Collections,
                title = "فتح الاستوديو واختيار الصور",
                subtitle = "التعرف التلقائي على الأبعاد الحقيقية",
                container = scheme.primary,
                onContainer = scheme.onPrimary,
                interaction = interaction,
                onClick = { onDismiss(); onPickFromGallery() },
            )
            // Camera
            ImportRow(
                icon = Icons.Filled.CameraAlt,
                title = "التقاط صورة بالكاميرا",
                subtitle = "تصوير مباشر للبطاقة أو الجواز",
                container = scheme.secondaryContainer,
                onContainer = scheme.onSecondaryContainer,
                interaction = interaction,
                onClick = { onDismiss(); onCapturePhoto() },
            )
            // Demo cards
            ImportRow(
                icon = Icons.Filled.AutoAwesome,
                title = "إضافة بطاقات تجريبية بالمقاسات الرسمية",
                subtitle = "وطنية، جواز سفر، بطاقة سكن",
                container = scheme.surfaceContainer,
                onContainer = scheme.onSurface,
                interaction = interaction,
                onClick = { onDismiss(); onAddDemoCards() },
            )
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun ImportRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    container: Color,
    onContainer: Color,
    interaction: MutableInteractionSource,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(container)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = onContainer, modifier = Modifier.size(24.dp))
        Column {
            Text(title, fontWeight = FontWeight.Black, color = onContainer, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, color = onContainer.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Card badges — top-start: size + angle, top-end: auto-fix, bottom-end: enhanced. */
@Composable
fun CardBadges(
    card: CardItem,
    modifier: Modifier = Modifier,
) {
    val totalDeg = SheetArranger.totalRotation(card).toInt()
    val isEnhanced = card.enhance != com.appprint.studio.core.model.EnhanceSettings.IDENTITY
    Box(modifier = modifier) {
        // top-start badge — size (and angle)
        BadgeLabel(
            text = "%.1f×%.1f مم%s".format(card.widthMm, card.heightMm, if (totalDeg > 0) " • ${totalDeg}°" else ""),
            background = Color.Black.copy(alpha = 0.75f),
            foreground = Color.White,
            modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
        )
        // top-end badge — automatic adjust
        if (card.autoShrunk || card.autoRotated) {
            BadgeLabel(
                text = if (card.autoShrunk) "تصغير تلقائي" else "تدوير تلقائي",
                background = Color(0xFFD9A51F),
                foreground = Color.Black,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
            )
        }
        // bottom-end badge — enhanced
        if (isEnhanced) {
            BadgeLabel(
                text = "محسّنة",
                background = Color(0xFF006C47),
                foreground = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
            )
        }
    }
}

@Composable
private fun BadgeLabel(
    text: String,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(background)
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            color = foreground,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
        )
    }
}

/** Long-press quick context sheet — header, 0–360° slider with quick angles, action grid. */
@Composable
fun QuickActionsSheet(
    state: com.appprint.studio.ui.TabatiUiState,
    viewModel: TabatiViewModel,
    onReplaceImage: (Long) -> Unit,
    onRequestSwap: () -> Unit,
    onDismiss: () -> Unit,
) {
    val card = state.cards.firstOrNull { it.id == state.longPressSheetCardId } ?: return
    val scheme = MaterialTheme.colorScheme
    val totalDeg = com.appprint.studio.core.arrange.SheetArranger.totalRotation(card).toInt()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = scheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // header — name + meta + close
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(card.name, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "%.1f × %.1f مم • زاوية %d°".format(card.widthMm, card.heightMm, totalDeg),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "إغلاق")
                }
            }

            // 0–360° rotation block
            Surface2 {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Rotate90DegreesCw, contentDescription = null, modifier = Modifier.size(18.dp), tint = scheme.primary)
                    Spacer(Modifier.size(8.dp))
                    Text("دوران حر 360°", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    Text(
                        text = "${totalDeg}°",
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(scheme.secondaryContainer)
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                        color = scheme.onSecondaryContainer,
                        fontWeight = FontWeight.Black,
                    )
                }
                Slider(
                    value = totalDeg.toFloat(),
                    onValueChange = { v -> viewModel.setCardAngle360(card.id, v, magneticSnap = false) },
                    valueRange = 0f..360f,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(0f, 90f, 180f, 270f).forEach { deg ->
                        val selected = totalDeg == deg.toInt()
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(percent = 50))
                                .background(if (selected) scheme.primary else scheme.surfaceContainerHigh)
                                .clickable { viewModel.setCardAngle360(card.id, deg, magneticSnap = false) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "${deg.toInt()}°",
                                fontWeight = FontWeight.ExtraBold,
                                color = if (selected) scheme.onPrimary else scheme.onSurface,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }

            // quick actions grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (card.imageUri != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickActionButton(
                            icon = Icons.Filled.AutoAwesome,
                            label = "تحسين تلقائي",
                            container = scheme.primary,
                            onContainer = scheme.onPrimary,
                            modifier = Modifier.weight(1f),
                        ) { viewModel.autoEnhanceSelected(); onDismiss() }
                        QuickActionButton(
                            icon = Icons.Filled.Crop,
                            label = "قص الأطراف",
                            container = scheme.secondaryContainer,
                            onContainer = scheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f),
                        ) { viewModel.autoCropSelected(); onDismiss() }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionButton(
                        icon = Icons.Filled.AutoAwesome,
                        label = "لوحة التحسين",
                        modifier = Modifier.weight(1f),
                    ) { viewModel.setSection(com.appprint.studio.ui.StudioSection.ENHANCE); onDismiss() }
                    QuickActionButton(
                        icon = Icons.Filled.Tune,
                        label = "تعديل القياس",
                        modifier = Modifier.weight(1f),
                    ) { viewModel.setSection(com.appprint.studio.ui.StudioSection.CARDS); onDismiss() }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionButton(
                        icon = Icons.Filled.Image,
                        label = "استبدال الصورة",
                        modifier = Modifier.weight(1f),
                    ) { onReplaceImage(card.id); onDismiss() }
                    if (state.cards.size > 1) {
                        QuickActionButton(
                            icon = Icons.Filled.SwapHoriz,
                            label = "تبديل مكان",
                            modifier = Modifier.weight(1f),
                        ) { onRequestSwap() }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionButton(
                        icon = Icons.Filled.ContentCopy,
                        label = "نسخ البطاقة",
                        modifier = Modifier.weight(1f),
                    ) { viewModel.duplicateCard(card.id); onDismiss() }
                    QuickActionButton(
                        icon = Icons.Filled.DeleteForever,
                        label = "حذف البطاقة",
                        container = scheme.tertiaryContainer,
                        onContainer = scheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f),
                    ) { viewModel.deleteCard(card.id); onDismiss() }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun Surface2(content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(scheme.surfaceContainer)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    onContainer: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(container)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = onContainer, modifier = Modifier.size(18.dp))
        Text(label, fontWeight = FontWeight.ExtraBold, color = onContainer, fontSize = 12.sp, maxLines = 2)
    }
}
