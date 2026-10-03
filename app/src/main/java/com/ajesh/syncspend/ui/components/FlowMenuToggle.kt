package com.ajesh.syncspend.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme

/** The flow's label, lowercase variant available via `.lowercase()` where a screen needs it mid-sentence. */
fun labelFor(flow: FlowType): String = when (flow) {
    FlowType.EXPENSE -> "Expense"
    FlowType.INCOME -> "Income"
}

/** The flow's accent colour, for the handful of screens (Stats, Transactions) that tint a word to match it. */
fun flowColor(flow: FlowType, colors: com.ajesh.syncspend.ui.theme.SyncSpendColors): Color = when (flow) {
    FlowType.EXPENSE -> colors.neg
    FlowType.INCOME -> colors.pos
}

private fun iconFor(flow: FlowType) = when (flow) {
    FlowType.EXPENSE -> SyncSpendIcons.ArrowOut
    FlowType.INCOME -> SyncSpendIcons.ArrowIn
}

/**
 * The compact Expense/Income pill used in a screen's title row (Transactions, Stats): tap to open a
 * small dropdown with the other option(s), rather than [FlowToggle]'s full-width segmented control
 * (used where there's room for it, e.g. Add/Edit Entry). [options] defaults to Expense/Income; the
 * Transactions page passes a third, Savings, entry (not a real [FlowType] — see `TransactionsScreen`).
 */
@Composable
fun FlowMenuToggle(flow: FlowType, onPick: (FlowType) -> Unit, options: List<FlowType> = listOf(FlowType.EXPENSE, FlowType.INCOME)) {
    var open by remember { mutableStateOf(false) }
    val colors = SyncSpendTheme.colors
    Box {
        Row(
            modifier = Modifier
                // Same selected colours as the Expense/Income toggle, so it reads in dark mode too.
                .background(colors.selectedBrush, RoundedCornerShape(14.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { open = !open },
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                iconFor(flow),
                null,
                tint = if (flow == FlowType.EXPENSE) colors.expenseOnSelected else colors.brand,
                modifier = Modifier.size(15.dp),
            )
            Box(Modifier.width(5.dp))
            Text(labelFor(flow), style = MaterialTheme.typography.labelLarge, color = colors.onSelected)
        }
        if (open) {
            FlowMenuPopup(
                current = flow,
                options = options,
                onPick = {
                    onPick(it)
                    open = false
                },
                onDismiss = { open = false },
            )
        }
    }
}

@Composable
private fun FlowMenuPopup(current: FlowType, options: List<FlowType>, onPick: (FlowType) -> Unit, onDismiss: () -> Unit) {
    val density = LocalDensity.current
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(0, with(density) { 42.dp.roundToPx() }),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        AnimatedVisibility(
            visibleState = visible,
            enter = fadeIn(tween(140)) + scaleIn(tween(140), initialScale = 0.92f, transformOrigin = TransformOrigin(1f, 0f)),
        ) {
            Column(
                modifier = Modifier
                    .shadow(14.dp, RoundedCornerShape(14.dp))
                    .background(SyncSpendTheme.colors.sheet, RoundedCornerShape(14.dp))
                    .border(1.dp, SyncSpendTheme.colors.line, RoundedCornerShape(14.dp))
                    .padding(6.dp)
                    // A Popup is measured against the whole screen, so the fillMaxWidth rows inside
                    // would stretch across it; pin the menu to a compact width.
                    .width(150.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                options.forEach { FlowMenuItem(labelFor(it), it, current, onPick) }
            }
        }
    }
}

@Composable
private fun FlowMenuItem(label: String, value: FlowType, current: FlowType, onPick: (FlowType) -> Unit) {
    val colors = SyncSpendTheme.colors
    val selected = value == current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) colors.selectedBrush else SolidColor(Color.Transparent), RoundedCornerShape(10.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onPick(value) }
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            iconFor(value),
            null,
            tint = when {
                value != FlowType.EXPENSE -> colors.brand
                selected -> colors.expenseOnSelected
                else -> colors.neg
            },
            modifier = Modifier.size(14.dp),
        )
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) colors.onSelected else colors.ink)
    }
}
