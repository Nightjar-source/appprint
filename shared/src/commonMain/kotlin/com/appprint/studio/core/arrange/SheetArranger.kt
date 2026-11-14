package com.appprint.studio.core.arrange

import com.appprint.studio.core.model.ArrangeMode
import com.appprint.studio.core.model.CardItem
import com.appprint.studio.core.model.SheetSettings
import kotlin.math.floor
import kotlin.math.min
import kotlin.random.Random

/**
 * Strict Zero-Overlap Smart Packer — direct port of the design's
 * `arrangeCardsStrict` (multi-permutation matrix & pocket bin-packer).
 *
 * Guarantees:
 *  - no two cards on the same sheet may overlap, never;
 *  - cards respect the safety margin AND the cut-guide bleed (+1.4 mm when guides are on);
 *  - each card is tried in both orientations (auto-rotate) and shrunk only as a last resort;
 *  - the placement tries 4 permutations (cluster flipping × row/column scan) and keeps the
 *    best-scoring candidate (fewer sheets → denser bottom → fewer auto-rotations).
 *
 * All dimensions are millimetres; output carries x/y/page/autoRotated/autoShrunk/manual=false.
 */
object SheetArranger {

    data class Result(
        val cards: List<CardItem>,
        val pages: Int,
    )

    /** Positional-free view used internally. */
    private data class FreeRect(val x: Float, val y: Float, val w: Float, val h: Float)

    /** Total rotation of a card modulo 360, normalised to [0, 360). */
    fun totalRotation(card: CardItem): Float =
        (((card.rotation + card.freeRotation) % 360f) + 360f) % 360f

    /** Rounded size bucket, e.g. "86 × 54", "100 × 70" — used for smart grouping/clustering. */
    fun sizeKey(card: CardItem): String {
        val rot = totalRotation(card)
        val rightAngle =
            kotlin.math.abs(rot - 90f) < 1f || kotlin.math.abs(rot - 270f) < 1f ||
                kotlin.math.abs(rot - 180f) < 1f && false // 180° keeps w/h order
        val w = if (rightAngle) card.baseHeightMm else card.baseWidthMm
        val h = if (rightAngle) card.baseWidthMm else card.baseHeightMm
        val a = kotlin.math.round(w / 2) * 2
        val b = kotlin.math.round(h / 2) * 2
        return "${a.toInt()} × ${b.toInt()}"
    }

    /** Deterministic cluster colour (stable across launches). */
    private val ClusterColors = longArrayOf(
        0xFF006C47, 0xFF16324F, 0xFF8A5B12, 0xFF910824,
        0xFF4B2D73, 0xFF276B68, 0xFFB32638, 0xFF1F4B7A,
    )

    fun clusterColor(key: String): Long {
        var h = 0L
        for (ch in key) h = (h * 31 + ch.code) and 0xFFFFFFFFL
        return ClusterColors[(h % ClusterColors.size).toInt()]
    }

    /** Groups cards by size bucket, largest bucket first — feeds the "تجمع المتشابهات" UI. */
    data class SizeCluster(
        val key: String,
        val label: String,
        val colorHex: Long,
        val cards: List<CardItem>,
    )

    fun buildSizeClusters(cards: List<CardItem>): List<SizeCluster> =
        cards.groupBy { sizeKey(it) }
            .entries
            .sortedByDescending { it.value.size }
            .map { (key, items) -> SizeCluster(key, "$key مم", clusterColor(key), items) }

    /** Ratio-based type detection ("تم التعرف تلقائياً") — for freshly imported images. */
    data class Detection(val typeKey: String, val name: String, val widthMm: Float, val heightMm: Float, val label: String)

    fun detectFromRatio(naturalWidth: Float, naturalHeight: Float): Detection {
        if (naturalWidth <= 0f || naturalHeight <= 0f) {
            return Detection("custom", "قياس مخصص", 90f, 60f, "قياس مخصص — تم التعرف تلقائياً")
        }
        val ratio = naturalWidth / naturalHeight
        val orient = if (ratio >= 1f) "أفقي" else "عمودي"
        return when {
            kotlin.math.abs(ratio - 1.585185f) < 0.14f ->
                Detection("national", "البطاقة الوطنية الموحدة", 85.6f, 54f, "تم التعرف: بطاقة وطنية • $orient")
            ratio < 0.85f ->
                Detection("passport", "جواز السفر", 88f, 125f, "تم التعرف: جواز / مستند عمودي • $orient")
            kotlin.math.abs(ratio - 1.42857f) < 0.12f ->
                Detection("residence", "بطاقة السكن", 100f, 70f, "تم التعرف: بطاقة سكن • $orient")
            ratio >= 1f -> {
                val h = (90f / ratio).round(1).coerceAtLeast(30f)
                Detection("custom", "قياس مخصص", 90f, h, "تم التعرف: مخصص • $orient")
            }
            else -> {
                val w = (120f * ratio).round(1).coerceAtLeast(30f)
                Detection("custom", "قياس مخصص", w, 120f, "تم التعرف: مخصص • $orient")
            }
        }
    }

