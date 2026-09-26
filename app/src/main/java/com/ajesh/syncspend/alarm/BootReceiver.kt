package com.ajesh.syncspend.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ajesh.syncspend.SyncSpendApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Alarms don't survive a reboot or an app update — re-book them all. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val app = context.applicationContext as SyncSpendApp
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                app.container.alarmScheduler.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
