package com.ajesh.syncspend.ui.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajesh.syncspend.data.datastore.PreferencesRepository
import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.data.db.entity.ForecastEntity
import com.ajesh.syncspend.data.db.entity.TransactionEntity
import com.ajesh.syncspend.data.repository.CategoryRepository
import com.ajesh.syncspend.data.repository.ForecastRepository
import com.ajesh.syncspend.data.repository.TransactionRepository
import com.ajesh.syncspend.domain.model.EntryNote
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.util.CurrencyFormatter
import com.ajesh.syncspend.util.DateUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** [entity] rides along so the edit dialog and action sheet can read the real row, not a reformatted string. */
data class ForecastRowUi(
    val entity: ForecastEntity,
    val note: String,
    val amountFormatted: String,
    val dateLabel: String,
    val categoryName: String?,
    /** [ForecastEntity.completed] — an explicit, persistent flag now, not derived from the date. */
    val struckThrough: Boolean,
)

data class ForecastUiState(
    /** Active list: every forecast except ones already linked to a real Expense (completedTransactionId != null) — those are considered done-and-filed, not shown here. */
    val rows: List<ForecastRowUi> = emptyList(),
    /** Sum of every row whose date is null or falls in the current calendar month (dated, past or future within it), regardless of completed status. */
    val summaryFormatted: String = "0",
    /** Same sum, for next month. */
    val nextMonthFormatted: String = "0",
    /** Counted from the full, unfiltered forecast list for the current month — a row hidden from [rows] by its linked-expense state still counts here. */
    val completedCount: Int = 0,
    val pendingCount: Int = 0,
    val currencySymbol: String = "₹",
    val expenseCategories: List<CategoryEntity> = emptyList(),
    /** Non-null (and the popup should show) only when stale pending forecasts exist and the user hasn't dismissed it this session. */
    val pendingReview: List<ForecastRowUi>? = null,
)

class ForecastViewModel(
    private val repository: ForecastRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesRepository: PreferencesRepository,
    private val reviewDismissedThisSession: MutableStateFlow<Boolean>,
) : ViewModel() {

    val uiState: StateFlow<ForecastUiState> = combine(
        repository.getAll(),
        preferencesRepository.preferences,
        categoryRepository.getAllByType(FlowType.EXPENSE),
        reviewDismissedThisSession,
    ) { forecasts, prefs, categories, dismissed ->
        val cur = prefs.currencyCode.symbol
        val thisMonth = YearMonth.from(LocalDate.now())
        val nextMonth = thisMonth.plusMonths(1)
        val categoriesById = categories.associateBy { it.id }

        fun ForecastEntity.toRowUi() = ForecastRowUi(
            entity = this,
            note = note,
            amountFormatted = cur + CurrencyFormatter.amount(amount),
            // A row's date is always the 1st of its month going forward (see ForecastDialog's
            // month chips) — shown as the month name, not a specific day.
            dateLabel = date?.let { DateUtils.monthYearLabel(it) } ?: "This month",
            categoryName = categoryId?.let { categoriesById[it]?.name },
            struckThrough = completed,
        )

        val summary = forecasts.filter { it.date == null || YearMonth.from(it.date) == thisMonth }.sumOf { it.amount }
        val nextMonthSum = forecasts.filter { it.date != null && YearMonth.from(it.date) == nextMonth }.sumOf { it.amount }
        val thisMonthForecasts = forecasts.filter { it.date == null || YearMonth.from(it.date) == thisMonth }
        val stalePending = forecasts.filter { !it.completed && it.date != null && YearMonth.from(it.date) < thisMonth }

        ForecastUiState(
            rows = forecasts.filter { it.completedTransactionId == null }.map { it.toRowUi() },
            summaryFormatted = cur + CurrencyFormatter.amount(summary),
            nextMonthFormatted = cur + CurrencyFormatter.amount(nextMonthSum),
            completedCount = thisMonthForecasts.count { it.completed },
            pendingCount = thisMonthForecasts.count { !it.completed },
            currencySymbol = cur,
            expenseCategories = categories,
            pendingReview = stalePending.takeIf { it.isNotEmpty() && !dismissed }?.map { it.toRowUi() },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ForecastUiState())

    fun save(existing: ForecastEntity?, note: String, amount: Double, date: LocalDate?, categoryId: Long?) {
        viewModelScope.launch {
            if (existing == null) {
                repository.insert(ForecastEntity(note = note, amount = amount, date = date, categoryId = categoryId))
            } else {
                // Editing a plain "marked done" forecast (no linked expense) un-does it — any further
                // action needed is a fresh, explicit Mark Done/Add Expense. A forecast already linked to a
                // real expense is left alone: its own fields are just a frozen plan, independent of the
                // transaction's live values, so editing it never touches completed/completedTransactionId.
                val revertCompletion = existing.completed && existing.completedTransactionId == null
                repository.update(
                    existing.copy(
                        note = note,
                        amount = amount,
                        date = date,
                        categoryId = categoryId,
                        completed = if (revertCompletion) false else existing.completed,
                    ),
                )
            }
        }
    }

    fun delete(forecast: ForecastEntity) {
        viewModelScope.launch { repository.delete(forecast) }
    }

    /** Persistent strikethrough, no transaction — "Add Expense" stays available afterward (see [ForecastEntity]'s doc). */
    fun markDone(forecast: ForecastEntity) {
        viewModelScope.launch { repository.update(forecast.copy(completed = true)) }
    }

    /** Reverses a plain [markDone] — only ever offered (see [ForecastActionSheet]) when there's no linked expense. */
    fun markUndone(forecast: ForecastEntity) {
        viewModelScope.launch { repository.update(forecast.copy(completed = false)) }
    }

    /**
     * Creates the real Expense and links it — the duplicate-prevention gate from here on is
     * [ForecastEntity.completedTransactionId], not [ForecastEntity.completed] alone.
     */
    fun addExpenseAndComplete(forecast: ForecastEntity, amount: Double, categoryId: Long, date: LocalDate, note: String) {
        viewModelScope.launch {
            val now = LocalDateTime.now()
            val transactionId = transactionRepository.insert(
                TransactionEntity(
                    amount = -amount,
                    description = EntryNote.normalize(note),
                    categoryId = categoryId,
                    date = date,
                    createdAt = System.currentTimeMillis(),
                    timeMinuteOfDay = if (date == now.toLocalDate()) now.hour * 60 + now.minute else null,
                    type = FlowType.EXPENSE,
                ),
            )
            repository.update(forecast.copy(completed = true, completedTransactionId = transactionId))
        }
    }

    /**
     * The past-month review popup. Confirming marks every stale pending forecast completed (rows are
     * preserved, never deleted) — that alone empties the next [uiState] emission's `pendingReview`, no
     * session flag needed. Dismissing makes no data change; it only flips the session-scoped
     * [reviewDismissedThisSession] flag so the popup stops reappearing until the app is next launched.
     */
    fun reviewStaleForecasts(confirm: Boolean) {
        if (confirm) {
            viewModelScope.launch {
                val ids = uiState.value.pendingReview.orEmpty().map { it.entity.id }
                if (ids.isNotEmpty()) repository.markCompleted(ids)
            }
        } else {
            reviewDismissedThisSession.value = true
        }
    }
}
