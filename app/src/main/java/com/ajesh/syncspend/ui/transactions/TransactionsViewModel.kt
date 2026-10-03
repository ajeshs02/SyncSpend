package com.ajesh.syncspend.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajesh.syncspend.data.datastore.PreferencesRepository
import com.ajesh.syncspend.data.datastore.UserPreferences
import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.data.db.entity.TransactionEntity
import com.ajesh.syncspend.data.db.entity.SubscriptionEntity
import com.ajesh.syncspend.data.db.entity.TransferEntity
import com.ajesh.syncspend.data.repository.CategoryRepository
import com.ajesh.syncspend.data.repository.SubscriptionRepository
import com.ajesh.syncspend.data.repository.TransactionRepository
import com.ajesh.syncspend.data.repository.TransferRepository
import com.ajesh.syncspend.domain.analytics.AnalyticsEngine
import com.ajesh.syncspend.domain.model.CalendarBounds
import com.ajesh.syncspend.domain.model.DateRange
import com.ajesh.syncspend.domain.model.EntryFilter
import com.ajesh.syncspend.domain.model.BillingCycle
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.ScopePeriod
import com.ajesh.syncspend.domain.model.TransactionsTab
import com.ajesh.syncspend.domain.model.TransferDirection
import com.ajesh.syncspend.domain.state.SharedSelectionState
import com.ajesh.syncspend.ui.transfer.toTransferRow
import com.ajesh.syncspend.util.CurrencyFormatter
import com.ajesh.syncspend.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/**
 * The Transactions page's top-right selector: Expense/Income are the real [FlowType]s; Savings is
 * **not** — it's a pure view filter that switches the Entries/Categories tabs to [TransferRepository]
 * data instead of [TransactionRepository] data (see `TransferEntity`'s doc for why Savings stopped
 * being a third [FlowType]). Adding/withdrawing still only ever happens via Home's Transfer action —
 * this filter is for browsing, editing and deleting existing transfers only.
 */
enum class TransactionsFlowFilter { EXPENSE, INCOME, SAVINGS }

private fun TransactionsFlowFilter.toFlowType(): FlowType = when (this) {
    TransactionsFlowFilter.EXPENSE -> FlowType.EXPENSE
    TransactionsFlowFilter.INCOME -> FlowType.INCOME
    TransactionsFlowFilter.SAVINGS -> error("Savings has no FlowType; check viewFilter before calling this")
}

