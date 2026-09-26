package com.ajesh.syncspend.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ajesh.syncspend.di.LocalAppContainer
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.ui.components.AnimatedAmountText
import com.ajesh.syncspend.ui.components.DateSeparator
import com.ajesh.syncspend.ui.components.FlowToggle
import com.ajesh.syncspend.ui.components.PeriodPickerSheet
import com.ajesh.syncspend.ui.components.SyncSpendChrome
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendCorners
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import com.ajesh.syncspend.ui.transactions.TxRow

@Composable
fun HomeScreen(
    onViewAllTransactions: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    onOpenReminders: () -> Unit,
) {
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
                    container.categoryRepository,
                    container.selectionState,
                )
            }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = SyncSpendTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = SyncSpendChrome.screenTopInset, bottom = SyncSpendChrome.screenBottomContentPadding),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Box(
                modifier = Modifier.size(26.dp).background(colors.darkGradient, RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(SyncSpendIcons.Spark, null, tint = Color(0xFF7FD39A), modifier = Modifier.size(15.dp)) }
            Text("SyncSpend", style = MaterialTheme.typography.headlineSmall.copy(letterSpacing = (-0.18).sp), color = colors.ink)
        }

        FlowToggle(
            type = state.flow,
            onSelect = viewModel::setFlow,
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 14.dp),
        )

        Row(
            modifier = Modifier
                .padding(start = 22.dp, end = 22.dp, top = 18.dp)
                .fillMaxWidth()
                .background(colors.pill, RoundedCornerShape(18.dp))
                .border(1.dp, colors.line, RoundedCornerShape(18.dp))
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NavArrow(SyncSpendIcons.Prev, "Previous period", state.canGoPrev, viewModel::prevPeriod)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { periodPickerOpen = true },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AnimatedContent(
                    targetState = state.scopeLabel,
                    transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
                    label = "scope-label",
                ) { label ->
                    Text(label, style = MaterialTheme.typography.titleMedium.copy(letterSpacing = (-0.15).sp), color = colors.ink)
                }
                Text(state.scopeSubLabel, fontSize = 10.sp, color = colors.sub, modifier = Modifier.padding(top = 1.dp))
            }
            NavArrow(SyncSpendIcons.Next, "Next period", state.canGoNext, viewModel::nextPeriod)
        }

        HeroCard(state, modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 14.dp))

        Row(
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ShortcutCard(Modifier.weight(1f), SyncSpendIcons.Repeat, "Subscription", "${state.subsCount} active", onOpenSubscriptions)
            ShortcutCard(Modifier.weight(1f), SyncSpendIcons.Bell, "Reminders", "${state.remindersCount} set", onOpenReminders)
        }

        Box(Modifier.padding(top = 16.dp).fillMaxWidth().height(1.dp).background(colors.line))
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Recent Transactions", style = MaterialTheme.typography.titleSmall, color = colors.ink)
            Text(
                "View All",
                style = MaterialTheme.typography.labelMedium,
                color = colors.acc,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onViewAllTransactions,
                ),
            )
        }

        if (state.recentGroups.isEmpty()) {
            Text(
                "No transactions yet — tap the + button to add your first one.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.sub,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
            )
        } else {
            Column(modifier = Modifier.padding(horizontal = 22.dp).padding(top = 2.dp)) {
                state.recentGroups.forEach { group ->
                    DateSeparator(label = group.label)
                    group.items.forEach { row ->
                        RecentRow(row) { container.selectionState.editingTransactionId.value = row.id }
                    }
                }
            }
        }
    }

    if (periodPickerOpen) {
        PeriodPickerSheet(
            currentScope = state.currentScope,
            earliestTransactionDate = state.earliestTransactionDate,
            showAllTime = false,
            onApply = viewModel::applyScope,
            onDismiss = { periodPickerOpen = false },
        )
    }
}

