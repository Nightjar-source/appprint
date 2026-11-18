@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.appprint.studio.screenshot

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.appprint.studio.core.model.AppTheme
import com.appprint.studio.core.platform.AndroidPlatform
import com.appprint.studio.ui.StudioSection
import com.appprint.studio.ui.TabatiViewModel
import com.appprint.studio.ui.theme.TabatiTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the real Compose UI on Robolectric and writes PNGs next to build/screenshots/.
 * These are the ground-truth images for reviewing the port against the HTML design.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h892dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StudioScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun setUp() {
        // the android app sets this in MainActivity.onCreate — Robolectric has no Activity here,
        // so initialize the platform context the same way before rendering
        AndroidPlatform.set(ApplicationProvider.getApplicationContext())
    }

    private fun capture(name: String) {
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val out = File("build/screenshots", "$name.png")
        out.parentFile?.mkdirs()
        out.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Real app screen (canvas + floating chrome) with the seven teaching sample cards. */
    private fun renderStudio(withSamples: Boolean = true): TabatiViewModel {
        val vm = TabatiViewModel()
        rule.setContent {
            com.appprint.studio.ui.AppRoot(viewModel = vm)
        }
        rule.waitForIdle()
        if (withSamples) {
            rule.runOnIdle { vm.addSamples() }
            rule.waitForIdle()
        }
        return vm
    }

    @Test
    fun empty_sheet_first_launch() {
        rule.setContent {
            com.appprint.studio.ui.AppRoot()
        }
        rule.waitForIdle()
        capture("01-empty-first-launch")
    }

    @Test
    fun demo_sheet() {
        renderStudio()
        capture("02-demo-sheet")
    }

    @Test
    fun paper_panel() {
        val vm = renderStudio()
        rule.runOnIdle { vm.setSection(StudioSection.PAPER) }
        rule.waitForIdle()
        capture("03-paper-panel")
    }

    @Test
    fun cards_panel() {
        val vm = renderStudio()
        rule.runOnIdle { vm.setSection(StudioSection.CARDS) }
        rule.waitForIdle()
        capture("04-cards-panel")
    }

    @Test
    fun enhance_panel() {
        val vm = renderStudio()
        rule.runOnIdle {
            // The panel only renders its controls when the selected card carries an image;
            // the teach cards are image-less, so mark one as imported for this capture.
            val first = vm.state.value.cards.first().id
            vm.updateCard(first) { it.copy(imageUri = "file:///sample-card.jpg") }
            vm.selectCard(first)
            vm.setSection(StudioSection.ENHANCE)
        }
        rule.waitForIdle()
        capture("05-enhance-panel")
    }

    @Test
    fun borders_panel() {
        val vm = renderStudio()
        rule.runOnIdle { vm.setSection(StudioSection.BORDERS) }
        rule.waitForIdle()
        capture("06-borders-panel")
    }

    @Test
    fun guide_panel() {
        val vm = renderStudio()
        rule.runOnIdle { vm.setSection(StudioSection.GUIDE) }
        rule.waitForIdle()
        capture("07-guide-panel")
    }

    @Test
    fun diag_settings_dump() {
        val vm = TabatiViewModel()
        val settings = vm.state.value.settings
        rule.setContent {
            TabatiTheme(theme = AppTheme.LIGHT) {
                com.appprint.studio.ui.screens.SheetCanvas(state = vm.state.value, viewModel = vm)
            }
        }
        rule.waitForIdle()
        capture("diag-%.0fx%.0f-%s-%s".format(
            settings.sheetWidthMm,
            settings.sheetHeightMm,
            settings.presetKey,
            settings.orientation.name,
        ))
    }

    @Test
    fun diag_manual_zoom_60() {
        val vm = TabatiViewModel()
        vm.setZoomMode(com.appprint.studio.ui.ZoomMode.MANUAL)
        vm.setManualZoom(60f)
        rule.setContent {
            TabatiTheme(theme = AppTheme.LIGHT) {
                com.appprint.studio.ui.screens.SheetCanvas(state = vm.state.value, viewModel = vm)
            }
        }
        rule.waitForIdle()
        capture("diag-manual-60")
    }
}
