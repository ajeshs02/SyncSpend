package com.ajesh.syncspend.data.repository

import com.ajesh.syncspend.data.db.dao.TransactionDao
import com.ajesh.syncspend.data.db.entity.TransactionEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val dao: TransactionDao) {
    fun getAll(): Flow<List<TransactionEntity>> = dao.getAll()
    suspend fun getAllOnce(): List<TransactionEntity> = dao.getAllOnce()
    fun getById(id: Long): Flow<TransactionEntity?> = dao.getById(id)
    fun getEarliestDate(): Flow<LocalDate?> = dao.getEarliestDate()
    fun getCount(): Flow<Int> = dao.getCount()
    suspend fun insert(transaction: TransactionEntity): Long = dao.insert(transaction)
    suspend fun update(transaction: TransactionEntity) = dao.update(transaction)
    suspend fun delete(transaction: TransactionEntity) = dao.delete(transaction)
    suspend fun countByCategory(categoryId: Long): Int = dao.countByCategory(categoryId)
}