/** One entry in a list (Home's recent list and the Entries tab). */
data class TxRow(
    val id: Long,
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

/** The Entries tab's rows. Null (in the flow) until first computed, so the screen never flashes an empty state. */
data class EntriesUi(val groups: List<DayGroupUi>)

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
    fun entries(ledger: Ledger, flow: FlowType, entryFilter: EntryFilter, customRange: DateRange?, today: LocalDate = LocalDate.now()): EntriesUi {
        val cur = ledger.prefs.currencyCode.symbol
        val categoriesById = ledger.categories.associateBy { it.id }
        val filtered = filtered(ledger, flow, entryFilter, customRange, today)
        return EntriesUi(
            AnalyticsEngine.groupByDay(filtered, today).map { g ->
                DayGroupUi(
                    label = g.label,
                    totalFormatted = cur + CurrencyFormatter.amount(g.totalAbs),
                    items = g.items.map { it.toTxRow(categoriesById, cur) },
                )
            },
        )
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
        transfers: List<TransferEntity>,
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
        val transferTotals = AnalyticsEngine.transferTotals(transfers, resolved)
        return buildStatsUi(summary, scope, ledger.prefs.currencyCode.symbol, monthly, active.size, transferTotals, today)
    }

    /** The Stats page shows both flows stacked (no more Expense/Income toggle) — one [StatsUi] build per flow, sharing the same ledger/scope/transfer totals. */
    fun statsBoth(
        ledger: Ledger,
        subscriptions: List<SubscriptionEntity>,
        transfers: List<TransferEntity>,
        scope: ScopePeriod,
        today: LocalDate = LocalDate.now(),
    ): Pair<StatsUi, StatsUi> =
        stats(ledger, subscriptions, transfers, FlowType.EXPENSE, scope, today) to
            stats(ledger, subscriptions, transfers, FlowType.INCOME, scope, today)

    private fun filtered(ledger: Ledger, flow: FlowType, entryFilter: EntryFilter, customRange: DateRange?, today: LocalDate): List<TransactionEntity> {
        return AnalyticsEngine.applyEntryFilter(AnalyticsEngine.flowFilter(ledger.tx, flow), entryFilter, customRange, today)
    }

    /** The Savings filter's Entries tab: both contributions and withdrawals, reusing the same [TxRow]/[DayGroupUi] shapes as real transactions. */
    fun transferEntries(ledger: TransferLedger, entryFilter: EntryFilter, customRange: DateRange?, today: LocalDate = LocalDate.now()): EntriesUi {
        val cur = ledger.prefs.currencyCode.symbol
        val categoriesById = ledger.incomeCategories.associateBy { it.id }
        val window = AnalyticsEngine.entryFilterWindow(entryFilter, customRange, today)
        val filtered = ledger.transfers.filter { window == null || it.date in window }
        val sorted = filtered.sortedWith(compareByDescending<TransferEntity> { it.date }.thenByDescending { it.createdAt })
        val groups = ArrayList<DayGroupUi>()
        var start = 0
        while (start < sorted.size) {
            val date = sorted[start].date
            var end = start
            var total = 0.0
            while (end < sorted.size && sorted[end].date == date) {
                total += sorted[end].amount
                end++
            }
            groups += DayGroupUi(
                label = AnalyticsEngine.dayLabel(date, today),
                totalFormatted = cur + CurrencyFormatter.amount(total),
                items = sorted.subList(start, end).map { it.toTransferRow(categoriesById, cur) },
            )
            start = end
        }
        return EntriesUi(groups)
    }

    /**
     * The Savings filter's Categories tab: contributions grouped by their source Income category,
     * plus a synthetic "Withdrawals" bucket for withdrawals (which have no category) — kept entirely
     * separate from real Income/Expense category analytics, never merged into [rollups].
     */
    fun transferRollups(ledger: TransferLedger, entryFilter: EntryFilter, customRange: DateRange?, today: LocalDate = LocalDate.now()): RollupsUi {
        val cur = ledger.prefs.currencyCode.symbol
        val window = AnalyticsEngine.entryFilterWindow(entryFilter, customRange, today)
        val filtered = ledger.transfers.filter { window == null || it.date in window }
        val contributions = filtered.filter { it.direction == TransferDirection.TO_SAVINGS }
        val withdrawals = filtered.filter { it.direction == TransferDirection.FROM_SAVINGS }
        val totalAbs = (contributions.sumOf { it.amount } + withdrawals.sumOf { it.amount }).let { if (it == 0.0) 1.0 else it }
        val byCategory = contributions.groupBy { it.categoryId }.map { (categoryId, items) ->
            val category = categoryId?.let { id -> ledger.incomeCategories.find { it.id == id } }
            val sum = items.sumOf { it.amount }
            CategoryRollupUi(
                categoryId = categoryId ?: -1L,
                name = category?.name ?: "Uncategorized",
                iconKey = category?.iconKey ?: "coin",
                count = items.size,
                totalFormatted = cur + CurrencyFormatter.amount(sum),
                sharePercent = ((sum / totalAbs) * 100).roundToInt(),
            )
        }
        val withdrawalsRollup = if (withdrawals.isEmpty()) {
            emptyList()
        } else {
            val sum = withdrawals.sumOf { it.amount }
            listOf(
                CategoryRollupUi(
                    categoryId = -2L,
                    name = "Withdrawals",
                    iconKey = "coin",
                    count = withdrawals.size,
                    totalFormatted = cur + CurrencyFormatter.amount(sum),
                    sharePercent = ((sum / totalAbs) * 100).roundToInt(),
                ),
            )
        }
        return RollupsUi((byCategory + withdrawalsRollup).sortedByDescending { it.sharePercent })
    }
}

/** What the Savings filter's tabs are built from — the Transfer-table equivalent of [Ledger]. */
internal data class TransferLedger(
    val transfers: List<TransferEntity>,
    val incomeCategories: List<CategoryEntity>,
    val prefs: UserPreferences,
)

/**
 * The Entries/Categories tabs' and Stats' date-range selection is a single [ScopePeriod] shared with
 * Home itself (round 10) — picking a period anywhere changes it everywhere, via the same
 * [SharedSelectionState.scope] `MutableStateFlow`. Unlike the old per-tab [EntryFilter] chip sets,
 * a [ScopePeriod] is equally valid for every flow/tab combo, so there's no "reset to this combo's
 * default" clamping needed anymore.
 */
data class PeriodNavUi(val label: String, val canGoPrev: Boolean, val canGoNext: Boolean)

/**
 * UI-only selections (tab, view filter) are plain StateFlows read on the main thread, so a tap moves
 * its pill immediately. Each tab's heavy state is its own flow, computed on `Dispatchers.Default` and
 * only while that tab is on screen (the screen collects just the visible one) — a tab switch no
 * longer rebuilds every row, and a revisited tab still holds its last value.
 */
