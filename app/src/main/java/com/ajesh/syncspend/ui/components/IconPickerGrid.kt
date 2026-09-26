package com.ajesh.syncspend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme

/**
 * The design's 6-column preset icon grid. Shared by every "+ Add" flow that
 * lets you pick an icon (Categories, Subscriptions, Reminders) and by the
 * category icon editor.
 */
@Composable
fun IconPickerGrid(
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    keys: List<String> = SyncSpendIcons.pickerKeys,
    columns: Int = 6,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        keys.chunked(columns).forEach { rowKeys ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                rowKeys.forEach { key ->
                    val selected = key == selectedKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(
                                if (selected) SyncSpendTheme.colors.acc.copy(alpha = 0.1f) else SyncSpendTheme.colors.card,
                                RoundedCornerShape(12.dp),
                            )
                            .border(1.dp, if (selected) SyncSpendTheme.colors.acc else SyncSpendTheme.colors.line, RoundedCornerShape(12.dp))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            SyncSpendIcons.iconFor(key),
                            null,
                            tint = if (selected) SyncSpendTheme.colors.acc else SyncSpendTheme.colors.ink,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
                repeat(columns - rowKeys.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}
