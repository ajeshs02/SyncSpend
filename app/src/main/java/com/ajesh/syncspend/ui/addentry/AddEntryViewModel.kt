package com.ajesh.syncspend.ui.addentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajesh.syncspend.data.datastore.PreferencesRepository
import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.data.db.entity.TransactionEntity
import com.ajesh.syncspend.data.repository.CategoryRepository
import com.ajesh.syncspend.data.repository.TransactionRepository
import com.ajesh.syncspend.domain.model.FlowType
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AddEntryUiState(
    val type: FlowType = FlowType.EXPENSE,
    val amountText: String = "0",
    val date: LocalDate = LocalDate.now(),
    val selectedCategory: CategoryEntity? = null,
    val categoriesForType: List<CategoryEntity> = emptyList(),
    val currencySymbol: String = "₹",
    val categoryPickerOpen: Boolean = false,
)

class AddEntryViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val type = MutableStateFlow(FlowType.EXPENSE)
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

    private val draft = combine(type, amountText, date, selectedCategory, categoryPickerOpen) { values ->
        @Suppress("UNCHECKED_CAST")
        Draft(
            type = values[0] as FlowType,
            amountText = values[1] as String,
            date = values[2] as LocalDate,
            selectedCategory = values[3] as CategoryEntity?,
            categoryPickerOpen = values[4] as Boolean,
        )
    }

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
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddEntryUiState())

    fun setType(newType: FlowType) {
        type.value = newType
        selectedCategory.value = null
    }

    fun pressDigit(digit: String) {
        val current = amountText.value
        amountText.value = if (current == "0") digit else current + digit
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

    fun createCategory(name: String, iconKey: String, onCreated: (CategoryEntity) -> Unit) {
        viewModelScope.launch {
            val currentType = type.value
            val nextOrder = categoryRepository.getAllOnce()
                .filter { it.type == currentType }
                .maxOfOrNull { it.sortOrder }
                ?.plus(1) ?: 0
            val newCategory = CategoryEntity(
                name = name,
                iconKey = iconKey,
                type = currentType,
                sortOrder = nextOrder,
            )
            val id = categoryRepository.insert(newCategory)
            val created = newCategory.copy(id = id)
            selectedCategory.value = created
            categoryPickerOpen.value = false
            onCreated(created)
        }
    }

    /** Mirrors the design's saveDraft: no category → open the picker; zero amount → just leave without saving. */
    fun save(onDone: () -> Unit) {
        val category = selectedCategory.value
        if (category == null) {
            categoryPickerOpen.value = true
            return
        }
        val amount = amountText.value.toDoubleOrNull() ?: 0.0
        if (amount == 0.0) {
            onDone()
            return
        }
        viewModelScope.launch {
            transactionRepository.insert(
                TransactionEntity(
                    amount = if (type.value == FlowType.INCOME) amount else -amount,
                    description = category.name,
                    categoryId = category.id,
                    date = date.value,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            amountText.value = "0"
            onDone()
        }
    }
}
