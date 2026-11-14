package com.appprint.studio.core.arrange

import com.appprint.studio.core.model.ArrangeMode
import com.appprint.studio.core.model.CardItem
import com.appprint.studio.core.model.PaperPreset
import com.appprint.studio.core.model.Orientation
import com.appprint.studio.core.model.SheetSettings
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class SheetArrangerTest {

    private fun card(id: Long, w: Float, h: Float) = CardItem(
        id = id,
        typeKey = "custom",
        name = "c$id",
        baseWidthMm = w,
        baseHeightMm = h,
        widthMm = w,
        heightMm = h,
    )

    private fun overlap(a: CardItem, b: CardItem): Boolean = !(
        a.xMm + a.widthMm <= b.xMm + 0.01f ||
            b.xMm + b.widthMm <= a.xMm + 0.01f ||
            a.yMm + a.heightMm <= b.yMm + 0.01f ||
            b.yMm + b.heightMm <= a.yMm + 0.01f
        )

    @Test
    fun arrange_never_overlaps_cards() {
        val cards = buildList<CardItem> {
            repeat(6) { add(card(it.toLong() + 1, 85.6f, 54f)) }
            repeat(2) { add(card(100 + it.toLong(), 88f, 125f)) }
            repeat(2) { add(card(200 + it.toLong(), 140f, 100f)) }
        }
        val result = SheetArranger.arrange(cards, SheetSettings(), "smart", false)
        for (page in result.cards.groupBy { it.page }) {
            val list = page.value
            for (i in list.indices) {
                for (j in i + 1 until list.size) {
                    assertTrue(!overlap(list[i], list[j]), "cards ${list[i].id} and ${list[j].id} overlap on page ${page.key}")
                }
            }
        }
    }

    @Test
    fun arrange_keeps_cards_inside_safe_sheet() {
        val settings = SheetSettings(presetKey = PaperPreset.A5.key, orientation = Orientation.PORTRAIT)
        val result = SheetArranger.arrange(
            listOf(card(1, 85.6f, 54f), card(2, 85.6f, 54f), card(3, 88f, 125f), card(4, 85.6f, 54f)),
            settings,
            "smart",
            false,
        )
        for (c in result.cards) {
            assertTrue(c.xMm >= 0f && c.yMm >= 0f, "card ${c.id} starts before origin")
            assertTrue(
                c.xMm + c.widthMm <= settings.sheetWidthMm + 0.01f,
                "card ${c.id} overflows width ${c.xMm + c.widthMm} > ${settings.sheetWidthMm}",
            )
            assertTrue(
                c.yMm + c.heightMm <= settings.sheetHeightMm + 0.01f,
                "card ${c.id} overflows height ${c.yMm + c.heightMm} > ${settings.sheetHeightMm}",
            )
        }
    }

    @Test
    fun arrange_grid_mode_places_everything() {
        val settings = SheetSettings(mode = ArrangeMode.GRID)
        val input = (1..12L).map { card(it, 85.6f, 54f) }
        val result = SheetArranger.arrange(input, settings, "smart", false)
        assertEquals(input.size, result.cards.size)
        assertTrue(result.pages >= 1)
    }
}
