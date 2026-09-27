package com.ajesh.syncspend.data.repository

import android.content.Context
import com.ajesh.syncspend.data.db.dao.TransactionDao
import com.ajesh.syncspend.data.db.entity.TransactionEntity
import com.ajesh.syncspend.widget.WidgetRefresher
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

class TransactionRepository(private val dao: TransactionDao, scope: CoroutineScope, private val context: Context) {
    /**
     * One live query shared by every screen (Home, Transactions, Settings...)
     * and kept warm for the app's lifetime, replaying the latest list. Without
     * this each screen ran its own `SELECT *` on every write, and a fresh
     * screen had to wait for Room before it could draw anything.
     */
    private val all = dao.getAll().shareIn(scope, SharingStarted.Eagerly, replay = 1)

    fun getAll(): Flow<List<TransactionEntity>> = all
    suspend fun getAllOnce(): List<TransactionEntity> = dao.getAllOnce()
    fun getById(id: Long): Flow<TransactionEntity?> = dao.getById(id)
    fun getEarliestDate(): Flow<LocalDate?> = dao.getEarliestDate()
    fun getCount(): Flow<Int> = dao.getCount()

    suspend fun insert(transaction: TransactionEntity): Long = dao.insert(transaction).also { refreshWidget() }
    suspend fun update(transaction: TransactionEntity) {
        dao.update(transaction)
        refreshWidget()
    }
    suspend fun delete(transaction: TransactionEntity) {
        dao.delete(transaction)
        refreshWidget()
    }
    suspend fun countByCategory(categoryId: Long): Int = dao.countByCategory(categoryId)

    /** Fire-and-forget: the widget shows today's expense total, so any change may affect it. */
    private fun refreshWidget() = WidgetRefresher.requestUpdate(context)
}
