package com.appprint.studio.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape

/**
 * Full-screen adaptive native-feeling splash, driven by Material 3 Expressive colour roles.
 * The background always fills the whole window (including behind the system bars); the
 * content itself stays inside safeDrawing insets on phones, tablets, landscape and foldables.
 */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val pulse = remember { Animatable(0.82f) }
    LaunchedEffect(Unit) {
        pulse.animateTo(1f, spring(dampingRatio = 0.68f, stiffness = 300f))
        while (true) {
            pulse.animateTo(1.035f, tween(900))
            pulse.animateTo(1f, tween(900))
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.surface),
    ) {
        val wide = maxWidth >= 840.dp
        val medium = maxWidth >= 600.dp
        val logoSize = if (medium) 216.dp else 156.dp
        val titleSize = if (medium) 46.sp else 34.sp
        val subtitleSize = if (medium) 15.sp else 13.sp

        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            if (wide) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SplashMark(scheme, pulse.value, logoSize)
                    Spacer(Modifier.width(32.dp))
                    SplashCopy(scheme, titleSize, subtitleSize, wide = true)
                }
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SplashMark(scheme, pulse.value, logoSize)
                    Spacer(Modifier.height(24.dp))
                    SplashCopy(scheme, titleSize, subtitleSize, wide = false)
                }
            }
        }
        CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 44.dp)
                .size(28.dp),
            color = scheme.primary,
            strokeWidth = 3.dp,
        )
    }
}

@Composable
private fun SplashMark(
    scheme: androidx.compose.material3.ColorScheme,
    scale: Float,
    size: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(scheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Print,
            contentDescription = null,
            tint = scheme.onPrimaryContainer,
            modifier = Modifier.size(size * 0.46f),
        )
    }
}

@Composable
private fun SplashCopy(
    scheme: androidx.compose.material3.ColorScheme,
    titleSize: androidx.compose.ui.unit.TextUnit,
    subtitleSize: androidx.compose.ui.unit.TextUnit,
    wide: Boolean,
) {
    Column(horizontalAlignment = if (wide) Alignment.Start else Alignment.CenterHorizontally) {
        Text(
            text = "طبعتي",
            color = scheme.onSurface,
            fontSize = titleSize,
            fontWeight = FontWeight.Black,
            textAlign = if (wide) TextAlign.Start else TextAlign.Center,
            maxLines = 1,
        )
        Text(
            text = "استوديو الطباعة الذكي 1:1",
            color = scheme.onSurfaceVariant,
            fontSize = subtitleSize,
            fontWeight = FontWeight.SemiBold,
            textAlign = if (wide) TextAlign.Start else TextAlign.Center,
            maxLines = 1,
        )
    }
}
