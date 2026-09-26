package com.ajesh.syncspend.ui.subsreminders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ajesh.syncspend.data.db.entity.ReminderEntity
import com.ajesh.syncspend.data.db.entity.SubscriptionEntity
import com.ajesh.syncspend.di.LocalAppContainer
import com.ajesh.syncspend.ui.components.ConfirmDialog
import com.ajesh.syncspend.ui.components.HeaderAddButton
import com.ajesh.syncspend.ui.components.SquareIconButton
import com.ajesh.syncspend.ui.components.SyncSpendChrome
import com.ajesh.syncspend.ui.components.rememberNotificationGate
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme

/** Subscriptions or Reminders (one screen, two modes) — both post real AlarmManager notifications. */
@Composable
fun SubsRemindersScreen(listMode: String, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: SubsRemindersViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                SubsRemindersViewModel(
                    container.subscriptionRepository,
                    container.reminderRepository,
                    container.preferencesRepository,
                    container.alarmScheduler,
                )
            }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SyncSpendTheme.colors
    val isSubs = listMode == "subs"
    val gate = rememberNotificationGate()

    var showAddSub by remember { mutableStateOf(false) }
    var editingSub by remember { mutableStateOf<SubscriptionEntity?>(null) }
    var deletingSub by remember { mutableStateOf<SubscriptionEntity?>(null) }
    var showAddReminder by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<ReminderEntity?>(null) }
    var deletingReminder by remember { mutableStateOf<ReminderEntity?>(null) }

    val rows = if (isSubs) state.subscriptionRows else state.reminderRows

    Column(modifier = Modifier.fillMaxSize().padding(top = SyncSpendChrome.screenTopInset)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SquareIconButton(
                SyncSpendIcons.Back, onBack, size = 36.dp, radius = 13.dp, iconSize = 17.dp,
                background = colors.pill,
                modifier = Modifier.border(1.dp, colors.line, RoundedCornerShape(13.dp)),
            )
            Text(if (isSubs) "Subscriptions" else "Reminders", style = MaterialTheme.typography.titleLarge, color = colors.ink, modifier = Modifier.weight(1f))
            HeaderAddButton("Add", onClick = { if (isSubs) showAddSub = true else showAddReminder = true })
        }

        Column(
            modifier = Modifier
                .padding(start = 22.dp, end = 22.dp, top = 16.dp)
                .fillMaxWidth()
                .background(colors.mintGradient, RoundedCornerShape(20.dp))
                .padding(16.dp),
        ) {
            Text(if (isSubs) "Monthly recurring" else "Active reminders", fontSize = 11.sp, color = colors.msub)
            Text(
                if (isSubs) state.monthlyRecurring else state.activeReminders,
                fontSize = 24.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                letterSpacing = (-0.24).sp,
                color = colors.mink,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        if (rows.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 40.dp, end = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (isSubs) "No subscriptions yet" else "No reminders yet",
                    style = MaterialTheme.typography.bodyMedium, color = colors.ink,
                )
                Text(
                    "Tap Add to create one — you'll get a real notification when it's due.",
                    style = MaterialTheme.typography.bodySmall, color = colors.sub,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 12.dp, bottom = SyncSpendChrome.screenBottomContentPadding),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                items(rows, key = { it.id }) { row ->
                    Row(
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .background(colors.card, RoundedCornerShape(16.dp))
                            .border(1.dp, colors.line, RoundedCornerShape(16.dp))
                            .padding(horizontal = 13.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(34.dp).background(colors.tile, RoundedCornerShape(11.dp)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(SyncSpendIcons.iconFor(row.iconKey), null, tint = colors.ink, modifier = Modifier.size(16.dp)) }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(row.name, style = MaterialTheme.typography.bodyMedium, color = colors.ink, maxLines = 1)
                            Text(row.meta, fontSize = 10.5.sp, color = colors.sub, modifier = Modifier.padding(top = 2.dp), maxLines = 1)
                        }
                        SquareIconButton(SyncSpendIcons.Pencil, {
                            if (isSubs) editingSub = state.subscriptions.find { it.id == row.id }
                            else editingReminder = state.reminders.find { it.id == row.id }
                        }, size = 30.dp, iconSize = 15.dp)
                        SquareIconButton(SyncSpendIcons.Trash, {
                            if (isSubs) deletingSub = state.subscriptions.find { it.id == row.id }
                            else deletingReminder = state.reminders.find { it.id == row.id }
                        }, size = 30.dp, iconSize = 15.dp)
                    }
                }
            }
        }
    }

    if (showAddSub || editingSub != null) {
        val editing = editingSub
        SubscriptionDialog(
            initial = editing,
            currencySymbol = state.currencySymbol,
            onSave = { name, icon, amount, cycle, due ->
                showAddSub = false
                editingSub = null
                gate(onDenied = { viewModel.saveSubscription(editing, name, icon, amount, cycle, due) }) {
                    viewModel.saveSubscription(editing, name, icon, amount, cycle, due)
                }
            },
            onDismiss = { showAddSub = false; editingSub = null },
        )
    }
    if (showAddReminder || editingReminder != null) {
        val editing = editingReminder
        ReminderDialog(
            initial = editing,
            onSave = { label, icon, schedule, date, minute ->
                showAddReminder = false
                editingReminder = null
                gate(onDenied = { viewModel.saveReminder(editing, label, icon, schedule, date, minute) }) {
                    viewModel.saveReminder(editing, label, icon, schedule, date, minute)
                }
            },
            onDismiss = { showAddReminder = false; editingReminder = null },
        )
    }
    deletingSub?.let { sub ->
        ConfirmDialog(
            title = "Delete subscription?",
            body = "“${sub.name}” will stop appearing on your list and its reminder is cancelled.",
            cta = "Delete",
            onConfirm = { viewModel.deleteSubscription(sub); deletingSub = null },
            onDismiss = { deletingSub = null },
        )
    }
    deletingReminder?.let { r ->
        ConfirmDialog(
            title = "Delete reminder?",
            body = "“${r.label}” will stop appearing on your list and its notification is cancelled.",
            cta = "Delete",
            onConfirm = { viewModel.deleteReminder(r); deletingReminder = null },
            onDismiss = { deletingReminder = null },
        )
    }
}
