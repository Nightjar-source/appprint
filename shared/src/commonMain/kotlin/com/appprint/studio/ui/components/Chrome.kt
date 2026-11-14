package com.appprint.studio.ui.components

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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.window.core.layout.WindowSizeClass

/**
 * «طبعتي» floating-chrome geometry — single source of truth.
 * Top bar floats edge-to-edge with the content scrolling beneath,
 * bottom pill (compact) or floating rail (medium+).
 *
 * Reference heights (design px == dp):
 *  - top bar: 48dp compact portrait · 40dp phone landscape · 46dp medium+ · 44dp expanded
 *  - top gap 6dp · bottom pill 56dp
 */
object Chrome {
    val EdgeInset: Dp = 12.dp
    val NavBarDp: Dp = 56.dp
    val TopBarGapDp: Dp = 6.dp

    /** Adaptive top-bar heights per the design spec. */
    @Composable
    fun topBarHeight(): Dp {
        val info = currentWindowAdaptiveInfoV2()
        val w = info.windowSizeClass
        val isCompact = !w.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
        val isExpanded = w.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
        val heightMediumUp = w.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
        return animateDpAsState(
            targetValue = when {
                isExpanded -> 46.dp
                isCompact && !heightMediumUp -> 40.dp
                isCompact -> 48.dp
                else -> 44.dp
            },
            animationSpec = Motion.defaultDp(),
            label = "topBarHeight",
        ).value
    }

    /** Reserved space above the content so it never hides behind the floating bar. */
    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
    @Composable
    fun topReserve(): Dp =
        WindowInsets.statusBars
            .union(WindowInsets.captionBar)
            .union(WindowInsets.displayCutout)
            .asPaddingValues()
            .calculateTopPadding() + TopBarGapDp + topBarHeight()

    /** Reserved space below the content so it never hides behind the pill. */
    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
    @Composable
    fun bottomReserve(): Dp =
        WindowInsets.navigationBars
            .union(WindowInsets.displayCutout)
            .asPaddingValues()
            .calculateBottomPadding() + 8.dp + NavBarDp + 8.dp

    /** Whether the compact (bottom pill) layout is active. */
    @Composable
    fun isCompact(): Boolean {
        val info = currentWindowAdaptiveInfoV2()
        return !info.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    }

    /** Whether the expanded (rail) layout is active. */
    @Composable
    fun isExpanded(): Boolean {
        val info = currentWindowAdaptiveInfoV2()
        return info.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
    }

    /** Navigation rail width: 64dp for medium/foldable, 72dp for expanded/tablet/DeX. */
    @Composable
    fun railWidth(): Dp {
        val expanded = isExpanded()
        return animateDpAsState(
            targetValue = if (expanded) 72.dp else 64.dp,
            animationSpec = Motion.defaultDp(),
            label = "railWidth",
        ).value
    }
}
