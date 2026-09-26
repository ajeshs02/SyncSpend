package com.ajesh.syncspend.data.repository

import com.ajesh.syncspend.data.db.dao.CategoryDao
import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.domain.model.FlowType
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val dao: CategoryDao) {
    fun getAllByType(type: FlowType): Flow<List<CategoryEntity>> = dao.getAllByType(type)
    fun getAll(): Flow<List<CategoryEntity>> = dao.getAll()
    suspend fun getAllOnce(): List<CategoryEntity> = dao.getAllOnce()
    suspend fun insert(category: CategoryEntity): Long = dao.insert(category)
    suspend fun update(category: CategoryEntity) = dao.update(category)
    suspend fun delete(category: CategoryEntity) = dao.delete(category)
    suspend fun updateSortOrders(categories: List<CategoryEntity>) = dao.updateAll(categories)

    /** Appends a category at the end of its type's order (archived rows count, so orders never collide). */
    suspend fun createNext(name: String, iconKey: String, type: FlowType): CategoryEntity {
        val next = dao.getAllOnce().filter { it.type == type }.maxOfOrNull { it.sortOrder }?.plus(1) ?: 0
        val fresh = CategoryEntity(name = name, iconKey = iconKey, type = type, sortOrder = next)
        return fresh.copy(id = dao.insert(fresh))
    }
}
