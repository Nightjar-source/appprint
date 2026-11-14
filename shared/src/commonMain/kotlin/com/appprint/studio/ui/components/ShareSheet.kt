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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
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
import com.appprint.studio.ui.strings.Str

/**
 * Share sheet — the design's exact order:
 *  طباعة مباشرة (primary) → PNG الحالية → كل الأوراق PNG → ملف HTML → مشاركة للتطبيقات.
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
    onDismiss: () -> Unit,
) {
    val items = listOf(
        ShareRow(Icons.Filled.Print, Str.share_direct_print, Str.share_direct_print_sub, primary = true) to onDirectPrint,
        ShareRow(Icons.Filled.Image, Str.share_png_one, Str.share_png_one_sub.format(currentPage + 1, pageCount)) to onSavePngCurrent,
        ShareRow(Icons.Filled.Collections, Str.share_png_all, Str.share_png_all_sub.format(pageCount)) to onSaveAllPngs,
        ShareRow(Icons.Filled.Code, Str.share_html, Str.share_html_sub) to onSaveHtml,
        ShareRow(Icons.Filled.PictureAsPdf, Str.share_apps, Str.share_apps_sub) to onSharePdf,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = Str.share_title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        items.forEach { (row, onClick) ->
            ShareRowItem(row = row, onClick = { onClick(); onDismiss() })
        }
        Spacer(Modifier.size(4.dp))
    }
}

private data class ShareRow(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val primary: Boolean = false,
)

@Composable
private fun ShareRowItem(row: ShareRow, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (row.primary) scheme.primaryContainer else scheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                row.icon,
                contentDescription = null,
                tint = if (row.primary) scheme.onPrimaryContainer else scheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(row.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = scheme.onSurface)
            Text(row.subtitle, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        }
    }
}
