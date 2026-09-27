package com.ajesh.syncspend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
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
 * tapping it opens [IconPickerDialog]. A larger [size] is used where choosing the icon is
 * a required step; [error] outlines it in the error colour when it is still missing.
 */
@Composable
fun IconPickTile(iconKey: String?, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 38.dp, error: Boolean = false) {
    val colors = SyncSpendTheme.colors
    val big = size > 44.dp
    Box(
        modifier = modifier
            .size(size)
            .semantics {
                contentDescription = if (iconKey == null) "Choose an icon" else "Change icon"
                role = Role.Button
            }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .background(colors.tile, RoundedCornerShape(if (big) 16.dp else 13.dp))
                .border(if (error) 1.5.dp else 1.dp, if (error) colors.neg else colors.line, RoundedCornerShape(if (big) 16.dp else 13.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (iconKey == null) SyncSpendIcons.Plus else SyncSpendIcons.iconFor(iconKey),
                null,
                tint = when {
                    error -> colors.neg
                    iconKey == null -> colors.sub
                    else -> colors.ink
                },
                modifier = Modifier.size(if (big) 24.dp else 18.dp),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 5.dp, y = 5.dp)
                .size(if (big) 22.dp else 18.dp)
                .background(colors.sheet, CircleShape)
                .padding(1.5.dp)
                .background(colors.button, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(SyncSpendIcons.Pencil, null, tint = colors.onButton, modifier = Modifier.size(if (big) 11.dp else 9.dp))
        }
    }
}

/**
 * Separate popup with the preset icon grid; picking an icon applies it and closes. [suggestions] (a few
 * icons that fit the name typed so far) get their own row above the full grid.
 */
@Composable
fun IconPickerDialog(selectedKey: String?, onPick: (String) -> Unit, onDismiss: () -> Unit, suggestions: List<String> = emptyList()) {
    DesignDialog(onDismiss) { IconPickerBody(selectedKey, onPick, suggestions) }
}

/** The popup's content, state-hoisted so it can be rendered on its own (screenshot tests). */
@Composable
internal fun IconPickerBody(selectedKey: String?, onPick: (String) -> Unit, suggestions: List<String>) {
    val colors = SyncSpendTheme.colors
    Column {
        DialogHeader(SyncSpendIcons.Spark, colors.ink, "Choose an icon", "Pick one that will make this easy to spot.")
        if (suggestions.isNotEmpty()) {
            Text("Suggested", style = MaterialTheme.typography.labelSmall, color = colors.sub, modifier = Modifier.padding(top = 14.dp))
            IconPickerGrid(
                selectedKey = selectedKey.orEmpty(),
                onSelect = onPick,
                keys = suggestions,
                modifier = Modifier.padding(top = 7.dp),
            )
            Text("All icons", style = MaterialTheme.typography.labelSmall, color = colors.sub, modifier = Modifier.padding(top = 14.dp))
        }
        IconPickerGrid(
            selectedKey = selectedKey.orEmpty(),
            onSelect = onPick,
            modifier = Modifier.padding(top = if (suggestions.isEmpty()) 16.dp else 7.dp),
        )
    }
}
