package com.ajesh.syncspend

import com.ajesh.syncspend.data.datastore.UserPreferences
import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.data.db.entity.TransactionEntity
import com.ajesh.syncspend.data.db.entity.TransferEntity
import com.ajesh.syncspend.domain.model.EntryFilter
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.ScopePeriod
import com.ajesh.syncspend.domain.model.TransferDirection
import com.ajesh.syncspend.ui.transactions.Ledger
import com.ajesh.syncspend.ui.transactions.TransactionsCompute
import java.time.LocalDate
import java.time.YearMonth
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

    // Round 10: Stats moved from StatsRange to the same ScopePeriod Home uses — these exercise the
    // four variants (Month/Year/LastMonths/AllTime) through TransactionsCompute.stats directly.
    @Test fun statsBuildForEveryScopeVariant() {
        val thisMonth = YearMonth.from(today)
        val scopes = listOf(
            ScopePeriod.Month(thisMonth),
            ScopePeriod.Year(today.year),
            ScopePeriod.LastMonths(3, thisMonth),
            ScopePeriod.AllTime,
        )
        scopes.forEach { scope ->
            val stats = TransactionsCompute.stats(ledger, emptyList(), emptyList(), FlowType.EXPENSE, scope, today)
            assertNotNull(stats)
            assertEquals(scope, stats.scope)
        }
    }

    @Test fun statsNameEntriesByCategoryEvenWithoutANote() {
        val noNotes = ledger.copy(tx = ledger.tx.map { it.copy(description = "") })
        val stats = TransactionsCompute.stats(noNotes, emptyList(), emptyList(), FlowType.EXPENSE, ScopePeriod.Month(YearMonth.from(today)), today)
        assertTrue(stats.topEntries.isNotEmpty())
        assertTrue(stats.topEntries.all { it.title == "Food" })
        assertTrue(stats.topEntries.all { it.subtitle.matches(Regex("""\d+ [A-Z][a-z]{2}""")) }) // just the date, no dangling separator
        assertTrue(stats.findings.any { it.startsWith("Largest single entry was Food at") })
    }

    @Test fun aNoteLeadsTheDateLineAndJoinsTheSentence() {
        val stats = TransactionsCompute.stats(ledger, emptyList(), emptyList(), FlowType.EXPENSE, ScopePeriod.Month(YearMonth.from(today)), today)
        val top = stats.topEntries.first()
        assertEquals("Food", top.title)
        assertEquals("Groceries · 27 Sep", top.subtitle) // the ₹1,241 entry, note "Groceries"
        assertTrue(stats.findings.any { it.startsWith("Largest single entry was Food (Groceries) at") })
    }

    // Stats stacks both flows (round 8, no more Expense/Income toggle) — the Savings card must read
    // identically off either build, since it's flow-independent by construction.
    @Test fun savingsSummaryIsIdenticalWhetherReadFromTheExpenseOrIncomeBuild() {
        val transfers = listOf(
            TransferEntity(id = 1, amount = 5000.0, direction = TransferDirection.TO_SAVINGS, date = today, createdAt = 1),
            TransferEntity(id = 2, amount = 1000.0, direction = TransferDirection.FROM_SAVINGS, date = today, createdAt = 2),
        )
        val (expense, income) = TransactionsCompute.statsBoth(ledger, emptyList(), transfers, ScopePeriod.Month(YearMonth.from(today)), today)
        assertEquals(expense.savingsSummary, income.savingsSummary)
        assertEquals("₹5,000", expense.savingsSummary.contributionsFormatted)
        assertEquals("₹1,000", expense.savingsSummary.withdrawalsFormatted)
    }

    // The picked scope drives the window, not today's date (round 10: ScopePeriod replaces the round-9
    // Custom-DateRange mechanism, but the same guarantee applies to any explicitly-picked month).
    @Test fun statsUsesThePickedScopeNotTodaysMonth() {
        val stats = TransactionsCompute.stats(ledger, emptyList(), emptyList(), FlowType.EXPENSE, ScopePeriod.Month(YearMonth.of(2026, 1)), today)
        assertEquals(ScopePeriod.Month(YearMonth.of(2026, 1)), stats.scope)
        // None of the January-less fixture rows fall in January 2026, so the window is honored as empty.
        assertEquals("₹0", stats.tiles.first().value)
    }
}
