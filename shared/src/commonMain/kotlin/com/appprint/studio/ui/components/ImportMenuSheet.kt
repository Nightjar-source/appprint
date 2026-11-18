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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.appprint.studio.ui.TabatiUiState
import com.appprint.studio.ui.strings.Str

/**
 * «إضافة بطاقات أو مستندات» — the design's import menu as a centered modal dialog.
 */
@Composable
fun ImportMenuSheet(
    onPickFromGallery: () -> Unit,
    onCapturePhoto: () -> Unit,
    onAddDemoCards: () -> Unit,
    onDismiss: () -> Unit,
    hasCards: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    HtmlDialog(maxWidth = 384.dp, onDismiss = onDismiss) {
        HtmlDialogHeader("إضافة بطاقات أو مستندات", onDismiss)

        // Primary action — gallery
        ImportRow(
            icon = Icons.Filled.Collections,
            title = "فتح الاستوديو واختيار الصور",
            subtitle = "التعرف التلقائي على الأبعاد الحقيقية",
            container = scheme.primary,
            onContainer = scheme.onPrimary,
            iconSize = 22.dp,
            onClick = { onDismiss(); onPickFromGallery() },
        )
        // Camera
        ImportRow(
            icon = Icons.Filled.CameraAlt,
            title = "التقاط صورة بالكاميرا",
            subtitle = "تصوير مباشر للبطاقة أو الجواز",
            container = scheme.secondaryContainer,
            onContainer = scheme.onSecondaryContainer,
            iconSize = 22.dp,
            onClick = { onDismiss(); onCapturePhoto() },
        )
        // Demo cards — only while the sheet is empty (a real user already working doesn't need examples)
        if (!hasCards) {
            ImportRow(
                icon = Icons.Filled.AutoAwesome,
                title = "إضافة بطاقات تجريبية بالمقاسات الرسمية",
                subtitle = "وطنية، جواز سفر، بطاقة سكن",
                container = scheme.surfaceContainer,
                onContainer = scheme.onSurface,
                iconSize = 20.dp,
                onClick = { onDismiss(); onAddDemoCards() },
            )
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
    iconSize: androidx.compose.ui.unit.Dp = 22.dp,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = if (container == MaterialTheme.colorScheme.surfaceContainer) 12.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = onContainer, modifier = Modifier.size(iconSize))
        Column {
            Text(
                title,
                fontWeight = FontWeight.Black,
                color = onContainer,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
            Text(
                subtitle,
                color = onContainer.copy(alpha = 0.75f),
                fontSize = 10.sp,
                lineHeight = 13.sp,
            )
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
    val w = formatDim(card.widthMm)
    val h = formatDim(card.heightMm)
    Box(modifier = modifier) {
        // top-start badge — size (and angle)
        BadgeLabel(
            text = "$w×$h مم${if (totalDeg > 0) " • ${totalDeg}°" else ""}",
            background = Color.Black.copy(alpha = 0.75f),
            foreground = Color.White,
            modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
            fontSize = 7.5f,
        )
        // top-end badge — automatic adjust
        if (card.autoShrunk || card.autoRotated) {
            BadgeLabel(
                text = if (card.autoShrunk) "تصغير تلقائي" else "تدوير تلقائي",
                background = Color(0xFFD9A51F),
                foreground = Color.Black,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                fontSize = 7f,
            )
        }
        // bottom-end badge — enhanced
        if (isEnhanced) {
            BadgeLabel(
                text = if (card.bulkEnhanced) "محسّنة - جماعي" else "محسّنة",
                background = Color(0xFF006C47),
                foreground = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                fontSize = 7f,
            )
        }
    }
}

private fun formatDim(v: Float): String =
    "%.1f".format(v).trimEnd('0').trimEnd('.')

@Composable
private fun BadgeLabel(
    text: String,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
    fontSize: Float = 7f,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(background)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            color = foreground,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
        )
    }
}

/** Long-press quick context — the design's centered modal (دوران حر 360° + صفي أزرار). */
@Composable
fun QuickActionsSheet(
    state: TabatiUiState,
    viewModel: TabatiViewModel,
    onReplaceImage: (Long) -> Unit,
    onRequestSwap: () -> Unit,
    onDismiss: () -> Unit,
) {
    val card = state.cards.firstOrNull { it.id == state.longPressSheetCardId } ?: return
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val totalDeg = SheetArranger.totalRotation(card).toInt()

    HtmlDialog(maxWidth = 448.dp, onDismiss = onDismiss) {
        // header — name + meta + close
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    card.name,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    maxLines = 1,
                )
                Text(
                    text = "${formatDim(card.widthMm)} × ${formatDim(card.heightMm)} مم • زاوية ${totalDeg}°",
                    fontSize = 11.sp,
                    color = scheme.onSurfaceVariant,
                )
            }
            androidx.compose.material3.IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "إغلاق", modifier = Modifier.size(18.dp))
            }
        }

        // 0–360° rotation block
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.surfaceContainer)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Filled.Autorenew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = scheme.onSurface,
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    "دوران حر 360°",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${totalDeg}°",
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(scheme.secondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    color = scheme.onSecondaryContainer,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                )
            }
            Slider(
                value = totalDeg.toFloat(),
                onValueChange = { v -> viewModel.setCardAngle360(card.id, v, magneticSnap = false) },
                valueRange = 0f..360f,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                listOf(0f, 90f, 180f, 270f).forEach { deg ->
                    val selected = totalDeg == deg.toInt()
                    Text(
                        text = "${deg.toInt()}°",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = if (selected) scheme.onPrimary else scheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .wrapContentHeight(Alignment.CenterVertically)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(if (selected) scheme.primary else scheme.surfaceContainerHigh)
                            .clickable { viewModel.setCardAngle360(card.id, deg, magneticSnap = false) }
                            .padding(vertical = 8.dp),
                    )
                }
            }
        }

        // quick actions grid — exactly the design's 2-col rows
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (card.imageUri != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionButton(
                        icon = Icons.Filled.AutoAwesome,
                        label = "تحسين تلقائي",
                        container = scheme.primary,
                        onContainer = scheme.onPrimary,
                        modifier = Modifier.weight(1f),
                    ) { viewModel.autoEnhanceSelectedDirect(card.id); onDismiss() }
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
                    icon = Icons.Filled.AutoFixHigh,
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
                    ) { onRequestSwap(); onDismiss() }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickActionButton(
                    icon = Icons.Filled.ContentCopy,
                    label = "نسخ البطاقة",
                    modifier = Modifier.weight(1f),
                ) { viewModel.duplicateCard(card.id); onDismiss() }
                QuickActionButton(
                    icon = Icons.Filled.Delete,
                    label = "حذف البطاقة",
                    container = scheme.tertiaryContainer,
                    onContainer = scheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f),
                ) { viewModel.deleteCard(card.id); onDismiss() }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    container: Color = MaterialTheme.colorScheme.surfaceContainer,
    onContainer: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = onContainer, modifier = Modifier.size(18.dp))
        Text(
            label,
            fontWeight = FontWeight.ExtraBold,
            color = onContainer,
            fontSize = 12.sp,
            maxLines = 2,
            lineHeight = 15.sp,
        )
    }
}
