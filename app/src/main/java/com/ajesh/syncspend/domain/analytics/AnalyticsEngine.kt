package com.ajesh.syncspend.domain.analytics

import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.data.db.entity.TransactionEntity
import com.ajesh.syncspend.domain.model.CategoryMover
import com.ajesh.syncspend.domain.model.CategoryRollup
import com.ajesh.syncspend.domain.model.DayTotal
import com.ajesh.syncspend.domain.model.DateRange
import com.ajesh.syncspend.domain.model.DayGroup
import com.ajesh.syncspend.domain.model.EntryFilter
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.MonthProjection
import com.ajesh.syncspend.domain.model.MonthTotal
import com.ajesh.syncspend.domain.model.ScopePeriod
import com.ajesh.syncspend.domain.model.SmallPurchases
import com.ajesh.syncspend.domain.model.StatsSummary
import com.ajesh.syncspend.domain.model.WeekdayStat
import com.ajesh.syncspend.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pure-Kotlin port of the design's reference analytics logic (scope/flow/
 * entry filters, day/category grouping, trend%, share%, findings). Kept as
 * plain list processing over broad DAO queries rather than per-scope SQL —
 * data volume for a personal tracker is small, and this keeps the port
 * traceable line-for-line against the design's JS.
 */
object AnalyticsEngine {

    fun inScope(tx: TransactionEntity, scope: ScopePeriod): Boolean = when (scope) {
        is ScopePeriod.Month -> inMonth(tx.date, scope.yearMonth)
        is ScopePeriod.Year -> tx.date.year == scope.year
        is ScopePeriod.LastMonths -> tx.date.year * 12 + tx.date.monthValue in
            (scope.startMonth.year * 12 + scope.startMonth.monthValue)..(scope.endMonth.year * 12 + scope.endMonth.monthValue)
        ScopePeriod.AllTime -> true
    }

    /** Year/month compare without allocating a YearMonth per transaction (this runs over every row). */
    private fun inMonth(date: LocalDate, month: YearMonth): Boolean = date.year == month.year && date.monthValue == month.monthValue

    fun scopeFilter(tx: List<TransactionEntity>, scope: ScopePeriod): List<TransactionEntity> =
        tx.filter { inScope(it, scope) }

    fun flowFilter(tx: List<TransactionEntity>, flow: FlowType): List<TransactionEntity> =
        tx.filter { if (flow == FlowType.INCOME) it.amount > 0 else it.amount < 0 }

    /** null for AllTime — there's no "previous all-time" to compare against. */
    fun previousScope(scope: ScopePeriod): ScopePeriod? = when (scope) {
        is ScopePeriod.Month -> ScopePeriod.Month(scope.yearMonth.minusMonths(1))
        is ScopePeriod.Year -> ScopePeriod.Year(scope.year - 1)
        is ScopePeriod.LastMonths -> ScopePeriod.LastMonths(scope.months, scope.endMonth.minusMonths(scope.months.toLong()))
        ScopePeriod.AllTime -> null
    }

    /**
     * Moves [scope] one step back (-1) or forward (+1): a month, a year, or the
     * end of a last-N-months window. Returns null at the edges — nothing after
     * [now], nothing before [lower] (the earliest month worth browsing).
     */
    fun stepScope(scope: ScopePeriod, delta: Int, now: YearMonth, lower: YearMonth): ScopePeriod? = when (scope) {
        is ScopePeriod.Month -> scope.yearMonth.plusMonths(delta.toLong())
            .takeIf { !it.isAfter(now) && !it.isBefore(lower) }?.let { ScopePeriod.Month(it) }
        is ScopePeriod.Year -> (scope.year + delta)
            .takeIf { it <= now.year && it >= lower.year }?.let { ScopePeriod.Year(it) }
        is ScopePeriod.LastMonths -> ScopePeriod.LastMonths(scope.months, scope.endMonth.plusMonths(delta.toLong()))
            .takeIf { !it.endMonth.isAfter(now) && !it.startMonth.isBefore(lower) }
        ScopePeriod.AllTime -> null
    }

