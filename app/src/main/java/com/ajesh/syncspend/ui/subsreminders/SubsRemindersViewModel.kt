package com.ajesh.syncspend.ui.subsreminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajesh.syncspend.alarm.AlarmScheduler
import com.ajesh.syncspend.data.datastore.PreferencesRepository
import com.ajesh.syncspend.data.db.entity.ReminderEntity
import com.ajesh.syncspend.data.db.entity.SubscriptionEntity
import com.ajesh.syncspend.data.repository.ReminderRepository
import com.ajesh.syncspend.data.repository.SubscriptionRepository
import com.ajesh.syncspend.domain.model.BillingCycle
import com.ajesh.syncspend.domain.model.ReminderSchedule
import com.ajesh.syncspend.util.CurrencyFormatter
import com.ajesh.syncspend.util.DateUtils
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ListRowUi(val id: Long, val name: String, val meta: String, val iconKey: String)

data class SubsRemindersUiState(
    val currencySymbol: String = "₹",
    val summaryValue: String = "",
    val subscriptionRows: List<ListRowUi> = emptyList(),
    val reminderRows: List<ListRowUi> = emptyList(),
    val subscriptions: List<SubscriptionEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val monthlyRecurring: String = "",
    val activeReminders: String = "",
)

/** Both lists (subscriptions and reminders) live in one ViewModel; the screen shows whichever mode it was opened in. */
class SubsRemindersViewModel(
    private val subscriptionRepository: SubscriptionRepository,
    private val reminderRepository: ReminderRepository,
    preferencesRepository: PreferencesRepository,
    private val alarmScheduler: AlarmScheduler,
) : ViewModel() {

    val uiState: StateFlow<SubsRemindersUiState> = combine(
        subscriptionRepository.getAll(),
        reminderRepository.getAll(),
        preferencesRepository.preferences,
    ) { subs, reminders, prefs ->
        val cur = prefs.currencyCode.symbol
        val monthly = subs.filter { it.active }.sumOf {
            when (it.billingCycle) {
                BillingCycle.WEEKLY -> it.amount * 52.0 / 12.0
                BillingCycle.MONTHLY -> it.amount
                BillingCycle.YEARLY -> it.amount / 12.0
            }
        }
        SubsRemindersUiState(
            currencySymbol = cur,
            subscriptions = subs,
            reminders = reminders,
            monthlyRecurring = cur + CurrencyFormatter.amount(monthly),
            activeReminders = "${reminders.count { it.active }} scheduled",
            subscriptionRows = subs.map {
                ListRowUi(
                    it.id, it.name,
                    "$cur${CurrencyFormatter.amount(it.amount)} · ${it.billingCycle.name.lowercase()} · ${DateUtils.shortDate(it.nextDueDate)}",
                    it.iconKey,
                )
            },
            reminderRows = reminders.map { ListRowUi(it.id, it.label, reminderMeta(it), it.iconKey) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubsRemindersUiState())

    fun saveSubscription(existing: SubscriptionEntity?, name: String, iconKey: String, amount: Double, cycle: BillingCycle, due: LocalDate) {
        viewModelScope.launch {
            val entity = (existing ?: SubscriptionEntity(name = name, iconKey = iconKey, amount = amount, billingCycle = cycle, nextDueDate = due, categoryId = null, active = true))
                .copy(name = name, iconKey = iconKey, amount = amount, billingCycle = cycle, nextDueDate = due)
            val saved = if (existing == null) entity.copy(id = subscriptionRepository.insert(entity)) else entity.also { subscriptionRepository.update(it) }
            alarmScheduler.scheduleSubscription(saved)
        }
    }

    fun deleteSubscription(subscription: SubscriptionEntity) {
        viewModelScope.launch {
            alarmScheduler.cancelSubscription(subscription.id)
            subscriptionRepository.delete(subscription)
        }
    }

    fun saveReminder(existing: ReminderEntity?, label: String, iconKey: String, schedule: ReminderSchedule, date: LocalDate, minuteOfDay: Int) {
        viewModelScope.launch {
            val entity = (existing ?: ReminderEntity(label = label, iconKey = iconKey, schedule = schedule, timeMinuteOfDay = minuteOfDay, nextTriggerDate = date, active = true))
                .copy(label = label, iconKey = iconKey, schedule = schedule, timeMinuteOfDay = minuteOfDay, nextTriggerDate = date, active = true)
            val saved = if (existing == null) entity.copy(id = reminderRepository.insert(entity)) else entity.also { reminderRepository.update(it) }
            alarmScheduler.scheduleReminder(saved)
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            alarmScheduler.cancelReminder(reminder.id)
            reminderRepository.delete(reminder)
        }
    }

    private fun reminderMeta(r: ReminderEntity): String {
        val h = r.timeMinuteOfDay / 60
        val m = r.timeMinuteOfDay % 60
        val time = "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
        val d = r.nextTriggerDate
        val whenText = when (r.schedule) {
            ReminderSchedule.DAILY -> "Every day"
            ReminderSchedule.WEEKLY -> "Every ${d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)}"
            ReminderSchedule.MONTHLY -> "${ordinal(d.dayOfMonth)} monthly"
            ReminderSchedule.ONCE -> "${DateUtils.shortDate(d)} ${d.year}" + if (!r.active) " (done)" else ""
        }
        return "$whenText · $time"
    }

    private fun ordinal(n: Int): String {
        val suffix = if (n in 11..13) "th" else when (n % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }
        return "$n$suffix"
    }
}
