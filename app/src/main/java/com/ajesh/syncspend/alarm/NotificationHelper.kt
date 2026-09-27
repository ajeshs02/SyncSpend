package com.ajesh.syncspend.alarm

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ajesh.syncspend.MainActivity
import com.ajesh.syncspend.R
import com.ajesh.syncspend.domain.model.AppLink

object NotificationHelper {
    const val CHANNEL_ID = "syncspend_reminders"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Daily log reminder, subscription due dates and custom reminders" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** A button on a notification. */
    class Action(val label: String, val intent: PendingIntent)

    /** The launch intent that follows [link] (and dismisses notification [cancelNotificationId], if given). */
    fun linkIntent(context: Context, link: AppLink, cancelNotificationId: Int? = null): Intent =
        Intent(context, MainActivity::class.java)
            .putExtra(AppLink.EXTRA_KIND, AppLink.kindOf(link))
            .putExtra(AppLink.EXTRA_ID, AppLink.idOf(link))
            .apply { cancelNotificationId?.let { putExtra(AppLink.EXTRA_CANCEL_NOTIFICATION, it) } }

    /** A button that opens [link] and dismisses the notification it sits on. */
    fun linkAction(context: Context, notificationId: Int, label: String, link: AppLink): Action {
        val intent = linkIntent(context, link, cancelNotificationId = notificationId)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return Action(
            label,
            PendingIntent.getActivity(context, notificationId * 10 + 1, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
        )
    }

    @SuppressLint("MissingPermission") // guarded above for API 33+
    fun show(
        context: Context,
        notificationId: Int,
        title: String,
        text: String,
        openIntent: Intent = Intent(context, MainActivity::class.java),
        actions: List<Action> = emptyList(),
    ) {
        // POST_NOTIFICATIONS is only a runtime permission on Android 13+; on older
        // releases it doesn't exist, so checking it would wrongly read as "denied".
        val permitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!permitted || !NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        openIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val tap = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(tap)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        actions.forEach { builder.addAction(0, it.label, it.intent) }
        val notification = builder.build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