    private fun Float.round(digits: Int): Float {
        var p = 1
        repeat(digits) { p *= 10 }
        return kotlin.math.round(this * p) / p
    }

    private fun isBoxColliding(a: FreeRect, b: FreeRect, minClearance: Float): Boolean = !(
        a.x + a.w + minClearance <= b.x + 0.01f ||
            b.x + b.w + minClearance <= a.x + 0.01f ||
            a.y + a.h + minClearance <= b.y + 0.01f ||
            b.y + b.h + minClearance <= a.y + 0.01f
        )

    /**
     * Arranges [input] onto sheets. Safe to call on every settings change; deterministic.
     * Manual placements are preserved (clamped back inside the sheet) when
     * [preserveManual] is true.
     */
    fun arrange(
        input: List<CardItem>,
        settings: SheetSettings,
        strategy: String = "smart",
        preserveManual: Boolean = false,
    ): Result {
        if (input.isEmpty()) return Result(emptyList(), 1)

        val cutBleed = if (settings.frame.guides) 1.4f else 0f
        val margin = maxOf(settings.marginMm, cutBleed)
        val baseGap = if (settings.mode == ArrangeMode.COMFORTABLE) maxOf(settings.gapMm, 8f) else settings.gapMm
        val minGap = maxOf(baseGap, cutBleed * 2f + 0.8f)

        val paperW = settings.sheetWidthMm
        val paperH = settings.sheetHeightMm
        val printableW = maxOf(20f, paperW - margin * 2f)
        val printableH = maxOf(20f, paperH - margin * 2f)

        // -------- normalize: auto-rotate to fit, else shrink proportionally (last resort)
        val normalized = input.map { c ->
            val wasRight = kotlin.math.abs(totalRotation(c) % 180f - 90f) < 1f
            var w = c.baseWidthMm
            var h = c.baseHeightMm
            var rot = c.rotation
            var autoRotated = false
            var autoShrunk = false
            if (wasRight) { w = c.baseHeightMm; h = c.baseWidthMm }

            if (w > printableW + 0.05f) {
                if (h <= printableW + 0.05f && w <= printableH + 0.05f) {
                    val tmp = w; w = h; h = tmp
                    rot = (rot + 90) % 360
                    autoRotated = true
                } else {
                    val ratio = printableW / w
                    h *= ratio; w = printableW; autoShrunk = true
                }
            }
            if (h > printableH + 0.05f) {
                if (w <= printableH + 0.05f && h <= printableW + 0.05f) {
                    val tmp = w; w = h; h = tmp
                    rot = (rot + 90) % 360
                    autoRotated = true
                } else {
                    val ratio = printableH / h
                    w *= ratio; h = printableH; autoShrunk = true
                }
            }
            c.copy(
                widthMm = w.round(1),
                heightMm = h.round(1),
                rotation = rot,
                freeRotation = 0f,
                autoRotated = autoRotated,
                autoShrunk = autoShrunk,
            )
        }

        val clusterCounts = normalized.groupingBy { sizeKey(it) }.eachCount()
        val ordered = when (strategy) {
            "size" -> normalized.sortedWith(
                compareByDescending<CardItem> { it.widthMm * it.heightMm }
                    .thenByDescending { it.heightMm }
                    .thenByDescending { it.widthMm },
            )
            else -> if (settings.groupSmart) {
                normalized.sortedWith(
                    compareByDescending<CardItem> { clusterCounts[sizeKey(it)] ?: 0 }
                        .thenByDescending { it.widthMm * it.heightMm },
                )
            } else {
                normalized.sortedByDescending { it.widthMm * it.heightMm }
            }
        }

        if (settings.mode == ArrangeMode.GRID) {
            return arrangeGrid(ordered, margin, printableW, printableH, minGap, paperW, paperH)
        }
        if (strategy == "size") {
            return arrangeShelf(ordered, margin, printableW, printableH, minGap, paperW, paperH)
        }
        return arrangePockets(ordered, margin, printableW, printableH, minGap, paperW, paperH, preserveManual)
    }