    /** "Last 3 months" while the window ends this month, otherwise its span ("Apr – Jun 2026"). */
    fun scopeLabel(scope: ScopePeriod, now: YearMonth = YearMonth.now()): String = when (scope) {
        is ScopePeriod.LastMonths ->
            if (scope.endMonth == now) "Last ${scope.months} months" else DateUtils.monthSpanLabel(scope.startMonth, scope.endMonth)
        else -> scopeLabelPlain(scope)
    }

    private fun scopeLabelPlain(scope: ScopePeriod): String = when (scope) {
        is ScopePeriod.Month -> "${scope.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.US)} ${scope.yearMonth.year}"
        is ScopePeriod.Year -> scope.year.toString()
        is ScopePeriod.LastMonths -> DateUtils.monthSpanLabel(scope.startMonth, scope.endMonth)
        ScopePeriod.AllTime -> "All time"
    }

    fun entryFilterOptions(flow: FlowType): List<EntryFilter> = if (flow == FlowType.INCOME) {
        listOf(EntryFilter.ALL)
    } else {
        listOf(
            EntryFilter.TODAY, EntryFilter.YESTERDAY, EntryFilter.THIS_WEEK,
            EntryFilter.THIS_MONTH, EntryFilter.LAST_MONTH, EntryFilter.CUSTOM,
        )
    }

    /** Falls back to THIS_MONTH (or ALL for income) if the stored filter isn't valid for [flow] — mirrors the design's `effFilter`. */
    fun effectiveEntryFilter(stored: EntryFilter, flow: FlowType): EntryFilter {
        val options = entryFilterOptions(flow)
        if (stored in options) return stored
        return if (flow == FlowType.INCOME) EntryFilter.ALL else EntryFilter.THIS_MONTH
    }

    fun applyEntryFilter(
        tx: List<TransactionEntity>,
        filter: EntryFilter,
        custom: DateRange?,
        today: LocalDate = LocalDate.now(),
    ): List<TransactionEntity> = when (filter) {
        EntryFilter.ALL -> tx
        EntryFilter.TODAY -> tx.filter { it.date == today }
        EntryFilter.YESTERDAY -> tx.filter { it.date == today.minusDays(1) }
        EntryFilter.THIS_WEEK -> tx.filter { !it.date.isBefore(today.minusDays(6)) && !it.date.isAfter(today) }
        EntryFilter.THIS_MONTH -> YearMonth.from(today).let { month -> tx.filter { inMonth(it.date, month) } }
        EntryFilter.LAST_MONTH -> YearMonth.from(today).minusMonths(1).let { month -> tx.filter { inMonth(it.date, month) } }
        EntryFilter.CUSTOM -> if (custom == null) tx else tx.filter { it.date in custom }
    }

    fun groupByDay(tx: List<TransactionEntity>, today: LocalDate = LocalDate.now()): List<DayGroup> {
        val sorted = tx.sortedWith(compareByDescending<TransactionEntity> { it.date }.thenByDescending { it.createdAt })
        val order = LinkedHashMap<String, MutableList<TransactionEntity>>()
        for (t in sorted) {
            val label = dayLabel(t.date, today)
            order.getOrPut(label) { mutableListOf() }.add(t)
        }
        return order.map { (label, items) -> DayGroup(label, items, items.sumOf { abs(it.amount) }) }
    }

