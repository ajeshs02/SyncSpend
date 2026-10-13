package com.ajesh.syncspend.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ajesh.syncspend.ui.components.DateSeparator
import com.ajesh.syncspend.ui.components.SyncSpendChrome
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendCorners
import com.ajesh.syncspend.ui.theme.SyncSpendTheme

@Composable
fun EntriesTab(
    groups: List<DayGroupUi>,
    onRowClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    if (groups.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
            Text(
                "No transactions in this range.",
                style = MaterialTheme.typography.bodySmall,
                color = SyncSpendTheme.colors.sub,
            )
        }
        return
    }
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(bottom = SyncSpendChrome.screenBottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        groups.forEach { group ->
            item(key = "header-${group.label}", contentType = "day-header") {
                DateSeparator(label = group.label, trailing = group.totalFormatted)
            }
            items(group.items, key = { it.id }, contentType = { "row" }) { row ->
                TxRowCard(row = row, onClick = { onRowClick(row.id) }, modifier = Modifier.animateItem())
            }
        }
    }
}

/** Like [EntriesTab] but without day-header grouping — used for [EntriesSortMode.BY_ENTRY], where rows are ordered by creation time rather than date. */
@Composable
fun FlatTxList(
    rows: List<TxRow>,
    onRowClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    if (rows.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
            Text(
                "No transactions in this range.",
                style = MaterialTheme.typography.bodySmall,
                color = SyncSpendTheme.colors.sub,
            )
        }
        return
    }
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(bottom = SyncSpendChrome.screenBottomContentPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(rows, key = { it.id }, contentType = { "row" }) { row ->
            TxRowCard(row = row, onClick = { onRowClick(row.id) }, modifier = Modifier.animateItem())
        }
    }
}

/**
 * Dispatches an [EntriesUi] to the right renderer for its [EntriesUi.sortMode] — shared by
 * [TransactionsScreen]'s Entries tab and [CategoryEntriesSheet], so both always render a given
 * [EntriesUi] identically.
 */
@Composable
fun EntriesList(
    ui: EntriesUi,
    onRowClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    when (ui.sortMode) {
        EntriesSortMode.BY_DATE -> EntriesTab(ui.groups, onRowClick, modifier, listState)
        EntriesSortMode.BY_ENTRY -> FlatTxList(ui.flatRows, onRowClick, modifier, listState)
    }
}

@Composable
internal fun TxRowCard(row: TxRow, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SyncSpendTheme.colors.card, SyncSpendCorners.chip)
            .border(1.dp, SyncSpendTheme.colors.line, SyncSpendCorners.chip)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(SyncSpendTheme.colors.tile, SyncSpendCorners.tile),
            contentAlignment = Alignment.Center,
        ) {
            Icon(SyncSpendIcons.iconFor(row.iconKey), null, tint = SyncSpendTheme.colors.ink, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                row.categoryLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = SyncSpendTheme.colors.ink,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            // The optional note wraps (the input length cap keeps it to about two lines); never cut off.
            if (row.note.isNotEmpty()) {
                Text(row.note, style = MaterialTheme.typography.bodySmall, color = SyncSpendTheme.colors.sub)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                row.amountFormatted,
                style = MaterialTheme.typography.bodyMedium,
                color = if (row.isPositive) SyncSpendTheme.colors.pos else SyncSpendTheme.colors.neg,
            )
            Text(row.dayLabel, style = MaterialTheme.typography.labelSmall, color = SyncSpendTheme.colors.sub, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
