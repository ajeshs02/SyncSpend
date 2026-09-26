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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme

/**
 * "Choose category" bottom sheet: 3-column grid of the current flow's
 * categories. Tapping the selected one clears it, tapping another picks it
 * and closes the sheet. The header "+" (and the empty-state button, since
 * the app ships with zero categories) opens the New-category dialog so a
 * category can be created without leaving the entry in progress.
 */
@Composable
fun CategoryPickerSheet(
    flow: FlowType,
    categories: List<CategoryEntity>,
    selectedId: Long?,
    onPick: (CategoryEntity?) -> Unit,
    onCreate: (name: String, iconKey: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var showCreate by remember { mutableStateOf(false) }

    DesignSheet(onDismiss = onDismiss) { close ->
        SheetHeader(
            title = "Choose category",
            onClose = close,
            actions = { SquareIconButton(SyncSpendIcons.Plus, { showCreate = true }, size = 30.dp, iconSize = 13.dp) },
        )

        if (categories.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "No ${if (flow == FlowType.INCOME) "income" else "expense"} categories yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SyncSpendTheme.colors.ink,
                )
                Text(
                    "Create one to file this entry under.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SyncSpendTheme.colors.sub,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                    textAlign = TextAlign.Center,
                )
                Box(
                    modifier = Modifier
                        .background(SyncSpendTheme.colors.button, RoundedCornerShape(15.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showCreate = true }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Text("Add your first category", style = MaterialTheme.typography.labelLarge, color = SyncSpendTheme.colors.onButton)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                categories.chunked(3).forEach { rowCats ->
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        rowCats.forEach { cat ->
                            val selected = cat.id == selectedId
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (selected) SyncSpendTheme.colors.acc.copy(alpha = 0.1f) else SyncSpendTheme.colors.card,
                                        RoundedCornerShape(16.dp),
                                    )
                                    .border(1.dp, if (selected) SyncSpendTheme.colors.acc else SyncSpendTheme.colors.line, RoundedCornerShape(16.dp))
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                        if (selected) onPick(null) else {
                                            onPick(cat)
                                            close()
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 13.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                Icon(
                                    SyncSpendIcons.iconFor(cat.iconKey),
                                    null,
                                    tint = if (selected) SyncSpendTheme.colors.acc else SyncSpendTheme.colors.ink,
                                    modifier = Modifier.size(17.dp),
                                )
                                Text(
                                    cat.name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                        lineHeight = androidx.compose.ui.unit.TextUnit(13f, androidx.compose.ui.unit.TextUnitType.Sp),
                                    ),
                                    color = if (selected) SyncSpendTheme.colors.acc else SyncSpendTheme.colors.ink,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                )
                            }
                        }
                        repeat(3 - rowCats.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }

    if (showCreate) {
        val existing = categories.size
        NameIconDialog(
            title = "New category",
            body = "Name it and pick an icon — it will be added to the ${if (flow == FlowType.INCOME) "income" else "expense"} list.",
            cta = "Create",
            initialName = "",
            initialIconKey = null,
            namePlaceholder = if (flow == FlowType.INCOME) "New Income ${existing + 1}" else "New Expense ${existing + 1}",
            onConfirm = { name, icon ->
                showCreate = false
                onCreate(name, icon)
            },
            onDismiss = { showCreate = false },
        )
    }
}
