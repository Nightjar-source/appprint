package com.appprint.studio.core.arrange

import com.appprint.studio.core.model.ArrangeMode
import com.appprint.studio.core.model.CardItem
import com.appprint.studio.core.model.SheetSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SheetArrangerTest {

    private fun card(id: Long, w: Float, h: Float, type: String = "id$id", rotation: Int = 0) = CardItem(
        id = id,
        typeKey = type,
        name = type,
        widthMm = w,
        heightMm = h,
        rotation = rotation,
    )

    private val a4 = SheetSettings(presetKey = "A4", landscape = false)
    private val eps = 0.1f // mm tolerance — placement rounding is 0.1 mm

    @Test
    fun placesTwelveNationalIdsAcrossPages() {
        val cards = (1..12L).map { card(it, 85.6f, 54f, "same") }
        val result = SheetArranger.arrange(cards, a4)
        assertEquals(12, result.cards.size)
        assertTrue(result.pages >= 1)
        result.cards.forEach { c ->
            assertTrue(c.xMm >= 0f, "x out of bounds for ${c.id}")
            assertTrue(c.yMm >= 0f, "y out of bounds for ${c.id}")
            assertTrue(c.xMm + c.effectiveWidth <= a4.sheetWidthMm, "right overflow for ${c.id}")
            assertTrue(c.yMm + c.effectiveHeight <= a4.sheetHeightMm, "bottom overflow for ${c.id}")
        }
    }

    @Test
    fun passportIsNotDistorttedAndStaysInsideSheet() {
        val cards = listOf(card(1, 88f, 125f, "passport")) + (2..7L).map { card(it, 85.6f, 54f, "national") }
        val result = SheetArranger.arrange(cards, a4)
        val passport = result.cards.first { it.typeKey == "passport" }
        assertEquals(88f / 125f, passport.widthMm / passport.heightMm, 0.01f)
        assertTrue(passport.widthMm <= a4.sheetWidthMm - a4.marginMm * 2)
    }

    @Test
    fun landscapeSheetFitsMoreColumns() {
        val cards = (1..8L).map { card(it, 85.6f, 54f, "same") }
        val portrait = SheetArranger.arrange(cards, a4)
        val landscape = SheetArranger.arrange(cards, a4.copy(landscape = true))
        val portraitPages = portrait.cards.maxOf { it.page }
        val landscapePages = landscape.cards.maxOf { it.page }
        assertTrue(landscapePages <= portraitPages)
    }

    @Test
    fun smartGroupingPlacesMoreFrequentTypeFirst() {
        val cards = listOf(
            card(1, 88f, 125f, "passport"),
            card(2, 85.6f, 54f, "national"),
            card(3, 85.6f, 54f, "national"),
            card(4, 85.6f, 54f, "national"),
        )
        // GRID mode keeps legacy row/group ordering (deterministic rows)
        val result = SheetArranger.arrange(
            cards,
            a4.copy(smartGroup = true, mode = ArrangeMode.GRID),
        )
        val order = result.cards.map { it.typeKey }
        assertTrue(order.lastIndexOf("national") < order.indexOf("passport"), "passport should come after all nationals: $order")
    }

    @Test
    fun emptyInputProducesSingleEmptyPage() {
        val result = SheetArranger.arrange(emptyList(), a4.copy(mode = ArrangeMode.COMFORTABLE))
        assertTrue(result.cards.isEmpty(), "no cards expected for empty input")
        assertEquals(1, result.pages, "empty input should still report one page")
    }

    @Test
    fun comfortableModeKeepsAtLeastEightMillimetresGapBetweenCards() {
        val cards = listOf(
            card(1, 85.6f, 54f, "national"),
            card(2, 85.6f, 54f, "national"),
            card(3, 88f, 125f, "passport"),
            card(4, 100f, 70f, "residence"),
            card(5, 140f, 100f, "ration"),
            card(6, 85.6f, 54f, "national"),
        )
        val result = SheetArranger.arrange(
            cards,
            a4.copy(mode = ArrangeMode.COMFORTABLE, smartGroup = false),
        )
        val minGap = 8f - eps
        result.cards.groupBy { it.page }.values.forEach { pageCards ->
            for (i in pageCards.indices) for (j in i + 1 until pageCards.size) {
                val p = pageCards[i]; val q = pageCards[j]
                val dx = maxOf(p.xMm, q.xMm) - minOf(p.xMm + p.effectiveWidth, q.xMm + q.effectiveWidth)
                val dy = maxOf(p.yMm, q.yMm) - minOf(p.yMm + p.effectiveHeight, q.yMm + q.effectiveHeight)
                assertTrue(
                    dx >= minGap || dy >= minGap,
                    "cards ${p.id} and ${q.id} closer than 8mm on page ${p.page} (dx=$dx, dy=$dy)",
                )
            }
        }
    }

    @Test
    fun oversizedCardOnFreshPageDoesNotOverlapFollowingCard() {
        val cards = listOf(
            card(1, 85.6f, 54f, "national"),
            card(2, 85.6f, 54f, "national"),
            card(3, 100f, 400f, "big"),
            card(4, 30f, 20f, "tag"),
        )
        val result = SheetArranger.arrange(cards, a4.copy(smartGroup = true))
        val big = result.cards.first { it.typeKey == "big" }
        val tag = result.cards.first { it.typeKey == "tag" }
        if (big.page == tag.page) {
            val separated =
                tag.xMm + tag.effectiveWidth <= big.xMm ||
                    big.xMm + big.effectiveWidth <= tag.xMm ||
                    tag.yMm + tag.effectiveHeight <= big.yMm ||
                    big.yMm + big.effectiveHeight <= tag.yMm
            assertTrue(separated, "tag overlaps oversized fallback card on page ${big.page}")
        }
    }

    @Test
    fun wideCardIsScaledToFitAndKeepsAspectRatio() {
        val result = SheetArranger.arrange(
            listOf(card(1, 300f, 100f, "banner")),
            a4.copy(mode = ArrangeMode.COMFORTABLE, smartGroup = false),
        )
        val c = result.cards.single()
        val contentW = a4.sheetWidthMm - a4.marginMm * 2f
        assertTrue(c.effectiveWidth <= contentW + eps, "scaled width must fit content: ${c.effectiveWidth}")
        assertEquals(300f / 100f, c.widthMm / c.heightMm, 0.02f, "aspect ratio must be preserved")
    }

    @Test
    fun comfortablePackingHasNoOverlapsAndStaysInsideSheet() {
        val types = listOf(88f to 125f, 85.6f to 54f, 85.6f to 54f, 140f to 100f, 100f to 70f, 85.6f to 54f)
        val cards = types.mapIndexed { i, (w, h) -> card(i + 1L, w, h, "t${i % 3}") }
        val result = SheetArranger.arrange(cards, a4.copy(mode = ArrangeMode.COMFORTABLE))

        assertEquals(6, result.cards.size)
        result.cards.forEach { c ->
            assertTrue(c.xMm >= a4.marginMm - eps, "x below margin for ${c.id}")
            assertTrue(c.yMm >= a4.marginMm - eps, "y below margin for ${c.id}")
            assertTrue(c.xMm + c.effectiveWidth <= a4.sheetWidthMm - a4.marginMm + eps, "right overflow for ${c.id}")
            assertTrue(c.yMm + c.effectiveHeight <= a4.sheetHeightMm - a4.marginMm + eps, "bottom overflow for ${c.id}")
        }
        val byPage = result.cards.groupBy { it.page }
        byPage.values.forEach { pageCards ->
            for (i in pageCards.indices) {
                for (j in i + 1 until pageCards.size) {
                    val a = pageCards[i]
                    val b = pageCards[j]
                    val separated =
                        a.xMm + a.effectiveWidth <= b.xMm ||
                        b.xMm + b.effectiveWidth <= a.xMm ||
                        a.yMm + a.effectiveHeight <= b.yMm ||
                        b.yMm + b.effectiveHeight <= a.yMm
                    assertTrue(separated, "cards ${a.id} and ${b.id} overlap on page ${a.page}")
                }
            }
        }
    }
}
