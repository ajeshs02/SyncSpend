package com.ajesh.syncspend

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import com.ajesh.syncspend.alarm.AlarmReceiver
import com.ajesh.syncspend.alarm.NotificationActionReceiver
import com.ajesh.syncspend.alarm.NotificationHelper
import com.ajesh.syncspend.domain.model.AppLink
import com.ajesh.syncspend.ui.subsreminders.ListRowUi
import com.ajesh.syncspend.ui.subsreminders.withHighlightFirst
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationLinksTest {
    private val context: Context = ApplicationProvider.getApplicationContext<Application>()

    // --- AppLink ----------------------------------------------------------------------------------------

    @Test fun aLinkSurvivesTheTripThroughIntentExtras() {
        listOf(AppLink.AddEntry, AppLink.Subscriptions(7), AppLink.Reminders(12), AppLink.Subscriptions(null)).forEach { link ->
            assertEquals(link, AppLink.parse(AppLink.kindOf(link), AppLink.idOf(link)))
        }
    }

    @Test fun anOrdinaryLaunchHasNoLink() {
        assertNull(AppLink.parse(null, AppLink.NO_ID))
        assertNull(AppLink.parse("something else", 5))
        assertEquals(AppLink.Reminders(null), AppLink.parse(AppLink.KIND_REMINDERS, AppLink.NO_ID))
    }

    // --- the daily reminder's buttons -------------------------------------------------------------------

    @Test fun theDailyReminderHasAddAndDoneButtons() {
        val actions = AlarmReceiver.dailyActions(context)
        assertEquals(listOf("Add", "Done"), actions.map { it.label })
    }

    @Test fun addOpensAddEntryAndAsksForTheNotificationToBeCleared() {
        val add = AlarmReceiver.dailyActions(context).first { it.label == "Add" }
        val intent = shadowOf(add.intent).savedIntent
        assertEquals("com.ajesh.syncspend.MainActivity", intent.component?.className)
        assertEquals(AppLink.KIND_ADD_ENTRY, intent.getStringExtra(AppLink.EXTRA_KIND))
        assertEquals(AlarmReceiver.NOTIF_DAILY, intent.getIntExtra(AppLink.EXTRA_CANCEL_NOTIFICATION, -1))
        assertEquals(true, shadowOf(add.intent).isActivityIntent)
    }

    @Test fun doneIsABroadcastThatOnlyClearsTheNotification() {
        val done = AlarmReceiver.dailyActions(context).first { it.label == "Done" }
        assertEquals(true, shadowOf(done.intent).isBroadcastIntent)
        val intent = shadowOf(done.intent).savedIntent

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("test", "test", NotificationManager.IMPORTANCE_DEFAULT))
        manager.notify(AlarmReceiver.NOTIF_DAILY, NotificationCompat.Builder(context, "test").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("x").build())
        assertEquals(1, shadowOf(manager).activeNotifications.size)

        NotificationActionReceiver().onReceive(context, intent)
        assertEquals(0, shadowOf(manager).activeNotifications.size)
    }

    @Test fun aStrayBroadcastDoesNotClearAnything() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("test", "test", NotificationManager.IMPORTANCE_DEFAULT))
        manager.notify(5, NotificationCompat.Builder(context, "test").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("x").build())
        NotificationActionReceiver().onReceive(context, Intent("com.example.other").putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, 5))
        assertEquals(1, shadowOf(manager).activeNotifications.size)
    }

    // --- taps on subscription / reminder notifications --------------------------------------------------

    @Test fun aSubscriptionNotificationOpensThatSubscription() {
        val intent = NotificationHelper.linkIntent(context, AppLink.Subscriptions(42))
        assertEquals(AppLink.Subscriptions(42), AppLink.parse(intent.getStringExtra(AppLink.EXTRA_KIND), intent.getLongExtra(AppLink.EXTRA_ID, -1)))
        assertEquals(-1, intent.getIntExtra(AppLink.EXTRA_CANCEL_NOTIFICATION, -1)) // tapping already dismisses it (autoCancel)
    }

    // --- the highlighted row goes to the top ------------------------------------------------------------

    private val rows = listOf(1L, 2L, 3L, 4L).map { ListRowUi(it, "n$it", "m", "bell") }

    @Test fun theHighlightedRowMovesToTheTopAndTheRestKeepTheirOrder() {
        assertEquals(listOf(3L, 1L, 2L, 4L), rows.withHighlightFirst(3).map { it.id })
        assertEquals(listOf(4L, 1L, 2L, 3L), rows.withHighlightFirst(4).map { it.id })
    }

    @Test fun noHighlightOrAnUnknownOneLeavesTheListAlone() {
        assertSame(rows, rows.withHighlightFirst(null))
        assertSame(rows, rows.withHighlightFirst(99))
        assertSame(rows, rows.withHighlightFirst(1)) // already first
    }
}
