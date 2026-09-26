package com.ajesh.syncspend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendCorners
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import kotlinx.coroutines.launch

/**
 * The design's bottom sheet: 26dp top radius, `--sheet` fill, rgba(8,14,10,.5)
 * scrim, no drag handle, 22dp side padding. [content] receives a `close`
 * lambda that animates the sheet away before dismissing (so picking an item
 * slides the sheet down instead of vanishing).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SyncSpendTheme.colors.sheet,
        scrimColor = Color(0x80080E0A),
        shape = SyncSpendCorners.sheetTop,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 22.dp),
        ) { content(close) }
    }
}

/** Title + close-button header row used at the top of every sheet. */
@Composable
fun SheetHeader(title: String, onClose: () -> Unit, actions: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = SyncSpendTheme.colors.ink)
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            actions()
            SquareIconButton(SyncSpendIcons.Close, onClose, size = 30.dp)
        }
    }
}

/** 30dp tile button with a centered icon (the design's close / plus / edit / trash chips). */
@Composable
fun SquareIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 28.dp,
    radius: androidx.compose.ui.unit.Dp = 10.dp,
    background: Color = SyncSpendTheme.colors.tile,
    tint: Color = SyncSpendTheme.colors.ink,
    iconSize: androidx.compose.ui.unit.Dp = 14.dp,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(background, RoundedCornerShape(radius))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                role = androidx.compose.ui.semantics.Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription ?: defaultDescription(icon), tint = if (enabled) tint else tint.copy(alpha = 0.35f), modifier = Modifier.size(iconSize))
    }
}

private fun defaultDescription(icon: androidx.compose.ui.graphics.vector.ImageVector): String = when (icon.name) {
    "trash" -> "Delete"
    "pencil" -> "Edit"
    "up" -> "Move up"
    "down" -> "Move down"
    "prev" -> "Previous"
    "next" -> "Next"
    else -> icon.name.replaceFirstChar { it.uppercase() }
}
