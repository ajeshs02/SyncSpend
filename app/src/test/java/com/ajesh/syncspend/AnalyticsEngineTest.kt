package com.ajesh.syncspend

import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.data.db.entity.TransactionEntity
import com.ajesh.syncspend.domain.analytics.AnalyticsEngine
import com.ajesh.syncspend.domain.model.DateRange
import com.ajesh.syncspend.domain.model.EntryFilter
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.ScopePeriod
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
        val s = AnalyticsEngine.stats(data, listOf(food, salary), ScopePeriod.Month(YearMonth.of(2026, 9)), FlowType.EXPENSE)
        assertEquals(550.0, s.total, 0.0)
        assertEquals(1500.0, s.previousTotal, 0.0)
        assertEquals(3, s.entryCount)
        assertEquals(3, s.activeDays)
        assertEquals(550.0 / 3, s.dailyAverage, 1e-9)
        assertEquals(-63, s.trendPercent)
        assertEquals(10000.0, s.otherFlowTotal, 0.0)
        assertNotNull(s.topCategory)
        assertEquals(250.0, kotlin.math.abs(s.biggestEntry!!.amount), 0.0)
        assertNull(AnalyticsEngine.stats(data, listOf(food), ScopePeriod.AllTime, FlowType.EXPENSE).trendPercent)
    }
}
