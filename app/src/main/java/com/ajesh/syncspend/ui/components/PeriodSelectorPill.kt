package com.ajesh.syncspend.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme

/**
 * The pill row used to pick/step through a [com.ajesh.syncspend.domain.model.ScopePeriod]: prev
 * arrow, a tappable label (opens [PeriodPickerSheet]) with an optional sub-label, next arrow.
 * Originally Home-only; extracted in round 10 so Stats and Transactions can reuse the exact same
 * picker experience instead of a second hand-written copy (they pass `subLabel = null`, since Home's
 * "N expenses" sub-line doesn't apply to either).
 */
@Composable
fun PeriodSelectorPill(
    label: String,
    subLabel: String?,
    canGoPrev: Boolean,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SyncSpendTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.pill, RoundedCornerShape(18.dp))
            .border(1.dp, colors.line, RoundedCornerShape(18.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PeriodNavArrow(SyncSpendIcons.Prev, "Previous period", canGoPrev, onPrev)
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedContent(
                targetState = label,
                transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
                label = "scope-label",
            ) { l ->
                Text(l, style = MaterialTheme.typography.titleMedium.copy(letterSpacing = (-0.15).sp), color = colors.ink)
            }
            if (subLabel != null) {
                Text(subLabel, fontSize = 10.sp, color = colors.sub, modifier = Modifier.padding(top = 1.dp))
            }
        }
        PeriodNavArrow(SyncSpendIcons.Next, "Next period", canGoNext, onNext)
    }
}

@Composable
private fun PeriodNavArrow(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(SyncSpendTheme.colors.card, RoundedCornerShape(11.dp))
            .border(1.dp, SyncSpendTheme.colors.line, RoundedCornerShape(11.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = SyncSpendTheme.colors.ink.copy(alpha = if (enabled) 1f else 0.3f), modifier = Modifier.size(17.dp))
    }
}
