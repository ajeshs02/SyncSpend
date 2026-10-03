package com.ajesh.syncspend.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ajesh.syncspend.data.db.entity.ForecastEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ForecastDao {
    @Insert
    suspend fun insert(forecast: ForecastEntity): Long

    @Update
    suspend fun update(forecast: ForecastEntity)

    @Delete
    suspend fun delete(forecast: ForecastEntity)

    /** Dated rows soonest first, undated ("this month") rows last. */
    @Query("SELECT * FROM forecasts ORDER BY date IS NULL ASC, date ASC")
    fun getAll(): Flow<List<ForecastEntity>>

    @Query("SELECT * FROM forecasts")
    suspend fun getAllOnce(): List<ForecastEntity>

    /** Called right after deleting a transaction, so no forecast is left pointing at a dead row — [ForecastEntity.completed] is deliberately untouched. */
    @Query("UPDATE forecasts SET completedTransactionId = NULL WHERE completedTransactionId = :transactionId")
    suspend fun clearCompletedLink(transactionId: Long)

    /** The past-month review popup's "Mark completed" action — rows are kept, only [ForecastEntity.completed] flips. */
    @Query("UPDATE forecasts SET completed = 1 WHERE id IN (:ids)")
    suspend fun markCompleted(ids: List<Long>)
}
