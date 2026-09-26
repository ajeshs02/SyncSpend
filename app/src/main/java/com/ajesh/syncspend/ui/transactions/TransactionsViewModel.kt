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
import com.ajesh.syncspend.domain.model.ScopePeriod
import com.ajesh.syncspend.domain.state.SharedSelectionState
import com.ajesh.syncspend.util.CurrencyFormatter
import java.time.LocalDate
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

enum class TransactionsTab { ENTRIES, CATEGORIES, ANALYTICS }

data class TxRow(
    val id: Long,
    val name: String,
    val categoryLabel: String,
    val amountFormatted: String,
    val isPositive: Boolean,
    val dayLabel: String,
    val iconKey: String,
)

data class DayGroupUi(val label: String, val totalFormatted: String, val items: List<TxRow>)

data class CategoryRollupUi(
    val categoryId: Long,
    val name: String,
    val iconKey: String,
    val count: Int,
    val totalFormatted: String,
    val sharePercent: Int,
)

data class TransactionsUiState(
    val flow: FlowType = FlowType.EXPENSE,
    val tab: TransactionsTab = TransactionsTab.ENTRIES,
    val entryFilter: EntryFilter = EntryFilter.THIS_MONTH,
    val entryFilterOptions: List<EntryFilter> = emptyList(),
    /** The picked from/to span while [entryFilter] is CUSTOM (kept when switching chips so it can be re-applied). */
    val customRange: DateRange? = null,
    val dayGroups: List<DayGroupUi> = emptyList(),
    val categoryRollups: List<CategoryRollupUi> = emptyList(),
    val currencySymbol: String = "₹",
    val earliestTransactionDate: LocalDate? = null,
    val stats: StatsUi? = null,
)

class TransactionsViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesRepository: PreferencesRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val selection: SharedSelectionState,
) : ViewModel() {

    private val tab = MutableStateFlow(TransactionsTab.ENTRIES)
    private val entryFilter = MutableStateFlow(EntryFilter.THIS_MONTH)
    private val customRange = MutableStateFlow<DateRange?>(null)

    private data class DataSources(
        val tx: List<TransactionEntity>,
        val categories: List<CategoryEntity>,
        val prefs: UserPreferences,
        val subscriptions: List<SubscriptionEntity>,
    )

    private data class Controls(
        val tab: TransactionsTab,
        val entryFilter: EntryFilter,
        val customRange: DateRange?,
    )

    private val dataSources = combine(
        transactionRepository.getAll(),
        categoryRepository.getAll(),
        preferencesRepository.preferences,
        subscriptionRepository.getAll(),
    ) { tx, categories, prefs, subs -> DataSources(tx, categories, prefs, subs) }

    private val controls = combine(tab, entryFilter, customRange) { t, ef, cr -> Controls(t, ef, cr) }

    val uiState: StateFlow<TransactionsUiState> = combine(
        dataSources,
        selection.scope,
        selection.flow,
        controls,
    ) { data, scope, flowType, controls -> compute(data, scope, flowType, controls) }
        // Grouping/sorting thousands of rows must not happen on the main thread.
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    private fun compute(data: DataSources, scope: ScopePeriod, flowType: FlowType, controls: Controls): TransactionsUiState {
        val cur = data.prefs.currencyCode.symbol
        val categoriesById = data.categories.associateBy { it.id }
        val flowTx = AnalyticsEngine.flowFilter(data.tx, flowType)
        val effFilter = AnalyticsEngine.effectiveEntryFilter(controls.entryFilter, flowType)
        val filtered = AnalyticsEngine.applyEntryFilter(flowTx, effFilter, controls.customRange)

        val dayGroups = AnalyticsEngine.groupByDay(filtered).map { g ->
            DayGroupUi(
                label = g.label,
                totalFormatted = cur + CurrencyFormatter.amount(g.totalAbs),
                items = g.items.map { it.toRow(categoriesById, cur) },
            )
        }
        val rollups = AnalyticsEngine.groupByCategory(filtered, data.categories).map {
            CategoryRollupUi(it.categoryId, it.name, it.iconKey, it.count, cur + CurrencyFormatter.amount(it.totalAbs), it.sharePercent)
        }

        return TransactionsUiState(
            flow = flowType,
            tab = controls.tab,
            entryFilter = effFilter,
            entryFilterOptions = AnalyticsEngine.entryFilterOptions(flowType),
            customRange = controls.customRange,
            dayGroups = dayGroups,
            categoryRollups = rollups,
            currencySymbol = cur,
            earliestTransactionDate = data.tx.minOfOrNull { it.date },
            stats = if (controls.tab == TransactionsTab.ANALYTICS) {
                val active = data.subscriptions.filter { it.active }
                val monthly = active.sumOf {
                    when (it.billingCycle) {
                        BillingCycle.WEEKLY -> it.amount * 52.0 / 12.0
                        BillingCycle.MONTHLY -> it.amount
                        BillingCycle.YEARLY -> it.amount / 12.0
                    }
                }
                buildStatsUi(AnalyticsEngine.stats(data.tx, data.categories, scope, flowType), cur, monthly, active.size)
            } else null,
        )
    }

    private fun TransactionEntity.toRow(categoriesById: Map<Long, CategoryEntity>, cur: String): TxRow {
        val category = categoriesById[categoryId]
        return TxRow(
            id = id,
            name = description,
            categoryLabel = category?.name ?: "Deleted category",
            amountFormatted = cur + CurrencyFormatter.amount(amount),
            isPositive = amount > 0,
            dayLabel = "${date.dayOfMonth} ${date.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.US)}",
            iconKey = category?.iconKey ?: "receipt",
        )
    }

    fun setFlow(type: FlowType) {
        selection.flow.value = type
    }

    fun selectTab(newTab: TransactionsTab) {
        tab.value = newTab
    }

    /** Any chip except Custom (Custom goes through the range picker, see [applyCustomRange]). */
    fun pickEntryFilter(filter: EntryFilter) {
        entryFilter.value = filter
    }

    fun applyCustomRange(range: DateRange) {
        customRange.value = range
        entryFilter.value = EntryFilter.CUSTOM
    }
}
