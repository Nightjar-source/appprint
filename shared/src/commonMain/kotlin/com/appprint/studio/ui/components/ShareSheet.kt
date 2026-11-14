package com.appprint.studio.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appprint.studio.ui.strings.Str

/**
 * Share sheet — one scrollable list of every export/share path:
 * PDF (primary, 300 DPI) · PNG (current page) · PNGs (all pages) · HTML print file · share via system apps.
 */
@Composable
fun ShareSheet(
    onExportPdf: () -> Unit,
    onExportPngCurrent: () -> Unit,
    onExportAllPngs: () -> Unit,
    onExportHtml: () -> Unit,
    onDismiss: () -> Unit,
    pageCount: Int,
) {
    val scheme = MaterialTheme.colorScheme
    val items = listOf(
        ShareRow(Icons.Filled.PictureAsPdf, Str.export_pdf, "ملف أصلي 300 DPI — بقياس حقيقي 1:1", primary = true) to onExportPdf,
        ShareRow(Icons.Filled.Image, Str.share_png, "الورقة الحالية كصورة واحدة") to onExportPngCurrent,
        ShareRow(Icons.Filled.Collections, Str.export_all_pngs, "ورقة ورقة ($pageCount أوراق)") to onExportAllPngs,
        ShareRow(Icons.Filled.Code, Str.export_html, "ملف يفتح في المتصفح ويطبع مباشرة") to onExportHtml,
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
        Spacer(Modifier.height(4.dp))
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
        Column {
            Text(row.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = scheme.onSurface)
            Text(row.subtitle, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        }
    }
}
