package com.ajesh.syncspend.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajesh.syncspend.ui.theme.SyncSpendTheme

/**
 * The design's time sheet: big clock readout, AM/PM toggle, a 12-cell hour grid
 * and a 4-cell minute grid (00/15/30/45). A ±1 nudge next to "Minutes" makes
 * every minute reachable, which the design's grid alone doesn't.
 * Works in minute-of-day (0..1439) like the rest of the app.
 */
@Composable
fun TimePickerSheet(
    initialMinuteOfDay: Int,
    onApply: (minuteOfDay: Int) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Reminder time",
) {
    val colors = SyncSpendTheme.colors
    var hour by remember { mutableIntStateOf((initialMinuteOfDay / 60).let { if (it % 12 == 0) 12 else it % 12 }) }
    var minute by remember { mutableIntStateOf(initialMinuteOfDay % 60) }
    var pm by remember { mutableStateOf(initialMinuteOfDay / 60 >= 12) }

    DesignSheet(onDismiss = onDismiss) { close ->
        SheetHeader(title, onClose = close)

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}",
                fontSize = 34.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.68).sp,
                color = colors.ink,
            )
            Text(
                if (pm) "PM" else "AM",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.sub,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }

        AnimatedSegmentedControl(
            options = listOf("AM", "PM"),
            selectedIndex = if (pm) 1 else 0,
            onSelect = { pm = it == 1 },
            height = 42.dp,
            modifier = Modifier.padding(top = 14.dp),
        )

        FieldCaption("Hour")
        ChipGrid(columns = 6, cells = (1..12).map { h -> GridCell(h.toString(), h == hour) { hour = h } })

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FieldCaption("Minutes")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 14.dp)) {
                PickerChip("−1", selected = false, vertical = 5.dp, radius = 10.dp, modifier = Modifier.width(38.dp)) {
                    minute = (minute + 59) % 60
                }
                PickerChip("+1", selected = false, vertical = 5.dp, radius = 10.dp, modifier = Modifier.width(38.dp)) {
                    minute = (minute + 1) % 60
                }
            }
        }
        ChipGrid(
            columns = 4,
            vertical = 11.dp,
            cells = listOf(0, 15, 30, 45).map { m -> GridCell(m.toString().padStart(2, '0'), m == minute) { minute = m } },
        )

        Row(modifier = Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            SheetButton("Cancel", primary = false, modifier = Modifier.weight(1f), onClick = close)
            SheetButton("Apply", primary = true, modifier = Modifier.weight(1f)) {
                onApply(((hour % 12) + if (pm) 12 else 0) * 60 + minute)
                close()
            }
        }
    }
}

@Composable
private fun FieldCaption(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = SyncSpendTheme.colors.sub,
        modifier = Modifier.padding(top = 14.dp, bottom = 7.dp),
    )
}

private class GridCell(val label: String, val selected: Boolean, val onClick: () -> Unit)

/** Rows of equal-width [PickerChip]s. */
@Composable
private fun ChipGrid(columns: Int, cells: List<GridCell>, vertical: Dp = 10.dp) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        cells.chunked(columns).forEach { rowCells ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                rowCells.forEach { cell ->
                    PickerChip(
                        label = cell.label,
                        selected = cell.selected,
                        modifier = Modifier.weight(1f),
                        vertical = vertical,
                        radius = 12.dp,
                        onClick = cell.onClick,
                    )
                }
            }
        }
    }
}