@Composable
private fun HeroCard(state: HomeUiState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(SyncSpendCorners.hero)
            .background(SyncSpendTheme.colors.darkGradient)
            .drawWithCache {
                // The design's soft green glow tucked into the top-right corner (brush built once per size, not per draw).
                val r = 100.dp.toPx()
                val center = Offset(size.width - 50.dp.toPx(), 30.dp.toPx())
                val glow = Brush.radialGradient(
                    colorStops = arrayOf(0f to Color(0x615FBF7D), 0.7f to Color.Transparent),
                    center = center,
                    radius = r,
                )
                onDrawBehind { drawCircle(brush = glow, radius = r, center = center) }
            }
            .padding(20.dp),
    ) {
        Column {
            Text(
                if (state.flow == FlowType.INCOME) "Total Income" else "Total Spending",
                fontSize = 12.5.sp,
                color = Color.White.copy(alpha = 0.66f),
            )
            Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(state.currencySymbol, fontSize = 23.sp, fontWeight = FontWeight.Normal, color = Color.White)
                AnimatedAmountText(
                    target = state.total,
                    ready = state.loaded,
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.68).sp,
                )
            }
            Row(modifier = Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                if (state.hasTrend) {
                    val good = if (state.flow == FlowType.INCOME) state.trendIsUp else !state.trendIsUp
                    Row(
                        modifier = Modifier
                            .background(if (good) Color(0x335FBF7D) else Color(0x38F08579), RoundedCornerShape(13.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            if (state.trendIsUp) SyncSpendIcons.Up else SyncSpendIcons.Down,
                            null,
                            tint = if (good) Color(0xFF9FE0B4) else Color(0xFFFFB3A8),
                            modifier = Modifier.size(11.dp),
                        )
                        Text("${state.trendPercent}%", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (good) Color(0xFF9FE0B4) else Color(0xFFFFB3A8))
                    }
                    Text(
                        "vs ${state.prevScopeLabel} (${state.currencySymbol}${state.prevTotalFormatted})",
                        fontSize = 11.5.sp,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                } else {
                    Text(
                        state.entryCountLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.82f),
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(13.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                    Text(state.avgNote, fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
private fun NavArrow(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(SyncSpendTheme.colors.card, RoundedCornerShape(11.dp))
            .border(1.dp, SyncSpendTheme.colors.line, RoundedCornerShape(11.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = SyncSpendTheme.colors.ink.copy(alpha = if (enabled) 1f else 0.3f), modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun ShortcutCard(modifier: Modifier, icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .background(SyncSpendTheme.colors.card, RoundedCornerShape(18.dp))
            .border(1.dp, SyncSpendTheme.colors.line, RoundedCornerShape(18.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier.size(30.dp).background(SyncSpendTheme.colors.tile, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = SyncSpendTheme.colors.ink, modifier = Modifier.size(16.dp)) }
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, color = SyncSpendTheme.colors.ink)
            Text(subtitle, fontSize = 10.sp, color = SyncSpendTheme.colors.sub, modifier = Modifier.padding(top = 1.dp))
        }
    }
}

@Composable
private fun RecentRow(row: TxRow, onClick: () -> Unit) {
    val colors = SyncSpendTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            modifier = Modifier.size(34.dp).background(colors.tile, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(SyncSpendIcons.iconFor(row.iconKey), null, tint = colors.ink, modifier = Modifier.size(16.dp)) }
        Column(modifier = Modifier.weight(1f)) {
            Text(row.name, style = MaterialTheme.typography.bodyMedium, color = colors.ink, maxLines = 1)
            Text(row.categoryLabel, fontSize = 11.sp, color = colors.sub, modifier = Modifier.padding(top = 2.dp), maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(row.amountFormatted, style = MaterialTheme.typography.bodyMedium, color = if (row.isPositive) colors.pos else colors.neg)
            Text(row.dayLabel, fontSize = 10.5.sp, color = colors.sub, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
