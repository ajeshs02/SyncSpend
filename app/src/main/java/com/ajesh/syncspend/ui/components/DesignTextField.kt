package com.ajesh.syncspend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.ajesh.syncspend.ui.theme.SyncSpendTheme

/** The design's tile-filled input: 14dp radius, 12/13 padding, accent caret. */
@Composable
fun DesignTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    textColor: Color = SyncSpendTheme.colors.ink,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SyncSpendTheme.colors.tile, RoundedCornerShape(14.dp))
            .padding(horizontal = 13.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty() && placeholder.isNotEmpty()) {
            Text(placeholder, style = textStyle, color = SyncSpendTheme.colors.sub)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = textStyle.copy(color = textColor),
            cursorBrush = SolidColor(SyncSpendTheme.colors.acc),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
