package com.ajesh.syncspend

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.ajesh.syncspend.alarm.NotificationHelper
import com.ajesh.syncspend.domain.model.AppLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** What MainActivity does with a notification's intent, before anything is composed. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityLinkTest {
    private val context: Context = ApplicationProvider.getApplicationContext<Application>()
    private val container get() = (context as SyncSpendApp).container

    private fun postNotification(id: Int): NotificationManager {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("test", "test", NotificationManager.IMPORTANCE_DEFAULT))
        manager.notify(id, NotificationCompat.Builder(context, "test").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("x").build())
        return manager
    }

    @Test fun aColdStartFromANotificationTapRemembersThePageAndClearsTheNotificationWhenAsked() {
        container.selectionState.pendingLink.value = null
        val manager = postNotification(1000)
        val intent = NotificationHelper.linkIntent(context, AppLink.AddEntry, cancelNotificationId = 1000)

        Robolectric.buildActivity(MainActivity::class.java, intent).create()

        assertEquals(AppLink.AddEntry, container.selectionState.pendingLink.value)
        assertEquals(0, shadowOf(manager).activeNotifications.size)
    }

    @Test fun anOrdinaryLaunchLeavesNoLink() {
        container.selectionState.pendingLink.value = null
        Robolectric.buildActivity(MainActivity::class.java, Intent(context, MainActivity::class.java)).create()
        assertNull(container.selectionState.pendingLink.value)
    }

    @Test fun aTapWhileTheAppIsOpenArrivesAsANewIntent() {
        container.selectionState.pendingLink.value = null
        val controller = Robolectric.buildActivity(MainActivity::class.java, Intent(context, MainActivity::class.java)).create()
        controller.newIntent(NotificationHelper.linkIntent(context, AppLink.Reminders(9)))
        assertEquals(AppLink.Reminders(9), container.selectionState.pendingLink.value)
    }
}
