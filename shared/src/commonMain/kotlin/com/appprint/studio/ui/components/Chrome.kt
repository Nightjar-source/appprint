package com.appprint.studio.ui.components

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Floating-chrome geometry — single source of truth.
 * The top oval bar and bottom pill/rail float above the edge-to-edge content;
 * screens reserve [topReserve] / [bottomReserve] so content scrolls beneath them.
 */
object Chrome {
    val TopBarHeight = 56.dp
    val TopBarGap = 8.dp
    val BottomBarHeight = 64.dp
    val BottomBarGap = 8.dp
    val EdgeInset = 12.dp

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun topReserve(): Dp =
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TopBarGap + TopBarHeight

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun bottomReserve(): Dp =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + BottomBarGap + BottomBarHeight + 12.dp
}
