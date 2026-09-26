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
import com.ajesh.syncspend.domain.model.EntryFilter
import com.ajesh.syncspend.domain.model.BillingCycle
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.ScopePeriod
import com.ajesh.syncspend.domain.state.SharedSelectionState
import com.ajesh.syncspend.util.CurrencyFormatter
import java.time.LocalDate
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    val flowMenuOpen: Boolean = false,
    val scopeLabel: String = "",
    val tab: TransactionsTab = TransactionsTab.ENTRIES,
    val entryFilter: EntryFilter = EntryFilter.THIS_MONTH,
    val entryFilterOptions: List<EntryFilter> = emptyList(),
    val dayGroups: List<DayGroupUi> = emptyList(),
    val categoryRollups: List<CategoryRollupUi> = emptyList(),
    val currencySymbol: String = "₹",
    val periodPickerOpen: Boolean = false,
    val earliestTransactionDate: LocalDate? = null,
    val currentScope: ScopePeriod = ScopePeriod.Month(java.time.YearMonth.now()),
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
    private val flowMenuOpen = MutableStateFlow(false)
    private val periodPickerOpen = MutableStateFlow(false)

    private data class DataSources(
        val tx: List<TransactionEntity>,
        val categories: List<CategoryEntity>,
        val prefs: UserPreferences,
        val subscriptions: List<SubscriptionEntity>,
    )

    private data class UiFlags(
        val tab: TransactionsTab,
        val entryFilter: EntryFilter,
        val flowMenuOpen: Boolean,
        val periodPickerOpen: Boolean,
    )

    private val dataSources = combine(
        transactionRepository.getAll(),
        categoryRepository.getAll(),
        preferencesRepository.preferences,
        subscriptionRepository.getAll(),
    ) { tx, categories, prefs, subs -> DataSources(tx, categories, prefs, subs) }

    private val uiFlags = combine(tab, entryFilter, flowMenuOpen, periodPickerOpen) { t, ef, fm, pp ->
        UiFlags(t, ef, fm, pp)
    }

    val uiState: StateFlow<TransactionsUiState> = combine(
        dataSources,
        selection.scope,
        selection.flow,
        uiFlags,
    ) { data, scope, flowType, flags -> compute(data, scope, flowType, flags) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    private fun compute(data: DataSources, scope: ScopePeriod, flowType: FlowType, flags: UiFlags): TransactionsUiState {
        val cur = data.prefs.currencyCode.symbol
        val flowTx = AnalyticsEngine.flowFilter(data.tx, flowType)
        val effFilter = AnalyticsEngine.effectiveEntryFilter(flags.entryFilter, flowType)
        val filtered = AnalyticsEngine.applyEntryFilter(flowTx, effFilter, scope)

        val dayGroups = AnalyticsEngine.groupByDay(filtered).map { g ->
            DayGroupUi(
                label = g.label,
                totalFormatted = cur + CurrencyFormatter.amount(g.totalAbs),
                items = g.items.map { it.toRow(data.categories, cur) },
            )
        }
        val rollups = AnalyticsEngine.groupByCategory(filtered, data.categories).map {
            CategoryRollupUi(it.categoryId, it.name, it.iconKey, it.count, cur + CurrencyFormatter.amount(it.totalAbs), it.sharePercent)
        }

        return TransactionsUiState(
            flow = flowType,
            flowMenuOpen = flags.flowMenuOpen,
            scopeLabel = AnalyticsEngine.scopeLabel(scope),
            tab = flags.tab,
            entryFilter = effFilter,
            entryFilterOptions = AnalyticsEngine.entryFilterOptions(flowType),
            dayGroups = dayGroups,
            categoryRollups = rollups,
            currencySymbol = cur,
            periodPickerOpen = flags.periodPickerOpen,
            earliestTransactionDate = data.tx.minOfOrNull { it.date },
            currentScope = scope,
            stats = if (flags.tab == TransactionsTab.ANALYTICS) {
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

    private fun TransactionEntity.toRow(categories: List<CategoryEntity>, cur: String): TxRow {
        val category = categories.find { it.id == categoryId }
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
        flowMenuOpen.value = false
    }

    fun closeFlowMenu() {
        flowMenuOpen.value = false
    }

    fun toggleFlowMenu() {
        flowMenuOpen.value = !flowMenuOpen.value
    }

    fun selectTab(newTab: TransactionsTab) {
        tab.value = newTab
    }

    fun pickEntryFilter(filter: EntryFilter) {
        entryFilter.value = filter
        if (filter == EntryFilter.CUSTOM) periodPickerOpen.value = true
    }

    fun openPeriodPicker() {
        periodPickerOpen.value = true
    }

    fun closePeriodPicker() {
        periodPickerOpen.value = false
    }

    fun applyScope(scope: ScopePeriod) {
        selection.scope.value = scope
        periodPickerOpen.value = false
        entryFilter.value = EntryFilter.CUSTOM
    }
}
