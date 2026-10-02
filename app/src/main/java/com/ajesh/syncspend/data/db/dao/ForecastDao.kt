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
}
