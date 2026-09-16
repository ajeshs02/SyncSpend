package com.ajesh.syncspend.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ajesh.syncspend.ui.components.SyncSpendChrome
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme

@Composable
fun CategoriesTab(rollups: List<CategoryRollupUi>, modifier: Modifier = Modifier) {
    if (rollups.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
            Text("No activity in this range.", style = MaterialTheme.typography.bodySmall, color = SyncSpendTheme.colors.sub)
        }
        return
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = SyncSpendChrome.screenBottomContentPadding),
    ) {
        items(rollups, key = { it.categoryId }) { rollup ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 9.dp)
                    .background(SyncSpendTheme.colors.cardGradient, RoundedCornerShape(18.dp))
                    .border(1.dp, SyncSpendTheme.colors.line, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 13.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(SyncSpendTheme.colors.tile, RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(SyncSpendIcons.iconFor(rollup.iconKey), null, tint = SyncSpendTheme.colors.ink, modifier = Modifier.size(16.dp))
                    }
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(start = 5.dp))
                    Column(modifier = Modifier.weight(1f).padding(start = 11.dp)) {
                        Text(rollup.name, style = MaterialTheme.typography.bodyMedium, color = SyncSpendTheme.colors.ink)
                        Text(
                            "${rollup.count} entries · ${rollup.sharePercent}% of spend",
                            style = MaterialTheme.typography.labelSmall,
                            color = SyncSpendTheme.colors.sub,
                        )
                    }
                    Text(rollup.totalFormatted, style = MaterialTheme.typography.bodyMedium, color = SyncSpendTheme.colors.ink)
                }
                androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .background(SyncSpendTheme.colors.dim, RoundedCornerShape(3.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((rollup.sharePercent / 100f).coerceIn(0f, 1f))
                            .height(5.dp)
                            .background(
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(SyncSpendTheme.colors.acc2, SyncSpendTheme.colors.acc),
                                ),
                                RoundedCornerShape(3.dp),
                            ),
                    )
                }
            }
        }
    }
}
