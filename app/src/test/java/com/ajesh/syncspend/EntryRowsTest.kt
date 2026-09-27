package com.ajesh.syncspend

import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.data.db.entity.TransactionEntity
import com.ajesh.syncspend.domain.model.EntryNote
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.ui.transactions.toTxRow
import com.ajesh.syncspend.util.DateUtils
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** What a list row shows: the category as the main text, the optional note, and the date with the time when known. */
class EntryRowsTest {
    private val day = LocalDate.of(2026, 9, 27)
    private val food = CategoryEntity(id = 1, name = "Food", iconKey = "utensils", type = FlowType.EXPENSE, sortOrder = 0)
    private val categories = mapOf(1L to food)

    private fun tx(note: String = "", categoryId: Long = 1, time: Int? = null, amount: Double = -250.0) =
        TransactionEntity(id = 7, amount = amount, description = note, categoryId = categoryId, date = day, createdAt = 1, timeMinuteOfDay = time)

    @Test fun theMainTextIsTheLiveCategoryAndTheNoteIsSeparate() {
        val row = tx(note = "with Rohan").toTxRow(categories, "₹")
        assertEquals("Food", row.categoryLabel)
        assertEquals("with Rohan", row.note)
        assertEquals("₹250", row.amountFormatted)
        assertFalse(row.isPositive)
        assertEquals("utensils", row.iconKey)
    }

    @Test fun noNoteMeansABlankNoteNotTheCategoryName() {
        assertEquals("", tx().toTxRow(categories, "₹").note)
    }

    @Test fun aMissingCategoryIsLabelledAndGetsTheGenericIcon() {
        val row = tx(categoryId = 99).toTxRow(categories, "₹")
        assertEquals("Deleted category", row.categoryLabel)
        assertEquals("receipt", row.iconKey)
    }

    @Test fun theDateLineCarriesTheTimeOnlyWhenKnown() {
        assertEquals("27 Sep", tx().toTxRow(categories, "₹").dayLabel)
        assertEquals("27 Sep - 14:35", tx(time = 14 * 60 + 35).toTxRow(categories, "₹").dayLabel)
        assertEquals("27 Sep - 00:05", tx(time = 5).toTxRow(categories, "₹").dayLabel)
        assertEquals("27 Sep - 09:00", tx(time = 9 * 60).toTxRow(categories, "₹").dayLabel)
    }

    @Test fun timeIsTwentyFourHour() {
        assertEquals("00:00", DateUtils.hhmm(0))
        assertEquals("13:07", DateUtils.hhmm(13 * 60 + 7))
        assertEquals("23:59", DateUtils.hhmm(23 * 60 + 59))
    }

    @Test fun theNoteCapOnlyLimitsTyping() {
        val full = "x".repeat(EntryNote.MAX_LENGTH)
        assertTrue(EntryNote.accepts("", full))
        assertFalse(EntryNote.accepts(full, full + "y")) // typing past the cap is refused
        val imported = "z".repeat(EntryNote.MAX_LENGTH + 30)
        assertTrue(EntryNote.accepts(imported, imported.dropLast(1))) // an over-long imported note can be edited down
        assertFalse(EntryNote.accepts(imported, imported + "a"))
        assertEquals("", EntryNote.normalize("   "))
        assertEquals("tea", EntryNote.normalize("  tea "))
    }
}
