package com.ajesh.syncspend.di

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.room.Room
import com.ajesh.syncspend.alarm.AlarmScheduler
import com.ajesh.syncspend.alarm.AlarmSchedulerImpl
import com.ajesh.syncspend.data.datastore.PreferencesRepository
import com.ajesh.syncspend.data.db.AppDatabase
import com.ajesh.syncspend.data.repository.CategoryRepository
import com.ajesh.syncspend.data.repository.ReminderRepository
import com.ajesh.syncspend.data.repository.SubscriptionRepository
import com.ajesh.syncspend.data.repository.TransactionRepository
import com.ajesh.syncspend.domain.state.SharedSelectionState

/**
 * Manual DI root (no Hilt/Dagger/Koin). [com.ajesh.syncspend.SyncSpendApp] owns
 * one instance; screens reach it via `LocalAppContainer` (added in Phase 2
 * alongside the navigation shell), broadcast receivers via
 * `(context.applicationContext as SyncSpendApp).container` directly.
 */
interface AppContainer {
    val database: AppDatabase
    val categoryRepository: CategoryRepository
    val transactionRepository: TransactionRepository
    val subscriptionRepository: SubscriptionRepository
    val reminderRepository: ReminderRepository
    val preferencesRepository: PreferencesRepository
    val selectionState: SharedSelectionState
    val alarmScheduler: AlarmScheduler
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val database: AppDatabase by lazy {
        Room.databaseBuilder(context, AppDatabase::class.java, "syncspend.db")
            // Pre-1.0, single-user personal app — no installs in the wild to
            // migrate yet. Real migrations start once the schema needs to
            // change under existing data.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    override val categoryRepository: CategoryRepository by lazy {
        CategoryRepository(database.categoryDao())
    }

    override val transactionRepository: TransactionRepository by lazy {
        TransactionRepository(database.transactionDao())
    }

    override val subscriptionRepository: SubscriptionRepository by lazy {
        SubscriptionRepository(database.subscriptionDao())
    }

    override val reminderRepository: ReminderRepository by lazy {
        ReminderRepository(database.reminderDao())
    }

    override val preferencesRepository: PreferencesRepository by lazy {
        PreferencesRepository(context)
    }

    override val selectionState: SharedSelectionState by lazy { SharedSelectionState() }

    override val alarmScheduler: AlarmScheduler by lazy {
        AlarmSchedulerImpl(context, subscriptionRepository, reminderRepository, preferencesRepository)
    }
}

/** Provided once at [com.ajesh.syncspend.MainActivity]'s root. */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("LocalAppContainer not provided — did you forget CompositionLocalProvider at the root?")
}
