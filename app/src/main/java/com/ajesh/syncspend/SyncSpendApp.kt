package com.ajesh.syncspend

import android.app.Application
import com.ajesh.syncspend.alarm.NotificationHelper
import com.ajesh.syncspend.di.AppContainer
import com.ajesh.syncspend.di.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Owns the app-wide manual-DI [AppContainer]. */
class SyncSpendApp : Application() {
    val container: AppContainer by lazy { DefaultAppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        // Defensive re-arm on cold start in case the OS dropped any alarm.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { container.alarmScheduler.rescheduleAll() }
    }
}
