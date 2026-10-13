package com.ajesh.syncspend.ui.transactions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ajesh.syncspend.ui.components.DesignSheet
import com.ajesh.syncspend.ui.components.SheetHeader
import com.ajesh.syncspend.ui.theme.SyncSpendTheme

/**
 * The entries belonging to one category, for the Transactions page's currently selected date range
 * and Income/Expense filter — opened by tapping a card on the Categories tab. [ui] is recomputed from
 * the same reactive data [TransactionsScreen]'s own Entries tab uses (see
 * [TransactionsViewModel.entriesForCategory]), so an edit or delete made via Edit Entry refreshes this
 * sheet automatically; it is rendered through the same [EntriesList] dispatcher as the main Entries
 * tab, so it always matches the currently selected sort mode (grouped-by-date with per-day totals, or
 * flat by creation time) exactly.
 */
@Composable
fun CategoryEntriesSheet(categoryName: String, ui: EntriesUi?, onRowClick: (Long) -> Unit, onDismiss: () -> Unit) {
    DesignSheet(onDismiss = onDismiss) { close ->
        SheetHeader(title = categoryName, onClose = close)

        val isEmpty = ui == null || (ui.groups.isEmpty() && ui.flatRows.isEmpty())
        if (isEmpty) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 12.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No entries in this category for this period.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SyncSpendTheme.colors.sub,
                )
            }
        } else {
            EntriesList(
                ui = requireNotNull(ui),
                onRowClick = onRowClick,
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).padding(top = 12.dp),
            )
        }
    }
}
