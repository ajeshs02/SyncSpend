package com.ajesh.syncspend.ui.components

import androidx.compose.foundation.layout.Column
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
 * title + explanation, an editable name, and the preset icon grid. [extra]
 * slots additional fields (amount, cycle, date, time...) between the name
 * and the icon grid.
 */
@Composable
fun NameIconDialog(
    title: String,
    body: String,
    cta: String,
    initialName: String,
    initialIconKey: String,
    namePlaceholder: String,
    onConfirm: (name: String, iconKey: String) -> Unit,
    onDismiss: () -> Unit,
    extraValid: Boolean = true,
    extra: @Composable () -> Unit = {},
) {
    var name by remember { mutableStateOf(initialName) }
    var iconKey by remember { mutableStateOf(initialIconKey) }
    DesignDialog(onDismiss) {
        DialogHeader(SyncSpendIcons.Plus, SyncSpendTheme.colors.ink, title, body)
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
            extra()
            Text(
                "Icon",
                style = MaterialTheme.typography.labelSmall,
                color = SyncSpendTheme.colors.sub,
                modifier = Modifier.padding(top = 14.dp, bottom = 7.dp),
            )
            IconPickerGrid(selectedKey = iconKey, onSelect = { iconKey = it })
        }
        DialogButtons(
            cta = cta,
            onCancel = onDismiss,
            onConfirm = { onConfirm(name.trim(), iconKey) },
            ctaEnabled = name.isNotBlank() && extraValid,
        )
    }
}
