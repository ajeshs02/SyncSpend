package com.ajesh.syncspend.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.ajesh.syncspend.domain.model.EntryFilter
import com.ajesh.syncspend.ui.theme.SyncSpendTheme

@Composable
fun FilterChipsRow(
    options: List<EntryFilter>,
    selected: EntryFilter,
    onSelect: (EntryFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .background(
                        if (isSelected) SyncSpendTheme.colors.darkGradient else SolidColor(SyncSpendTheme.colors.pill),
                        RoundedCornerShape(15.dp),
                    )
                    .border(1.dp, SyncSpendTheme.colors.line, RoundedCornerShape(15.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(option) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    option.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Color.White else SyncSpendTheme.colors.sub,
                )
            }
        }
    }
}
