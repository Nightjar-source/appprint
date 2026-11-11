package com.appprint.studio.core.arrange

import com.appprint.studio.core.model.CardItem
import com.appprint.studio.core.model.SheetSettings
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * UI-free nesting / packing engine for placing cards on sheets at true 1:1 scale.
 *
 * Two modes:
 *  - [com.appprint.studio.core.model.ArrangeMode.GRID] — predictable rows of uniform cells
 *    (faithful port of the original «طابق حجم» tool), row-wrap with group breaks.
 *  - [com.appprint.studio.core.model.ArrangeMode.COMFORTABLE] — real bin packing:
 *    **MaxRects best-short-side-fit** with 90° rotation candidates. Each card is tried
 *    in both orientations and the placement that minimizes the leftover short side of
 *    the smallest usable free rectangle wins, then the free space is re-split
 *    (maximal-rectangles algorithm), so empty gaps get filled — cards sit side by side
 *    according to the empty spaces actually present on the sheet, at true 1:1 scale.
 */
object SheetArranger {

    data class Result(
        val cards: List<CardItem>,
        val pages: Int,
    )

    /** @return arranged cards (with x/y/page set) + number of sheets required. */
    fun arrange(input: List<CardItem>, settings: SheetSettings): Result {
        if (input.isEmpty()) return Result(emptyList(), 1)

        val margin = settings.marginMm.coerceIn(0f, settings.sheetWidthMm / 3f)
        val contentW = maxOf(20f, settings.sheetWidthMm - margin * 2f)
        val contentH = maxOf(20f, settings.sheetHeightMm - margin * 2f)
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

        return if (settings.mode == com.appprint.studio.core.model.ArrangeMode.GRID) {
            arrangeGrid(normalized, settings, margin, contentW, contentH, gap)
        } else {
            arrangeMaxRects(normalized, margin, contentW, contentH, gap)
        }
    }

    /** Consume the footprint of [placed] (card + full surrounding gap) from the free space. */
    private fun consumeRect(free: MutableList<FreeRect>, placed: CardItem, gap: Float) {
        splitFreeRects(
            free,
            FreeRect(
                placed.xMm - gap,
                placed.yMm - gap,
                placed.effectiveWidth + gap * 2f,
                placed.effectiveHeight + gap * 2f,
            ),
        )
    }

    // ------------------------------------------------------------ GRID (legacy shelf rows)

    private fun arrangeGrid(
        normalized: List<CardItem>,
        settings: SheetSettings,
        margin: Float,
        contentW: Float,
        contentH: Float,
        gap: Float,
    ): Result {
        val gridColW = normalized.maxOfOrNull { it.effectiveWidth } ?: 0f
        val gridRowH = normalized.maxOfOrNull { it.effectiveHeight } ?: 0f

        var cursorX = margin
        var cursorY = margin
        var rowHeight = 0f
        var page = 0
        val out = ArrayList<CardItem>(normalized.size)
        var lastGroup = ""

        for (card in normalized) {
            val cellW = gridColW
            val cellH = gridRowH
            val group = card.typeKey

            // Group break: start a new row (and possibly a new page)
            if (settings.smartGroup && lastGroup.isNotEmpty() && group != lastGroup) {
                cursorX = margin
                cursorY += rowHeight + gap
                rowHeight = 0f
                if (out.isNotEmpty() && cursorY + cellH > settings.sheetHeightMm - margin) {
                    page += 1
                    cursorY = margin
                }
            }
            lastGroup = group

            // Line wrap
            if (cursorX + cellW > settings.sheetWidthMm - margin && cursorX > margin) {
                cursorX = margin
                cursorY += rowHeight + gap
                rowHeight = 0f
            }
            // Page overflow
            if (cursorY + cellH > settings.sheetHeightMm - margin && out.isNotEmpty()) {
                page += 1
                cursorX = margin
                cursorY = margin
                rowHeight = 0f
            }

            val placeX = cursorX + (gridColW - card.effectiveWidth) / 2f
            val placed = card.copy(
                xMm = round1(placeX),
                yMm = round1(cursorY),
                page = page,
            )
            out.add(placed)
            cursorX += cellW + gap
            rowHeight = maxOf(rowHeight, cellH)
        }

        val sorted = out.sortedWith(compareBy({ it.page }, { it.yMm }, { it.xMm }))
        return Result(sorted, page + 1)
    }

    // ------------------------------------------------------------ MaxRects bin packing

    /** Free rectangle on the sheet, in millimetres. */
    private data class FreeRect(val x: Float, val y: Float, val w: Float, val h: Float)

    private fun arrangeMaxRects(
        cards: List<CardItem>,
        margin: Float,
        contentW: Float,
        contentH: Float,
        gap: Float,
    ): Result {
        var page = 0
        var free = mutableListOf(FreeRect(margin, margin, contentW, contentH))
        var placedOnPage = 0
        val out = ArrayList<CardItem>(cards.size)

        for (card in cards) {
            var placed = findBestPlacement(card, free, page, gap)
            if (placed == null) {
                if (placedOnPage > 0) {
                    // start a new sheet and retry there
                    page += 1
                    free = mutableListOf(FreeRect(margin, margin, contentW, contentH))
                    placedOnPage = 0
                    placed = findBestPlacement(card, free, page, gap)
                }
                if (placed == null) {
                    // Last-resort placement at the sheet origin. Its footprint MUST be consumed
                    // so subsequent cards never stack on top of it. (fit() guarantees it fits.)
                    placed = card.copy(xMm = margin, yMm = margin, page = page).also { p ->
                        consumeRect(free, p, gap)
                    }
                }
            }
            out.add(placed)
            placedOnPage += 1
        }

        out.sortWith(compareBy({ it.page }, { it.yMm }, { it.xMm }))
        return Result(out, page + 1)
    }

