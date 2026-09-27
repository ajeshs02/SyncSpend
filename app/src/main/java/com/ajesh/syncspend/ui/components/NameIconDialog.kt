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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ajesh.syncspend.ui.icons.IconKind
import com.ajesh.syncspend.ui.icons.IconSuggestions
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme

/**
 * The design's "New category / New subscription / New reminder" modal: a title + explanation, an
 * editable name, and an icon chosen through the tappable tile in the header (which opens
 * [IconPickerDialog], with a "Suggested" row that follows the name). [extra] slots additional fields
 * (amount, cycle, date, time...) below the name and is told whether to show its errors.
 *
 * The confirm button is never disabled, since a disabled button doesn't say *why*. Tapping it while
 * something is missing (a name, an icon, or [extraValid] = false) instead outlines each missing field in
 * red with a short message and shakes it. [initialIconKey] = null means "no icon yet": choosing one is
 * required. Passing [onDelete] (edit mode) adds a red delete button above Cancel/Save, labelled [deleteLabel].
 */
@Composable
fun NameIconDialog(
    title: String,
    body: String,
    cta: String,
    initialName: String,
    initialIconKey: String?,
    namePlaceholder: String,
    iconKind: IconKind,
    onConfirm: (name: String, iconKey: String) -> Unit,
    onDismiss: () -> Unit,
    extraValid: Boolean = true,
    onDelete: (() -> Unit)? = null,
    deleteLabel: String = "Delete",
    extra: @Composable (showErrors: Boolean) -> Unit = {},
) {
    var name by remember { mutableStateOf(initialName) }
    var iconKey by remember { mutableStateOf(initialIconKey) }
    var pickingIcon by remember { mutableStateOf(false) }
    // Set by a tap on the confirm button that couldn't go ahead; the errors then follow what is still missing.
    var attempted by remember { mutableStateOf(false) }
    var shake by remember { mutableIntStateOf(0) }
    val chosen = iconKey

    DesignDialog(onDismiss) {
        NameIconDialogBody(
            title = title, body = body, cta = cta,
            name = name, onNameChange = { name = it }, namePlaceholder = namePlaceholder,
            iconKey = chosen, onIconClick = { pickingIcon = true },
            showErrors = attempted, shake = shake, extraValid = extraValid,
            onDelete = onDelete, deleteLabel = deleteLabel,
            onCancel = onDismiss,
            onConfirm = {
                if (name.isBlank() || chosen == null || !extraValid) {
                    attempted = true
                    shake++
                } else {
                    onConfirm(name.trim(), chosen)
                }
            },
            extra = extra,
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
            suggestions = IconSuggestions.suggest(iconKind, name),
        )
    }
}

/** The dialog's content, state-hoisted so it can be rendered on its own (screenshot tests). */
@Composable
internal fun NameIconDialogBody(
    title: String,
    body: String,
    cta: String,
    name: String,
    onNameChange: (String) -> Unit,
    namePlaceholder: String,
    iconKey: String?,
    onIconClick: () -> Unit,
    showErrors: Boolean,
    shake: Int,
    extraValid: Boolean,
    onDelete: (() -> Unit)?,
    deleteLabel: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    extra: @Composable (showErrors: Boolean) -> Unit,
) {
    val colors = SyncSpendTheme.colors
    val nameMissing = name.isBlank()
    val iconMissing = iconKey == null
    Column {
        DialogHeader(
            leading = {
                IconPickTile(
                    iconKey, onClick = onIconClick, size = 52.dp,
                    error = showErrors && iconMissing,
                    modifier = Modifier.shakeWhen(shake, iconMissing),
                )
            },
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
                onValueChange = onNameChange,
                placeholder = namePlaceholder,
                error = showErrors && nameMissing,
                modifier = Modifier.padding(top = 14.dp).shakeWhen(shake, nameMissing),
            )
            if (showErrors && nameMissing) {
                Text("Enter a name", style = MaterialTheme.typography.labelSmall, color = colors.neg, modifier = Modifier.padding(top = 6.dp))
            }
            if (iconMissing) {
                Text(
                    if (showErrors) "Please choose an icon" else "Tap the icon at the top to choose one.",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (showErrors) colors.neg else colors.sub,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Column(modifier = Modifier.shakeWhen(shake, !extraValid)) { extra(showErrors) }
        }
        if (onDelete != null) {
            DeleteRow(label = deleteLabel, onClick = onDelete)
        }
        DialogButtons(cta = cta, onCancel = onCancel, onConfirm = onConfirm)
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
