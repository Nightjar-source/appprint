package com.appprint.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.appprint.studio.ui.strings.Str

/**
 * Unified confirmation dialog — icon in a tonal circle, title, body,
 * one primary action + optional secondary action, and a plain cancel.
 */
@Composable
fun AppConfirmDialog(
    icon: ImageVector,
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    cancelLabel: String = Str.cancel,
    onCancel: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(scheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = scheme.onSecondaryContainer,
                    modifier = Modifier.size(26.dp),
                )
            }
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onPrimary,
                colors = ButtonDefaults.textButtonColors(
                    containerColor = scheme.primaryContainer,
                    contentColor = scheme.onPrimaryContainer,
                ),
                shape = RoundedCornerShape(percent = 50),
            ) {
                Text(primaryLabel, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            Row {
                if (secondaryLabel != null && onSecondary != null) {
                    TextButton(onClick = onSecondary) {
                        Text(secondaryLabel, color = scheme.onSurfaceVariant)
                    }
                }
                if (onCancel != null) {
                    TextButton(onClick = onCancel) {
                        Text(cancelLabel, color = scheme.onSurfaceVariant)
                    }
                }
            }
        },
    )
}
