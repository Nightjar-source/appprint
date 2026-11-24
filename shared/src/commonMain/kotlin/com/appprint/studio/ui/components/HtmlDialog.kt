package com.appprint.studio.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centered modal dialog exactly like the design (`.m3-dialog`):
 * dimmed scrim, surface-high card with outlineVariant border, radius 24,
 * spring entrance (scale 0.86 → 1 + slide up). No bottom-sheet chrome.
 */
@Composable
fun HtmlDialog(
    maxWidth: Dp = 384.dp,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 310f))
    }
    val scrim = remember { MutableInteractionSource() }
    val dialog = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            // Dialogue content must clear cutouts, status bar and system buttons.
            .safeDrawingPadding()
            .background(Color.Black.copy(alpha = 0.45f * progress.value))
            .clickable(interactionSource = scrim, indication = null, onClick = onDismiss)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = progress.value
                    val s = 0.86f + 0.14f * progress.value
                    scaleX = s
                    scaleY = s
                    translationY = (1f - progress.value) * 48f
                }
                .clip(RoundedCornerShape(24.dp))
                .background(scheme.surfaceContainerHigh)
                .border(1.dp, scheme.outlineVariant, RoundedCornerShape(24.dp))
                .clickable(interactionSource = dialog, indication = null) { /* eat taps */ }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
        }
    }
}

/** Design-style dialog header: title (start-aligned) + circular close X. */
@Composable
fun HtmlDialogHeader(title: String, onDismiss: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "إغلاق",
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
