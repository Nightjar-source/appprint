package com.appprint.studio.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.appprint.studio.ui.theme.Motion
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Rubber-band overscroll for the page strip: when the row is already at an edge, a further
 * drag moves the whole strip toward the screen edge with resistance, then springs back to
 * its natural position. Normal scrolling inside the row is left untouched.
 */
@Composable
fun Modifier.overscrollRubberBand(state: LazyListState): Modifier {
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val resist = 0.42f
    val maxPull = 120f

    fun atStart(): Boolean = state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0

    fun atEnd(): Boolean {
        val info = state.layoutInfo
        val last = info.visibleItemsInfo.lastOrNull() ?: return true
        return last.index == info.totalItemsCount - 1 &&
            last.offset + last.size <= info.viewportEndOffset + 1
    }

    return this
        .graphicsLayer { translationX = offset.value }
        .pointerInput(state) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                var pulled = false
                var startX = down.position.x
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    val dx = change.position.x - startX
                    val beyondStart = atStart() && dx > 0f
                    val beyondEnd = atEnd() && dx < 0f
                    if (beyondStart || beyondEnd || pulled) {
                        pulled = true
                        change.consume()
                        val raw = if (beyondStart) dx else dx
                        val resisted = (raw * resist).coerceIn(-maxPull, maxPull)
                        scope.launch { offset.snapTo(resisted) }
                    }
                    if (!change.pressed) break
                }
                if (pulled && abs(offset.value) > 0.5f) {
                    scope.launch { offset.animateTo(0f, animationSpec = Motion.default()) }
                }
            }
        }
}
