package com.example

import com.example.ui.components.formatUnitBreakdown
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitBreakdownTest {

    @Test
    fun keepsDistinctUnitsSeparatedInsteadOfSumming() {
        val summary = formatUnitBreakdown(
            listOf(
                "шт." to 6,
                "шт." to 12,
                "л." to 40,
                "компл." to 5,
                "шт." to 0,
                "кг." to -3
            )
        )
        assertEquals("40 л. · 18 шт. · 5 компл.", summary)
    }

    @Test
    fun reportsEmptyWhenNoPositiveQuantities() {
        assertEquals(
            "Остатков пока нет",
            formatUnitBreakdown(listOf("шт." to 0, "л." to 0))
        )
    }

    @Test
    fun summarizesExtraUnitsCleanlyBeyondMaxUnits() {
        val summary = formatUnitBreakdown(
            listOf(
                "шт." to 25,
                "л." to 10,
                "кг." to 8,
                "м." to 4,
                "уп." to 2
            ),
            maxUnits = 3
        )
        assertEquals("25 шт. · 10 л. · 8 кг. · +2 ед.", summary)
    }
}
