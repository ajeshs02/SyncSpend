package com.ajesh.syncspend.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ajesh.syncspend.di.LocalAppContainer
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.ui.components.AppLogo
import com.ajesh.syncspend.ui.components.DateSeparator
import com.ajesh.syncspend.ui.components.FlowToggle
import com.ajesh.syncspend.ui.components.PeriodPickerSheet
import com.ajesh.syncspend.ui.components.SyncSpendChrome
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendCorners
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import com.ajesh.syncspend.ui.transactions.TxRow
import kotlin.random.Random
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onViewAllTransactions: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenStats: () -> Unit,
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
    // The toggle reads the selection itself (main-thread, instant) rather than waiting for the
    // computed state to come back from the background, so its pill moves on the tap.
    val flow by container.selectionState.flow.collectAsStateWithLifecycle()
    val colors = SyncSpendTheme.colors

    // The title, flow toggle and period selector stay put; only what is below them scrolls, so
    // content can never slide under the status bar.
    Column(modifier = Modifier.fillMaxSize().padding(top = SyncSpendChrome.screenTopInset)) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            AppLogo(size = 26.dp, radius = 9.dp)
            Text("SyncSpend", style = MaterialTheme.typography.headlineSmall.copy(letterSpacing = (-0.18).sp), color = colors.ink)
        }

        FlowToggle(
            type = flow,
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

        // Nothing below the header until the first real state exists, so the first frame never flashes
        // a 0 total or the "No transactions yet" text before the rows arrive.
        if (state.loaded) Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = SyncSpendChrome.screenBottomContentPadding),
        ) {
        HeroCard(
            state,
            onClick = onOpenStats,
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 14.dp),
        )

        Row(
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ShortcutCard(Modifier.weight(1f), SyncSpendIcons.Repeat, "Subscriptions & EMIs", "${state.subsCount} active", onOpenSubscriptions)
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
                "No transactions yet. Tap the + button to add your first one.",
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

/**
 * Styled like an actual debit-card face — flat colour, a contactless-style glyph and brand mark up
 * top, the balance where a card's number would sit, and a period/brand row at the bottom in place
 * of a cardholder name and expiry. It's just another [SyncSpendTheme.colors.cardGradient] consumer
 * like the rest of the app's highlighted cards (the same pale mint in light / dark tinted panel in
 * dark), so its text uses the ordinary theme-aware ink/sub/accent tokens rather than a fixed color.
 */
@Composable
private fun HeroCard(state: HomeUiState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = SyncSpendTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(SyncSpendCorners.hero)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .background(colors.cardGradient)
            .padding(24.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    SyncSpendIcons.Wifi,
                    contentDescription = null,
                    tint = colors.pos,
                    modifier = Modifier.size(20.dp).rotate(90f),
                )
                Text(
                    "SyncSpend",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp,
                    color = colors.ink.copy(alpha = 0.75f),
                )
            }
            Text(
                if (state.flow == FlowType.INCOME) "Total Income" else "Total Spending",
                fontSize = 12.5.sp,
                color = colors.sub,
                modifier = Modifier.padding(top = 18.dp),
            )
            Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(state.currencySymbol, fontSize = 23.sp, fontWeight = FontWeight.Normal, color = colors.ink)
                Text(
                    state.totalFormatted,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.68).sp,
                    color = colors.ink,
                )
            }
            Row(modifier = Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                if (state.hasTrend) {
                    val good = if (state.flow == FlowType.INCOME) state.trendIsUp else !state.trendIsUp
                    Row(
                        modifier = Modifier
                            .background(colors.dim, RoundedCornerShape(13.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            if (state.trendIsUp) SyncSpendIcons.Up else SyncSpendIcons.Down,
                            null,
                            tint = if (good) colors.pos else colors.neg,
                            modifier = Modifier.size(11.dp),
                        )
                        Text("${state.trendPercent}%", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (good) colors.pos else colors.neg)
                    }
                    Text(
                        "vs ${state.prevScopeLabel} (${state.currencySymbol}${state.prevTotalFormatted})",
                        fontSize = 11.5.sp,
                        color = colors.sub,
                    )
                } else {
                    Text(
                        state.entryCountLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.ink.copy(alpha = 0.82f),
                        modifier = Modifier
                            .background(colors.dim, RoundedCornerShape(13.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                    RotatingInsight(state.insights, modifier = Modifier.weight(1f))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    state.scopeLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp,
                    color = colors.sub,
                )
                AppLogo(size = 18.dp, radius = 6.dp)
            }
        }
    }
}

/**
 * One of the hero's averages, changing to a different random one every few seconds with a
 * cross-fade. The timer only runs while the screen is visible (STARTED), so nothing ticks in the
 * background. Keyed on [lines] itself (not just its size): toggling Income/Expense swaps in a
 * same-size but different-content list, and keying on size alone left the rotation running with a
 * stale index into the new content — this restarts it cleanly instead.
 */
@Composable
internal fun RotatingInsight(lines: List<String>, modifier: Modifier = Modifier) {
    if (lines.isEmpty()) return
    var index by remember { mutableIntStateOf(Random.nextInt(lines.size)) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lines, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (lines.size > 1) {
                delay(HomeInsights.ROTATE_MILLIS)
                index = HomeInsights.nextIndex(index, lines.size)
            }
        }
    }
    Crossfade(targetState = index.coerceIn(lines.indices), animationSpec = tween(1200), modifier = modifier, label = "hero-insight") { i ->
        Text(lines[i.coerceIn(lines.indices)], fontSize = 11.5.sp, color = SyncSpendTheme.colors.sub, maxLines = 1)
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
            Text(title, style = MaterialTheme.typography.labelLarge, color = SyncSpendTheme.colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            modifier = Modifier.size(34.dp).background(colors.tile, SyncSpendCorners.tile),
            contentAlignment = Alignment.Center,
        ) { Icon(SyncSpendIcons.iconFor(row.iconKey), null, tint = colors.ink, modifier = Modifier.size(16.dp)) }
        Column(modifier = Modifier.weight(1f)) {
            Text(row.categoryLabel, style = MaterialTheme.typography.bodyMedium, color = colors.ink, maxLines = 1)
            // The optional note wraps (the input length cap keeps it to about two lines); never cut off.
            if (row.note.isNotEmpty()) {
                Text(row.note, fontSize = 11.sp, color = colors.sub, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(row.amountFormatted, style = MaterialTheme.typography.bodyMedium, color = if (row.isPositive) colors.pos else colors.neg)
            Text(row.dayLabel, fontSize = 10.5.sp, color = colors.sub, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
