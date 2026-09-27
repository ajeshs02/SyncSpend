package com.ajesh.syncspend.screenshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import com.ajesh.syncspend.data.db.entity.ReminderEntity
import com.ajesh.syncspend.data.db.entity.SubscriptionEntity
import com.ajesh.syncspend.di.DefaultAppContainer
import com.ajesh.syncspend.domain.model.BillingCycle
import com.ajesh.syncspend.domain.model.ReminderSchedule
import com.ajesh.syncspend.ui.components.BottomFadeAndNav
import com.ajesh.syncspend.ui.subsreminders.SubsRemindersScreen
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-xxhdpi")
class SubsRemindersScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private fun container(): Pair<DefaultAppContainer, LongArray> {
        val c = sampleContainer()
        val today = LocalDate.now()
        val ids = runBlocking {
            longArrayOf(
                c.subscriptionRepository.insert(SubscriptionEntity(name = "Spotify", iconKey = "music", amount = 119.0, billingCycle = BillingCycle.MONTHLY, nextDueDate = today.plusDays(2), categoryId = null, active = true)),
                c.subscriptionRepository.insert(SubscriptionEntity(name = "Netflix", iconKey = "film", amount = 649.0, billingCycle = BillingCycle.MONTHLY, nextDueDate = today.plusDays(9), categoryId = null, active = true)),
                c.subscriptionRepository.insert(SubscriptionEntity(name = "Gym", iconKey = "dumbbell", amount = 1200.0, billingCycle = BillingCycle.MONTHLY, nextDueDate = today.plusDays(20), categoryId = null, active = true)),
                c.reminderRepository.insert(ReminderEntity(label = "Pay rent", iconKey = "house", schedule = ReminderSchedule.MONTHLY, timeMinuteOfDay = 9 * 60, nextTriggerDate = today.plusDays(3), active = true)),
                c.reminderRepository.insert(ReminderEntity(label = "Water plants", iconKey = "heart", schedule = ReminderSchedule.WEEKLY, timeMinuteOfDay = 8 * 60, nextTriggerDate = today.plusDays(1), active = true)),
            )
        }
        return c to ids
    }

    @Test fun theSubscriptionANotificationWasAboutIsOnTopAndOutlined() {
        val (c, ids) = container()
        rule.snapshot(
            "subs_highlighted", container = c,
            beforeCapture = { waitUntil(15_000) { onAllNodes(hasText("Gym")).fetchSemanticsNodes().isNotEmpty() } },
        ) {
            Box(Modifier.fillMaxSize()) {
                SubsRemindersScreen(listMode = "subs", highlightId = ids[2], onBack = {})
                BottomFadeAndNav(currentRoute = "home", onNavigate = {}, onAddClick = {})
            }
        }
    }

    @Test fun remindersNoLongerListTheDailyLogReminder() {
        val (c, ids) = container()
        rule.snapshot(
            "reminders_no_daily_row", dark = true, container = c,
            beforeCapture = { waitUntil(15_000) { onAllNodes(hasText("Pay rent")).fetchSemanticsNodes().isNotEmpty() } },
        ) {
            Box(Modifier.fillMaxSize()) {
                SubsRemindersScreen(listMode = "alerts", highlightId = ids[4], onBack = {})
                BottomFadeAndNav(currentRoute = "home", onNavigate = {}, onAddClick = {})
            }
        }
        rule.onAllNodes(hasText("Daily log reminder")).assertCountEquals(0)
    }
}
