package com.appprint.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

private val IraqiRed = Color(0xFFC8102E)
private val IraqiGreen = Color(0xFF007A3D)
private val Gold = Color(0xFFD9A51F)
private val Paper = Color(0xFFFDFAF1)
private val Ink = Color(0xFF151816)

/** Five-pointed star path centered in a [size]x[size] box. */
private fun starPath(size: Float): Path {
    val path = Path()
    val cx = size / 2f
    val cy = size / 2f
    val outer = size / 2f
    val inner = size / 5f
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) outer else inner
        val angle = Math.toRadians((i * 36 - 90).toDouble()).toFloat()
        val x = cx + radius * cos(angle)
        val y = cy + radius * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/**
 * In-app brand logo — same artwork as the store icon (artwork/icon-1024.png):
 * dark rounded-square, Iraqi flag ID card, gold star, dashed gold cut frame.
 */
@Composable
fun IraqiLogo(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.22f))
            .background(Ink),
        contentAlignment = Alignment.Center,
    ) {
        // the flag ID card
        Box(
            modifier = Modifier
                .size(width = size * 0.66f, height = size * 0.44f)
                .clip(RoundedCornerShape(size * 0.08f))
                .background(Paper),
        ) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().weight(0.32f).background(IraqiRed))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(0.36f)
                        .background(Paper),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(size * 0.16f)
                            .clip(CircleShape)
                            .background(IraqiGreen),
                    )
                }
                Box(Modifier.fillMaxWidth().weight(0.32f).background(IraqiGreen))
            }
        }
        // gold star at the top-end corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(size * 0.05f)
                .size(size * 0.24f),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawPath(starPath(size.minDimension), Gold)
            }
        }
    }
}

/** Small logo for the header — icon only. */
@Composable
fun IraqiLogoHeader(size: Dp = 36.dp, modifier: Modifier = Modifier) = IraqiLogo(size = size, modifier = modifier)
