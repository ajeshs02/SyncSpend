package com.ajesh.syncspend.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajesh.syncspend.alarm.DailyReminderManager
import com.ajesh.syncspend.data.datastore.PreferencesRepository
import com.ajesh.syncspend.data.datastore.UserPreferences
import com.ajesh.syncspend.data.repository.TransactionRepository
import com.ajesh.syncspend.domain.analytics.AnalyticsEngine
import com.ajesh.syncspend.domain.model.ColorPalette
import com.ajesh.syncspend.domain.model.CurrencyCode
import com.ajesh.syncspend.domain.model.FontChoice
import com.ajesh.syncspend.domain.model.ThemeMode
import com.ajesh.syncspend.domain.state.SharedSelectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val prefs: UserPreferences = UserPreferences(),
    val exportNote: String = "",
)

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val transactionRepository: TransactionRepository,
    private val selection: SharedSelectionState,
    private val dailyReminder: DailyReminderManager,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.preferences,
        transactionRepository.getAll(),
        selection.scope,
    ) { prefs, tx, scope ->
        val inScope = AnalyticsEngine.scopeFilter(tx, scope).size
        SettingsUiState(prefs, "$inScope entries · ${AnalyticsEngine.scopeLabel(scope)} selected")
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setCurrency(code: CurrencyCode) {
        viewModelScope.launch { preferencesRepository.setCurrency(code) }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { dailyReminder.setEnabled(enabled) }
    }

    fun setReminderTime(minuteOfDay: Int) {
        viewModelScope.launch { dailyReminder.setTime(minuteOfDay) }
    }

    fun setColorPalette(palette: ColorPalette) {
        viewModelScope.launch { preferencesRepository.setColorPalette(palette) }
    }

    fun setFontChoice(choice: FontChoice) {
        viewModelScope.launch { preferencesRepository.setFontChoice(choice) }
    }
}
