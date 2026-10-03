package com.ajesh.syncspend.data.repository

import com.ajesh.syncspend.data.db.dao.TransferDao
import com.ajesh.syncspend.data.db.entity.TransferEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

/** Read by the Transfer screen, the Transactions page's Savings filter, and the Stats summary card — one warm, shared stream, same shape as [TransactionRepository]'s. */
class TransferRepository(private val dao: TransferDao, scope: CoroutineScope) {
    private val all = dao.getAll().shareIn(scope, SharingStarted.Eagerly, replay = 1)

    fun getAll(): Flow<List<TransferEntity>> = all
    suspend fun getAllOnce(): List<TransferEntity> = dao.getAllOnce()
    suspend fun insert(transfer: TransferEntity): Long = dao.insert(transfer)
    suspend fun update(transfer: TransferEntity) = dao.update(transfer)
    suspend fun delete(transfer: TransferEntity) = dao.delete(transfer)
    suspend fun countByCategory(categoryId: Long): Int = dao.countByCategory(categoryId)
}
