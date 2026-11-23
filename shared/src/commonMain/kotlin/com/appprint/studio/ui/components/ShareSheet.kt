package com.appprint.studio.ui.components

import com.appprint.studio.core.text.fmt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appprint.studio.ui.strings.Str

/**
 * «تصدير ومشاركة وطباعة» — the design's centered modal dialog, ordered as required:
 * 1) original social share → 2) PNG images → 3) sheet preview + HTML → 4) SYSTEM PRINT (last).
 */
@Composable
fun ShareSheet(
    pageCount: Int,
    currentPage: Int,
    onDirectPrint: () -> Unit,
    onSavePngCurrent: () -> Unit,
    onSaveAllPngs: () -> Unit,
    onSaveHtml: () -> Unit,
    onSharePdf: () -> Unit,
    onSharePreview: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    HtmlDialog(maxWidth = 448.dp, onDismiss = onDismiss) {
        HtmlDialogHeader(Str.share_title, onDismiss)

        // ── 1) المشاركة الأصلية للتواصل الاجتماعي — full-width primary row ──
        PrimaryShareRow(
            icon = Icons.Filled.IosShare,
            title = Str.share_apps,
            sub = Str.share_apps_sub,
            onClick = { onDismiss(); onSharePdf() },
        )

        // ── 2) الصور: PNG الحالية / كل الأوراق ──
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GridAction(
                icon = Icons.Filled.Image,
                title = Str.share_png_one,
                sub = Str.share_png_one_sub.fmt(currentPage + 1),
                onClick = { onDismiss(); onSavePngCurrent() },
                modifier = Modifier.weight(1f),
            )
            GridAction(
                icon = Icons.Filled.Collections,
                title = Str.share_png_all,
                sub = Str.share_png_all_sub.fmt(pageCount),
                onClick = { onDismiss(); onSaveAllPngs() },
                modifier = Modifier.weight(1f),
            )
        }

        // ── 3) معاينة الورقة قبل الطباعة + ملف HTML ──
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GridAction(
                icon = Icons.Filled.Visibility,
                title = Str.share_preview,
                sub = Str.share_preview_sub,
                onClick = { onDismiss(); onSharePreview() },
                modifier = Modifier.weight(1f),
            )
            GridAction(
                icon = Icons.Filled.Code,
                title = Str.share_html,
                sub = Str.share_html_sub,
                onClick = { onDismiss(); onSaveHtml() },
                modifier = Modifier.weight(1f),
            )
        }

        // ── 4) طباعة النظام — آخر زر ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.secondaryContainer)
                .pressScale(interaction)
                .clickable(interactionSource = interaction, indication = null) {
                    onDismiss()
                    onDirectPrint()
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.Print, contentDescription = null, tint = scheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    Str.share_direct_print,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = scheme.onSecondaryContainer,
                )
                Text(
                    Str.share_direct_print_sub,
                    fontSize = 10.sp,
                    color = scheme.onSecondaryContainer.copy(alpha = 0.85f),
                )
            }
        }
        Spacer(Modifier.height(2.dp))
    }
}

@Composable
private fun PrimaryShareRow(
    icon: ImageVector,
    title: String,
    sub: String,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.primary)
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Black, color = scheme.onPrimary)
            Text(sub, fontSize = 10.sp, color = scheme.onPrimary.copy(alpha = 0.85f))
        }
    }
}

/** The design's 2×2 export cards — surfaceContainer, icon on top, title + tiny sub. */
@Composable
private fun GridAction(
    icon: ImageVector,
    title: String,
    sub: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surfaceContainer)
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = scheme.onSurface, modifier = Modifier.size(20.dp))
        Text(
            title,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = scheme.onSurface,
            lineHeight = 15.sp,
        )
        Text(
            sub,
            fontSize = 10.sp,
            color = scheme.onSurfaceVariant,
            lineHeight = 13.sp,
        )
    }
}
