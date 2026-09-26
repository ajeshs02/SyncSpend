package com.ajesh.syncspend.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ajesh.syncspend.di.LocalAppContainer
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.ui.components.AnimatedSegmentedControl
import com.ajesh.syncspend.ui.components.SyncSpendChrome
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendCorners
import com.ajesh.syncspend.ui.theme.SyncSpendTheme

@Composable
fun HomeScreen(onViewAllTransactions: () -> Unit) {
    val container = LocalAppContainer.current
    var periodPickerOpen by remember { mutableStateOf(false) }
    val viewModel: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    container.transactionRepository,
                    container.subscriptionRepository,
                    container.reminderRepository,
                    container.preferencesRepository,
                    container.selectionState,
                )
            }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = SyncSpendChrome.screenTopInset, bottom = SyncSpendChrome.screenBottomContentPadding)
            .padding(horizontal = 22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(SyncSpendTheme.colors.darkGradient, RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(SyncSpendIcons.Spark, null, tint = Color(0xFF7FD39A), modifier = Modifier.size(15.dp))
            }
            androidx.compose.foundation.layout.Spacer(Modifier.width(9.dp))
            Text("SyncSpend", style = MaterialTheme.typography.headlineSmall, color = SyncSpendTheme.colors.ink)
        }

        androidx.compose.foundation.layout.Spacer(Modifier.height(14.dp))
        AnimatedSegmentedControl(
            options = listOf("Expense", "Income"),
            selectedIndex = if (state.flow == FlowType.EXPENSE) 0 else 1,
            onSelect = { viewModel.setFlow(if (it == 0) FlowType.EXPENSE else FlowType.INCOME) },
        )

        androidx.compose.foundation.layout.Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SyncSpendTheme.colors.pill, RoundedCornerShape(18.dp))
                .border(1.dp, SyncSpendTheme.colors.line, RoundedCornerShape(18.dp))
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonthArrow(SyncSpendIcons.Prev, onClick = viewModel::prevMonth)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { periodPickerOpen = true },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(state.scopeLabel, style = MaterialTheme.typography.titleMedium, color = SyncSpendTheme.colors.ink)
                Text(state.scopeSubLabel, style = MaterialTheme.typography.labelSmall, color = SyncSpendTheme.colors.sub)
            }
            MonthArrow(SyncSpendIcons.Next, onClick = viewModel::nextMonth)
        }

        androidx.compose.foundation.layout.Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SyncSpendTheme.colors.darkGradient, SyncSpendCorners.hero)
                .padding(20.dp),
        ) {
            Column {
                Text(
                    if (state.flow == FlowType.INCOME) "Total Income" else "Total Spending",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.66f),
                )
                androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(state.currencySymbol, fontSize = 23.sp, color = Color.White)
                    androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp))
                    AnimatedContent(
                        targetState = state.totalFormatted,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "hero-total",
                    ) { amount ->
                        Text(amount, fontSize = 34.sp, fontWeight = FontWeight.Medium, color = Color.White)
                    }
                }
                androidx.compose.foundation.layout.Spacer(Modifier.height(14.dp))
                if (state.hasTrend) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val up = state.trendIsUp
                        val goodDirection = if (state.flow == FlowType.INCOME) up else !up
                        Row(
                            modifier = Modifier
                                .background(
                                    (if (goodDirection) Color(0xFF5FBF7D) else Color(0xFFF08579)).copy(alpha = 0.2f),
                                    RoundedCornerShape(13.dp),
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (up) SyncSpendIcons.Up else SyncSpendIcons.Down,
                                null,
                                tint = if (goodDirection) Color(0xFF9FE0B4) else Color(0xFFFFB3A8),
                                modifier = Modifier.size(11.dp),
                            )
                            Text(
                                "${state.trendPercent}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (goodDirection) Color(0xFF9FE0B4) else Color(0xFFFFB3A8),
                            )
                        }
                        androidx.compose.foundation.layout.Spacer(Modifier.width(9.dp))
                        Text(
                            "vs ${state.prevScopeLabel} (${state.currencySymbol}${state.prevTotalFormatted})",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                    }
                } else if (!state.hasEntriesInScope) {
                    Text(
                        "No entries yet for this period.",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }
        }

        androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ShortcutCard(
                modifier = Modifier.weight(1f),
                icon = SyncSpendIcons.Repeat,
                title = "Subscription",
                subtitle = "${state.subsCount} active",
            )
            ShortcutCard(
                modifier = Modifier.weight(1f),
                icon = SyncSpendIcons.Bell,
                title = "Reminders",
                subtitle = "${state.remindersCount} set",
            )
        }

        androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Recent Transactions", style = MaterialTheme.typography.titleSmall, color = SyncSpendTheme.colors.ink)
            Text(
                "View All",
                style = MaterialTheme.typography.labelMedium,
                color = SyncSpendTheme.colors.acc,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onViewAllTransactions,
                ),
            )
        }
        androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
        if (!state.hasEntriesInScope) {
            Text(
                "No transactions yet — tap the + button to add your first one.",
                style = MaterialTheme.typography.bodySmall,
                color = SyncSpendTheme.colors.sub,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }

    if (periodPickerOpen) {
        com.ajesh.syncspend.ui.components.PeriodPickerSheet(
            currentScope = state.currentScope,
            earliestTransactionDate = state.earliestTransactionDate,
            onApply = {
                viewModel.applyScope(it)
                periodPickerOpen = false
            },
            onDismiss = { periodPickerOpen = false },
        )
    }
}

@Composable
private fun MonthArrow(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(SyncSpendTheme.colors.card, RoundedCornerShape(11.dp))
            .border(1.dp, SyncSpendTheme.colors.line, RoundedCornerShape(11.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = SyncSpendTheme.colors.ink, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun ShortcutCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
) {
    Row(
        modifier = modifier
            .background(SyncSpendTheme.colors.card, RoundedCornerShape(18.dp))
            .border(1.dp, SyncSpendTheme.colors.line, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(SyncSpendTheme.colors.tile, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = SyncSpendTheme.colors.ink, modifier = Modifier.size(16.dp))
        }
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, color = SyncSpendTheme.colors.ink)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = SyncSpendTheme.colors.sub)
        }
    }
}
