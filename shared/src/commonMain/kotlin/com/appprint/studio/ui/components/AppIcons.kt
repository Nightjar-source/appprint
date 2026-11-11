package com.appprint.studio.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Custom brand glyphs (24dp grid, evenOdd/nonzero fills).
 * Kept tiny on purpose — the app avoids material-icons-extended to stay light.
 */
object AppIcons {

    val Print: ImageVector = ImageVector.Builder(
        name = "AppPrint",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        // top paper tray
        path(fill = SolidColor(Color.White), pathFillType = androidx.compose.ui.graphics.vector.PathFillType.NonZero) {
            moveTo(6f, 3f)
            horizontalLineTo(18f)
            verticalLineTo(7f)
            horizontalLineTo(6f)
            close()
        }
        // body with punched output slot (evenOdd)
        path(fill = SolidColor(Color.White), pathFillType = androidx.compose.ui.graphics.vector.PathFillType.EvenOdd) {
            moveTo(19f, 8f)
            horizontalLineTo(5f)
            curveTo(3.34f, 8f, 2f, 9.34f, 2f, 11f)
            verticalLineTo(17f)
            horizontalLineTo(6f)
            verticalLineTo(21f)
            horizontalLineTo(18f)
            verticalLineTo(17f)
            horizontalLineTo(22f)
            verticalLineTo(11f)
            curveTo(22f, 9.34f, 20.66f, 8f, 19f, 8f)
            close()
            moveTo(16f, 15f)
            horizontalLineTo(8f)
            verticalLineTo(20f)
            horizontalLineTo(16f)
            verticalLineTo(15f)
            close()
        }
    }.build()

