package com.appprint.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * The app's confirmation surface — one design for every destructive action.
 *
 * Structure (mirrors the M3 alert-dialog anatomy with our brand):
 *  tonal error icon · clear title · short body copy · actions row (End aligned)
 * with the destructive action as a filled tonal button in the error container.
 *
 * Sizing is fixed (max 420dp, min 300dp) so dialogs never stretch on tablets and
 * never shrink below a comfortable reading width on phones.
 */
@Composable
fun AppConfirmDialog(
    icon: ImageVector,
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    cancelLabel: String? = "إلغاء",
    onCancel: (() -> Unit)? = null,
    destructive: Boolean = true,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val accent = if (destructive) scheme.errorContainer else scheme.primaryContainer
    val onAccent = if (destructive) scheme.onErrorContainer else scheme.onPrimaryContainer

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = scheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            modifier = modifier
                .widthIn(min = 300.dp, max = 420.dp)
                .fillMaxWidth(0.92f),
        ) {
            Column(
                modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = onAccent,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = scheme.onSurface,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (cancelLabel != null && onCancel != null) {
                        TextButton(onClick = onCancel) { Text(cancelLabel, maxLines = 1) }
                    }
                    if (secondaryLabel != null && onSecondary != null) {
                        TextButton(onClick = onSecondary) { Text(secondaryLabel, maxLines = 1) }
                    }
                    FilledTonalButton(
                        onClick = onPrimary,
                        shape = RoundedCornerShape(percent = 50),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = accent,
                            contentColor = onAccent,
                        ),
                    ) { Text(primaryLabel, maxLines = 1) }
                }
            }
        }
    }
}
