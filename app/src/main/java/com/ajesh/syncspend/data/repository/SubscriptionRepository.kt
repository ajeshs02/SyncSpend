package com.ajesh.syncspend.data.repository

import com.ajesh.syncspend.data.db.dao.SubscriptionDao
import com.ajesh.syncspend.data.db.entity.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

class SubscriptionRepository(private val dao: SubscriptionDao) {
    fun getAll(): Flow<List<SubscriptionEntity>> = dao.getAll()
    suspend fun getActiveOnce(): List<SubscriptionEntity> = dao.getActiveOnce()
    suspend fun insert(subscription: SubscriptionEntity): Long = dao.insert(subscription)
    suspend fun update(subscription: SubscriptionEntity) = dao.update(subscription)
    suspend fun delete(subscription: SubscriptionEntity) = dao.delete(subscription)
}
