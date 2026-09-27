package com.ajesh.syncspend.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.data.repository.CategoryRepository
import com.ajesh.syncspend.data.repository.TransactionRepository
import com.ajesh.syncspend.domain.model.FlowType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoriesUiState(
    val type: FlowType = FlowType.EXPENSE,
    val categories: List<CategoryEntity> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesViewModel(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val type = MutableStateFlow(FlowType.EXPENSE)

    val uiState: StateFlow<CategoriesUiState> = combine(
        type,
        type.flatMapLatest { categoryRepository.getAllByType(it) },
    ) { t, cats -> CategoriesUiState(t, cats) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    fun setType(newType: FlowType) {
        type.value = newType
    }

    fun add(name: String, iconKey: String) {
        viewModelScope.launch { categoryRepository.createNext(name, iconKey, type.value) }
    }

    fun update(category: CategoryEntity, name: String, iconKey: String) {
        viewModelScope.launch { categoryRepository.update(category.copy(name = name, iconKey = iconKey)) }
    }

    /** Up/down buttons: swap with the neighbour, then renumber so sortOrder stays dense. */
    fun move(category: CategoryEntity, direction: Int) {
        val list = uiState.value.categories.toMutableList()
        val from = list.indexOfFirst { it.id == category.id }
        val to = from + direction
        if (from < 0 || to !in list.indices) return
        val moved = list.removeAt(from)
        list.add(to, moved)
        viewModelScope.launch {
            categoryRepository.updateSortOrders(list.mapIndexed { index, c -> c.copy(sortOrder = index) })
        }
    }

    /** Removes the row if nothing references it; otherwise archives it so old entries keep their label. */
    fun delete(category: CategoryEntity) {
        viewModelScope.launch {
            if (transactionRepository.countByCategory(category.id) > 0) {
                categoryRepository.update(category.copy(archived = true))
            } else {
                categoryRepository.delete(category)
            }
        }
    }
}
