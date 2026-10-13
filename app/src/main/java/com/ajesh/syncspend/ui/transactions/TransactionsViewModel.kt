package com.ajesh.syncspend.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajesh.syncspend.data.datastore.PreferencesRepository
import com.ajesh.syncspend.data.datastore.UserPreferences
import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.data.db.entity.TransactionEntity
import com.ajesh.syncspend.data.db.entity.SubscriptionEntity
import com.ajesh.syncspend.data.repository.CategoryRepository
import com.ajesh.syncspend.data.repository.SubscriptionRepository
import com.ajesh.syncspend.data.repository.TransactionRepository
import com.ajesh.syncspend.domain.analytics.AnalyticsEngine
import com.ajesh.syncspend.domain.model.CalendarBounds
import com.ajesh.syncspend.domain.model.DateRange
import com.ajesh.syncspend.domain.model.EntryFilter
import com.ajesh.syncspend.domain.model.BillingCycle
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.ScopePeriod
import com.ajesh.syncspend.domain.model.TransactionsTab
import com.ajesh.syncspend.domain.state.SharedSelectionState
import com.ajesh.syncspend.util.CurrencyFormatter
import com.ajesh.syncspend.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/** Which field the Entries tab sorts by — see [EntriesUi]'s doc for the rendering split this implies. */
enum class EntriesSortMode { BY_DATE, BY_ENTRY }

/** One entry in a list (Home's recent list, the Entries tab, and the category-entries popup). */
data class TxRow(
    val id: Long,
    val categoryId: Long,
    /** The category's current name: the row's main text. */
    val categoryLabel: String,
    /** The entry's optional note (blank when there is none), wrapped under the category. */
    val note: String,
    val amountFormatted: String,
    val isPositive: Boolean,
    /** "27 Sep", or "27 Sep - 14:35" when the entry's time is known. */
    val dayLabel: String,
    val iconKey: String,
)

/** The row shown for this entry; [categoriesById] resolves the live category name and icon. */
internal fun TransactionEntity.toTxRow(categoriesById: Map<Long, CategoryEntity>, currencySymbol: String): TxRow {
    val category = categoriesById[categoryId]
    return TxRow(
        id = id,
        categoryId = categoryId,
        categoryLabel = category?.name ?: "Deleted category",
        note = description,
        amountFormatted = currencySymbol + CurrencyFormatter.amount(amount),
        isPositive = amount > 0,
        dayLabel = DateUtils.entryDayLabel(date, timeMinuteOfDay),
        iconKey = category?.iconKey ?: "receipt",
    )
}

data class DayGroupUi(val label: String, val totalFormatted: String, val items: List<TxRow>)

data class CategoryRollupUi(
    val categoryId: Long,
    val name: String,
    val iconKey: String,
    val count: Int,
    val totalFormatted: String,
    val sharePercent: Int,
)

/**
 * The Entries tab's rows — also reused by the category-entries popup (same shape, filtered further
 * to one category). [groups] is populated only for [EntriesSortMode.BY_DATE] (date-grouped, with
 * per-day totals); [flatRows] only for [EntriesSortMode.BY_ENTRY] (one flat list, newest-created
 * first — see [TransactionsCompute]'s private `buildEntriesUi` for why a genuinely chronological
 * creation-time sort can't also be date-grouped). [totalFormatted] is always populated — the Overall
 * Total shown beside the date-range label — independent of sort mode. Null (in the flow) until first
 * computed, so the screen never flashes an empty state.
 */
data class EntriesUi(
    val sortMode: EntriesSortMode,
    val groups: List<DayGroupUi>,
    val flatRows: List<TxRow>,
    val totalFormatted: String,
)

/** The Categories tab's rollups. */
data class RollupsUi(val rollups: List<CategoryRollupUi>)

/** What every tab is built from. */
internal data class Ledger(
    val tx: List<TransactionEntity>,
    val categories: List<CategoryEntity>,
    val prefs: UserPreferences,
)

