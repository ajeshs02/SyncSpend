package com.ajesh.syncspend.di

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.room.Room
import com.ajesh.syncspend.alarm.AlarmScheduler
import com.ajesh.syncspend.alarm.AlarmSchedulerImpl
import com.ajesh.syncspend.alarm.DailyReminderManager
import com.ajesh.syncspend.data.datastore.PreferencesRepository
import com.ajesh.syncspend.data.db.AppDatabase
import com.ajesh.syncspend.data.db.MIGRATION_3_4
import com.ajesh.syncspend.data.db.MIGRATION_4_5
import com.ajesh.syncspend.data.db.MIGRATION_5_6
import com.ajesh.syncspend.data.db.MIGRATION_6_7
import com.ajesh.syncspend.data.db.MIGRATION_7_8
import com.ajesh.syncspend.data.repository.CategoryRepository
import com.ajesh.syncspend.data.repository.CategorySeeder
import com.ajesh.syncspend.data.repository.ForecastRepository
import com.ajesh.syncspend.data.repository.ReminderRepository
import com.ajesh.syncspend.data.repository.SubscriptionRepository
import com.ajesh.syncspend.data.repository.TransactionRepository
import com.ajesh.syncspend.data.repository.TransferRepository
import com.ajesh.syncspend.domain.state.SharedSelectionState
import com.ajesh.syncspend.widget.WidgetRefresher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

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
    val forecastRepository: ForecastRepository
    val transferRepository: TransferRepository
    val preferencesRepository: PreferencesRepository
    val selectionState: SharedSelectionState
    val alarmScheduler: AlarmScheduler
    val dailyReminderManager: DailyReminderManager

    /**
     * "Not now" on the Forecast past-month review popup — in-memory only, never persisted, so it
     * resets to false (popup eligible again) on every fresh app launch rather than suppressing the
     * popup for the rest of the calendar month.
     */
    val forecastReviewDismissedThisSession: MutableStateFlow<Boolean>

    /** App-lifetime scope for the shared, always-warm data streams. */
    val appScope: CoroutineScope

    /** True once transactions, categories and preferences are loaded — the splash screen waits for it. */
    val ready: StateFlow<Boolean>

    /** Loads the shared streams so the first screen can draw complete data. */
    suspend fun warmUp()

    /** Wipes transactions, categories, subscriptions, reminders, forecasts and transfers, then restores the starter categories. */
    suspend fun clearAllData()
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _ready = MutableStateFlow(false)
    override val ready: StateFlow<Boolean> = _ready.asStateFlow()

    override suspend fun warmUp() {
        // Starter categories first, so the very first screens (and pickers) already have them.
        categorySeeder.seedIfNeeded()
        combine(transactionRepository.getAll(), categoryRepository.getAll(), preferencesRepository.preferences) { _, _, _ -> }.first()
        _ready.value = true
    }

    override val database: AppDatabase by lazy {
        // Deliberately no destructive fallback: the database holds real data now.
        // Any future schema change needs an explicit Migration (a missing one
        // fails loudly instead of silently wiping the user's entries).
        Room.databaseBuilder(context, AppDatabase::class.java, "syncspend.db")
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
            .build()
    }

    private val categorySeeder: CategorySeeder by lazy { CategorySeeder(categoryRepository, preferencesRepository) }

    override val categoryRepository: CategoryRepository by lazy {
        CategoryRepository(database.categoryDao(), appScope)
    }

    override val transactionRepository: TransactionRepository by lazy {
        TransactionRepository(database.transactionDao(), appScope, context, database.forecastDao())
    }

    override val subscriptionRepository: SubscriptionRepository by lazy {
        SubscriptionRepository(database.subscriptionDao())
    }

    override val reminderRepository: ReminderRepository by lazy {
        ReminderRepository(database.reminderDao())
    }

    override val forecastRepository: ForecastRepository by lazy {
        ForecastRepository(database.forecastDao())
    }

    override val transferRepository: TransferRepository by lazy {
        TransferRepository(database.transferDao(), appScope)
    }

    override val preferencesRepository: PreferencesRepository by lazy {
        PreferencesRepository(context, appScope)
    }

    override val selectionState: SharedSelectionState by lazy { SharedSelectionState() }

    override val forecastReviewDismissedThisSession = MutableStateFlow(false)

    override val alarmScheduler: AlarmScheduler by lazy {
        AlarmSchedulerImpl(context, subscriptionRepository, reminderRepository, preferencesRepository)
    }

    override val dailyReminderManager: DailyReminderManager by lazy {
        DailyReminderManager(preferencesRepository, alarmScheduler)
    }

    override suspend fun clearAllData() {
        withContext(Dispatchers.IO) { database.clearAllTables() }
        categorySeeder.reseed()
        WidgetRefresher.requestUpdate(context)
    }
}

/** Provided once at [com.ajesh.syncspend.MainActivity]'s root. */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("LocalAppContainer not provided. Did you forget CompositionLocalProvider at the root?")
}
