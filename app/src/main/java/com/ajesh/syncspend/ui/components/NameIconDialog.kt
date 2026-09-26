package com.ajesh.syncspend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme

/**
 * The design's "New category / New subscription / New reminder" modal: a
 * title + explanation, an editable name, and an icon chosen through the tappable
 * tile in the header (which opens [IconPickerDialog]). [extra] slots additional
 * fields (amount, cycle, date, time...) below the name.
 *
 * [initialIconKey] = null means "no icon yet": the icon is then mandatory and
 * the confirm button stays disabled until one is picked. Passing [onDelete]
 * (edit mode) adds a red delete button above Cancel/Save, labelled [deleteLabel].
 */
@Composable
fun NameIconDialog(
    title: String,
    body: String,
    cta: String,
    initialName: String,
    initialIconKey: String?,
    namePlaceholder: String,
    onConfirm: (name: String, iconKey: String) -> Unit,
    onDismiss: () -> Unit,
    extraValid: Boolean = true,
    onDelete: (() -> Unit)? = null,
    deleteLabel: String = "Delete",
    extra: @Composable () -> Unit = {},
) {
    var name by remember { mutableStateOf(initialName) }
    var iconKey by remember { mutableStateOf(initialIconKey) }
    var pickingIcon by remember { mutableStateOf(false) }
    val chosen = iconKey

    DesignDialog(onDismiss) {
        DialogHeader(
            leading = { IconPickTile(chosen, onClick = { pickingIcon = true }) },
            title = title,
            body = body,
        )
        Column(
            modifier = Modifier
                .heightIn(max = 460.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            DesignTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = namePlaceholder,
                modifier = Modifier.padding(top = 14.dp),
            )
            if (chosen == null) {
                Text(
                    "Tap the icon at the top to choose one.",
                    style = MaterialTheme.typography.labelSmall,
                    color = SyncSpendTheme.colors.sub,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            extra()
        }
        if (onDelete != null) {
            DeleteRow(label = deleteLabel, onClick = onDelete)
        }
        DialogButtons(
            cta = cta,
            onCancel = onDismiss,
            onConfirm = { if (chosen != null) onConfirm(name.trim(), chosen) },
            ctaEnabled = name.isNotBlank() && chosen != null && extraValid,
        )
    }

    if (pickingIcon) {
        IconPickerDialog(
            selectedKey = chosen,
            onPick = {
                iconKey = it
                pickingIcon = false
            },
            onDismiss = { pickingIcon = false },
        )
    }
}

/** Full-width red "Delete …" button used inside edit dialogs. */
@Composable
private fun DeleteRow(label: String, onClick: () -> Unit) {
    val colors = SyncSpendTheme.colors
    Row(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .height(44.dp)
            .background(colors.neg.copy(alpha = 0.10f), RoundedCornerShape(15.dp))
            .border(1.dp, colors.neg.copy(alpha = 0.28f), RoundedCornerShape(15.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(SyncSpendIcons.Trash, null, tint = colors.neg, modifier = Modifier.size(15.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = colors.neg, modifier = Modifier.padding(start = 7.dp))
    }
}
