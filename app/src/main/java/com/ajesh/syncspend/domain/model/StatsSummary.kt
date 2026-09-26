package com.ajesh.syncspend.domain.model

import com.ajesh.syncspend.data.db.entity.TransactionEntity
import java.time.DayOfWeek
import java.time.YearMonth

data class MonthTotal(val month: YearMonth, val total: Double)

/** The heaviest single day in the range. */
data class DayTotal(val date: java.time.LocalDate, val total: Double, val entryCount: Int)

/** Spending on one weekday over the range: the raw [total] and the average per such day (fair across ranges of any length). */
data class WeekdayStat(val day: DayOfWeek, val total: Double, val average: Double)

/** How much a category changed against the previous period; [delta] > 0 means it grew. */
data class CategoryMover(val categoryId: Long, val name: String, val iconKey: String, val current: Double, val previous: Double) {
    val delta: Double get() = current - previous
}

/** Straight-line forecast for the current month from the days elapsed so far. */
data class MonthProjection(
    val soFar: Double,
    val projected: Double,
    val dayOfMonth: Int,
    val daysInMonth: Int,
    val previousMonthTotal: Double,
)

/** Many small entries that quietly add up: those below [threshold] each. */
data class SmallPurchases(val count: Int, val total: Double, val sharePercent: Int, val threshold: Double)

/** Everything the Stats tab shows, computed once from the range's transactions. */
data class StatsSummary(
    val flow: FlowType,
    val range: DateRange,
    val previousRange: DateRange?,
    val total: Double,
    val previousTotal: Double,
    val entryCount: Int,
    val averagePerEntry: Double,
    val activeDays: Int,
    val dailyAverage: Double,
    /** Signed % change vs the previous period; null when there's nothing to compare against. */
    val trendPercent: Int?,
    val otherFlowTotal: Double,
    /** (income − expense) / income over the range, null when there is no income. */
    val savingsRatePercent: Int?,
    val topCategory: CategoryRollup?,
    val categories: List<CategoryRollup>,
    val biggestEntry: TransactionEntity?,
    /** The largest entries in the range (up to three), biggest first. */
    val topEntries: List<TransactionEntity>,
    val busiestDay: DayTotal?,
    /** Six calendar months ending with the range's last month, oldest first. */
    val monthly: List<MonthTotal>,
    /** Monday first. */
    val weekday: List<WeekdayStat>,
    val movers: List<CategoryMover>,
    val projection: MonthProjection?,
    /** Days in the range (up to today) with nothing logged in this flow. */
    val noSpendDays: Int,
    val smallPurchases: SmallPurchases?,
)
