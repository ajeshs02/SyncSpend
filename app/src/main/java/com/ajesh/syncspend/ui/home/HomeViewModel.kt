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
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.util.CurrencyFormatter
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
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
    val subsCount: Int = 0,
    val remindersCount: Int = 0,
)

class HomeViewModel(
    private val transactionRepository: TransactionRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val reminderRepository: ReminderRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val selectedMonth = MutableStateFlow(YearMonth.now())
    private val flow = MutableStateFlow(FlowType.EXPENSE)

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

    val uiState: StateFlow<HomeUiState> = combine(sources, selectedMonth, flow, ::compute)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun compute(sources: Sources, month: YearMonth, flowType: FlowType): HomeUiState {
        val monthTx = sources.tx.filter { YearMonth.from(it.date) == month }
        val flowTx = monthTx.filter { if (flowType == FlowType.INCOME) it.amount > 0 else it.amount < 0 }
        val total = flowTx.sumOf { abs(it.amount) }
        return HomeUiState(
            flow = flowType,
            scopeLabel = "${month.month.getDisplayName(TextStyle.FULL, Locale.US)} ${month.year}",
            scopeSubLabel = "${flowTx.size} " + if (flowType == FlowType.INCOME) "income entries logged" else "expenses logged",
            totalFormatted = CurrencyFormatter.amount(total),
            currencySymbol = sources.prefs.currencyCode.symbol,
            hasEntriesInScope = flowTx.isNotEmpty(),
            subsCount = sources.subs.count { it.active },
            remindersCount = sources.reminders.count { it.active },
        )
    }

    fun prevMonth() {
        selectedMonth.value = selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        selectedMonth.value = selectedMonth.value.plusMonths(1)
    }

    fun setFlow(type: FlowType) {
        flow.value = type
    }
}
