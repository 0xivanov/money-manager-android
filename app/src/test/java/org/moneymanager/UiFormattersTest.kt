package org.moneymanager

import androidx.compose.ui.geometry.Offset
import java.math.BigDecimal
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UiFormattersTest {
    @Test
    fun `localized decimal accepts European grouping and comma decimal`() {
        assertEquals(BigDecimal("1234.56"), parseLocalizedDecimal("1.234,56", Locale.GERMANY))
    }

    @Test
    fun `localized decimal accepts US grouping and dot decimal`() {
        assertEquals(BigDecimal("1234.56"), parseLocalizedDecimal("1,234.56", Locale.US))
    }

    @Test
    fun `single locale grouping separator is not mistaken for a decimal`() {
        assertEquals(BigDecimal("1234"), parseLocalizedDecimal("1,234", Locale.US))
        assertEquals(BigDecimal("1234"), parseLocalizedDecimal("1.234", Locale.GERMANY))
        assertEquals(BigDecimal("12.50"), parseLocalizedDecimal("12,50", Locale.US))
        assertNull(parseLocalizedDecimal("1,23,4", Locale.US))
    }

    @Test
    fun `currency format includes grouping and requested currency`() {
        assertEquals("€1,234.50", BigDecimal("1234.5").money("EUR", Locale.US))
    }

    @Test
    fun `donut center and outside do not select a category`() {
        val totals = listOf(CategoryTotal("groceries", BigDecimal.TEN))

        assertNull(findPieCategoryForTap(Offset(50f, 50f), 100f, 100f, totals))
        assertNull(findPieCategoryForTap(Offset(120f, 50f), 100f, 100f, totals))
    }

    @Test
    fun `category titles turn identifiers into readable labels`() {
        assertEquals("Groceries", categoryTitle("groceries"))
        assertEquals("Dining Out", categoryTitle("dining_out"))
        assertEquals("Going Out", categoryTitle("going_out"))
    }
}
