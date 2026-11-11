package com.appprint.studio.core.arrange

import com.appprint.studio.core.model.CardItem
import com.appprint.studio.core.model.SheetSettings
import kotlin.math.roundToInt

/**
 * UI-free nesting / packing engine for placing cards on sheets at true 1:1 scale.
 * Faithful port of «طابق حجم» logic from the original HTML tool:
 *  - sort (smart groups by frequency, then area) or by area
 *  - auto-rotate cards that are wider than they are tall when that fits better
 *  - shrink-to-fit when a card exceeds the sheet content width
 *  - row wrap, page overflow, group-break newlines, grid-cell centering
 */
object SheetArranger {

    data class Result(
        val cards: List<CardItem>,
        val pages: Int,
    )

    /** @return arranged cards (with x/y/page set) + number of sheets required. */
    fun arrange(input: List<CardItem>, settings: SheetSettings): Result {
        if (input.isEmpty()) return Result(emptyList(), 1)

        val sheetW = settings.sheetWidthMm
        val sheetH = settings.sheetHeightMm
        val margin = settings.marginMm.coerceIn(0f, sheetW / 3f)
        val contentW = maxOf(20f, sheetW - margin * 2f)
        val contentH = maxOf(20f, sheetH - margin * 2f)
        val gap = settings.effectiveGapMm

        val ordered = if (settings.smartGroup) {
            val counts = input.groupingBy { it.typeKey }.eachCount()
            input.sortedWith { a, b ->
                val countDiff = (counts[b.typeKey] ?: 0) - (counts[a.typeKey] ?: 0)
                if (countDiff != 0) countDiff
                else (b.widthMm * b.heightMm).compareTo(a.widthMm * a.heightMm)
            }
        } else {
            input.sortedByDescending { it.widthMm * it.heightMm }
        }

        val normalized = ordered.map { fit(it, contentW, contentH) }

        val gridColW = if (settings.mode == com.appprint.studio.core.model.ArrangeMode.GRID)
            normalized.maxOfOrNull { it.effectiveWidth } ?: 0f else 0f
        val gridRowH = if (settings.mode == com.appprint.studio.core.model.ArrangeMode.GRID)
            normalized.maxOfOrNull { it.effectiveHeight } ?: 0f else 0f

        var cursorX = margin
        var cursorY = margin
        var rowHeight = 0f
        var page = 0
        var maxUsedBottom = margin
        val out = ArrayList<CardItem>(normalized.size)
        var lastGroup = ""

        for (card in normalized) {
            val cellW = if (settings.mode == com.appprint.studio.core.model.ArrangeMode.GRID) gridColW else card.effectiveWidth
            val cellH = if (settings.mode == com.appprint.studio.core.model.ArrangeMode.GRID) gridRowH else card.effectiveHeight
            val group = card.typeKey

            // Group break: start a new row (and possibly a new page)
            if (settings.smartGroup && lastGroup.isNotEmpty() && group != lastGroup) {
                cursorX = margin
                cursorY += rowHeight + gap
                rowHeight = 0f
                if (out.isNotEmpty() && cursorY + cellH > sheetH - margin) {
                    page += 1
                    cursorY = margin
                }
            }
            lastGroup = group

            // Line wrap
            if (cursorX + cellW > sheetW - margin && cursorX > margin) {
                cursorX = margin
                cursorY += rowHeight + gap
                rowHeight = 0f
            }
            // Page overflow
            if (cursorY + cellH > sheetH - margin && out.isNotEmpty()) {
                page += 1
                cursorX = margin
                cursorY = margin
                rowHeight = 0f
            }

            val placeX = if (settings.mode == com.appprint.studio.core.model.ArrangeMode.GRID)
                cursorX + (gridColW - card.effectiveWidth) / 2f else cursorX
            val placed = card.copy(
                xMm = round1(placeX),
                yMm = round1(cursorY),
                page = page,
            )
            out.add(placed)
            cursorX += cellW + gap
            rowHeight = maxOf(rowHeight, cellH)
            maxUsedBottom = maxOf(maxUsedBottom, cursorY + cellH)
        }

        val sorted = out.sortedWith(compareBy({ it.page }, { it.yMm }, { it.xMm }))
        return Result(sorted, page + 1)
    }

    /** Rotate or shrink a card so it fits inside the content area without changing proportions. */
    private fun fit(card: CardItem, contentW: Float, contentH: Float): CardItem {
        val effW = card.effectiveWidth
        val effH = card.effectiveHeight
        if (effW <= contentW) return card

        val canRotate = effH <= contentW && effW <= contentH && effH < effW
        return if (canRotate) {
            // Flip user rotation 90° so the long side follows the sheet
            card.copy(rotation = if (card.rotation == 90) 0 else 90)
        } else {
            val scale = contentW / effW
            card.copy(
                widthMm = round1(card.widthMm * scale),
                heightMm = round1(card.heightMm * scale),
            )
        }
    }

    private fun round1(v: Float): Float = (v * 10f).roundToInt() / 10f
}
