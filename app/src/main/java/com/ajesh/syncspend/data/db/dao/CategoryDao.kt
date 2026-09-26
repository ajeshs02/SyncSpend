package com.ajesh.syncspend.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.domain.model.FlowType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE type = :type AND archived = 0 ORDER BY sortOrder ASC")
    fun getAllByType(type: FlowType): Flow<List<CategoryEntity>>

    /** Includes archived rows — used to resolve labels on existing entries. */
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    fun getAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    suspend fun getAllOnce(): List<CategoryEntity>

    /** Batch update (Room runs it in one transaction) — used to renumber sortOrder after a reorder. */
    @Update
    suspend fun updateAll(categories: List<CategoryEntity>)
}
