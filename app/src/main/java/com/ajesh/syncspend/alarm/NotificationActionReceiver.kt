package com.ajesh.syncspend.alarm

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

/**
 * Handles a notification button that only dismisses it ("Done" on the daily log reminder). It touches no
 * data, so it does no more than cancel the notification.
 */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DISMISS) return
        val id = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (id >= 0) NotificationManagerCompat.from(context).cancel(id)
    }

    companion object {
        internal const val ACTION_DISMISS = "com.ajesh.syncspend.action.DISMISS_NOTIFICATION"
        internal const val EXTRA_NOTIFICATION_ID = "notification_id"

        fun dismissAction(context: Context, notificationId: Int, label: String): NotificationHelper.Action {
            val intent = Intent(context, NotificationActionReceiver::class.java)
                .setAction(ACTION_DISMISS)
                .putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            return NotificationHelper.Action(
                label,
                PendingIntent.getBroadcast(context, notificationId * 10 + 2, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
            )
        }
    }
}
