package com.ajesh.syncspend.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajesh.syncspend.data.datastore.PreferencesRepository
import com.ajesh.syncspend.data.datastore.UserPreferences
import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.data.db.entity.ReminderEntity
import com.ajesh.syncspend.data.db.entity.SubscriptionEntity
import com.ajesh.syncspend.data.db.entity.TransactionEntity
import com.ajesh.syncspend.data.repository.CategoryRepository
import com.ajesh.syncspend.data.repository.ReminderRepository
import com.ajesh.syncspend.data.repository.SubscriptionRepository
import com.ajesh.syncspend.data.repository.TransactionRepository
import com.ajesh.syncspend.domain.analytics.AnalyticsEngine
import com.ajesh.syncspend.domain.model.CalendarBounds
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.ScopePeriod
import com.ajesh.syncspend.domain.state.SharedSelectionState
import com.ajesh.syncspend.ui.transactions.DayGroupUi
import com.ajesh.syncspend.ui.transactions.toTxRow
import com.ajesh.syncspend.util.CurrencyFormatter
import com.ajesh.syncspend.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    /** False only for the placeholder shown before the first real state is computed. */
    val loaded: Boolean = false,
    val flow: FlowType = FlowType.EXPENSE,
    val scopeLabel: String = "",
    val scopeSubLabel: String = "",
    val total: Double = 0.0,
    val totalFormatted: String = "0",
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
    val canGoPrev: Boolean = false,
    val canGoNext: Boolean = false,
    val earliestTransactionDate: java.time.LocalDate? = null,
    val entryCountLabel: String = "",
    val avgNote: String = "",
    /** The latest entries in the selected scope, grouped by day. */
    val recentGroups: List<DayGroupUi> = emptyList(),
)

class HomeViewModel(
    private val transactionRepository: TransactionRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val reminderRepository: ReminderRepository,
    private val preferencesRepository: PreferencesRepository,
    private val categoryRepository: CategoryRepository,
    private val selection: SharedSelectionState,
) : ViewModel() {

    private data class Sources(
        val tx: List<TransactionEntity>,
        val subs: List<SubscriptionEntity>,
        val reminders: List<ReminderEntity>,
        val prefs: UserPreferences,
        val categories: List<CategoryEntity>,
    )

    private val sources = combine(
        transactionRepository.getAll(),
        subscriptionRepository.getAll(),
        reminderRepository.getAll(),
        preferencesRepository.preferences,
        categoryRepository.getAll(),
    ) { tx, subs, reminders, prefs, cats -> Sources(tx, subs, reminders, prefs, cats) }

    val uiState: StateFlow<HomeUiState> = combine(sources, selection.scope, selection.flow, ::compute)
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun compute(sources: Sources, scope: ScopePeriod, flowType: FlowType): HomeUiState {
        val flowTx = AnalyticsEngine.flowFilter(sources.tx, flowType)
        val scopeTx = AnalyticsEngine.scopeFilter(flowTx, scope)
        val total = scopeTx.sumOf { kotlin.math.abs(it.amount) }

        val prevScope = AnalyticsEngine.previousScope(scope)
        val prevTotal = prevScope?.let { AnalyticsEngine.scopeFilter(flowTx, it).sumOf { t -> kotlin.math.abs(t.amount) } } ?: 0.0
        val trend = AnalyticsEngine.trendPercent(total, prevTotal)
        val cur = sources.prefs.currencyCode.symbol
        val now = java.time.YearMonth.now()
        val lower = CalendarBounds.dataLowerMonth(now, sources.tx.minOfOrNull { it.date })
        val recent = scopeTx.sortedWith(compareByDescending<TransactionEntity> { it.date }.thenByDescending { it.createdAt }).take(10)
        val categoriesById = sources.categories.associateBy { it.id }

        return HomeUiState(
            loaded = true,
            flow = flowType,
            scopeLabel = AnalyticsEngine.scopeLabel(scope),
            scopeSubLabel = "${scopeTx.size} " + if (flowType == FlowType.INCOME) "income entries logged" else "expenses logged",
            total = total,
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
            canGoPrev = AnalyticsEngine.stepScope(scope, -1, now, lower) != null,
            canGoNext = AnalyticsEngine.stepScope(scope, 1, now, lower) != null,
            earliestTransactionDate = sources.tx.minOfOrNull { it.date },
            entryCountLabel = "${scopeTx.size} entries",
            avgNote = "Averaging $cur${CurrencyFormatter.amount(if (scopeTx.isEmpty()) 0.0 else total / scopeTx.size)} per entry",
            recentGroups = AnalyticsEngine.groupByDay(recent).map { group ->
                DayGroupUi(
                    label = group.label,
                    totalFormatted = cur + CurrencyFormatter.amount(group.totalAbs),
                    items = group.items.map { it.toTxRow(categoriesById, cur) },
                )
            },
        )
    }

    fun applyScope(newScope: ScopePeriod) {
        selection.scope.value = newScope
    }

    /** ‹ › step the current selection by one month / year / window, never past the present or before the oldest entry's month. */
    fun prevPeriod() = step(-1)

    fun nextPeriod() = step(1)

    private fun step(delta: Int) {
        val now = java.time.YearMonth.now()
        val lower = CalendarBounds.dataLowerMonth(now, uiState.value.earliestTransactionDate)
        AnalyticsEngine.stepScope(selection.scope.value, delta, now, lower)?.let { selection.scope.value = it }
    }

    fun setFlow(type: FlowType) {
        selection.flow.value = type
    }
}
