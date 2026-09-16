package com.ajesh.syncspend.domain.analytics

import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.data.db.entity.TransactionEntity
import com.ajesh.syncspend.domain.model.CategoryRollup
import com.ajesh.syncspend.domain.model.DayGroup
import com.ajesh.syncspend.domain.model.EntryFilter
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.ScopePeriod
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
        is ScopePeriod.Month -> YearMonth.from(tx.date) == scope.yearMonth
        is ScopePeriod.Year -> tx.date.year == scope.year
        ScopePeriod.AllTime -> true
    }

    fun scopeFilter(tx: List<TransactionEntity>, scope: ScopePeriod): List<TransactionEntity> =
        tx.filter { inScope(it, scope) }

    fun flowFilter(tx: List<TransactionEntity>, flow: FlowType): List<TransactionEntity> =
        tx.filter { if (flow == FlowType.INCOME) it.amount > 0 else it.amount < 0 }

    /** null for AllTime — there's no "previous all-time" to compare against. */
    fun previousScope(scope: ScopePeriod): ScopePeriod? = when (scope) {
        is ScopePeriod.Month -> ScopePeriod.Month(scope.yearMonth.minusMonths(1))
        is ScopePeriod.Year -> ScopePeriod.Year(scope.year - 1)
        ScopePeriod.AllTime -> null
    }

    fun scopeLabel(scope: ScopePeriod): String = when (scope) {
        is ScopePeriod.Month -> "${scope.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.US)} ${scope.yearMonth.year}"
        is ScopePeriod.Year -> scope.year.toString()
        ScopePeriod.AllTime -> "All time"
    }

    fun entryFilterOptions(flow: FlowType): List<EntryFilter> = if (flow == FlowType.INCOME) {
        listOf(EntryFilter.ALL)
    } else {
        listOf(
            EntryFilter.TODAY, EntryFilter.YESTERDAY, EntryFilter.THIS_WEEK, EntryFilter.LAST_WEEK,
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
        scope: ScopePeriod,
        today: LocalDate = LocalDate.now(),
    ): List<TransactionEntity> = when (filter) {
        EntryFilter.ALL -> tx
        EntryFilter.TODAY -> tx.filter { it.date == today }
        EntryFilter.YESTERDAY -> tx.filter { it.date == today.minusDays(1) }
        EntryFilter.THIS_WEEK -> tx.filter { !it.date.isBefore(today.minusDays(6)) && !it.date.isAfter(today) }
        EntryFilter.LAST_WEEK -> tx.filter { !it.date.isBefore(today.minusDays(13)) && !it.date.isAfter(today.minusDays(7)) }
        EntryFilter.THIS_MONTH -> tx.filter { YearMonth.from(it.date) == YearMonth.from(today) }
        EntryFilter.LAST_MONTH -> tx.filter { YearMonth.from(it.date) == YearMonth.from(today).minusMonths(1) }
        EntryFilter.CUSTOM -> scopeFilter(tx, scope)
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
}
