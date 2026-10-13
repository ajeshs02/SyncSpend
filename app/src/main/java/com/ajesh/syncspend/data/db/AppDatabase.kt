package com.ajesh.syncspend.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ajesh.syncspend.data.db.dao.CategoryDao
import com.ajesh.syncspend.data.db.dao.ForecastDao
import com.ajesh.syncspend.data.db.dao.ReminderDao
import com.ajesh.syncspend.data.db.dao.SubscriptionDao
import com.ajesh.syncspend.data.db.dao.TransactionDao
import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.data.db.entity.ForecastEntity
import com.ajesh.syncspend.data.db.entity.ReminderEntity
import com.ajesh.syncspend.data.db.entity.SubscriptionEntity
import com.ajesh.syncspend.data.db.entity.TransactionEntity

/**
 * Version 13 (see [MIGRATION_3_4], [MIGRATION_4_5], [MIGRATION_5_6], [MIGRATION_6_7],
 * [MIGRATION_7_8], [MIGRATION_8_9], [MIGRATION_9_10], [MIGRATION_10_11], [MIGRATION_11_12],
 * [MIGRATION_12_13]). There is no destructive-migration fallback (see
 * [com.ajesh.syncspend.di.DefaultAppContainer]) — bumping [Database.version] requires shipping a
 * real `Migration`.
 */
@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        SubscriptionEntity::class,
        ReminderEntity::class,
        ForecastEntity::class,
    ],
    version = 13,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun reminderDao(): ReminderDao
    abstract fun forecastDao(): ForecastDao
}
