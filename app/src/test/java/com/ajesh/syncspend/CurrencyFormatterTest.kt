package com.ajesh.syncspend

import com.ajesh.syncspend.util.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * A whole-unit value never shows a decimal point; a value with real cents always shows exactly two —
 * the display reflects the value's own precision, independent of the Settings "allow paise" toggle
 * (that toggle only gates new input, never how an already-stored amount is shown).
 */
class CurrencyFormatterTest {
    @Test fun wholeNumbersHaveNoDecimals() {
        assertEquals("0", CurrencyFormatter.amount(0.0))
        assertEquals("7", CurrencyFormatter.amount(7.0))
        assertEquals("999", CurrencyFormatter.amount(999.0))
        assertEquals("1,000", CurrencyFormatter.amount(1000.0))
        assertEquals("85,000", CurrencyFormatter.amount(85000.0))
        assertEquals("1,234,567", CurrencyFormatter.amount(1_234_567.0))
    }

    @Test fun valuesWithCentsShowExactlyTwoDecimals() {
        assertEquals("4,840.50", CurrencyFormatter.amount(4840.5))
        assertEquals("1,528.50", CurrencyFormatter.amount(1528.5))
        assertEquals("4,840.49", CurrencyFormatter.amount(4840.49))
        assertEquals("0.05", CurrencyFormatter.amount(0.05))
        assertEquals("99,999.99", CurrencyFormatter.amount(99999.99))
    }

    @Test fun centsRoundToTheNearestOne() {
        // 4840.495 rounds to 4840.50 (halves round up).
        assertEquals("4,840.50", CurrencyFormatter.amount(4840.495))
        // A carry into the whole part lands exactly on a whole number, so it shows no decimals at all.
        assertEquals("5", CurrencyFormatter.amount(4.999))
    }

    @Test fun signIsAlwaysDropped() {
        assertEquals("320", CurrencyFormatter.amount(-320.0))
        assertEquals("12.50", CurrencyFormatter.amount(-12.5))
    }

    @Test fun onlyAWholeValueOmitsTheDecimalPoint() {
        listOf(0.0, 12.0, 1_000_000.0).forEach {
            assertFalse(CurrencyFormatter.amount(it), CurrencyFormatter.amount(it).contains('.'))
        }
        listOf(12.5, -12.5, 1234.56, 99999.99).forEach {
            assert(CurrencyFormatter.amount(it).contains('.')) { "expected a decimal point for $it" }
        }
    }

    @Test fun groupDigitsMatchesThousandsSeparatorFormatting() {
        val random = java.util.Random(11)
        val samples = List(20_000) { random.nextInt(1_000_000) } + listOf(0, 1, 999, 1000, 999_999)
        samples.forEach {
            assertEquals(
                "$it",
                java.lang.String.format(java.util.Locale.US, "%,d", it),
                CurrencyFormatter.groupDigits(it.toString()),
            )
        }
    }
}
