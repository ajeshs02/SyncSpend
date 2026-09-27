package com.ajesh.syncspend

import com.ajesh.syncspend.data.datastore.UserPreferences
import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.data.db.entity.TransactionEntity
import com.ajesh.syncspend.domain.model.EntryFilter
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.StatsRange
import com.ajesh.syncspend.ui.transactions.Ledger
import com.ajesh.syncspend.ui.transactions.TransactionsCompute
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The per-tab builders behind the Transactions screen (pure: no coroutines, no Android). */
class TransactionsComputeTest {
    private val today = LocalDate.of(2026, 9, 27)
    private val food = CategoryEntity(id = 1, name = "Food", iconKey = "utensils", type = FlowType.EXPENSE, sortOrder = 0)
    private val salary = CategoryEntity(id = 2, name = "Salary", iconKey = "bank", type = FlowType.INCOME, sortOrder = 1)

    private fun tx(id: Long, amount: Double, cat: Long, daysAgo: Long, desc: String = "e$id") =
        TransactionEntity(id = id, amount = amount, description = desc, categoryId = cat, date = today.minusDays(daysAgo), createdAt = id)

    private val ledger = Ledger(
        tx = listOf(
            tx(1, -250.0, 1, 0, "Coffee"),
            tx(2, -1241.0, 1, 0, "Groceries"),
            tx(3, -180.0, 1, 1, "Lunch"),
            tx(4, -320.0, 1, 40, "Old"),
            tx(5, 85_000.0, 2, 3, "Pay"),
        ),
        categories = listOf(food, salary),
        prefs = UserPreferences(),
    )

    @Test fun entriesAreFilteredByFlowAndRangeAndGroupedByDay() {
        val out = TransactionsCompute.entries(ledger, FlowType.EXPENSE, EntryFilter.THIS_MONTH, null, today)
        assertEquals(listOf("Today", "Yesterday"), out.groups.map { it.label })
        assertEquals(listOf("Groceries", "Coffee"), out.groups[0].items.map { it.note }) // newest first within a day
        assertEquals("₹1,491", out.groups[0].totalFormatted)
        // "Old" is outside this month and income is a different flow.
        assertEquals(3, out.groups.sumOf { it.items.size })
    }

    @Test fun amountsAreWholeUnits() {
        val out = TransactionsCompute.entries(ledger, FlowType.INCOME, EntryFilter.THIS_MONTH, null, today)
        assertEquals("₹85,000", out.groups.single().items.single().amountFormatted)
        assertTrue(out.groups.single().totalFormatted, !out.groups.single().totalFormatted.contains('.'))
    }

    @Test fun rollupsGroupByCategoryForTheSameFilter() {
        val out = TransactionsCompute.rollups(ledger, FlowType.EXPENSE, EntryFilter.THIS_MONTH, null, today)
        val row = out.rollups.single()
        assertEquals("Food", row.name)
        assertEquals(3, row.count)
        assertEquals("₹1,671", row.totalFormatted)
    }

    @Test fun incomeUsesTheSameDateChipsAsExpenses() {
        // Sunday 27 Sep: this week is 21-27 Sep, and the salary was on the 24th.
        assertEquals(1, TransactionsCompute.entries(ledger, FlowType.INCOME, EntryFilter.THIS_WEEK, null, today).groups.sumOf { it.items.size })
        assertEquals(0, TransactionsCompute.entries(ledger, FlowType.INCOME, EntryFilter.LAST_MONTH, null, today).groups.sumOf { it.items.size })
    }

    @Test fun statsBuildForEveryRange() {
        StatsRange.entries.forEach { range ->
            val stats = TransactionsCompute.stats(ledger, emptyList(), FlowType.EXPENSE, range, today)
            assertNotNull(stats)
            assertEquals(range, stats.range)
        }
    }
}
