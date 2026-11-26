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
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass

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

    /** Adaptive top-bar heights per the design spec (measured off LocalWindowInfo — safe on Robolectric). */
    @Composable
    fun topBarHeight(): Dp {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val info = androidx.compose.ui.platform.LocalWindowInfo.current
        return with(density) {
            val w = info.containerSize.width.toDp()
            val h = info.containerSize.height.toDp()
            when {
                w >= 840.dp -> 46.dp
                w < 600.dp && h < 480.dp -> 40.dp
                w < 600.dp -> 48.dp
                else -> 44.dp
            }
        }
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
            .calculateBottomPadding() + 22.dp + NavBarDp

    /** Whether the compact (bottom pill) layout is active (design: width < 768.dp). */
    @Composable
    fun isCompact(): Boolean = !railVisible()

    /** Compose-first adaptive device classes (Kotlin Multiplatform): compact / medium /
     *  expanded plus foldable posture. A 600dp landscape phone is medium and gets the rail;
     *  a foldable in tabletop posture is treated as medium+ even while folded narrow. */
    @Composable
    fun adaptiveInfo() = currentWindowAdaptiveInfo()

    /** Whether the side rail is active — medium+, expanded, or a foldable in tabletop posture. */
    @Composable
    fun railVisible(): Boolean {
        val info = currentWindowAdaptiveInfo()
        val width = info.windowSizeClass.windowWidthSizeClass
        return width == WindowWidthSizeClass.MEDIUM ||
            width == WindowWidthSizeClass.EXPANDED ||
            info.windowPosture.isTabletop
    }

    /** Whether the expanded (widest rail / panel) layout is active. */
    @Composable
    fun isExpanded(): Boolean {
        val info = currentWindowAdaptiveInfo()
        return info.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED
    }

    /** Foldables opened flat (tabletop posture) — used to keep the dual-pane layout. */
    @Composable
    fun isFoldableTabletop(): Boolean = currentWindowAdaptiveInfo().windowPosture.isTabletop

    /** Rail (design: 768.dp) unchanged; width wraps content like the design's aside. */
    fun railWidth(): Dp = 72.dp
}
