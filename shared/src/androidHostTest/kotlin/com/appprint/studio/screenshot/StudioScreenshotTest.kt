@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.appprint.studio.screenshot

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.appprint.studio.core.platform.AndroidPlatform
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

    @Test
    fun empty_sheet_first_launch() {
        rule.setContent {
            com.appprint.studio.ui.AppRoot()
        }
        rule.waitForIdle()
        capture("01-empty-first-launch")
    }

    @Test
    fun paper_section() {
        rule.setContent {
            com.appprint.studio.ui.AppRoot()
        }
        rule.waitForIdle()
        capture("02-default")
    }
}