/**
 * The per-tab builders, kept free of coroutines and Android so they can be unit-tested and run on
 * `Dispatchers.Default`. Each tab is built on its own — switching tabs never rebuilds the others.
 */
internal object TransactionsCompute {
    fun entries(
        ledger: Ledger,
        flow: FlowType,
        entryFilter: EntryFilter,
        customRange: DateRange?,
        sortMode: EntriesSortMode,
        today: LocalDate = LocalDate.now(),
    ): EntriesUi {
        val cur = ledger.prefs.currencyCode.symbol
        val categoriesById = ledger.categories.associateBy { it.id }
        val filtered = filtered(ledger, flow, entryFilter, customRange, today)
        return buildEntriesUi(filtered, categoriesById, sortMode, cur, today)
    }

    /**
     * The category-entries popup's rows: the same filtered pipeline as [entries] (same flow +
     * date-range filter), additionally restricted to one category. Always transaction-date order
     * ([EntriesSortMode.BY_DATE]), independent of the main Entries tab's sort selection — the popup
     * never shows a sort control of its own, so there's no [sortMode] parameter to accidentally wire
     * up wrong here.
     */
    fun entriesForCategory(
        ledger: Ledger,
        flow: FlowType,
        entryFilter: EntryFilter,
        customRange: DateRange?,
        categoryId: Long,
        today: LocalDate = LocalDate.now(),
    ): EntriesUi {
        val cur = ledger.prefs.currencyCode.symbol
        val categoriesById = ledger.categories.associateBy { it.id }
        val filtered = filtered(ledger, flow, entryFilter, customRange, today).filter { it.categoryId == categoryId }
        return buildEntriesUi(filtered, categoriesById, EntriesSortMode.BY_DATE, cur, today)
    }

    /**
     * Shared by [entries] and [entriesForCategory]: [totalFormatted] is `sum of abs(amount)` over
     * [rows] — the same convention the per-day group totals already use — computed once here so it's
     * trivially consistent regardless of sort mode or whether this is the full list or one category's.
     */
    private fun buildEntriesUi(
        rows: List<TransactionEntity>,
        categoriesById: Map<Long, CategoryEntity>,
        sortMode: EntriesSortMode,
        cur: String,
        today: LocalDate,
    ): EntriesUi {
        val totalFormatted = cur + CurrencyFormatter.amount(rows.sumOf { abs(it.amount) })
        return when (sortMode) {
            EntriesSortMode.BY_DATE -> EntriesUi(
                sortMode = sortMode,
                groups = AnalyticsEngine.groupByDay(rows, today).map { g ->
                    DayGroupUi(
                        label = g.label,
                        totalFormatted = cur + CurrencyFormatter.amount(g.totalAbs),
                        items = g.items.map { it.toTxRow(categoriesById, cur) },
                    )
                },
                flatRows = emptyList(),
                totalFormatted = totalFormatted,
            )
            EntriesSortMode.BY_ENTRY -> EntriesUi(
                sortMode = sortMode,
                groups = emptyList(),
                // id (autoincrement, always unique) as a deterministic tie-break for rows that happen
                // to share an identical createdAt millisecond — without it, sortedByDescending's stable
                // sort would leave ties in whatever arbitrary pre-sort order they arrived in.
                flatRows = rows.sortedWith(compareByDescending<TransactionEntity> { it.createdAt }.thenByDescending { it.id })
                    .map { it.toTxRow(categoriesById, cur) },
                totalFormatted = totalFormatted,
            )
        }
    }

    fun rollups(ledger: Ledger, flow: FlowType, entryFilter: EntryFilter, customRange: DateRange?, today: LocalDate = LocalDate.now()): RollupsUi {
        val cur = ledger.prefs.currencyCode.symbol
        val filtered = filtered(ledger, flow, entryFilter, customRange, today)
        return RollupsUi(
            AnalyticsEngine.groupByCategory(filtered, ledger.categories).map {
                CategoryRollupUi(it.categoryId, it.name, it.iconKey, it.count, cur + CurrencyFormatter.amount(it.totalAbs), it.sharePercent)
            },
        )
    }