class TransactionsViewModel(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    preferencesRepository: PreferencesRepository,
    subscriptionRepository: SubscriptionRepository,
    transferRepository: TransferRepository,
    private val selection: SharedSelectionState,
) : ViewModel() {

    // Savings is never written into `selection.flow` (it has no FlowType value for it anyway, and
    // that field is shared app-wide with Home/Add Entry) — it's purely local UI state here, combined
    // with the shared flow below into one [TransactionsFlowFilter] for this screen's own use.
    private val _savingsMode = MutableStateFlow(false)
    private fun currentViewFilter(savings: Boolean = _savingsMode.value, f: FlowType = selection.flow.value) =
        if (savings) TransactionsFlowFilter.SAVINGS
        else if (f == FlowType.EXPENSE) TransactionsFlowFilter.EXPENSE else TransactionsFlowFilter.INCOME

    private val _tab = MutableStateFlow(TransactionsTab.ENTRIES)

    val tab: StateFlow<TransactionsTab> = _tab

    val viewFilter: StateFlow<TransactionsFlowFilter> = combine(selection.flow, _savingsMode) { f, s -> currentViewFilter(s, f) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), currentViewFilter())

    val earliestDate: StateFlow<LocalDate?> = transactionRepository.getEarliestDate()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The period picker's current selection — [SharedSelectionState.scope] directly, shared with Home. */
    val scope: StateFlow<ScopePeriod> = selection.scope

    /** The period pill's label and prev/next arrow enablement, reactive to both [scope] and [earliestDate]. */
    val periodNav: StateFlow<PeriodNavUi> = combine(selection.scope, earliestDate) { scope, earliest ->
        val now = YearMonth.now()
        val lower = CalendarBounds.dataLowerMonth(now, earliest)
        PeriodNavUi(
            label = AnalyticsEngine.scopeLabel(scope),
            canGoPrev = AnalyticsEngine.stepScope(scope, -1, now, lower) != null,
            canGoNext = AnalyticsEngine.stepScope(scope, 1, now, lower) != null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PeriodNavUi(AnalyticsEngine.scopeLabel(selection.scope.value), false, false))

    private val ledger = combine(
        transactionRepository.getAll(),
        categoryRepository.getAll(),
        preferencesRepository.preferences,
    ) { tx, categories, prefs -> Ledger(tx, categories, prefs) }

    private val transferLedger = combine(
        transferRepository.getAll(),
        categoryRepository.getAllByType(FlowType.INCOME),
        preferencesRepository.preferences,
    ) { transfers, incomeCategories, prefs -> TransferLedger(transfers, incomeCategories, prefs) }

    private data class ListControls(val viewFilter: TransactionsFlowFilter, val scope: ScopePeriod, val earliest: LocalDate?)

    private val listControls = combine(viewFilter, selection.scope, earliestDate, ::ListControls)

    // Grouping/sorting thousands of rows must not happen on the main thread. The shared ScopePeriod is
    // resolved to a plain DateRange once here, then fed through EntryFilter.CUSTOM into the existing,
    // unchanged compute functions below — the same adapter CUSTOM itself already used, so none of
    // those functions (or their tests) needed to change shape for this round's picker swap.
    val entries: StateFlow<EntriesUi?> = combine(ledger, transferLedger, listControls) { l, tl, c ->
        val range = AnalyticsEngine.scopeRange(c.scope, LocalDate.now(), c.earliest)
        if (c.viewFilter == TransactionsFlowFilter.SAVINGS) TransactionsCompute.transferEntries(tl, EntryFilter.CUSTOM, range)
        else TransactionsCompute.entries(l, c.viewFilter.toFlowType(), EntryFilter.CUSTOM, range)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val rollups: StateFlow<RollupsUi?> = combine(ledger, transferLedger, listControls) { l, tl, c ->
        val range = AnalyticsEngine.scopeRange(c.scope, LocalDate.now(), c.earliest)
        if (c.viewFilter == TransactionsFlowFilter.SAVINGS) TransactionsCompute.transferRollups(tl, EntryFilter.CUSTOM, range)
        else TransactionsCompute.rollups(l, c.viewFilter.toFlowType(), EntryFilter.CUSTOM, range)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Both flows, stacked on the Stats page (no more Expense/Income toggle there) — expense first, income second. */
    val stats: StateFlow<Pair<StatsUi, StatsUi>?> = combine(
        ledger, subscriptionRepository.getAll(), transferRepository.getAll(), selection.scope,
    ) { l, subs, transfers, scope ->
        TransactionsCompute.statsBoth(l, subs, transfers, scope)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The Transactions page's 3-way selector. Picking Savings never touches [selection]'s shared flow (see this class's doc); picking Expense/Income drops Savings mode and writes through as before. */
    fun pickViewFilter(filterValue: TransactionsFlowFilter) {
        when (filterValue) {
            TransactionsFlowFilter.SAVINGS -> _savingsMode.value = true
            TransactionsFlowFilter.EXPENSE -> { _savingsMode.value = false; selection.flow.value = FlowType.EXPENSE }
            TransactionsFlowFilter.INCOME -> { _savingsMode.value = false; selection.flow.value = FlowType.INCOME }
        }
    }

    fun selectTab(newTab: TransactionsTab) {
        _tab.value = newTab
    }

    /** Picked directly from [PeriodPickerSheet] — writes through to the shared [SharedSelectionState.scope]. */
    fun applyScope(newScope: ScopePeriod) {
        selection.scope.value = newScope
    }

    fun prevPeriod() = stepScope(-1)
    fun nextPeriod() = stepScope(1)

    private fun stepScope(delta: Int) {
        val now = YearMonth.now()
        val lower = CalendarBounds.dataLowerMonth(now, earliestDate.value)
        AnalyticsEngine.stepScope(selection.scope.value, delta, now, lower)?.let { selection.scope.value = it }
    }
}
