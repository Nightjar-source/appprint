package com.appprint.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring

/**
 * Liquid Glass surface — M3 Expressive frosted pill/shape for floating chrome:
 * translucent tonal container + hairline outline + deep soft shadow + top rim light.
 */
@Composable
fun glassColor(): Color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f)

/** Top specular rim light — the signature edge of liquid glass. */
private fun Modifier.glassRim(): Modifier = this.drawWithContent {
    drawContent()
    drawRect(
        brush = Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = 0.34f),
            0.28f to Color.White.copy(alpha = 0.08f),
            0.5f to Color.Transparent,
        ),
        size = size.copy(height = size.height * 0.55f),
    )
}

@Composable
fun GlassContainer(
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.26f),
                spotColor = Color.Black.copy(alpha = 0.40f),
            )
            .clip(shape)
            .background(glassColor())
            .glassRim()
            .border(1.dp, Color.White.copy(alpha = 0.28f), shape),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Round glass action button with a bouncy press scale (M3 Expressive motion). */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.86f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 500f),
        label = "glassButtonPress",
    )
    Box(
        modifier = modifier
            .size(48.dp)
            .scale(scale)
            .shadow(
                elevation = 10.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.28f),
                spotColor = Color.Black.copy(alpha = 0.4f),
            )
            .clip(CircleShape)
            .background(glassColor().copy(alpha = if (enabled) 0.45f else 0.30f))
            .glassRim()
            .border(
                1.dp,
                Color.White.copy(alpha = 0.28f),
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxSize(),
            interactionSource = interaction,
        ) {
            content()
        }
    }
}
