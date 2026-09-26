package com.ajesh.syncspend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme

/**
 * The tappable icon tile in "New category / subscription / reminder" dialogs
 * and edit sheets. Shows the current icon (or a `+` while none is chosen yet)
 * with a small pencil badge on its bottom-right corner so it reads as editable;
 * tapping it opens [IconPickerDialog].
 */
@Composable
fun IconPickTile(iconKey: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = SyncSpendTheme.colors
    Box(
        modifier = modifier
            .size(38.dp)
            .semantics {
                contentDescription = if (iconKey == null) "Choose an icon" else "Change icon"
                role = Role.Button
            }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(colors.tile, RoundedCornerShape(13.dp))
                .border(1.dp, colors.line, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (iconKey == null) SyncSpendIcons.Plus else SyncSpendIcons.iconFor(iconKey),
                null,
                tint = if (iconKey == null) colors.sub else colors.ink,
                modifier = Modifier.size(18.dp),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 5.dp, y = 5.dp)
                .size(18.dp)
                .background(colors.sheet, CircleShape)
                .padding(1.5.dp)
                .background(colors.button, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(SyncSpendIcons.Pencil, null, tint = colors.onButton, modifier = Modifier.size(9.dp))
        }
    }
}

/** Separate popup with the preset icon grid; picking an icon applies it and closes. */
@Composable
fun IconPickerDialog(selectedKey: String?, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    DesignDialog(onDismiss) {
        DialogHeader(SyncSpendIcons.Spark, SyncSpendTheme.colors.ink, "Choose an icon", "Pick one that will make this easy to spot.")
        IconPickerGrid(
            selectedKey = selectedKey.orEmpty(),
            onSelect = onPick,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}
