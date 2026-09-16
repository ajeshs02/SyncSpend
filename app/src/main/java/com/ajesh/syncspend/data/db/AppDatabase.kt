package com.ajesh.syncspend.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ajesh.syncspend.data.db.dao.CategoryDao
import com.ajesh.syncspend.data.db.dao.ReminderDao
import com.ajesh.syncspend.data.db.dao.SubscriptionDao
import com.ajesh.syncspend.data.db.dao.TransactionDao
import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.data.db.entity.ReminderEntity
import com.ajesh.syncspend.data.db.entity.SubscriptionEntity
import com.ajesh.syncspend.data.db.entity.TransactionEntity

/**
 * fallbackToDestructiveMigration (see [com.ajesh.syncspend.di.DefaultAppContainer])
 * pre-1.0 — single-user personal app, no installs in the wild to migrate yet.
 */
@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        SubscriptionEntity::class,
        ReminderEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun reminderDao(): ReminderDao
}
