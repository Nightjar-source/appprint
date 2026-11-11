package com.appprint.studio.core.arrange

import com.appprint.studio.core.model.CardItem
import com.appprint.studio.core.model.SheetSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SheetArrangerTest {

    private fun card(id: Long, w: Float, h: Float, type: String = "id$id") = CardItem(
        id = id,
        typeKey = type,
        name = type,
        widthMm = w,
        heightMm = h,
    )

    private val a4 = SheetSettings(presetKey = "A4", landscape = false)

    @Test
    fun placesTwelveNationalIdsAcrossPages() {
        val cards = (1..12L).map { card(it, 85.6f, 54f) }
        val result = SheetArranger.arrange(cards, a4)
        assertEquals(12, result.cards.size)
        assertTrue(result.pages >= 1)
        // everything stays inside the sheet
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
        // no distortion: aspect ratio preserved
        assertEquals(88f / 125f, passport.widthMm / passport.heightMm, 0.01f)
        assertTrue(passport.widthMm <= a4.sheetWidthMm - a4.marginMm * 2)
    }

    @Test
    fun landscapeSheetFitsMoreColumns() {
        val cards = (1..8L).map { card(it, 85.6f, 54f, "n$it") }
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
        val result = SheetArranger.arrange(cards, a4.copy(smartGroup = true))
        val order = result.cards.map { it.typeKey }
        assertTrue(order.lastIndexOf("national") < order.indexOf("passport"), "passport should come after all nationals: $order")
    }
}
