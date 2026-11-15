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
    // HTML's star_morph path (material icon), normalized to the canvas box.
    val starPoints = listOf(
        0.5f to 0.083f,   // top tip
        0.614f to 0.368f,
        0.917f to 0.396f,
        0.687f to 0.597f,
        0.758f to 0.896f,
        0.5f to 0.737f,
        0.242f to 0.896f,
        0.312f to 0.597f,
        0.083f to 0.396f,
        0.386f to 0.368f,
    )
    Canvas(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
    ) {
        drawCircle(color = accent, radius = this.size.minDimension / 2f, center = center)
        val s = this.size.minDimension * 0.78f
        val path = Path()
        starPoints.forEachIndexed { index, (x, y) ->
            val px = (center.x - s / 2f) + x * s
            val py = (center.y - s / 2f) + y * s
            if (index == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        drawPath(path, color = onAccent)
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