    /** Best short-side-fit placement among both orientations; splits free rects on success. */
    private fun findBestPlacement(
        card: CardItem,
        free: MutableList<FreeRect>,
        page: Int,
        gap: Float,
    ): CardItem? {
        // candidate orientations: user's rotation wins ties; we always try both
        val candidates = buildList {
            add(card.rotation)
            add(if (card.rotation == 90) 0 else 90)
        }

        var bestX = 0f
        var bestY = 0f
        var bestRot = card.rotation
        var bestShort = Float.MAX_VALUE
        var bestArea = Float.MAX_VALUE
        var found = false

        for (rot in candidates) {
            val w = if (rot == 90) card.heightMm else card.widthMm
            val h = if (rot == 90) card.widthMm else card.heightMm
            for (rect in free) {
                if (w <= rect.w + 0.001f && h <= rect.h + 0.001f) {
                    val leftoverW = rect.w - w
                    val leftoverH = rect.h - h
                    val short = min(leftoverW, leftoverH)
                    val area = rect.w * rect.h - w * h // residual free area
                    if (short < bestShort - 0.001f ||
                        (abs(short - bestShort) <= 0.001f && area < bestArea) ||
                        (abs(short - bestShort) <= 0.001f && abs(area - bestArea) <= 0.001f && rot == card.rotation)
                    ) {
                        bestShort = short
                        bestArea = area
                        bestX = rect.x
                        bestY = rect.y
                        bestRot = rot
                        found = true
                    }
                }
            }
        }

        if (!found) return null

        // split the free space around the used rect — inflates by the full gap on
        // ALL sides so every pair of cards is separated by at least `gap` (8mm in COMFORTABLE)
        val placed = card.copy(
            xMm = round1(bestX),
            yMm = round1(bestY),
            rotation = bestRot,
            page = page,
        )
        consumeRect(free, placed, gap)

        return placed
    }

    /** Maximal-rectangles split: every free rect intersecting [used] is split into up to 4 leftovers. */
    private fun splitFreeRects(free: MutableList<FreeRect>, used: FreeRect) {
        val additions = mutableListOf<FreeRect>()
        val iterator = free.iterator()
        while (iterator.hasNext()) {
            val r = iterator.next()
            if (r.x >= used.x + used.w || used.x >= r.x + r.w) continue
            if (r.y >= used.y + used.h || used.y >= r.y + r.h) continue

            // left leftover
            if (r.x < used.x) {
                val w = used.x - r.x
                if (w > 0.5f) additions.add(FreeRect(r.x, r.y, w, r.h))
            }
            // right leftover
            if (r.x + r.w > used.x + used.w) {
                val right = used.x + used.w
                val w = r.x + r.w - right
                if (w > 0.5f) additions.add(FreeRect(right, r.y, w, r.h))
            }
            // top leftover
            if (r.y < used.y) {
                val h = used.y - r.y
                if (h > 0.5f) additions.add(FreeRect(r.x, r.y, r.w, h))
            }
            // bottom leftover
            if (r.y + r.h > used.y + used.h) {
                val bottom = used.y + used.h
                val h = r.y + r.h - bottom
                if (h > 0.5f) additions.add(FreeRect(r.x, bottom, r.w, h))
            }
            iterator.remove()
        }
        free.addAll(additions)
        pruneFreeRects(free)
    }

    /** Drop any free rect that is fully contained in another. */
    private fun pruneFreeRects(free: MutableList<FreeRect>) {
        if (free.size < 2) return
        val sorted = free.sortedByDescending { it.w * it.h }
        val keep = ArrayList<FreeRect>(sorted.size)
        for (r in sorted) {
            val contained = keep.any { k ->
                k.x <= r.x && k.y <= r.y && k.x + k.w >= r.x + r.w && k.y + k.h >= r.y + r.h
            }
            if (!contained) keep.add(r)
        }
        free.clear()
        free.addAll(keep)
    }

    // ------------------------------------------------------------ shared helpers

    /**
     * Ensure a card fits inside the content area without changing its proportions:
     * try the other orientation first, then scale down preserving aspect ratio.
     * Guarantees effectiveWidth ≤ contentW AND effectiveHeight ≤ contentH.
     */
    private fun fit(card: CardItem, contentW: Float, contentH: Float): CardItem {
        val effW = card.effectiveWidth
        val effH = card.effectiveHeight
        if (effW <= contentW && effH <= contentH) return card

        val rotW = effH
        val rotH = effW
        if (rotW <= contentW && rotH <= contentH) {
            // Flip user rotation 90° so the long side follows the sheet
            return card.copy(rotation = if (card.rotation == 90) 0 else 90)
        }
        val scale = minOf(contentW / effW, contentH / effH, 1f)
        return card.copy(
            widthMm = round1(card.widthMm * scale),
            heightMm = round1(card.heightMm * scale),
        )
    }

    private fun round1(v: Float): Float = (v * 10f).roundToInt() / 10f
}
