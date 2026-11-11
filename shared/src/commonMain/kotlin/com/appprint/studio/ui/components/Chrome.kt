package com.appprint.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateBottomPadding
import androidx.compose.foundation.layout.calculateTopPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable

/**
 * Single source of truth for the floating-chrome geometry (top oval bar, bottom pill bar).
 * Screens reserve these insets so content scrolls underneath the floating bars —
 * true edge-to-edge, no opaque color blocks.
 */
object Chrome {
    /** Height of the floating oval title bar (without status-bar inset). */
    val TopBarHeight = 56.dp
    /** Vertical gap between the status bar and the floating top bar. */
    val TopBarGap = 8.dp
    /** Height of the floating bottom pill bar (content area, without nav-bar inset). */
    val BottomBarHeight = 62.dp
    /** Gap between the nav-bar inset and the floating bottom bar. */
    val BottomBarGap = 8.dp
    /** Horizontal gap used to inset the floating bars from the screen edges. */
    val EdgeInset = 12.dp

    /** Total vertical space the top chrome occupies, incl. status-bar inset. */
    @Composable
    fun topReserve(): Dp =
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TopBarGap + TopBarHeight

    /** Total vertical space the bottom chrome occupies, incl. nav-bar inset. */
    @Composable
    fun bottomReserve(): Dp =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + BottomBarGap + BottomBarHeight + 12.dp
}

/**
 * Edge-to-edge horizontal strip: content scrolls under both screen edges and
 * the edges are masked with a horizontal fade — per Material 3 Expressive,
 * the strip melts into the scaffold instead of painting a block.
 */
@Composable
fun EdgeFadeRow(
    edgeColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier.fillMaxWidth()) {
        content()
        // start-edge fade
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.09f)
                .background(Brush.horizontalGradient(listOf(edgeColor, Color.Transparent))),
        )
        // end-edge fade (mirrored manually to work in RTL and LTR)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.09f)
                .align(Alignment.CenterEnd)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, edgeColor))),
        )
    }
}
