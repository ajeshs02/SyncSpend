package com.ajesh.syncspend.ui.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajesh.syncspend.data.datastore.PreferencesRepository
import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.data.db.entity.TransferEntity
import com.ajesh.syncspend.data.repository.CategoryRepository
import com.ajesh.syncspend.data.repository.TransferRepository
import com.ajesh.syncspend.domain.analytics.AnalyticsEngine
import com.ajesh.syncspend.domain.model.DateRange
import com.ajesh.syncspend.domain.model.EntryFilter
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.TransferDirection
import com.ajesh.syncspend.ui.transactions.TxRow
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransferUiState(
    /** All-time, not scoped to the entry filter below — a balance is a point-in-time total (see [AnalyticsEngine.savingsBalance]). */
    val savingsBalance: Double = 0.0,
    val currencySymbol: String = "₹",
    /** "Add to Savings" picks its source from here — reusing the Income category list, not a parallel system. */
    val incomeCategories: List<CategoryEntity> = emptyList(),
    val entryFilter: EntryFilter = EntryFilter.THIS_MONTH,
    val customRange: DateRange? = null,
    /** The oldest transfer's date, if any — bounds the Custom range picker and anchors its "All time" preset. */
    val earliestTransferDate: LocalDate? = null,
    /** Both directions together, newest-added first (see [TransferDao.getAll]'s `ORDER BY createdAt DESC`) — not grouped by day. */
    val rows: List<TxRow> = emptyList(),
)

class TransferViewModel(
    private val transferRepository: TransferRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val entryFilter = MutableStateFlow(EntryFilter.THIS_MONTH)
    private val customRange = MutableStateFlow<DateRange?>(null)

    val uiState: StateFlow<TransferUiState> = combine(
        transferRepository.getAll(),
        categoryRepository.getAllByType(FlowType.INCOME),
        preferencesRepository.preferences,
        entryFilter,
        customRange,
    ) { transfers, incomeCategories, prefs, filter, custom ->
        val cur = prefs.currencyCode.symbol
        val categoriesById = incomeCategories.associateBy { it.id }
        val window = AnalyticsEngine.entryFilterWindow(filter, custom)
        val filtered = transfers.filter { window == null || it.date in window }
        val sorted = filtered.sortedByDescending { it.createdAt }

        TransferUiState(
            savingsBalance = AnalyticsEngine.savingsBalance(transfers),
            currencySymbol = cur,
            incomeCategories = incomeCategories,
            entryFilter = filter,
            customRange = custom,
            earliestTransferDate = transfers.minOfOrNull { it.date },
            rows = sorted.map { it.toTransferRow(categoriesById, cur) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransferUiState())

    fun pickEntryFilter(filter: EntryFilter) {
        entryFilter.value = filter
    }

    fun applyCustomRange(range: DateRange) {
        customRange.value = range
        entryFilter.value = EntryFilter.CUSTOM
    }

    fun add(amount: Double, direction: TransferDirection, date: LocalDate, categoryId: Long?, note: String) {
        viewModelScope.launch {
            val now = LocalDateTime.now()
            transferRepository.insert(
                TransferEntity(
                    amount = amount,
                    direction = direction,
                    date = date,
                    categoryId = categoryId.takeIf { direction == TransferDirection.TO_SAVINGS },
                    note = note,
                    createdAt = System.currentTimeMillis(),
                    timeMinuteOfDay = if (date == now.toLocalDate()) now.hour * 60 + now.minute else null,
                ),
            )
        }
    }

    fun update(transfer: TransferEntity, amount: Double, date: LocalDate, categoryId: Long?, note: String) {
        viewModelScope.launch {
            transferRepository.update(
                transfer.copy(
                    amount = amount,
                    date = date,
                    categoryId = categoryId.takeIf { transfer.direction == TransferDirection.TO_SAVINGS },
                    note = note,
                ),
            )
        }
    }

    fun delete(transfer: TransferEntity) {
        viewModelScope.launch { transferRepository.delete(transfer) }
    }
}

/** Reuses the Entries tab's own row/card shape (see the plan: Savings transfers are "visible in the existing transaction lists" via the same component). */
internal fun TransferEntity.toTransferRow(categoriesById: Map<Long, CategoryEntity>, currencySymbol: String): TxRow {
    val isContribution = direction == TransferDirection.TO_SAVINGS
    val sourceName = categoryId?.let { categoriesById[it]?.name }
    return TxRow(
        id = id,
        categoryLabel = if (isContribution) "Added to Savings" else "Withdrawn from Savings",
        note = listOfNotNull(sourceName, note.ifBlank { null }).joinToString(" · "),
        amountFormatted = currencySymbol + com.ajesh.syncspend.util.CurrencyFormatter.amount(amount),
        // Green for a contribution, red for a withdrawal — same isPositive-driven coloring the real
        // Entries rows already use (colors.pos / colors.neg), not a bespoke treatment.
        isPositive = isContribution,
        dayLabel = com.ajesh.syncspend.util.DateUtils.entryDayLabel(date, timeMinuteOfDay),
        iconKey = "coin",
    )
}
