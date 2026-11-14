package com.appprint.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Oval snackbar under the floating bar — hugs its message, never blocks the sheet. */
@Composable
fun AppSnackbar(
    message: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .height(36.dp)
            .shadow(6.dp, RoundedCornerShape(percent = 50), clip = false)
            .clip(RoundedCornerShape(percent = 50))
            .background(scheme.inverseSurface)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = scheme.inverseOnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
