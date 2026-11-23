package com.appprint.studio.core.text

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatTest {

    @Test
    fun formatsArabicStringArguments() {
        assertEquals(
            "تم تبديل الموضع مع «بطاقة ٢» في ورقة 3",
            "تم تبديل الموضع مع «%s» في ورقة %d".fmt("بطاقة ٢", 3),
        )
    }

    @Test
    fun formatsNumbersAndPercents() {
        assertEquals("تمت إضافة 7 صور", "تمت إضافة %d صور".fmt(7))
        assertEquals("أُزيلت الزوائد (4%)", "أُزيلت الزوائد (%d%%)".fmt(4))
    }

    @Test
    fun formatsDecimalAndHexPadding() {
        assertEquals("12.3 مم", "%.1f مم".fmt(12.34f))
        assertEquals("#00aabb", "#%06x".fmt(0xAABB))
    }
}
