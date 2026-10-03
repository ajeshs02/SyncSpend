package com.ajesh.syncspend.data.repository

import com.ajesh.syncspend.data.db.dao.ForecastDao
import com.ajesh.syncspend.data.db.entity.ForecastEntity
import kotlinx.coroutines.flow.Flow

class ForecastRepository(private val dao: ForecastDao) {
    fun getAll(): Flow<List<ForecastEntity>> = dao.getAll()
    suspend fun getAllOnce(): List<ForecastEntity> = dao.getAllOnce()
    suspend fun insert(forecast: ForecastEntity): Long = dao.insert(forecast)
    suspend fun update(forecast: ForecastEntity) = dao.update(forecast)
    suspend fun delete(forecast: ForecastEntity) = dao.delete(forecast)
    suspend fun clearCompletedLink(transactionId: Long) = dao.clearCompletedLink(transactionId)
    suspend fun markCompleted(ids: List<Long>) = dao.markCompleted(ids)
}
