package com.appprint.studio.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Shared canvas transform state — hoisted so the floating top bar (zoom buttons)
 * and the sheet canvas act on the same zoom/pan exactly like a real graphics app.
 */
class CanvasViewState {
    var zoom by mutableFloatStateOf(1f)
    var panX by mutableFloatStateOf(0f)
    var panY by mutableFloatStateOf(0f)

    fun zoomIn() {
        zoom = (zoom * 1.33f).coerceIn(1f, 4.5f)
    }

    fun zoomOut() {
        zoom = (zoom / 1.33f).coerceIn(1f, 4.5f)
    }

    fun reset() {
        zoom = 1f
        panX = 0f
        panY = 0f
    }
}

@Composable
fun rememberCanvasViewState(): CanvasViewState = remember { CanvasViewState() }
