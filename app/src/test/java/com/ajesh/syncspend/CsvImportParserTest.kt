package com.ajesh.syncspend

import com.ajesh.syncspend.csv.CategoryIconGuesser
import com.ajesh.syncspend.csv.CsvImportParser
import com.ajesh.syncspend.csv.DuplicateTracker
import com.ajesh.syncspend.data.db.entity.TransactionEntity
import com.ajesh.syncspend.domain.model.FlowType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvImportParserTest {

    /** The exact example from the previous tracker's export notes (CRLF, quoted note, Uncategorized). */
    private val oldTrackerFile =
        "Date,Type,Category,Amount,Note\r\n" +
            "2026-09-01,INCOME,Salary,85000,\r\n" +
            "2026-09-03,EXPENSE,Food,450,\"Lunch, with team\"\r\n" +
            "2026-09-05,EXPENSE,Uncategorized,120,\r\n"

    @Test fun readsThePreviousTrackersExport() {
        val out = CsvImportParser.parse(oldTrackerFile)
        assertNull(out.headerError)
        assertEquals(0, out.skipped)
        assertEquals(3, out.rows.size)

        val salary = out.rows[0]
        assertEquals(LocalDate.of(2026, 9, 1), salary.date)
        assertEquals(FlowType.INCOME, salary.type)
        assertEquals("Salary", salary.categoryName)
        assertEquals(85000.0, salary.amount, 0.0)
        assertEquals("", salary.description)

        assertEquals("Lunch, with team", out.rows[1].description)
        assertEquals(FlowType.EXPENSE, out.rows[1].type)
        assertEquals(CsvImportParser.UNCATEGORIZED, out.rows[2].categoryName)
    }

    @Test fun readsOurOwnExportHeader() {
        val out = CsvImportParser.parse("date,type,category,description,amount\n2026-09-14,EXPENSE,Food,Lunch,250.00\n")
        assertNull(out.headerError)
        assertEquals("Lunch", out.rows.single().description)
        assertEquals(250.0, out.rows.single().amount, 0.0)
    }

    @Test fun columnOrderAndCaseDoNotMatter() {
        val out = CsvImportParser.parse("﻿ AMOUNT ,note,TYPE,Date,Category\n99.5,tea,expense,2026-01-02,Snacks\n")
        assertNull(out.headerError)
        val r = out.rows.single()
        assertEquals(99.5, r.amount, 0.0)
        assertEquals("tea", r.description)
        assertEquals(FlowType.EXPENSE, r.type)
        assertEquals("Snacks", r.categoryName)
    }

    @Test fun acceptsSemicolonSeparatedSpreadsheetExports() {
        val out = CsvImportParser.parse("Date;Type;Category;Amount;Note\n2026-09-03;EXPENSE;Food;450;lunch\n")
        assertNull(out.headerError)
        assertEquals(450.0, out.rows.single().amount, 0.0)
    }

    @Test fun toleratesAmountFormattingAndOtherDateStyles() {
        val out = CsvImportParser.parse("date,type,category,amount\n03/09/2026,EXPENSE,Food,\"₹1,250.50\"\n2026-09-04,INCOME,Salary,-300\n")
        assertEquals(LocalDate.of(2026, 9, 3), out.rows[0].date)
        assertEquals(1250.5, out.rows[0].amount, 0.0)
        assertEquals(300.0, out.rows[1].amount, 0.0)
    }

    @Test fun blankCategoryBecomesUncategorized() {
        val out = CsvImportParser.parse("date,type,category,amount\n2026-09-03,EXPENSE,,10\n")
        assertEquals(CsvImportParser.UNCATEGORIZED, out.rows.single().categoryName)
    }

    @Test fun skipsBadRowsAndReportsWhy() {
        val out = CsvImportParser.parse(
            "Date,Type,Category,Amount,Note\n" +
                "2026-02-31,EXPENSE,Food,10,\n" +
                "2026-09-03,TRANSFER,Food,10,\n" +
                "2026-09-03,EXPENSE,Food,abc,\n" +
                "2026-09-03,EXPENSE,Food,0,\n" +
                "2026-09-03,EXPENSE,Food,10,ok\n",
        )
        assertEquals(1, out.rows.size)
        assertEquals(4, out.skipped)
        assertEquals(4, out.problems.size)
        assertTrue(out.problems.first().startsWith("Row 2"))
    }

    @Test fun rejectsFilesThatAreNotExpenseCsvs() {
        val out = CsvImportParser.parse("name,email\nA,a@b.c\n")
        assertNotNull(out.headerError)
        assertTrue(out.rows.isEmpty())
        assertNotNull(CsvImportParser.parse("").headerError)
    }

    @Test fun decodesUtf16WithBom() {
        val text = "date,type,category,amount\n2026-09-03,EXPENSE,Food,10\n"
        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + text.toByteArray(Charsets.UTF_16LE)
        assertEquals(text, CsvImportParser.decode(bytes))
        assertEquals(text, CsvImportParser.decode(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + text.toByteArray()))
    }

    // ---- duplicate detection ----

    private fun tx(amount: Double, category: Long = 1, description: String = "Lunch", date: LocalDate = LocalDate.of(2026, 9, 3)) =
        TransactionEntity(amount = amount, description = description, categoryId = category, date = date, createdAt = 0)

    @Test fun reImportingTheSameRowsAddsNothing() {
        val tracker = DuplicateTracker(listOf(tx(-450.0), tx(-450.0)))
        val d = LocalDate.of(2026, 9, 3)
        assertTrue(tracker.consumeIfDuplicate(d, -450.0, 1, "Lunch"))
        assertTrue(tracker.consumeIfDuplicate(d, -450.0, 1, "Lunch"))
        // Only two existed, so a third identical row is genuinely new.
        assertFalse(tracker.consumeIfDuplicate(d, -450.0, 1, "Lunch"))
    }

    @Test fun differentAmountCategoryOrTextIsNotADuplicate() {
        val tracker = DuplicateTracker(listOf(tx(-450.0)))
        val d = LocalDate.of(2026, 9, 3)
        assertFalse(tracker.consumeIfDuplicate(d, -451.0, 1, "Lunch"))
        assertFalse(tracker.consumeIfDuplicate(d, -450.0, 2, "Lunch"))
        assertFalse(tracker.consumeIfDuplicate(d, -450.0, 1, "Dinner"))
        assertFalse(tracker.consumeIfDuplicate(d.plusDays(1), -450.0, 1, "Lunch"))
        assertFalse(tracker.consumeIfDuplicate(d, 450.0, 1, "Lunch"))
    }

    // ---- icon guessing ----

    @Test fun guessesIconsForThePreviousTrackersDefaults() {
        assertEquals("utensils", CategoryIconGuesser.guess("Food"))
        assertEquals("cart", CategoryIconGuesser.guess("Groceries"))
        assertEquals("fuel", CategoryIconGuesser.guess("Transportation / Fuel"))
        assertEquals("receipt", CategoryIconGuesser.guess("Recharge, Bills & Subscriptions"))
        assertEquals("film", CategoryIconGuesser.guess("Entertainment"))
        assertEquals("house", CategoryIconGuesser.guess("Home"))
        assertEquals("coin", CategoryIconGuesser.guess("Savings"))
        assertEquals("card", CategoryIconGuesser.guess("Debt / Liability"))
        assertEquals("bank", CategoryIconGuesser.guess("Salary"))
        assertEquals("brief", CategoryIconGuesser.guess("Freelance"))
        assertEquals("gift", CategoryIconGuesser.guess("Gift"))
    }

    @Test fun guessesByKeywordAndFallsBackToTag() {
        assertEquals("pill", CategoryIconGuesser.guess("Pharmacy & Medicines"))
        assertEquals("plane", CategoryIconGuesser.guess("Holiday trip"))
        assertEquals("tag", CategoryIconGuesser.guess("Zzz"))
        assertEquals("tag", CategoryIconGuesser.guess("Uncategorized"))
    }
}
