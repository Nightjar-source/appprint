@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.appprint.studio.screenshot

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.appprint.studio.core.platform.AndroidPlatform
import com.appprint.studio.ui.AppRoot
import com.appprint.studio.ui.TabatiViewModel
import com.appprint.studio.ui.ZoomMode
import com.appprint.studio.ui.strings.Str
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Touch-level regression tests: selection, drag direction and zoom must follow the finger 1:1. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h892dp-xxhdpi")
class StudioInteractionTest {

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun setUp() {
        AndroidPlatform.set(ApplicationProvider.getApplicationContext())
    }

    private fun studio(): TabatiViewModel {
        val vm = TabatiViewModel()
        rule.setContent { AppRoot(viewModel = vm) }
        rule.waitForIdle()
        rule.runOnIdle { vm.addSamples() }
        rule.waitForIdle()
        return vm
    }

    @Test
    fun tappingCardSelectsIt() {
        val vm = studio()
        val card = vm.state.value.cards.first()
        rule.runOnIdle { vm.setCardAngle360(card.id, 0f); vm.selectCard(null) }
        rule.waitForIdle()
        rule.onNodeWithTag("card-${card.id}").performTouchInput { down(center); up() }
        rule.waitForIdle()
        assertEquals(card.id, vm.state.value.selectedCardId)
    }

    @Test
    fun draggingRightMovesCardRight_and_upMovesItUp() {
        val vm = studio()
        val card = vm.state.value.cards.first()
        rule.runOnIdle { vm.setCardAngle360(card.id, 0f); vm.selectCard(card.id) }
        rule.waitForIdle()
        rule.onNodeWithTag("card-${card.id}").performTouchInput {
            down(center)
            moveBy(Offset(60f, -45f))
            up()
        }
        rule.waitForIdle()
        val moved = vm.state.value.cards.first { it.id == card.id }
        assertTrue("drag right must increase x (was ${card.xMm}, now ${moved.xMm})", moved.xMm > card.xMm)
        assertTrue("drag up must decrease y (was ${card.yMm}, now ${moved.yMm})", moved.yMm < card.yMm)
        // 60 injected px ≈ 20dp on screen; at fit the sheet is ~0.46 dp-per-mm-scaled,
        // so a 1:1 finger-follow must move the card ≈ 11 mm, not half of that.
        assertTrue("drag must follow the finger 1:1, got ${moved.xMm - card.xMm}mm", moved.xMm - card.xMm > 7f)
    }

    @Test
    fun zoomInButtonEnlargesTheSheet() {
        val vm = studio()
        rule.runOnIdle { vm.zoomFit() }
        rule.waitForIdle()
        rule.onAllNodesWithContentDescription(Str.zoom_in)[0].performTouchInput { down(center); up() }
        rule.waitForIdle()
        val s = vm.state.value
        assertEquals(ZoomMode.MANUAL, s.zoomMode)
        assertTrue("zoom in must raise the percent, got ${s.manualZoomPercent}", s.manualZoomPercent > 100f)
    }
}
