package com.ajesh.syncspend

import com.ajesh.syncspend.util.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** The app has no paise/cents: every figure is whole units with thousands separators. */
class CurrencyFormatterTest {
    @Test fun wholeNumbersHaveNoDecimals() {
        assertEquals("0", CurrencyFormatter.amount(0.0))
        assertEquals("7", CurrencyFormatter.amount(7.0))
        assertEquals("999", CurrencyFormatter.amount(999.0))
        assertEquals("1,000", CurrencyFormatter.amount(1000.0))
        assertEquals("85,000", CurrencyFormatter.amount(85000.0))
        assertEquals("1,234,567", CurrencyFormatter.amount(1_234_567.0))
    }

    @Test fun fractionsRoundToTheNearestUnit() {
        assertEquals("4,841", CurrencyFormatter.amount(4840.5))
        assertEquals("4,840", CurrencyFormatter.amount(4840.49))
        assertEquals("1,529", CurrencyFormatter.amount(1528.5))
    }

    @Test fun signIsDroppedAndNeverShowsADecimalPoint() {
        assertEquals("320", CurrencyFormatter.amount(-320.0))
        listOf(0.0, 12.0, -12.5, 1234.56, 99999.99).forEach {
            assertFalse(CurrencyFormatter.amount(it), CurrencyFormatter.amount(it).contains('.'))
        }
    }
}