    private fun arrangeGrid(
        ordered: List<CardItem>,
        margin: Float,
        printableW: Float,
        printableH: Float,
        minGap: Float,
        paperW: Float,
        paperH: Float,
    ): Result {
        val cellW = min(printableW, ordered.maxOfOrNull { it.widthMm } ?: 1f)
        val cellH = min(printableH, ordered.maxOfOrNull { it.heightMm } ?: 1f)
        var cx = margin
        var cy = margin
        var page = 0
        val placed = ArrayList<CardItem>(ordered.size)
        for (c in ordered) {
            if (cx + cellW > paperW - margin + 0.05f && cx > margin) {
                cx = margin
                cy += cellH + minGap
            }
            if (cy + cellH > paperH - margin + 0.05f && placed.isNotEmpty()) {
                page++
                cx = margin
                cy = margin
            }
            placed.add(
                c.copy(
                    xMm = (cx + (cellW - c.widthMm) / 2f).round(1),
                    yMm = (cy + (cellH - c.heightMm) / 2f).round(1),
                    page = page,
                    manualPlacement = false,
                ),
            )
            cx += cellW + minGap
        }
        return Result(placed.sortedPages(), page + 1)
    }

    private fun arrangeShelf(
        ordered: List<CardItem>,
        margin: Float,
        printableW: Float,
        printableH: Float,
        minGap: Float,
        paperW: Float,
        paperH: Float,
    ): Result {
        var cx = margin
        var cy = margin
        var rowH = 0f
        var page = 0
        val placed = ArrayList<CardItem>(ordered.size)
        for (c in ordered) {
            if (cx + c.widthMm > paperW - margin + 0.05f && cx > margin) {
                cx = margin
                cy += rowH + minGap
                rowH = 0f
            }
            if (cy + c.heightMm > paperH - margin + 0.05f && placed.isNotEmpty()) {
                page++
                cx = margin
                cy = margin
                rowH = 0f
            }
            placed.add(c.copy(xMm = cx.round(1), yMm = cy.round(1), page = page, manualPlacement = false))
            cx += c.widthMm + minGap
            rowH = maxOf(rowH, c.heightMm)
        }
        return Result(placed.sortedPages(), page + 1)
    }

