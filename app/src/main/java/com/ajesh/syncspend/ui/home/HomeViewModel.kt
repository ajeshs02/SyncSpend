package com.ajesh.syncspend.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajesh.syncspend.data.datastore.PreferencesRepository
import com.ajesh.syncspend.data.datastore.UserPreferences
import com.ajesh.syncspend.data.db.entity.ReminderEntity
import com.ajesh.syncspend.data.db.entity.SubscriptionEntity
import com.ajesh.syncspend.data.db.entity.TransactionEntity
import com.ajesh.syncspend.data.repository.ReminderRepository
import com.ajesh.syncspend.data.repository.SubscriptionRepository
import com.ajesh.syncspend.data.repository.TransactionRepository
import com.ajesh.syncspend.domain.analytics.AnalyticsEngine
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.ScopePeriod
import com.ajesh.syncspend.domain.state.SharedSelectionState
import com.ajesh.syncspend.util.CurrencyFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val flow: FlowType = FlowType.EXPENSE,
    val scopeLabel: String = "",
    val scopeSubLabel: String = "",
    val totalFormatted: String = "0.00",
    val currencySymbol: String = "₹",
    val hasEntriesInScope: Boolean = false,
    val hasTrend: Boolean = false,
    val trendPercent: Int = 0,
    val trendIsUp: Boolean = false,
    val prevScopeLabel: String = "",
    val prevTotalFormatted: String = "",
    val subsCount: Int = 0,
    val remindersCount: Int = 0,
    val currentScope: ScopePeriod = ScopePeriod.Month(java.time.YearMonth.now()),
    val earliestTransactionDate: java.time.LocalDate? = null,
)

class HomeViewModel(
    private val transactionRepository: TransactionRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val reminderRepository: ReminderRepository,
    private val preferencesRepository: PreferencesRepository,
    private val selection: SharedSelectionState,
) : ViewModel() {

    private data class Sources(
        val tx: List<TransactionEntity>,
        val subs: List<SubscriptionEntity>,
        val reminders: List<ReminderEntity>,
        val prefs: UserPreferences,
    )

    private val sources = combine(
        transactionRepository.getAll(),
        subscriptionRepository.getAll(),
        reminderRepository.getAll(),
        preferencesRepository.preferences,
    ) { tx, subs, reminders, prefs -> Sources(tx, subs, reminders, prefs) }

    val uiState: StateFlow<HomeUiState> = combine(sources, selection.scope, selection.flow, ::compute)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun compute(sources: Sources, scope: ScopePeriod, flowType: FlowType): HomeUiState {
        val flowTx = AnalyticsEngine.flowFilter(sources.tx, flowType)
        val scopeTx = AnalyticsEngine.scopeFilter(flowTx, scope)
        val total = scopeTx.sumOf { kotlin.math.abs(it.amount) }

        val prevScope = AnalyticsEngine.previousScope(scope)
        val prevTotal = prevScope?.let { AnalyticsEngine.scopeFilter(flowTx, it).sumOf { t -> kotlin.math.abs(t.amount) } } ?: 0.0
        val trend = AnalyticsEngine.trendPercent(total, prevTotal)

        return HomeUiState(
            flow = flowType,
            scopeLabel = AnalyticsEngine.scopeLabel(scope),
            scopeSubLabel = "${scopeTx.size} " + if (flowType == FlowType.INCOME) "income entries logged" else "expenses logged",
            totalFormatted = CurrencyFormatter.amount(total),
            currencySymbol = sources.prefs.currencyCode.symbol,
            hasEntriesInScope = scopeTx.isNotEmpty(),
            hasTrend = trend != null,
            trendPercent = trend?.let { kotlin.math.abs(it) } ?: 0,
            trendIsUp = (trend ?: 0) > 0,
            prevScopeLabel = prevScope?.let { AnalyticsEngine.scopeLabel(it) } ?: "",
            prevTotalFormatted = CurrencyFormatter.amount(prevTotal),
            subsCount = sources.subs.count { it.active },
            remindersCount = sources.reminders.count { it.active },
            currentScope = scope,
            earliestTransactionDate = sources.tx.minOfOrNull { it.date },
        )
    }

    fun applyScope(newScope: ScopePeriod) {
        selection.scope.value = newScope
    }

    fun prevMonth() {
        val current = selection.scope.value
        if (current is ScopePeriod.Month) selection.scope.value = ScopePeriod.Month(current.yearMonth.minusMonths(1))
    }

    fun nextMonth() {
        val current = selection.scope.value
        if (current is ScopePeriod.Month) selection.scope.value = ScopePeriod.Month(current.yearMonth.plusMonths(1))
    }

    fun setFlow(type: FlowType) {
        selection.flow.value = type
    }
}
