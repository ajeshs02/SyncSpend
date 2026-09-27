package com.ajesh.syncspend.ui.subsreminders

import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ajesh.syncspend.data.db.entity.ReminderEntity
import com.ajesh.syncspend.data.db.entity.SubscriptionEntity
import com.ajesh.syncspend.di.LocalAppContainer
import com.ajesh.syncspend.ui.components.DesignSwitch
import com.ajesh.syncspend.ui.components.HeaderAddButton
import com.ajesh.syncspend.ui.components.SquareIconButton
import com.ajesh.syncspend.ui.components.SyncSpendChrome
import com.ajesh.syncspend.ui.components.rememberNotificationGate
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme

/** Subscriptions or Reminders (one screen, two modes) — both post real AlarmManager notifications. */
@Composable
fun SubsRemindersScreen(listMode: String, highlightId: Long?, onBack: () -> Unit) {
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
    var showAddReminder by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<ReminderEntity?>(null) }

    // A notification opened this page for one entry: it goes to the top and is outlined.
    val rows = (if (isSubs) state.subscriptionRows else state.reminderRows).withHighlightFirst(highlightId)

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
                    "Tap Add to create one. You'll get a real notification when it's due.",
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
                items(rows, key = { it.id }, contentType = { "row" }) { row ->
                    ListRow(
                        row = row,
                        highlighted = highlightId != null && row.id == highlightId,
                        modifier = Modifier.animateItem(),
                        onClick = {
                            if (isSubs) editingSub = state.subscriptions.find { it.id == row.id }
                            else editingReminder = state.reminders.find { it.id == row.id }
                        },
                        onToggle = { on ->
                            when {
                                isSubs -> state.subscriptions.find { it.id == row.id }?.let { sub ->
                                    if (on) gate(onDenied = { viewModel.setSubscriptionActive(sub, true) }) { viewModel.setSubscriptionActive(sub, true) }
                                    else viewModel.setSubscriptionActive(sub, false)
                                }
                                else -> state.reminders.find { it.id == row.id }?.let { reminder ->
                                    if (on) {
                                        gate(onDenied = { if (!viewModel.setReminderActive(reminder, true)) editingReminder = reminder }) {
                                            // A one-time reminder that already passed can't just be switched on: ask for a new date.
                                            if (!viewModel.setReminderActive(reminder, true)) editingReminder = reminder
                                        }
                                    } else viewModel.setReminderActive(reminder, false)
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    if (showAddSub) {
        SubscriptionDialog(
            currencySymbol = state.currencySymbol,
            onSave = { name, icon, amount, cycle, due, remind ->
                showAddSub = false
                gate(onDenied = { viewModel.saveSubscription(null, name, icon, amount, cycle, due, remind) }) {
                    viewModel.saveSubscription(null, name, icon, amount, cycle, due, remind)
                }
            },
            onDismiss = { showAddSub = false },
        )
    }
    editingSub?.let { sub ->
        SubscriptionEditSheet(
            initial = sub,
            currencySymbol = state.currencySymbol,
            onSave = { name, icon, amount, cycle, due, remind ->
                gate(onDenied = { viewModel.saveSubscription(sub, name, icon, amount, cycle, due, remind) }) {
                    viewModel.saveSubscription(sub, name, icon, amount, cycle, due, remind)
                }
            },
            onDelete = { viewModel.deleteSubscription(sub) },
            onDismiss = { editingSub = null },
        )
    }
    if (showAddReminder) {
        ReminderDialog(
            onSave = { label, icon, schedule, date, minute ->
                showAddReminder = false
                gate(onDenied = { viewModel.saveReminder(null, label, icon, schedule, date, minute) }) {
                    viewModel.saveReminder(null, label, icon, schedule, date, minute)
                }
            },
            onDismiss = { showAddReminder = false },
        )
    }
    editingReminder?.let { reminder ->
        ReminderEditSheet(
            initial = reminder,
            onSave = { label, icon, schedule, date, minute ->
                gate(onDenied = { viewModel.saveReminder(reminder, label, icon, schedule, date, minute) }) {
                    viewModel.saveReminder(reminder, label, icon, schedule, date, minute)
                }
            },
            onDelete = { viewModel.deleteReminder(reminder) },
            onDismiss = { editingReminder = null },
        )
    }
}

/** Icon · name/meta · on/off switch. Tap the row to edit; the switch mutes or resumes its notification. */
@Composable
private fun ListRow(row: ListRowUi, highlighted: Boolean, onClick: () -> Unit, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val colors = SyncSpendTheme.colors
    val dim by animateFloatAsState(if (row.active) 1f else 0.5f, tween(200), label = "row-dim")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.card, RoundedCornerShape(16.dp))
            .then(if (highlighted) Modifier.background(colors.acc.copy(alpha = 0.09f), RoundedCornerShape(16.dp)) else Modifier)
            .border(if (highlighted) 1.5.dp else 1.dp, if (highlighted) colors.acc else colors.line, RoundedCornerShape(16.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            modifier = Modifier.size(34.dp).graphicsLayer { alpha = dim }.background(colors.tile, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(SyncSpendIcons.iconFor(row.iconKey), null, tint = colors.ink, modifier = Modifier.size(16.dp)) }
        Column(modifier = Modifier.weight(1f).graphicsLayer { alpha = dim }) {
            Text(row.name, style = MaterialTheme.typography.bodyMedium, color = colors.ink, maxLines = 1)
            Text(row.meta, fontSize = 10.5.sp, color = colors.sub, modifier = Modifier.padding(top = 2.dp), maxLines = 2)
        }
        DesignSwitch(checked = row.active, onCheckedChange = onToggle)
    }
}