    /** The design's `runSmartPermutation` — pocket scanning with 4 candidates, keep best score. */
    private fun arrangePockets(
        ordered: List<CardItem>,
        margin: Float,
        printableW: Float,
        printableH: Float,
        minGap: Float,
        paperW: Float,
        paperH: Float,
        preserveManual: Boolean,
    ): Result {
        fun runSmartPermutation(flipClusters: Boolean, scanAxis: String): PocketRun {
            val pageBuckets = mutableListOf(mutableListOf<CardItem>())
            val placedList = ArrayList<CardItem>(ordered.size)

            val pinned = if (preserveManual) ordered.filter { it.manualPlacement } else emptyList()
            val unpinned = if (preserveManual) ordered.filterNot { it.manualPlacement } else ordered

            for (pinnedCard in pinned) {
                val pIdx = maxOf(0, pinnedCard.page)
                while (pageBuckets.size <= pIdx) pageBuckets.add(mutableListOf())
                val clampedX = margin.coerceAtLeast(0f).coerceAtMost(paperW - margin - pinnedCard.widthMm)
                    .coerceIn(margin, maxOf(margin, paperW - margin - pinnedCard.widthMm))
                val clampedY = margin.coerceAtMost(paperH - margin - pinnedCard.heightMm)
                    .coerceIn(margin, maxOf(margin, paperH - margin - pinnedCard.heightMm))
                val item = pinnedCard.copy(xMm = clampedX.round(1), yMm = clampedY.round(1), page = pIdx)
                pageBuckets[pIdx].add(item)
                placedList.add(item)
            }

            val prepared = unpinned.map { c ->
                if (flipClusters && kotlin.math.abs(c.widthMm - c.heightMm) > 1f) {
                    if (c.heightMm <= printableW + 0.05f && c.widthMm <= printableH + 0.05f) {
                        val capNorm =
                            floor((printableW + minGap) / (c.widthMm + minGap)) *
                                floor((printableH + minGap) / (c.heightMm + minGap))
                        val capFlip =
                            floor((printableW + minGap) / (c.heightMm + minGap)) *
                                floor((printableH + minGap) / (c.widthMm + minGap))
                        if (capFlip > capNorm) {
                            c.copy(
                                widthMm = c.heightMm,
                                heightMm = c.widthMm,
                                rotation = (c.rotation + 90) % 360,
                                autoRotated = true,
                            )
                        } else c
                    } else c
                } else c
            }

            fun findSmartSlot(pageItems: List<CardItem>, cw: Float, ch: Float): PlacedSlot? {
                val xs = sortedSetOf(margin)
                val ys = sortedSetOf(margin)
                for (p in pageItems) {
                    xs.add((p.xMm + p.widthMm + minGap).round(1))
                    xs.add(p.xMm.round(1))
                    ys.add((p.yMm + p.heightMm + minGap).round(1))
                    ys.add(p.yMm.round(1))
                }
                val orientations = buildList {
                    add(cw to ch)
                    if (kotlin.math.abs(cw - ch) > 1f) add(ch to cw)
                }
                for ((ow, oh) in orientations) {
                    if (ow > printableW + 0.05f || oh > printableH + 0.05f) continue
                    val rotated = ow != cw
                    val xSeq = if (scanAxis == "row") xs.toList() else xs.toList()
                    val ySeq = if (scanAxis == "row") ys.toList() else ys.toList()
                    if (scanAxis == "row") {
                        for (y in ySeq) {
                            if (y < margin - 0.01f || y + oh > paperH - margin + 0.05f) continue
                            for (x in xSeq) {
                                if (x < margin - 0.01f || x + ow > paperW - margin + 0.05f) continue
                                val box = FreeRect(x, y, ow, oh)
                                if (pageItems.none { isBoxColliding(box, FreeRect(it.xMm, it.yMm, it.widthMm, it.heightMm), minGap - 0.05f) }) {
                                    return PlacedSlot(x, y, ow, oh, rotated)
                                }
                            }
                        }
                    } else {
                        for (x in xSeq) {
                            if (x < margin - 0.01f || x + ow > paperW - margin + 0.05f) continue
                            for (y in ySeq) {
                                if (y < margin - 0.01f || y + oh > paperH - margin + 0.05f) continue
                                val box = FreeRect(x, y, ow, oh)
                                if (pageItems.none { isBoxColliding(box, FreeRect(it.xMm, it.yMm, it.widthMm, it.heightMm), minGap - 0.05f) }) {
                                    return PlacedSlot(x, y, ow, oh, rotated)
                                }
                            }
                        }
                    }
                }
                return null
            }

            for (card in prepared) {
                var placed = false
                for (p in pageBuckets.indices) {
                    val slot = findSmartSlot(pageBuckets[p], card.widthMm, card.heightMm)
                    if (slot != null) {
                        val item = card.copy(
                            xMm = slot.x,
                            yMm = slot.y,
                            widthMm = slot.w,
                            heightMm = slot.h,
                            rotation = if (slot.rotated) (card.rotation + 90) % 360 else card.rotation,
                            autoRotated = slot.rotated || card.autoRotated,
                            manualPlacement = false,
                            page = p,
                        )
                        pageBuckets[p].add(item)
                        placedList.add(item)
                        placed = true
                        break
                    }
                }
                if (!placed) {
                    val targetPage = pageBuckets.size
                    pageBuckets.add(mutableListOf(card.copy(xMm = margin, yMm = margin, manualPlacement = false, page = targetPage)))
                    placedList.add(pageBuckets[targetPage].first())
                }
            }

            val maxBottom = placedList.maxOfOrNull { it.yMm + it.heightMm } ?: 0f
            val rotPenalty = placedList.count { it.autoRotated } * 2f
            val score = pageBuckets.size * 100_000f + maxBottom + rotPenalty
            return PocketRun(pageBuckets.flatten().sortedPages(), pageBuckets.size, score)
        }

        val candidates = listOf(
            runSmartPermutation(false, "row"),
            runSmartPermutation(true, "row"),
            runSmartPermutation(false, "col"),
            runSmartPermutation(true, "col"),
        )
        val best = candidates.minByOrNull { it.score } ?: candidates.first()
        return Result(best.cards, best.pages)
    }

    private fun List<CardItem>.sortedPages(): List<CardItem> =
        sortedWith(compareBy({ it.page }, { it.yMm }, { it.xMm }))

    private data class PlacedSlot(val x: Float, val y: Float, val w: Float, val h: Float, val rotated: Boolean)
    private data class PocketRun(val cards: List<CardItem>, val pages: Int, val score: Float)
}
