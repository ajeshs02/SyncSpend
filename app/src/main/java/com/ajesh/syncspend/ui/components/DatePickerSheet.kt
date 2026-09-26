package com.ajesh.syncspend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import com.ajesh.syncspend.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * The design's custom calendar sheet ("Entry date"). Used wherever the spec
 * says to use the in-app date picker (Edit Entry). Per the user's requirement
 * the browsable months start ~3 months back from the current month (extended
 * further back only if an existing entry is older) and stop at the current
 * month — no endless scrollback through empty years.
 */
@Composable
fun DatePickerSheet(
    initial: LocalDate,
    earliestTransactionDate: LocalDate?,
    onApply: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val upper = remember { YearMonth.now() }
    val lower = remember(earliestTransactionDate, initial) {
        val defaultLower = upper.minusMonths(3)
        listOfNotNull(defaultLower, earliestTransactionDate?.let { YearMonth.from(it) }, YearMonth.from(initial))
            .minOf { it }
    }
    // The upper bound must also contain the entry's own date if it was future-dated.
    val effectiveUpper = remember(initial) { maxOf(upper, YearMonth.from(initial)) }

    var selected by remember { mutableStateOf(initial) }
    var viewing by remember { mutableStateOf(YearMonth.from(initial)) }
    val colors = SyncSpendTheme.colors

    DesignSheet(onDismiss = onDismiss) { close ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Text(
                    "ENTRY DATE",
                    fontSize = 10.5.sp,
                    letterSpacing = 0.63.sp,
                    color = colors.sub,
                )
                Text(
                    DateUtils.longDate(selected),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp,
                    color = colors.ink,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            SquareIconButton(SyncSpendIcons.Close, close, size = 30.dp)
        }

        Row(modifier = Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            SquareIconButton(
                SyncSpendIcons.Prev, { viewing = viewing.minusMonths(1) },
                size = 32.dp, radius = 11.dp, iconSize = 17.dp, enabled = viewing.isAfter(lower),
            )
            Text(
                "${viewing.month.getDisplayName(TextStyle.FULL, Locale.US)} ${viewing.year}",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                color = colors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            SquareIconButton(
                SyncSpendIcons.Next, { viewing = viewing.plusMonths(1) },
                size = 32.dp, radius = 11.dp, iconSize = 17.dp, enabled = viewing.isBefore(effectiveUpper),
            )
        }

        Row(modifier = Modifier.padding(top = 12.dp).fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.sub,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                )
            }
        }

        val firstOffset = viewing.atDay(1).dayOfWeek.value % 7 // Sunday-first, like the design
        val cells: List<Int?> = List(firstOffset) { null } + (1..viewing.lengthOfMonth()).toList()
        Column(modifier = Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            cells.chunked(7).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    week.forEach { day ->
                        val isSelected = day != null && selected == viewing.atDay(day)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(
                                    if (isSelected) colors.selectedBrush else SolidColor(androidx.compose.ui.graphics.Color.Transparent),
                                    CircleShape,
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    enabled = day != null,
                                ) { if (day != null) selected = viewing.atDay(day) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (day != null) {
                                Text(
                                    day.toString(),
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                                    color = if (isSelected) colors.onSelected else colors.ink,
                                )
                            }
                        }
                    }
                    repeat(7 - week.size) { Box(Modifier.weight(1f)) }
                }
            }
        }

        Row(modifier = Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            SheetButton("Cancel", primary = false, modifier = Modifier.weight(1f), onClick = close)
            SheetButton("Apply", primary = true, modifier = Modifier.weight(1f)) {
                onApply(selected)
                close()
            }
        }
    }
}