    fun stats(
        ledger: Ledger,
        subscriptions: List<SubscriptionEntity>,
        flow: FlowType,
        scope: ScopePeriod,
        today: LocalDate = LocalDate.now(),
    ): StatsUi {
        val active = subscriptions.filter { it.active }
        val monthly = active.sumOf {
            when (it.billingCycle) {
                BillingCycle.WEEKLY -> it.amount * 52.0 / 12.0
                BillingCycle.MONTHLY -> it.amount
                BillingCycle.YEARLY -> it.amount / 12.0
            }
        }
        val earliest = ledger.tx.minOfOrNull { it.date }
        val resolved = AnalyticsEngine.scopeRange(scope, today, earliest)
        val previous = AnalyticsEngine.previousScope(scope)?.let { AnalyticsEngine.scopeRange(it, today, earliest) }
        val summary = AnalyticsEngine.stats(ledger.tx, ledger.categories, resolved, previous, flow, today)
        return buildStatsUi(summary, scope, ledger.prefs.currencyCode.symbol, monthly, active.size, today)
    }

    /** The Stats page shows both flows stacked (no more Expense/Income toggle) — one [StatsUi] build per flow, sharing the same ledger/scope. */
    fun statsBoth(
        ledger: Ledger,
        subscriptions: List<SubscriptionEntity>,
        scope: ScopePeriod,
        today: LocalDate = LocalDate.now(),
    ): Pair<StatsUi, StatsUi> =
        stats(ledger, subscriptions, FlowType.EXPENSE, scope, today) to
            stats(ledger, subscriptions, FlowType.INCOME, scope, today)

    private fun filtered(ledger: Ledger, flow: FlowType, entryFilter: EntryFilter, customRange: DateRange?, today: LocalDate): List<TransactionEntity> {
        return AnalyticsEngine.applyEntryFilter(AnalyticsEngine.flowFilter(ledger.tx, flow), entryFilter, customRange, today)
    }
}

/**
 * The Entries/Categories tabs' and Stats' date-range selection — Transactions' own independent scope
 * ([SharedSelectionState.transactionsScope]), separate from Home's own ([SharedSelectionState.homeScope]).
 */
data class PeriodNavUi(val label: String, val canGoPrev: Boolean, val canGoNext: Boolean)

/**
 * UI-only selections (tab, view filter, sort mode) are plain StateFlows read on the main thread, so a
 * tap moves its pill immediately. Each tab's heavy state is its own flow, computed on
 * `Dispatchers.Default` and only while that tab is on screen (the screen collects just the visible
 * one) — a tab switch no longer rebuilds every row, and a revisited tab still holds its last value.
 */
