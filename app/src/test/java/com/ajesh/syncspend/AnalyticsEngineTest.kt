package com.ajesh.syncspend

import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.data.db.entity.TransactionEntity
import com.ajesh.syncspend.data.db.entity.TransferEntity
import com.ajesh.syncspend.domain.analytics.AnalyticsEngine
import com.ajesh.syncspend.domain.model.CategoryRollup
import com.ajesh.syncspend.domain.model.DateRange
import com.ajesh.syncspend.domain.model.EntryFilter
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.ScopePeriod
import com.ajesh.syncspend.domain.model.TransferDirection
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsEngineTest {
    private val today = LocalDate.of(2026, 9, 14)
    private val food = CategoryEntity(id = 1, name = "Food", iconKey = "coffee", type = FlowType.EXPENSE, sortOrder = 0)
    private val salary = CategoryEntity(id = 2, name = "Salary", iconKey = "brief", type = FlowType.INCOME, sortOrder = 0)
    private var nextId = 1L
    private fun tx(amount: Double, date: LocalDate, cat: Long = 1) =
        TransactionEntity(id = nextId++, amount = amount, description = "t", categoryId = cat, date = date, createdAt = nextId)

    private val data = listOf(
        tx(-250.0, LocalDate.of(2026, 9, 14)),
        tx(-200.0, LocalDate.of(2026, 9, 13)),
        tx(-100.0, LocalDate.of(2026, 9, 8)),
        tx(-1500.0, LocalDate.of(2026, 8, 28), cat = 1),
        tx(10000.0, LocalDate.of(2026, 9, 1), cat = 2),
        tx(10000.0, LocalDate.of(2026, 8, 1), cat = 2),
    )

    @Test fun scopeAndFlowFiltering() {
        val expenses = AnalyticsEngine.flowFilter(data, FlowType.EXPENSE)
        assertEquals(4, expenses.size)
        val sept = AnalyticsEngine.scopeFilter(expenses, ScopePeriod.Month(YearMonth.of(2026, 9)))
        assertEquals(3, sept.size)
        assertEquals(4, AnalyticsEngine.scopeFilter(expenses, ScopePeriod.Year(2026)).size)
        assertEquals(4, AnalyticsEngine.scopeFilter(expenses, ScopePeriod.AllTime).size)
    }

    @Test fun thisWeekIsTheCalendarWeekStartingMonday() {
        val e = AnalyticsEngine.flowFilter(data, FlowType.EXPENSE)
        // today = Monday 14 Sep: the week is 14-20 Sep. Sunday 13 Sep belongs to the previous week.
        assertEquals(1, AnalyticsEngine.applyEntryFilter(e, EntryFilter.THIS_WEEK, null, today).size)
        assertEquals(3, AnalyticsEngine.applyEntryFilter(e, EntryFilter.THIS_MONTH, null, today).size)
        assertEquals(1, AnalyticsEngine.applyEntryFilter(e, EntryFilter.LAST_MONTH, null, today).size)
        // On the following Sunday (20 Sep) the same week still runs from Monday 14 Sep.
        assertEquals(1, AnalyticsEngine.applyEntryFilter(e, EntryFilter.THIS_WEEK, null, LocalDate.of(2026, 9, 20)).size)
        // ...and on Monday 21 Sep it starts over.
        assertEquals(0, AnalyticsEngine.applyEntryFilter(e, EntryFilter.THIS_WEEK, null, LocalDate.of(2026, 9, 21)).size)
    }

    @Test fun theFilterCoversTheWholePeriodSoALaterEntryIsNeverHidden() {
        val thursday = LocalDate.of(2026, 9, 17)
        val later = listOf(tx(-10.0, LocalDate.of(2026, 9, 20)), tx(-10.0, LocalDate.of(2026, 9, 29)))
        assertEquals(1, AnalyticsEngine.applyEntryFilter(later, EntryFilter.THIS_WEEK, null, thursday).size)
        assertEquals(2, AnalyticsEngine.applyEntryFilter(later, EntryFilter.THIS_MONTH, null, thursday).size)
    }

    @Test fun expenseGetsAllFiveDateChipsIncomeSkipsThisWeekAndLastWeek() {
        assertEquals(listOf("THIS_WEEK", "LAST_WEEK", "THIS_MONTH", "LAST_MONTH", "CUSTOM"), AnalyticsEngine.entryFilterOptions(FlowType.EXPENSE).map { it.name })
        assertEquals(listOf("THIS_MONTH", "LAST_MONTH", "CUSTOM"), AnalyticsEngine.entryFilterOptions(FlowType.INCOME).map { it.name })
    }

    @Test fun headerRangesFollowEachChip() {
        val thursday = LocalDate.of(2026, 9, 17)
        assertEquals(DateRange(LocalDate.of(2026, 9, 14), thursday), AnalyticsEngine.entryFilterRange(EntryFilter.THIS_WEEK, null, thursday))
        assertEquals(DateRange(LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 27)), AnalyticsEngine.entryFilterRange(EntryFilter.THIS_WEEK, null, LocalDate.of(2026, 9, 27)))
        assertEquals(DateRange(LocalDate.of(2026, 9, 1), thursday), AnalyticsEngine.entryFilterRange(EntryFilter.THIS_MONTH, null, thursday))
        assertEquals(DateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)), AnalyticsEngine.entryFilterRange(EntryFilter.LAST_MONTH, null, thursday))
        assertEquals(DateRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)), AnalyticsEngine.entryFilterRange(EntryFilter.LAST_MONTH, null, LocalDate.of(2026, 3, 3)))
        val custom = DateRange(LocalDate.of(2026, 6, 2), LocalDate.of(2026, 6, 9))
        assertEquals(custom, AnalyticsEngine.entryFilterRange(EntryFilter.CUSTOM, custom, thursday))
        assertNull(AnalyticsEngine.entryFilterRange(EntryFilter.CUSTOM, null, thursday))
    }

    @Test fun customRangeIsInclusiveOnBothEnds() {
        val e = AnalyticsEngine.flowFilter(data, FlowType.EXPENSE)
        val range = DateRange(LocalDate.of(2026, 8, 28), LocalDate.of(2026, 9, 8))
        val picked = AnalyticsEngine.applyEntryFilter(e, EntryFilter.CUSTOM, range, today)
        assertEquals(2, picked.size) // 28 Aug and 8 Sep, both edges included
        assertEquals(e.size, AnalyticsEngine.applyEntryFilter(e, EntryFilter.CUSTOM, null, today).size)
    }

    @Test fun dayGroupingLabelsAndTotals() {
        val groups = AnalyticsEngine.groupByDay(AnalyticsEngine.flowFilter(data, FlowType.EXPENSE), today)
        assertEquals(listOf("Today", "Yesterday", "8 Sep 2026", "28 Aug 2026"), groups.map { it.label })
        assertEquals(250.0, groups[0].totalAbs, 0.0)
    }

    @Test fun categoryRollupSharesAndDeletedFallback() {
        val rollups = AnalyticsEngine.groupByCategory(
            listOf(tx(-300.0, today, 1), tx(-100.0, today, 99)),
            listOf(food),
        )
        assertEquals("Food", rollups[0].name)
        assertEquals(75, rollups[0].sharePercent)
        assertEquals("Deleted category", rollups[1].name)
    }

    @Test fun trendNeedsAPreviousPeriod() {
        assertNull(AnalyticsEngine.trendPercent(100.0, 0.0))
        assertEquals(-50, AnalyticsEngine.trendPercent(50.0, 100.0))
        assertEquals(25, AnalyticsEngine.trendPercent(125.0, 100.0))
    }

    @Test fun statsSummary() {
        val sept = DateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))
        val aug = DateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31))
        val s = AnalyticsEngine.stats(data, listOf(food, salary), sept, aug, FlowType.EXPENSE, today)
        assertEquals(550.0, s.total, 0.0)
        assertEquals(1500.0, s.previousTotal, 0.0)
        assertEquals(3, s.entryCount)
        assertEquals(3, s.activeDays)
        assertEquals(550.0 / 3, s.dailyAverage, 1e-9)
        assertEquals(-63, s.trendPercent)
        assertEquals(10000.0, s.otherFlowTotal, 0.0)
        assertTrue(s.netSavingsRatePercent!! in 94..95) // (10000 - 550) / 10000 = 94.5%
        assertNotNull(s.topCategory)
        assertEquals(250.0, kotlin.math.abs(s.biggestEntry!!.amount), 0.0)
        assertNull(AnalyticsEngine.stats(data, listOf(food), sept, null, FlowType.EXPENSE, today).trendPercent)
    }

    // Round 10: Stats/Transactions moved off StatsRange onto ScopePeriod + AnalyticsEngine.scopeRange
    // (the one new adapter this round added) + the already-existing previousScope/scopeLabel Home
    // itself relies on. These replace the old direct StatsRange.resolve/previous/describe tests.
    @Test fun scopeRangeIsCalendarAlignedForEveryVariant() {
        val month = ScopePeriod.Month(YearMonth.of(2026, 9))
        assertEquals(DateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)), AnalyticsEngine.scopeRange(month, today, null))

        val year = ScopePeriod.Year(2026)
        assertEquals(DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)), AnalyticsEngine.scopeRange(year, today, null))

        val lastThree = ScopePeriod.LastMonths(3, YearMonth.of(2026, 9))
        assertEquals(DateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30)), AnalyticsEngine.scopeRange(lastThree, today, null))

        // All Time boundaries: a known earliest date, none yet (falls back to today), and earliest == today.
        assertEquals(DateRange(LocalDate.of(2025, 3, 3), today), AnalyticsEngine.scopeRange(ScopePeriod.AllTime, today, LocalDate.of(2025, 3, 3)))
        assertEquals(DateRange(today, today), AnalyticsEngine.scopeRange(ScopePeriod.AllTime, today, null))
        assertEquals(DateRange(today, today), AnalyticsEngine.scopeRange(ScopePeriod.AllTime, today, today))
    }

    @Test fun previousScopeResolvesToTheImmediatelyPrecedingWindowOfTheSameShape() {
        val month = ScopePeriod.Month(YearMonth.of(2026, 9))
        val prevMonth = AnalyticsEngine.previousScope(month)
        assertEquals(DateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)), AnalyticsEngine.scopeRange(prevMonth!!, today, null))

        val year = ScopePeriod.Year(2026)
        val prevYear = AnalyticsEngine.previousScope(year)
        assertEquals(DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)), AnalyticsEngine.scopeRange(prevYear!!, today, null))

        val lastThree = ScopePeriod.LastMonths(3, YearMonth.of(2026, 9))
        val prevThree = AnalyticsEngine.previousScope(lastThree)
        assertEquals(DateRange(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30)), AnalyticsEngine.scopeRange(prevThree!!, today, null))

        // No previous period for All Time — nothing to compare an open-ended window against.
        assertNull(AnalyticsEngine.previousScope(ScopePeriod.AllTime))
    }

    @Test fun scopeLabelReadsLastNMonthsOnlyWhileTheWindowIsTheCurrentOne() {
        val lastThree = ScopePeriod.LastMonths(3, YearMonth.of(2026, 9))
        // The window still ends at "now" — reads as the rolling shortcut.
        assertEquals("Last 3 months", AnalyticsEngine.scopeLabel(lastThree, now = YearMonth.of(2026, 9)))
        // A month has passed since — no longer "current", so it reads as its fixed span instead.
        assertEquals("Jul - Sep 2026", AnalyticsEngine.scopeLabel(lastThree, now = YearMonth.of(2026, 10)))
        assertEquals("September 2026", AnalyticsEngine.scopeLabel(ScopePeriod.Month(YearMonth.of(2026, 9))))
    }

    @Test fun monthlySeriesCoversSixMonthsIncludingEmptyOnes() {
        val series = AnalyticsEngine.monthlySeries(AnalyticsEngine.flowFilter(data, FlowType.EXPENSE), YearMonth.of(2026, 9))
        assertEquals(6, series.size)
        assertEquals(YearMonth.of(2026, 4), series.first().month)
        assertEquals(0.0, series[0].total, 0.0)
        assertEquals(1500.0, series[4].total, 0.0) // August
        assertEquals(550.0, series[5].total, 0.0) // September
    }

    @Test fun weekdayPatternAveragesPerOccurrence() {
        // 2026-09-14 is a Monday. Range 1-14 Sep has two of each weekday.
        val range = DateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 14))
        val tx = AnalyticsEngine.flowFilter(data, FlowType.EXPENSE).filter { it.date in range }
        val pattern = AnalyticsEngine.weekdayPattern(tx, range, today)
        assertEquals(7, pattern.size)
        assertEquals(java.time.DayOfWeek.MONDAY, pattern.first().day)
        assertEquals(250.0, pattern[0].total, 0.0) // Mon 14 Sep
        assertEquals(125.0, pattern[0].average, 0.0) // two Mondays in the range
        assertEquals(100.0, pattern[1].total, 0.0) // Tue 8 Sep
        assertEquals(200.0, pattern[6].total, 0.0) // Sun 13 Sep
        assertEquals(100.0, pattern[6].average, 0.0)
    }

    @Test fun categoryMoversRankByAbsoluteChange() {
        val cur = listOf(
            CategoryRollup(1, "Food", "coffee", 3, 500.0, 50),
            CategoryRollup(2, "Travel", "plane", 1, 300.0, 30),
        )
        val prev = listOf(
            CategoryRollup(1, "Food", "coffee", 3, 200.0, 40),
            CategoryRollup(3, "Rent", "house", 1, 900.0, 60),
        )
        val movers = AnalyticsEngine.categoryMovers(cur, prev)
        assertEquals(listOf("Rent", "Food", "Travel"), movers.map { it.name })
        assertEquals(300.0, movers[1].delta, 0.0)
        assertEquals(-900.0, movers[0].delta, 0.0)
    }

    @Test fun projectionOnlyForTheCurrentMonthAfterEnoughDays() {
        val sept = DateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))
        val tx = AnalyticsEngine.flowFilter(data, FlowType.EXPENSE).filter { it.date in sept }
        val p = AnalyticsEngine.monthProjection(tx, sept, 1500.0, today)!!
        assertEquals(550.0, p.soFar, 0.0)
        assertEquals(550.0 / 14 * 30, p.projected, 1e-9)
        assertNull(AnalyticsEngine.monthProjection(tx, sept, 1500.0, LocalDate.of(2026, 9, 3))) // too early in the month
        assertNull(AnalyticsEngine.monthProjection(tx, DateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)), 0.0, today))
    }

    @Test fun noSpendDaysAndSmallPurchases() {
        val sept = DateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))
        val active = setOf(LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 13), LocalDate.of(2026, 9, 8))
        assertEquals(11, AnalyticsEngine.noSpendDays(active, sept, today)) // 14 days elapsed, 3 active

        val many = List(10) { tx(-10.0, today) } + tx(-900.0, today)
        val sp = AnalyticsEngine.smallPurchases(many, 1000.0, 1000.0 / 11)!!
        assertEquals(10, sp.count)
        assertEquals(10, sp.sharePercent)
        assertNull(AnalyticsEngine.smallPurchases(listOf(tx(-10.0, today)), 10.0, 10.0))
    }

    @Test fun topEntriesAreTheLargestByAbsoluteAmount() {
        val top = AnalyticsEngine.topEntries(AnalyticsEngine.flowFilter(data, FlowType.EXPENSE))
        assertEquals(listOf(1500.0, 250.0, 200.0), top.map { kotlin.math.abs(it.amount) })
        assertEquals(1, AnalyticsEngine.topEntries(AnalyticsEngine.flowFilter(data, FlowType.EXPENSE), limit = 1).size)
        assertEquals(emptyList<TransactionEntity>(), AnalyticsEngine.topEntries(emptyList()))
    }

    @Test fun busiestDaySumsEntriesOnTheSameDate() {
        val day = LocalDate.of(2026, 9, 2)
        val list = listOf(tx(-100.0, day), tx(-250.0, day), tx(-300.0, LocalDate.of(2026, 9, 5)))
        val busiest = AnalyticsEngine.busiestDay(list)!!
        assertEquals(day, busiest.date)
        assertEquals(350.0, busiest.total, 0.0)
        assertEquals(2, busiest.entryCount)
        assertNull(AnalyticsEngine.busiestDay(emptyList()))
    }

    @Test fun busiestDayTieGoesToTheEarlierDate() {
        val list = listOf(tx(-100.0, LocalDate.of(2026, 9, 9)), tx(-100.0, LocalDate.of(2026, 9, 3)))
        assertEquals(LocalDate.of(2026, 9, 3), AnalyticsEngine.busiestDay(list)!!.date)
    }

    // --- Transfer accounting (Part D.3) ---

    private fun expenseTx(amount: Double, date: LocalDate = today) = TransactionEntity(
        id = nextId++, amount = -amount, description = "", categoryId = 1, date = date, createdAt = nextId, type = FlowType.EXPENSE,
    )

    private fun incomeTx(amount: Double, date: LocalDate = today) = TransactionEntity(
        id = nextId++, amount = amount, description = "", categoryId = 1, date = date, createdAt = nextId, type = FlowType.INCOME,
    )

    private fun transfer(amount: Double, direction: TransferDirection, date: LocalDate = today) =
        TransferEntity(id = nextId++, amount = amount, direction = direction, date = date, createdAt = nextId)

    @Test fun savingsBalanceIsContributionsMinusWithdrawals() {
        val transfers = listOf(
            transfer(8000.0, TransferDirection.TO_SAVINGS),
            transfer(3000.0, TransferDirection.FROM_SAVINGS),
        )
        assertEquals(5000.0, AnalyticsEngine.savingsBalance(transfers), 0.0)
    }

    /** The spec's own worked example: Income 50,000 / Expenses 42,000 / contributions 8,000 / withdrawals 3,000. */
    @Test fun availableAmountMatchesTheSpecsWorkedExample() {
        val tx = listOf(incomeTx(50000.0), expenseTx(40000.0), expenseTx(2000.0))
        val transfers = listOf(transfer(8000.0, TransferDirection.TO_SAVINGS), transfer(3000.0, TransferDirection.FROM_SAVINGS))
        assertEquals(50000.0, AnalyticsEngine.totalIncome(tx), 0.0)
        assertEquals(42000.0, AnalyticsEngine.totalExpense(tx), 0.0)
        assertEquals(5000.0, AnalyticsEngine.savingsBalance(transfers), 0.0)
        assertEquals(3000.0, AnalyticsEngine.availableAmount(tx, transfers), 0.0)
    }

    @Test fun aWithdrawalIncreasesAvailableAmountByExactlyWhatItRemovesFromSavingsBalance() {
        val tx = listOf(incomeTx(10000.0))
        val before = AnalyticsEngine.availableAmount(tx, listOf(transfer(4000.0, TransferDirection.TO_SAVINGS)))
        val after = AnalyticsEngine.availableAmount(tx, listOf(transfer(4000.0, TransferDirection.TO_SAVINGS), transfer(1000.0, TransferDirection.FROM_SAVINGS)))
        assertEquals(1000.0, after - before, 0.0)
    }

    @Test fun transferTotalsOnlyCountsTransfersWithinTheRange() {
        val range = DateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))
        val transfers = listOf(
            transfer(1000.0, TransferDirection.TO_SAVINGS, LocalDate.of(2026, 9, 1)), // in range (start boundary)
            transfer(500.0, TransferDirection.FROM_SAVINGS, LocalDate.of(2026, 9, 30)), // in range (end boundary)
            transfer(2000.0, TransferDirection.TO_SAVINGS, LocalDate.of(2026, 8, 31)), // just before
            transfer(2000.0, TransferDirection.TO_SAVINGS, LocalDate.of(2026, 10, 1)), // just after
        )
        val totals = AnalyticsEngine.transferTotals(transfers, range)
        assertEquals(1000.0, totals.contributions, 0.0)
        assertEquals(500.0, totals.withdrawals, 0.0)
        assertEquals(500.0, totals.net, 0.0)
    }

    // The Savings screen's "Current Year"/"All time" custom-range presets (round 8).
    @Test fun currentYearSpansJan1ToDec31OfTodaysYear() {
        val range = DateRange.currentYear(LocalDate.of(2026, 3, 5))
        assertEquals(LocalDate.of(2026, 1, 1), range.start)
        assertEquals(LocalDate.of(2026, 12, 31), range.end)
    }

    @Test fun allTimeSpansFromTheEarliestKnownDateToToday() {
        val range = DateRange.allTime(LocalDate.of(2024, 6, 1), today)
        assertEquals(LocalDate.of(2024, 6, 1), range.start)
        assertEquals(today, range.end)
    }

    @Test fun allTimeFallsBackToTodayWhenThereIsNothingYet() {
        val range = DateRange.allTime(null, today)
        assertEquals(today, range.start)
        assertEquals(today, range.end)
    }

    // The Stats page's "Last year" custom-range preset (round 9).
    @Test fun lastYearSpansJan1ToDec31OfThePriorYear() {
        val range = DateRange.lastYear(LocalDate.of(2026, 3, 5))
        assertEquals(LocalDate.of(2025, 1, 1), range.start)
        assertEquals(LocalDate.of(2025, 12, 31), range.end)
    }
}
