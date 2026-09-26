package com.ajesh.syncspend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme

/**
 * The "Category / Choose a category" card that opens the category picker sheet.
 * Shared by Add Entry and the widget's quick-add panel so both pick a category
 * the same way.
 */
@Composable
fun CategoryField(category: CategoryEntity?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = SyncSpendTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.card, RoundedCornerShape(18.dp))
            .border(1.dp, colors.line, RoundedCornerShape(18.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            modifier = Modifier.size(32.dp).background(colors.tile, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                SyncSpendIcons.iconFor(category?.iconKey ?: "layers"),
                null,
                tint = if (category != null) colors.ink else colors.sub,
                modifier = Modifier.size(16.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("Category", style = MaterialTheme.typography.labelSmall, color = colors.sub)
            Text(
                category?.name ?: "Choose a category",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.ink,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(SyncSpendIcons.Down, null, tint = colors.sub, modifier = Modifier.size(14.dp))
    }
}
