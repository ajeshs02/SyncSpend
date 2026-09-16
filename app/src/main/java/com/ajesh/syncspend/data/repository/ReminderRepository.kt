package com.ajesh.syncspend.data.repository

import com.ajesh.syncspend.data.db.dao.ReminderDao
import com.ajesh.syncspend.data.db.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

class ReminderRepository(private val dao: ReminderDao) {
    fun getAll(): Flow<List<ReminderEntity>> = dao.getAll()
    suspend fun getActiveOnce(): List<ReminderEntity> = dao.getActiveOnce()
    suspend fun insert(reminder: ReminderEntity): Long = dao.insert(reminder)
    suspend fun update(reminder: ReminderEntity) = dao.update(reminder)
    suspend fun delete(reminder: ReminderEntity) = dao.delete(reminder)
}
