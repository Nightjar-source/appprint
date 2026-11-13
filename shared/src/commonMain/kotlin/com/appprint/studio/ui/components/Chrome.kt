package com.appprint.studio.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.appprint.studio.ui.theme.Motion
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.window.core.layout.WindowSizeClass

/**
 * Floating-chrome geometry — single source of truth.
 * The top oval bar and bottom pill/rail float above the edge-to-edge content;
 * screens reserve [topReserve] / [bottomReserve] so content scrolls beneath them.
 *
 * Both reserves are computed from the *current* height class and animate with the
 * effects spring, so rotation / fold / window resize glides — never jumps.
 */
object Chrome {
    val TopBarGap = 8.dp
    val BottomBarGap = 8.dp
    val EdgeInset = 12.dp
    val NavBarHeight = 64.dp

    /** Official adaptive top-bar heights — compact portrait 52dp, phone landscape ≤46dp, medium/expanded 48dp. */
    @Composable
    fun topBarHeight(): Dp {
        val info = currentWindowAdaptiveInfoV2()
        val w = info.windowSizeClass
        val isCompact = !w.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
        val isExpanded = w.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
        val heightMediumUp = w.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
        return animateDpAsState(
            targetValue = when {
                isExpanded -> 48.dp
                isCompact && !heightMediumUp -> 44.dp
                isCompact -> 52.dp
                else -> 46.dp
            },
            animationSpec = Motion.effects(),
            label = "topBarHeight",
        ).value
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun topReserve(): Dp =
        WindowInsets.statusBars
            .union(WindowInsets.captionBar)
            .union(WindowInsets.displayCutout)
            .asPaddingValues()
            .calculateTopPadding() + TopBarGap + topBarHeight()

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun bottomReserve(): Dp =
        WindowInsets.navigationBars
            .union(WindowInsets.displayCutout)
            .asPaddingValues()
            .calculateBottomPadding() + BottomBarGap + 64.dp + 12.dp
}
