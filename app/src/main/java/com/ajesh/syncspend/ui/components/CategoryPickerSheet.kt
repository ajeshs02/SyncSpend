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
 * and closes the sheet. It only *picks* — categories are created, renamed and
 * deleted exclusively on the Categories page.
 */
@Composable
fun CategoryPickerSheet(
    flow: FlowType,
    categories: List<CategoryEntity>,
    selectedId: Long?,
    onPick: (CategoryEntity?) -> Unit,
    onDismiss: () -> Unit,
    onAddCategory: (() -> Unit)? = null,
) {
    DesignSheet(onDismiss = onDismiss) { close ->
        SheetHeader(
            title = "Choose category",
            onClose = close,
            actions = {
                if (onAddCategory != null) {
                    SquareIconButton(SyncSpendIcons.Plus, onClick = { close(); onAddCategory() }, size = 30.dp)
                }
            },
        )

        if (categories.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "No ${labelFor(flow).lowercase()} categories yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SyncSpendTheme.colors.ink,
                )
                Text(
                    "Add some on the Categories page, then pick one here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SyncSpendTheme.colors.sub,
                    modifier = Modifier.padding(top = 4.dp),
                    textAlign = TextAlign.Center,
                )
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
                            // In Light theme, `acc` as a pale 10%-alpha tint with `acc`-colored text was too
                            // low-contrast to read comfortably, so a selected tile there gets a solid acc fill
                            // with the dark onAcc foreground instead — the same pairing selected chips already
                            // use. Dark theme keeps its original tint/foreground, which already reads fine.
                            val colors = SyncSpendTheme.colors
                            val selectedBg = if (colors.isDark) colors.acc.copy(alpha = 0.1f) else colors.acc
                            val selectedFg = if (colors.isDark) colors.acc else colors.onAcc
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (selected) selectedBg else colors.card, RoundedCornerShape(16.dp))
                                    .border(1.dp, if (selected) colors.acc else colors.line, RoundedCornerShape(16.dp))
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
                                    tint = if (selected) selectedFg else colors.ink,
                                    modifier = Modifier.size(17.dp),
                                )
                                Text(
                                    cat.name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                        lineHeight = androidx.compose.ui.unit.TextUnit(13f, androidx.compose.ui.unit.TextUnitType.Sp),
                                    ),
                                    color = if (selected) selectedFg else colors.ink,
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
}