class TransactionsViewModel(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    preferencesRepository: PreferencesRepository,
    subscriptionRepository: SubscriptionRepository,
    private val selection: SharedSelectionState,
) : ViewModel() {

    private val _tab = MutableStateFlow(TransactionsTab.ENTRIES)
    val tab: StateFlow<TransactionsTab> = _tab

    private val _sortMode = MutableStateFlow(EntriesSortMode.BY_DATE)
    val sortMode: StateFlow<EntriesSortMode> = _sortMode

    /** Transactions' own independent Expense/Income toggle — never shared with Home. */
    val viewFilter: StateFlow<FlowType> = selection.transactionsFlow

    val earliestDate: StateFlow<LocalDate?> = transactionRepository.getEarliestDate()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The period picker's current selection — Transactions' own independent scope, never shared with Home. */
    val scope: StateFlow<ScopePeriod> = selection.transactionsScope

    /** The period pill's label and prev/next arrow enablement, reactive to both [scope] and [earliestDate]. */
    val periodNav: StateFlow<PeriodNavUi> = combine(selection.transactionsScope, earliestDate) { scope, earliest ->
        val now = YearMonth.now()
        val lower = CalendarBounds.dataLowerMonth(now, earliest)
        PeriodNavUi(
            label = AnalyticsEngine.scopeLabel(scope),
            canGoPrev = AnalyticsEngine.stepScope(scope, -1, now, lower) != null,
            canGoNext = AnalyticsEngine.stepScope(scope, 1, now, lower) != null,
        )
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000),
        PeriodNavUi(AnalyticsEngine.scopeLabel(selection.transactionsScope.value), false, false),
    )

    private val ledger = combine(
        transactionRepository.getAll(),
        categoryRepository.getAll(),
        preferencesRepository.preferences,
    ) { tx, categories, prefs -> Ledger(tx, categories, prefs) }

    private data class ListControls(val viewFilter: FlowType, val scope: ScopePeriod, val earliest: LocalDate?, val sortMode: EntriesSortMode)

    private val listControls = combine(viewFilter, selection.transactionsScope, earliestDate, sortMode, ::ListControls)

    // Grouping/sorting thousands of rows must not happen on the main thread. The scope is resolved to
    // a plain DateRange once here, then fed through EntryFilter.CUSTOM into the compute functions below.
    val entries: StateFlow<EntriesUi?> = combine(ledger, listControls) { l, c ->
        val range = AnalyticsEngine.scopeRange(c.scope, LocalDate.now(), c.earliest)
        TransactionsCompute.entries(l, c.viewFilter, EntryFilter.CUSTOM, range, c.sortMode)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val rollups: StateFlow<RollupsUi?> = combine(ledger, listControls) { l, c ->
        val range = AnalyticsEngine.scopeRange(c.scope, LocalDate.now(), c.earliest)
        TransactionsCompute.rollups(l, c.viewFilter, EntryFilter.CUSTOM, range)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Both flows, stacked on the Stats page (no more Expense/Income toggle there) — expense first, income second. */
    val stats: StateFlow<Pair<StatsUi, StatsUi>?> = combine(
        ledger, subscriptionRepository.getAll(), selection.transactionsScope,
    ) { l, subs, scope ->
        TransactionsCompute.statsBoth(l, subs, scope)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * The category-entries popup, opened from the Categories tab: a fresh, cheap, short-lived
     * `StateFlow` per distinct category — driven by the same reactive [ledger]/[listControls] as
     * [entries], so any edit/delete made through Edit Entry automatically re-flows through here too,
     * with no manual refresh call needed.
     */
    fun entriesForCategory(categoryId: Long): StateFlow<EntriesUi?> =
        combine(ledger, listControls) { l, c ->
            val range = AnalyticsEngine.scopeRange(c.scope, LocalDate.now(), c.earliest)
            TransactionsCompute.entriesForCategory(l, c.viewFilter, EntryFilter.CUSTOM, range, categoryId)
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun pickViewFilter(flowValue: FlowType) {
        selection.transactionsFlow.value = flowValue
    }

    fun setSortMode(mode: EntriesSortMode) {
        _sortMode.value = mode
    }

    fun selectTab(newTab: TransactionsTab) {
        _tab.value = newTab
    }

    /** Picked directly from [PeriodPickerSheet] — writes through to Transactions' own independent scope. */
    fun applyScope(newScope: ScopePeriod) {
        selection.transactionsScope.value = newScope
    }

    fun prevPeriod() = stepScope(-1)
    fun nextPeriod() = stepScope(1)

    private fun stepScope(delta: Int) {
        val now = YearMonth.now()
        val lower = CalendarBounds.dataLowerMonth(now, earliestDate.value)
        AnalyticsEngine.stepScope(selection.transactionsScope.value, delta, now, lower)?.let { selection.transactionsScope.value = it }
    }
}
