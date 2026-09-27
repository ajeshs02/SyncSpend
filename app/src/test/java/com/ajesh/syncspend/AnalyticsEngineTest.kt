package com.ajesh.syncspend

import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.data.db.entity.TransactionEntity
import com.ajesh.syncspend.domain.analytics.AnalyticsEngine
import com.ajesh.syncspend.domain.model.CategoryRollup
import com.ajesh.syncspend.domain.model.DateRange
import com.ajesh.syncspend.domain.model.EntryFilter
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.ScopePeriod
import com.ajesh.syncspend.domain.model.StatsRange
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

    @Test fun entryFiltersUseRollingWeeks() {
        val e = AnalyticsEngine.flowFilter(data, FlowType.EXPENSE)
        assertEquals(1, AnalyticsEngine.applyEntryFilter(e, EntryFilter.TODAY, null, today).size)
        assertEquals(1, AnalyticsEngine.applyEntryFilter(e, EntryFilter.YESTERDAY, null, today).size)
        // 8 Sep is exactly 6 days back -> inside "this week"
        assertEquals(3, AnalyticsEngine.applyEntryFilter(e, EntryFilter.THIS_WEEK, null, today).size)
        assertEquals(1, AnalyticsEngine.applyEntryFilter(e, EntryFilter.LAST_MONTH, null, today).size)
    }

    @Test fun lastWeekFilterIsGone() {
        assertEquals(
            listOf("TODAY", "YESTERDAY", "THIS_WEEK", "THIS_MONTH", "LAST_MONTH", "CUSTOM"),
            AnalyticsEngine.entryFilterOptions(FlowType.EXPENSE).map { it.name },
        )
    }

    @Test fun customRangeIsInclusiveOnBothEnds() {
        val e = AnalyticsEngine.flowFilter(data, FlowType.EXPENSE)
        val range = DateRange(LocalDate.of(2026, 8, 28), LocalDate.of(2026, 9, 8))
        val picked = AnalyticsEngine.applyEntryFilter(e, EntryFilter.CUSTOM, range, today)
        assertEquals(2, picked.size) // 28 Aug and 8 Sep, both edges included
        assertEquals(e.size, AnalyticsEngine.applyEntryFilter(e, EntryFilter.CUSTOM, null, today).size)
    }

    @Test fun incomeOnlyOffersAll() {
        assertEquals(listOf(EntryFilter.ALL), AnalyticsEngine.entryFilterOptions(FlowType.INCOME))
        assertEquals(EntryFilter.ALL, AnalyticsEngine.effectiveEntryFilter(EntryFilter.TODAY, FlowType.INCOME))
        assertEquals(EntryFilter.THIS_MONTH, AnalyticsEngine.effectiveEntryFilter(EntryFilter.ALL, FlowType.EXPENSE))
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
        assertTrue(s.savingsRatePercent!! in 94..95) // (10000 - 550) / 10000 = 94.5%
        assertNotNull(s.topCategory)
        assertEquals(250.0, kotlin.math.abs(s.biggestEntry!!.amount), 0.0)
        assertNull(AnalyticsEngine.stats(data, listOf(food), sept, null, FlowType.EXPENSE, today).trendPercent)
    }

    @Test fun statsRangesAreCalendarAlignedWithAMatchingPreviousWindow() {
        val thisMonth = StatsRange.THIS_MONTH.resolve(today, null)
        assertEquals(DateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)), thisMonth)
        assertEquals(DateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)), StatsRange.THIS_MONTH.previous(thisMonth))
        val three = StatsRange.LAST_3_MONTHS.resolve(today, null)
        assertEquals(DateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30)), three)
        assertEquals(DateRange(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30)), StatsRange.LAST_3_MONTHS.previous(three))
        val year = StatsRange.THIS_YEAR.resolve(today, null)
        assertEquals(DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)), StatsRange.THIS_YEAR.previous(year))
        val all = StatsRange.ALL_TIME.resolve(today, LocalDate.of(2025, 3, 3))
        assertEquals(DateRange(LocalDate.of(2025, 3, 3), today), all)
        assertNull(StatsRange.ALL_TIME.previous(all))
        assertEquals("Jul - Sep 2026", StatsRange.LAST_3_MONTHS.describe(three))
        assertEquals("September 2026", StatsRange.THIS_MONTH.describe(thisMonth))
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
}
