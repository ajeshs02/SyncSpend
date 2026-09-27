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
import com.ajesh.syncspend.domain.model.DateRange
import com.ajesh.syncspend.domain.model.EntryFilter
import com.ajesh.syncspend.domain.model.BillingCycle
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.StatsRange
import com.ajesh.syncspend.domain.model.TransactionsTab
import com.ajesh.syncspend.domain.state.SharedSelectionState
import com.ajesh.syncspend.util.CurrencyFormatter
import com.ajesh.syncspend.util.DateUtils
import java.time.LocalDate
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

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

    fun stats(ledger: Ledger, subscriptions: List<SubscriptionEntity>, flow: FlowType, range: StatsRange, today: LocalDate = LocalDate.now()): StatsUi {
        val active = subscriptions.filter { it.active }
        val monthly = active.sumOf {
            when (it.billingCycle) {
                BillingCycle.WEEKLY -> it.amount * 52.0 / 12.0
                BillingCycle.MONTHLY -> it.amount
                BillingCycle.YEARLY -> it.amount / 12.0
            }
        }
        val resolved = range.resolve(today, ledger.tx.minOfOrNull { it.date })
        val summary = AnalyticsEngine.stats(ledger.tx, ledger.categories, resolved, range.previous(resolved), flow, today)
        return buildStatsUi(summary, range, ledger.prefs.currencyCode.symbol, monthly, active.size)
    }

    private fun filtered(ledger: Ledger, flow: FlowType, entryFilter: EntryFilter, customRange: DateRange?, today: LocalDate): List<TransactionEntity> {
        val effective = AnalyticsEngine.effectiveEntryFilter(entryFilter, flow)
        return AnalyticsEngine.applyEntryFilter(AnalyticsEngine.flowFilter(ledger.tx, flow), effective, customRange, today)
    }
}

/**
 * UI-only selections (tab, filter chips, stats range) are plain StateFlows read on the main thread,
 * so a tap moves its pill immediately. Each tab's heavy state is its own flow, computed on
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
    private val _entryFilter = MutableStateFlow(EntryFilter.THIS_MONTH)
    private val _customRange = MutableStateFlow<DateRange?>(null)
    private val _statsRange = MutableStateFlow(StatsRange.THIS_MONTH)

    val tab: StateFlow<TransactionsTab> = _tab
    val entryFilter: StateFlow<EntryFilter> = _entryFilter
    /** The picked from/to span while the filter is CUSTOM (kept when switching chips so it can be re-applied). */
    val customRange: StateFlow<DateRange?> = _customRange
    val statsRange: StateFlow<StatsRange> = _statsRange
    val flow: StateFlow<FlowType> = selection.flow

    val earliestDate: StateFlow<LocalDate?> = transactionRepository.getEarliestDate()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val ledger = combine(
        transactionRepository.getAll(),
        categoryRepository.getAll(),
        preferencesRepository.preferences,
    ) { tx, categories, prefs -> Ledger(tx, categories, prefs) }

    private data class ListControls(val flow: FlowType, val entryFilter: EntryFilter, val customRange: DateRange?)

    private val listControls = combine(selection.flow, _entryFilter, _customRange) { f, ef, cr -> ListControls(f, ef, cr) }

    // Grouping/sorting thousands of rows must not happen on the main thread.
    val entries: StateFlow<EntriesUi?> = combine(ledger, listControls) { l, c ->
        TransactionsCompute.entries(l, c.flow, c.entryFilter, c.customRange)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val rollups: StateFlow<RollupsUi?> = combine(ledger, listControls) { l, c ->
        TransactionsCompute.rollups(l, c.flow, c.entryFilter, c.customRange)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val stats: StateFlow<StatsUi?> = combine(ledger, subscriptionRepository.getAll(), selection.flow, _statsRange) { l, subs, flowType, range ->
        TransactionsCompute.stats(l, subs, flowType, range)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setFlow(type: FlowType) {
        selection.flow.value = type
    }

    fun selectTab(newTab: TransactionsTab) {
        _tab.value = newTab
    }

    /** Any chip except Custom (Custom goes through the range picker, see [applyCustomRange]). */
    fun pickEntryFilter(filter: EntryFilter) {
        _entryFilter.value = filter
    }

    fun selectStatsRange(range: StatsRange) {
        _statsRange.value = range
    }

    fun applyCustomRange(range: DateRange) {
        _customRange.value = range
        _entryFilter.value = EntryFilter.CUSTOM
    }
}
