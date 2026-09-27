package com.ajesh.syncspend.ui.transactions

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ajesh.syncspend.di.LocalAppContainer
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.StatsRange
import com.ajesh.syncspend.ui.components.ChipsRow
import com.ajesh.syncspend.ui.components.FlowMenuToggle
import com.ajesh.syncspend.ui.components.SyncSpendChrome
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import com.ajesh.syncspend.util.DateUtils
import java.time.LocalDate

/**
 * The standalone Stats page (bottom nav), reached instead of a tab inside Transactions. Reuses
 * [TransactionsViewModel] — its `stats`/`statsRange` never depended on the Entries/Categories
 * tab or filters, so a second instance here is a clean, minimal reuse rather than a new ViewModel.
 */
@Composable
fun StatsScreen() {
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
    val flow by viewModel.flow.collectAsStateWithLifecycle()
    val statsRange by viewModel.statsRange.collectAsStateWithLifecycle()
    val colors = SyncSpendTheme.colors
    val flowColor by animateColorAsState(if (flow == FlowType.INCOME) colors.pos else colors.neg, tween(200), label = "flow-word")
    val dateClause = remember(statsRange) { statsDateClause(statsRange, LocalDate.now()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = SyncSpendChrome.screenTopInset)
            .padding(horizontal = 22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Stats",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 21.sp),
                color = colors.ink,
                modifier = Modifier.padding(vertical = 3.dp),
            )
            Box(modifier = Modifier.weight(1f))
            FlowMenuToggle(flow = flow, onPick = viewModel::setFlow)
        }

        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 10.dp))
        Text(
            buildAnnotatedString {
                append("Showing stats for ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = flowColor)) {
                    append(if (flow == FlowType.INCOME) "income" else "expenses")
                }
                append(dateClause?.let { " $it" } ?: " of all time")
            },
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = colors.sub,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 20.dp))
        ChipsRow(
            labels = StatsRange.entries.map { it.label },
            selectedIndex = statsRange.ordinal,
            onSelect = { viewModel.selectStatsRange(StatsRange.entries[it]) },
        )

        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 12.dp))
        val stats by viewModel.stats.collectAsStateWithLifecycle()
        stats?.let { StatsTab(it, modifier = Modifier.fillMaxWidth()) }
    }
}

/** "on 21 Sep", "from 21 Sep to 27 Sep", or null for [StatsRange.ALL_TIME]. */
internal fun statsDateClause(range: StatsRange, today: LocalDate): String? {
    if (range == StatsRange.ALL_TIME) return null
    val resolved = range.resolve(today, null)
    val end = minOf(resolved.end, today)
    fun label(d: LocalDate) = DateUtils.smartDate(d, today)
    return if (resolved.start == end) "on ${label(resolved.start)}" else "from ${label(resolved.start)} to ${label(end)}"
}
