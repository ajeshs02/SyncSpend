package com.ajesh.syncspend.ui.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajesh.syncspend.data.datastore.PreferencesRepository
import com.ajesh.syncspend.data.db.entity.ForecastEntity
import com.ajesh.syncspend.data.repository.ForecastRepository
import com.ajesh.syncspend.util.CurrencyFormatter
import com.ajesh.syncspend.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** [entity] rides along so the edit dialog can prefill from the real row, not a reformatted string. */
data class ForecastRowUi(
    val entity: ForecastEntity,
    val note: String,
    val amountFormatted: String,
    val dateLabel: String,
    /** Only ever true for a *dated* row whose day has passed — an undated ("this month") row is never struck through. */
    val struckThrough: Boolean,
)

data class ForecastUiState(
    val rows: List<ForecastRowUi> = emptyList(),
    /** Sum of every row whose date is null or falls in the current calendar month (dated, past or future within it). */
    val summaryFormatted: String = "0",
    val currencySymbol: String = "₹",
)

class ForecastViewModel(
    private val repository: ForecastRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<ForecastUiState> = combine(
        repository.getAll(),
        preferencesRepository.preferences,
    ) { forecasts, prefs ->
        val cur = prefs.currencyCode.symbol
        val today = LocalDate.now()
        val thisMonth = YearMonth.from(today)
        val summary = forecasts.filter { it.date == null || YearMonth.from(it.date) == thisMonth }.sumOf { it.amount }
        ForecastUiState(
            rows = forecasts.map { f ->
                ForecastRowUi(
                    entity = f,
                    note = f.note,
                    amountFormatted = cur + CurrencyFormatter.amount(f.amount),
                    dateLabel = f.date?.let { DateUtils.shortDate(it) } ?: "This month",
                    struckThrough = f.date != null && f.date.isBefore(today),
                )
            },
            summaryFormatted = cur + CurrencyFormatter.amount(summary),
            currencySymbol = cur,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ForecastUiState())

    fun save(existing: ForecastEntity?, note: String, amount: Double, date: LocalDate?) {
        viewModelScope.launch {
            if (existing == null) repository.insert(ForecastEntity(note = note, amount = amount, date = date))
            else repository.update(existing.copy(note = note, amount = amount, date = date))
        }
    }

    fun delete(forecast: ForecastEntity) {
        viewModelScope.launch { repository.delete(forecast) }
    }
}
