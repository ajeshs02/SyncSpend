package com.ajesh.syncspend

import com.ajesh.syncspend.csv.CsvFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class CsvFormatTest {
    @Test fun escapesOnlyWhenNeeded() {
        assertEquals("plain", CsvFormat.escape("plain"))
        assertEquals("\"a,b\"", CsvFormat.escape("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", CsvFormat.escape("say \"hi\""))
    }

    @Test fun roundTripsAwkwardFields() {
        val fields = listOf("2026-09-14", "EXPENSE", "Food, Drink", "He said \"wow\"\nsecond line", "250.00")
        val parsed = CsvFormat.parse(CsvFormat.row(CsvFormat.HEADER) + "\n" + CsvFormat.row(fields) + "\n")
        assertEquals(2, parsed.size)
        assertEquals(CsvFormat.HEADER, parsed[0])
        assertEquals(fields, parsed[1])
    }

    @Test fun handlesCrlfAndMissingTrailingNewline() {
        val parsed = CsvFormat.parse("a,b\r\nc,d")
        assertEquals(listOf(listOf("a", "b"), listOf("c", "d")), parsed)
    }

    @Test fun keepsEmptyFieldsAndSkipsBlankLines() {
        val parsed = CsvFormat.parse("a,,c\n\nd,e,\n")
        assertEquals(listOf(listOf("a", "", "c"), listOf("d", "e", "")), parsed)
    }
}
