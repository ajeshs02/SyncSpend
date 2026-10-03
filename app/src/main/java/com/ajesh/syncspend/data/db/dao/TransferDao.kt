package com.ajesh.syncspend.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ajesh.syncspend.data.db.entity.TransferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {
    @Insert
    suspend fun insert(transfer: TransferEntity): Long

    @Update
    suspend fun update(transfer: TransferEntity)

    @Delete
    suspend fun delete(transfer: TransferEntity)

    /** Most recently added first — the Savings screen is a single unified feed, not grouped by date. */
    @Query("SELECT * FROM transfers ORDER BY createdAt DESC")
    fun getAll(): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers")
    suspend fun getAllOnce(): List<TransferEntity>

    @Query("SELECT COUNT(*) FROM transfers WHERE categoryId = :categoryId")
    suspend fun countByCategory(categoryId: Long): Int
}
