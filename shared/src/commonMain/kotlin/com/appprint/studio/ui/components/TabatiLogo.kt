package com.appprint.studio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.appprint.studio.ui.theme.rememberReemKufiFamily

/**
 * «طبعتي» wordmark — the design's logo: a star glyph in a green circle + Reem Kufi Bold
 * «طبعتي». The mark has no font dependency — it is drawn as vector paths so it
 * renders identically on every platform.
 */
@Composable
fun TabatiLogoMark(size: Dp, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    Canvas(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
    ) {
        drawCircle(color = accent, radius = this.size.minDimension / 2f, center = center)
        val s = this.size.minDimension * 0.56f
        val cx = center.x
        val cy = center.y
        val star = Path().apply {
            moveTo(cx, cy - s / 2f)
            lineTo(cx + s * 0.16f, cy - s * 0.16f)
            lineTo(cx + s / 2f, cy)
            lineTo(cx + s * 0.16f, cy + s * 0.16f)
            lineTo(cx, cy + s / 2f)
            lineTo(cx - s * 0.16f, cy + s * 0.16f)
            lineTo(cx - s / 2f, cy)
            lineTo(cx - s * 0.16f, cy - s * 0.16f)
            close()
        }
        drawPath(star, color = onAccent)
    }
}

/** «طبعتي» — the app wordmark with the mark leading. */
@Composable
fun TabatiWordmark(fontSize: TextUnit, modifier: Modifier = Modifier) {
    Text(
        text = "طبعتي",
        fontFamily = rememberReemKufiFamily(),
        fontWeight = FontWeight.Bold,
        fontSize = fontSize,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}
