package com.ajesh.syncspend.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.ajesh.syncspend.ui.components.PeriodSelectorPill
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
    onOpenForecast: () -> Unit,
    onOpenTransfer: () -> Unit,
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

        PeriodSelectorPill(
            label = state.scopeLabel,
            subLabel = state.scopeSubLabel,
            canGoPrev = state.canGoPrev,
            canGoNext = state.canGoNext,
            onPrev = viewModel::prevPeriod,
            onNext = viewModel::nextPeriod,
            onTap = { periodPickerOpen = true },
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 18.dp),
        )

        // Nothing below the header until the first real state exists, so the first frame never flashes
        // a 0 total or the "No transactions yet" text before the rows arrive.
        if (state.loaded) {
            // A dedicated, explicit gap below the fixed period-selector — its own element (not folded
            // into the scroll container's padding) so it's never ambiguous whether it's actually there.
            Spacer(Modifier.height(18.dp))

            // Only the title/toggle/period-selector above stay fixed — hero, tiles and Recent
            // Transactions all scroll together now, so the extra tile row never crowds Recent
            // Transactions out of view on a short screen.
            Column(
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

                // 2x2 grid: each row shares one height (IntrinsicSize.Max) so "Subscriptions & EMIs"
                // wrapping to a second line doesn't make its card taller than its row-mates.
                Column(modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShortcutCard(Modifier.weight(1f).fillMaxHeight(), SyncSpendIcons.Repeat, "Subscriptions & EMIs", "${state.subsCount} active", onOpenSubscriptions)
                        ShortcutCard(Modifier.weight(1f).fillMaxHeight(), SyncSpendIcons.Bell, "Reminders", "${state.remindersCount} set", onOpenReminders)
                    }
                    Row(modifier = Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShortcutCard(Modifier.weight(1f).fillMaxHeight(), SyncSpendIcons.Cal, "Forecast", "Plan ahead", onOpenForecast)
                        ShortcutCard(Modifier.weight(1f).fillMaxHeight(), SyncSpendIcons.Coin, "Savings", "Move to savings", onOpenTransfer)
                    }
                }

                // Breathing room before the scrollable section starts, per the ask for a visible gap here.
                Box(Modifier.padding(top = 18.dp).fillMaxWidth().height(1.dp).background(colors.line))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Recent Transactions", style = MaterialTheme.typography.titleSmall, color = colors.ink)
                    ViewAllButton(onClick = onViewAllTransactions)
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
    }

    if (periodPickerOpen) {
        PeriodPickerSheet(
            currentScope = state.currentScope,
            earliestTransactionDate = state.earliestTransactionDate,
            // Was false — but the shared scope (round 10) means Home can already land on All Time via
            // Stats/Transactions, just couldn't select it directly. Now it can, consistently.
            showAllTime = true,
            onApply = viewModel::applyScope,
            onDismiss = { periodPickerOpen = false },
        )
    }
}

/**
 * Styled like an actual debit-card face — flat colour, a contactless-style glyph and brand mark up
 * top, the balance where a card's number would sit, and a period/brand row at the bottom in place
 * of a cardholder name and expiry. It's just another [SyncSpendTheme.colors.cardGradient] consumer
 * like the rest of the app's highlighted cards (the flat accent, same in both themes), so its text
 * uses [SyncSpendTheme.colors.mink]/`msub` — the "text on the accent fill" tokens — rather than the
 * plain-surface `ink`/`sub`.
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
            .padding(18.dp),
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
                    tint = colors.mink,
                    modifier = Modifier.size(20.dp).rotate(90f),
                )
                Text(
                    "SyncSpend",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp,
                    color = colors.mink.copy(alpha = 0.75f),
                )
            }
            Text(
                when (state.flow) {
                    FlowType.INCOME -> "Total Income"
                    FlowType.EXPENSE -> "Total Spending"
                },
                fontSize = 12.5.sp,
                color = colors.msub,
                modifier = Modifier.padding(top = 12.dp),
            )
            Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(state.currencySymbol, fontSize = 23.sp, fontWeight = FontWeight.Normal, color = colors.mink)
                Text(
                    state.totalFormatted,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.68).sp,
                    color = colors.mink,
                )
            }
            Row(modifier = Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                if (state.hasTrend) {
                    // More income or more saved is good; more spent is not.
                    val good = if (state.flow == FlowType.EXPENSE) !state.trendIsUp else state.trendIsUp
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
                            tint = if (good) colors.mink else colors.neg,
                            modifier = Modifier.size(11.dp),
                        )
                        Text("${state.trendPercent}%", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = if (good) colors.mink else colors.neg)
                    }
                    Text(
                        "vs ${state.prevScopeLabel} (${state.currencySymbol}${state.prevTotalFormatted})",
                        fontSize = 12.5.sp,
                        color = colors.msub,
                    )
                } else {
                    Text(
                        state.entryCountLabel,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.mink.copy(alpha = 0.82f),
                        modifier = Modifier
                            .background(colors.dim, RoundedCornerShape(13.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                    RotatingInsight(state.insights, modifier = Modifier.weight(1f))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    state.scopeLabel,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp,
                    color = colors.msub,
                )
                // Bumped up from 18dp: the user asked for this corner badge to read as clearly larger.
                AppLogo(size = 25.dp, radius = 8.dp)
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
        Text(lines[i.coerceIn(lines.indices)], fontSize = 12.5.sp, color = SyncSpendTheme.colors.msub, maxLines = 1)
    }
}


/**
 * Four of these fill a 2x2 grid now — a 2-line title so a longer label like "Subscriptions & EMIs"
 * still fits at half the row's width. The caller passes `fillMaxHeight()` inside a row sized with
 * `IntrinsicSize.Max`, so both cards in a row always share one height regardless of which one wraps.
 */
@Composable
private fun ShortcutCard(modifier: Modifier, icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .background(SyncSpendTheme.colors.card, RoundedCornerShape(16.dp))
            .border(1.dp, SyncSpendTheme.colors.line, RoundedCornerShape(16.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier.size(26.dp).background(SyncSpendTheme.colors.tile, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = SyncSpendTheme.colors.ink, modifier = Modifier.size(14.dp)) }
        Text(
            title,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, lineHeight = 14.sp),
            color = SyncSpendTheme.colors.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(subtitle, fontSize = 10.sp, color = SyncSpendTheme.colors.sub, modifier = Modifier.padding(top = 2.dp))
    }
}

/**
 * In Light theme, `colors.acc` is a pale mint meant for fills, not for small text directly on the
 * background — as plain text it read as low-contrast, so it becomes a compact green pill with a
 * dark foreground instead (the same acc-fill/onAcc-text pairing selected chips already use). Dark
 * theme keeps the original plain-text treatment, which already reads fine there.
 */
@Composable
private fun ViewAllButton(onClick: () -> Unit) {
    val colors = SyncSpendTheme.colors
    if (colors.isDark) {
        Text(
            "View All",
            style = MaterialTheme.typography.labelMedium,
            color = colors.acc,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        )
    } else {
        Text(
            "View All",
            style = MaterialTheme.typography.labelMedium,
            color = colors.onAcc,
            modifier = Modifier
                .background(colors.acc, RoundedCornerShape(12.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )
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
