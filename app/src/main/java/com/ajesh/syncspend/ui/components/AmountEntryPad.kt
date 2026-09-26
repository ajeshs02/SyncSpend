package com.ajesh.syncspend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme

/**
 * The design's 3x4 keypad: digits 1-9, then [date key] 0 [backspace]. There is
 * deliberately no decimal key — the design puts the date key where "." would
 * normally sit. Shared by the Add Entry screen and the widget quick-add panel.
 */
@Composable
fun AmountEntryPad(
    dateLabel: String,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDateKey: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { d ->
                    PadKey(Modifier.weight(1f), SyncSpendTheme.colors.card, onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                        onDigit(d)
                    }) { Text(d, fontSize = 19.sp, fontWeight = FontWeight.Medium, color = SyncSpendTheme.colors.ink) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PadKey(Modifier.weight(1f), SyncSpendTheme.colors.pill, onClick = onDateClick, enabled = showDateKey) {
                if (showDateKey) {
                    Icon(SyncSpendIcons.Cal, null, tint = SyncSpendTheme.colors.ink, modifier = Modifier.size(15.dp))
                    Box(Modifier.width(6.dp))
                    Text(dateLabel, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = SyncSpendTheme.colors.ink)
                }
            }
            PadKey(Modifier.weight(1f), SyncSpendTheme.colors.card, onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                onDigit("0")
            }) { Text("0", fontSize = 19.sp, fontWeight = FontWeight.Medium, color = SyncSpendTheme.colors.ink) }
            PadKey(Modifier.weight(1f), SyncSpendTheme.colors.pill, onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                onBackspace()
            }) { Icon(SyncSpendIcons.Backspace, "Delete", tint = SyncSpendTheme.colors.ink, modifier = Modifier.size(21.dp)) }
        }
    }
}

@Composable
private fun PadKey(
    modifier: Modifier,
    bg: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier = modifier
            .height(52.dp)
            .scale(if (pressed) 0.95f else 1f)
            .background(bg, RoundedCornerShape(16.dp))
            .border(1.dp, SyncSpendTheme.colors.line, RoundedCornerShape(16.dp))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) { content() }
}