    val Pdf: ImageVector = ImageVector.Builder(
        name = "AppPdf",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.White), pathFillType = androidx.compose.ui.graphics.vector.PathFillType.EvenOdd) {
            // document with folded corner
            moveTo(6f, 2f)
            horizontalLineTo(14f)
            lineTo(19f, 7f)
            verticalLineTo(21.5f)
            curveTo(19f, 22.33f, 18.33f, 23f, 17.5f, 23f)
            horizontalLineTo(6f)
            curveTo(5.17f, 23f, 4.5f, 22.33f, 4.5f, 21.5f)
            verticalLineTo(3.5f)
            curveTo(4.5f, 2.67f, 5.17f, 2f, 6f, 2f)
            close()
            moveTo(13.5f, 3.5f)
            verticalLineTo(8.5f)
            horizontalLineTo(18.5f)
            close()
            // "PDF" bars
            moveTo(7.5f, 13f)
            horizontalLineTo(16.5f)
            verticalLineTo(14.6f)
            horizontalLineTo(7.5f)
            close()
            moveTo(7.5f, 17f)
            horizontalLineTo(13f)
            verticalLineTo(18.6f)
            horizontalLineTo(7.5f)
            close()
        }
    }.build()

    val Upload: ImageVector = ImageVector.Builder(
        name = "AppUpload",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(11f, 3f)
            horizontalLineTo(13f)
            verticalLineTo(10.17f)
            lineTo(15.6f, 7.74f)
            lineTo(17f, 9.14f)
            lineTo(12f, 14.14f)
            lineTo(7f, 9.14f)
            lineTo(8.4f, 7.74f)
            lineTo(11f, 10.34f)
            close()
            moveTo(4.5f, 17f)
            horizontalLineTo(19.5f)
            verticalLineTo(20.5f)
            horizontalLineTo(4.5f)
            close()
        }
    }.build()

    val Image: ImageVector = ImageVector.Builder(
        name = "AppImage",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.White), pathFillType = androidx.compose.ui.graphics.vector.PathFillType.EvenOdd) {
            moveTo(5f, 3f)
            curveTo(3.34f, 3f, 2f, 4.34f, 2f, 6f)
            verticalLineTo(18f)
            curveTo(2f, 19.66f, 3.34f, 21f, 5f, 21f)
            horizontalLineTo(19f)
            curveTo(20.66f, 21f, 22f, 19.66f, 22f, 18f)
            verticalLineTo(6f)
            curveTo(22f, 4.34f, 20.66f, 3f, 19f, 3f)
            close()
            // sun
            moveTo(8.5f, 8f)
            moveTo(8.5f, 6.5f)
            arcToRelative(1.5f, 1.5f, 0f, true, true, 0.001f, 0f)
            close()
            // mountain
            moveTo(4.5f, 17f)
            lineTo(10f, 11.5f)
            lineTo(14f, 15.5f)
            lineTo(17f, 12.5f)
            lineTo(21.5f, 17f)
            close()
        }
    }.build()

    val Sparkle: ImageVector = ImageVector.Builder(
        name = "AppSparkle",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 2f)
            lineTo(14.5f, 8.6f)
            lineTo(21f, 11f)
            lineTo(14.5f, 13.4f)
            lineTo(12f, 20f)
            lineTo(9.5f, 13.4f)
            lineTo(3f, 11f)
            lineTo(9.5f, 8.6f)
            close()
        }
    }.build()

    val Copy: ImageVector = ImageVector.Builder(
        name = "AppCopy",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(16f, 1f)
            horizontalLineTo(4f)
            curveTo(2.34f, 1f, 1f, 2.34f, 1f, 4f)
            verticalLineTo(18f)
            horizontalLineTo(4f)
            verticalLineTo(4f)
            horizontalLineTo(16f)
            close()
            moveTo(19f, 5f)
            horizontalLineTo(8f)
            curveTo(6.34f, 5f, 5f, 6.34f, 5f, 8f)
            verticalLineTo(20f)
            curveTo(5f, 21.66f, 6.34f, 23f, 8f, 23f)
            horizontalLineTo(19f)
            curveTo(20.66f, 23f, 22f, 21.66f, 22f, 20f)
            verticalLineTo(8f)
            curveTo(22f, 6.34f, 20.66f, 5f, 19f, 5f)
            close()
        }
    }.build()

    /** Half-filled disc — light/dark theme toggle. */
    val Theme: ImageVector = ImageVector.Builder(
        name = "AppTheme",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.White), pathFillType = androidx.compose.ui.graphics.vector.PathFillType.EvenOdd) {
            moveTo(12f, 3f)
            arcToRelative(9f, 9f, 0f, true, false, 0.001f, 0f)
            close()
            moveTo(12f, 5.5f)
            verticalLineTo(18.5f)
            arcToRelative(6.5f, 6.5f, 0f, false, false, 0f, -13f)
            close()
        }
    }.build()

    /** Camera — capture a card photo (body + lens, EvenOdd cut-out). */
    val Camera: ImageVector = ImageVector.Builder(
        name = "AppCamera",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.White), pathFillType = androidx.compose.ui.graphics.vector.PathFillType.EvenOdd) {
            // body with top bump
            moveTo(4f, 7f)
            horizontalLineTo(8.2f)
            lineTo(10.2f, 4.2f)
            curveTo(10.5f, 3.8f, 10.9f, 3.5f, 11.4f, 3.5f)
            horizontalLineTo(12.6f)
            curveTo(13.1f, 3.5f, 13.5f, 3.8f, 13.8f, 4.2f)
            lineTo(15.8f, 7f)
            horizontalLineTo(20f)
            curveTo(21.1f, 7f, 22f, 7.9f, 22f, 9f)
            verticalLineTo(19f)
            curveTo(22f, 20.1f, 21.1f, 21f, 20f, 21f)
            horizontalLineTo(4f)
            curveTo(2.9f, 21f, 2f, 20.1f, 2f, 19f)
            verticalLineTo(9f)
            curveTo(2f, 7.9f, 2.9f, 7f, 4f, 7f)
            close()
            // lens hole
            moveTo(12f, 9.5f)
            arcToRelative(3.9f, 3.9f, 0f, true, true, -0.001f, 0f)
            close()
        }
    }.build()
}
