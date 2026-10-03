package com.ajesh.syncspend.ui.addentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajesh.syncspend.data.datastore.PreferencesRepository
import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.data.db.entity.TransactionEntity
import com.ajesh.syncspend.data.repository.CategoryRepository
import com.ajesh.syncspend.data.repository.TransactionRepository
import com.ajesh.syncspend.domain.model.EntryNote
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.state.SharedSelectionState
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

data class AddEntryUiState(
    val type: FlowType = FlowType.EXPENSE,
    val amountText: String = "0",
    val date: LocalDate = LocalDate.now(),
    val selectedCategory: CategoryEntity? = null,
    val categoriesForType: List<CategoryEntity> = emptyList(),
    val currencySymbol: String = "₹",
    val categoryPickerOpen: Boolean = false,
    val allowDecimalInput: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class AddEntryViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesRepository: PreferencesRepository,
    private val selection: SharedSelectionState,
) : ViewModel() {

    // Reads the shared selection (Home's toggle) live rather than snapshotting it once — this
    // screen's own ViewModel survives across bottom-nav tab switches (NavHost keeps its
    // ViewModelStore via saveState/restoreState), so a one-time snapshot at construction would go
    // stale the moment the toggle changes elsewhere without this screen being reopened.
    private val type = selection.flow
    private val amountText = MutableStateFlow("0")
    private val date = MutableStateFlow(LocalDate.now())
    private val selectedCategory = MutableStateFlow<CategoryEntity?>(null)
    private val categoryPickerOpen = MutableStateFlow(false)

    private val categoriesForType = type.flatMapLatest { categoryRepository.getAllByType(it) }

    private data class Draft(
        val type: FlowType,
        val amountText: String,
        val date: LocalDate,
        val selectedCategory: CategoryEntity?,
        val categoryPickerOpen: Boolean,
    )

    private val draft = combine(
        type, amountText, date, selectedCategory, categoryPickerOpen,
    ) { t, amt, d, cat, pickerOpen -> Draft(t, amt, d, cat, pickerOpen) }

    val uiState: StateFlow<AddEntryUiState> = combine(
        draft,
        categoriesForType,
        preferencesRepository.preferences,
    ) { d, categories, prefs ->
        AddEntryUiState(
            type = d.type,
            amountText = d.amountText,
            date = d.date,
            selectedCategory = d.selectedCategory,
            categoriesForType = categories,
            currencySymbol = prefs.currencyCode.symbol,
            categoryPickerOpen = d.categoryPickerOpen,
            allowDecimalInput = prefs.allowDecimalInput,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddEntryUiState())

    fun setType(newType: FlowType) {
        selection.flow.value = newType
        selectedCategory.value = null
    }

    /**
     * [digit] is either "0".."9" or "." (the decimal key, only meaningful when the Settings "allow
     * paise/cents" toggle is on). The integer part is capped at [MAX_INTEGER_DIGITS] digits; once a
     * "." is present, at most two digits are kept after it — both caps are enforced here by simply
     * ignoring a keypress that would exceed them, rather than truncating afterwards.
     */
    fun pressDigit(digit: String) {
        val current = amountText.value
        if (digit == ".") {
            if (preferencesRepository.current()?.allowDecimalInput != true) return
            if ('.' in current) return
            amountText.value = current + "."
            return
        }
        val dot = current.indexOf('.')
        if (dot < 0) {
            amountText.value = when {
                current == "0" -> digit
                current.length >= MAX_INTEGER_DIGITS -> return
                else -> current + digit
            }
        } else {
            if (current.length - dot - 1 >= MAX_DECIMAL_DIGITS) return
            amountText.value = current + digit
        }
    }

    fun pressBackspace() {
        val current = amountText.value
        amountText.value = if (current.length > 1) current.dropLast(1) else "0"
    }

    fun setDate(newDate: LocalDate) {
        date.value = newDate
    }

    fun openCategoryPicker() {
        categoryPickerOpen.value = true
    }

    fun closeCategoryPicker() {
        categoryPickerOpen.value = false
    }

    fun selectCategory(category: CategoryEntity) {
        selectedCategory.value = category
    }

    fun clearCategory() {
        selectedCategory.value = null
    }

    /**
     * Mirrors the design's saveDraft: no category → open the picker (that popup is feedback enough,
     * no separate warning needed); an amount that isn't greater than zero → [onInvalidAmount] instead of
     * silently leaving the screen. [note] is the optional note typed on the screen (held there, so typing
     * never waits on this ViewModel).
     */
    fun save(note: String, onDone: () -> Unit, onInvalidAmount: () -> Unit = {}) {
        val category = selectedCategory.value
        if (category == null) {
            categoryPickerOpen.value = true
            return
        }
        val rawAmount = amountText.value.toDoubleOrNull() ?: 0.0
        if (rawAmount <= 0.0) {
            onInvalidAmount()
            return
        }
        // Rounded to the nearest cent once, right here where a typed string becomes a stored value —
        // see AnalyticsEngine's accounting-model doc for why. The keypad already caps input at 2
        // decimal digits, so this is mostly a defensive guard, not a correction.
        val amount = (rawAmount * 100).roundToLong() / 100.0
        val currentType = type.value
        viewModelScope.launch {
            // The time is only known when the entry is for today: a back-dated entry was not made "now".
            val now = LocalDateTime.now()
            transactionRepository.insert(
                TransactionEntity(
                    // Expense is negative; Income is stored positive.
                    amount = if (currentType == FlowType.EXPENSE) -amount else amount,
                    description = EntryNote.normalize(note),
                    categoryId = category.id,
                    date = date.value,
                    createdAt = System.currentTimeMillis(),
                    timeMinuteOfDay = if (date.value == now.toLocalDate()) now.hour * 60 + now.minute else null,
                    type = currentType,
                ),
            )
            amountText.value = "0"
            onDone()
        }
    }

    private companion object {
        const val MAX_INTEGER_DIGITS = 6
        const val MAX_DECIMAL_DIGITS = 2
    }
}