    private fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> "${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.SHORT, Locale.US)} ${date.year}"
    }

    fun groupByCategory(tx: List<TransactionEntity>, categories: List<CategoryEntity>): List<CategoryRollup> {
        val total = tx.sumOf { abs(it.amount) }.let { if (it == 0.0) 1.0 else it }
        return tx.groupBy { it.categoryId }
            .map { (categoryId, items) ->
                val category = categories.find { it.id == categoryId }
                val sum = items.sumOf { abs(it.amount) }
                CategoryRollup(
                    categoryId = categoryId,
                    name = category?.name ?: "Deleted category",
                    iconKey = category?.iconKey ?: "receipt",
                    count = items.size,
                    totalAbs = sum,
                    sharePercent = ((sum / total) * 100).roundToInt(),
                )
            }
            .sortedByDescending { it.totalAbs }
    }

    fun trendPercent(current: Double, previous: Double): Int? =
        if (previous > 0) (((current - previous) / previous) * 100).roundToInt() else null

    fun dailyAverage(spend: Double, tx: List<TransactionEntity>): Double {
        val activeDays = tx.map { it.date }.distinct().size.coerceAtLeast(1)
        return spend / activeDays
    }

    fun biggestEntry(tx: List<TransactionEntity>): TransactionEntity? = tx.maxByOrNull { abs(it.amount) }

    /**
     * Builds the Stats tab from the whole transaction list for one [range] (and
     * the [previous] window to compare against): flow filtering, category rollup,
     * daily average over distinct active days, largest single entry, the
     * opposite-flow total for the net figure, plus the six-month series, weekday
     * pattern, category movers, month projection and small-purchase share.
     * Pure — no Android or formatting concerns.
     */
    fun stats(
        all: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        range: DateRange,
        previous: DateRange?,
        flow: FlowType,
        today: LocalDate = LocalDate.now(),
    ): StatsSummary {
        val flowTx = flowFilter(all, flow)
        val inRange = flowTx.filter { it.date in range }
        val previousTx = if (previous == null) emptyList() else flowTx.filter { it.date in previous }
        val total = inRange.sumOf { abs(it.amount) }
        val previousTotal = previousTx.sumOf { abs(it.amount) }
        val opposite = FlowType.values().first { it != flow }
        val otherTotal = flowFilter(all, opposite).filter { it.date in range }.sumOf { abs(it.amount) }
        val income = if (flow == FlowType.INCOME) total else otherTotal
        val expense = if (flow == FlowType.EXPENSE) total else otherTotal
        val activeDates = inRange.mapTo(HashSet()) { it.date }
        val rollups = groupByCategory(inRange, categories)
        val average = if (inRange.isEmpty()) 0.0 else total / inRange.size

        return StatsSummary(
            flow = flow,
            range = range,
            previousRange = previous,
            total = total,
            previousTotal = previousTotal,
            entryCount = inRange.size,
            averagePerEntry = average,
            activeDays = activeDates.size,
            dailyAverage = total / activeDates.size.coerceAtLeast(1),
            trendPercent = trendPercent(total, previousTotal),
            otherFlowTotal = otherTotal,
            savingsRatePercent = if (income > 0) (((income - expense) / income) * 100).roundToInt() else null,
            topCategory = rollups.firstOrNull(),
            categories = rollups,
            biggestEntry = biggestEntry(inRange),
            topEntries = topEntries(inRange),
            busiestDay = busiestDay(inRange),
            monthly = monthlySeries(flowTx, YearMonth.from(minOf(range.end, today))),
            weekday = weekdayPattern(inRange, range, today),
            movers = if (previous != null && previousTotal > 0) categoryMovers(rollups, groupByCategory(previousTx, categories)) else emptyList(),
            projection = monthProjection(inRange, range, previousTotal, today),
            noSpendDays = noSpendDays(activeDates, range, today),
            smallPurchases = smallPurchases(inRange, total, average),
        )
    }

    /** The [limit] largest entries by absolute amount, biggest first. */
    fun topEntries(tx: List<TransactionEntity>, limit: Int = 3): List<TransactionEntity> =
        tx.sortedByDescending { abs(it.amount) }.take(limit)

    /** The single day with the highest total, or null for no entries (earliest such day wins a tie). */
    fun busiestDay(tx: List<TransactionEntity>): DayTotal? =
        tx.groupBy { it.date }
            .map { (date, items) -> DayTotal(date, items.sumOf { abs(it.amount) }, items.size) }
            .sortedWith(compareByDescending<DayTotal> { it.total }.thenBy { it.date })
            .firstOrNull()

    /** Totals for the [count] calendar months ending at [end], oldest first (empty months are zero). */
    fun monthlySeries(flowTx: List<TransactionEntity>, end: YearMonth, count: Int = 6): List<MonthTotal> {
        val first = end.minusMonths(count - 1L)
        val from = first.atDay(1)
        val to = end.atEndOfMonth()
        val totals = HashMap<Int, Double>()
        for (t in flowTx) {
            if (t.date.isBefore(from) || t.date.isAfter(to)) continue
            totals.merge(t.date.year * 12 + t.date.monthValue, abs(t.amount), Double::plus)
        }
        return List(count) { i ->
            val m = first.plusMonths(i.toLong())
            MonthTotal(m, totals[m.year * 12 + m.monthValue] ?: 0.0)
        }
    }

    /**
     * Spend per weekday (Monday first). The average divides by how many of that
     * weekday fall in the range up to [today], so a range containing five
     * Saturdays but four Sundays isn't skewed.
     */
    fun weekdayPattern(tx: List<TransactionEntity>, range: DateRange, today: LocalDate): List<WeekdayStat> {
        val totals = DoubleArray(7)
        for (t in tx) totals[t.date.dayOfWeek.ordinal] += abs(t.amount)
        val end = minOf(range.end, today)
        val days = if (end.isBefore(range.start)) 0L else java.time.temporal.ChronoUnit.DAYS.between(range.start, end) + 1
        val startIndex = range.start.dayOfWeek.ordinal
        return java.time.DayOfWeek.values().map { day ->
            val i = day.ordinal
            val occurrences = days / 7 + if (((i - startIndex + 7) % 7) < days % 7) 1 else 0
            WeekdayStat(day, totals[i], if (occurrences > 0) totals[i] / occurrences else 0.0)
        }
    }

    /** The biggest absolute changes per category against the previous period (up to [limit]). */
    fun categoryMovers(current: List<CategoryRollup>, previous: List<CategoryRollup>, limit: Int = 3): List<CategoryMover> {
        val now = current.associateBy { it.categoryId }
        val before = previous.associateBy { it.categoryId }
        return (now.keys + before.keys).map { id ->
            val ref = now[id] ?: before.getValue(id)
            CategoryMover(id, ref.name, ref.iconKey, now[id]?.totalAbs ?: 0.0, before[id]?.totalAbs ?: 0.0)
        }.filter { it.delta != 0.0 }.sortedByDescending { abs(it.delta) }.take(limit)
    }

    /** Only for exactly the current calendar month, and only once there are enough days to extrapolate from. */
    fun monthProjection(inRange: List<TransactionEntity>, range: DateRange, previousMonthTotal: Double, today: LocalDate): MonthProjection? {
        val month = YearMonth.from(today)
        if (range.start != month.atDay(1) || range.end != month.atEndOfMonth() || today.dayOfMonth < MIN_PROJECTION_DAY) return null
        val soFar = inRange.filter { !it.date.isAfter(today) }.sumOf { abs(it.amount) }
        return MonthProjection(soFar, soFar / today.dayOfMonth * month.lengthOfMonth(), today.dayOfMonth, month.lengthOfMonth(), previousMonthTotal)
    }

    fun noSpendDays(activeDates: Set<LocalDate>, range: DateRange, today: LocalDate): Int {
        val end = minOf(range.end, today)
        if (end.isBefore(range.start)) return 0
        val days = java.time.temporal.ChronoUnit.DAYS.between(range.start, end) + 1
        return (days - activeDates.count { !it.isAfter(end) }).toInt().coerceAtLeast(0)
    }

    /** Entries below 30% of the average entry, if there are enough of them to matter. */
    fun smallPurchases(inRange: List<TransactionEntity>, total: Double, average: Double): SmallPurchases? {
        if (inRange.size < 8 || total <= 0) return null
        val threshold = average * 0.3
        val small = inRange.filter { abs(it.amount) < threshold }
        if (small.size < 5) return null
        val sum = small.sumOf { abs(it.amount) }
        return SmallPurchases(small.size, sum, ((sum / total) * 100).roundToInt(), threshold)
    }

    private const val MIN_PROJECTION_DAY = 5
}
