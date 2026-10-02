package com.ajesh.syncspend.ui.transactions

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ajesh.syncspend.di.LocalAppContainer
import com.ajesh.syncspend.domain.analytics.AnalyticsEngine
import com.ajesh.syncspend.domain.model.DateRange
import com.ajesh.syncspend.domain.model.EntryFilter
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.TransactionsTab
import com.ajesh.syncspend.ui.components.AnimatedSegmentedControl
import com.ajesh.syncspend.ui.components.CustomRangeSheet
import com.ajesh.syncspend.ui.components.FlowMenuToggle
import com.ajesh.syncspend.ui.components.SegmentIcon
import com.ajesh.syncspend.ui.components.SyncSpendChrome
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import com.ajesh.syncspend.util.DateUtils
import java.time.LocalDate

@Composable
fun TransactionsScreen() {
    val container = LocalAppContainer.current
    val viewModel: TransactionsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                TransactionsViewModel(
                    container.transactionRepository,
                    container.categoryRepository,
                    container.preferencesRepository,
                    container.subscriptionRepository,
                    container.selectionState,
                )
            }
        },
    )
    // Selections are cheap main-thread state, so a tap moves its pill at once; each tab's rows
    // come from their own flow, collected only while that tab is showing.
    val flow by viewModel.flow.collectAsStateWithLifecycle()
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val storedFilter by viewModel.entryFilter.collectAsStateWithLifecycle()
    val customRange by viewModel.customRange.collectAsStateWithLifecycle()
    val filter = storedFilter
    val filterOptions = remember(flow, tab) { entryFilterOptionsFor(flow, tab) }
    // The dates the current chip covers, folded into the "Showing ..." sentence.
    val dateClause = remember(filter, customRange) { headerDateClause(filter, customRange, LocalDate.now()) }
    var rangePickerOpen by remember { mutableStateOf(false) }
    val colors = SyncSpendTheme.colors
    val flowColor by animateColorAsState(com.ajesh.syncspend.ui.components.flowColor(flow, colors), tween(200), label = "flow-word")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = SyncSpendChrome.screenTopInset)
            .padding(horizontal = 22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Transactions",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 23.sp),
                color = SyncSpendTheme.colors.ink,
                modifier = Modifier.padding(vertical = 3.dp),
            )
            Box(modifier = Modifier.weight(1f))
            FlowMenuToggle(flow = flow, onPick = viewModel::setFlow)
        }

        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 10.dp))
        Text(
            buildAnnotatedString {
                append("Showing ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = flowColor)) {
                    append(com.ajesh.syncspend.ui.components.labelFor(flow).lowercase() + if (flow == FlowType.EXPENSE) "s" else "")
                }
                dateClause?.let { append(" $it") }
            },
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = colors.sub,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 20.dp))
        val tabIcons = remember(colors) {
            listOf(SyncSpendIcons.Receipt, SyncSpendIcons.Layers)
                .map { SegmentIcon(it, colors.onSelected, colors.sub) }
        }
        AnimatedSegmentedControl(
            options = listOf("Entries", "Categories"),
            selectedIndex = tab.ordinal,
            onSelect = { viewModel.selectTab(TransactionsTab.entries[it]) },
            icons = tabIcons,
            iconSize = 14.dp,
        )

        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 14.dp))
        FilterChipsRow(
            options = filterOptions,
            selected = filter,
            customLabel = customRange?.let { DateUtils.rangeLabel(it.start, it.end) },
            onSelect = { f ->
                if (f == EntryFilter.CUSTOM) rangePickerOpen = true else viewModel.pickEntryFilter(f)
            },
        )

        // The gap under the chips sits outside the scrolling list, so scrolled rows/cards clip a
        // clear band below the chips instead of running flush against them.
        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 12.dp))
        // Only the visible tab is collected, so only its rows are ever built. Until its first
        // state arrives nothing is drawn (never a flash of "No transactions…").
        when (tab) {
            TransactionsTab.ENTRIES -> {
                val entries by viewModel.entries.collectAsStateWithLifecycle()
                entries?.let {
                    EntriesTab(
                        groups = it.groups,
                        onRowClick = { id -> container.selectionState.editingTransactionId.value = id },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            TransactionsTab.CATEGORIES -> {
                val rollups by viewModel.rollups.collectAsStateWithLifecycle()
                rollups?.let { CategoriesTab(rollups = it.rollups, modifier = Modifier.fillMaxWidth()) }
            }
        }
    }

    if (rangePickerOpen) {
        val earliest by viewModel.earliestDate.collectAsStateWithLifecycle()
        CustomRangeSheet(
            initial = customRange,
            earliestTransactionDate = earliest,
            onApply = viewModel::applyCustomRange,
            onDismiss = { rangePickerOpen = false },
        )
    }
}

/** "on 21 Sep", "from 21 Sep to 27 Sep", or null before a custom range has been picked. */
internal fun headerDateClause(filter: EntryFilter, customRange: DateRange?, today: LocalDate): String? {
    fun clause(range: DateRange): String {
        fun label(d: LocalDate) = DateUtils.smartDate(d, today)
        return if (range.start == range.end) "on ${label(range.start)}" else "from ${label(range.start)} to ${label(range.end)}"
    }
    return AnalyticsEngine.entryFilterRange(filter, customRange, today)?.let(::clause)
}
