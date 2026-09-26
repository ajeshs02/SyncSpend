package com.ajesh.syncspend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajesh.syncspend.ui.theme.SyncSpendTheme

/**
 * Day header for grouped lists: a soft date label, a hairline that fades out to
 * the right, and an optional [trailing] figure (the day's total). Small on
 * purpose — it should organise the list, not compete with the rows.
 */
@Composable
fun DateSeparator(label: String, modifier: Modifier = Modifier, trailing: String? = null) {
    val colors = SyncSpendTheme.colors
    val fade = remember(colors.line) { Brush.horizontalGradient(listOf(colors.line, Color.Transparent)) }
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp, color = colors.sub)
        Box(
            Modifier
                .weight(1f)
                .padding(start = 10.dp)
                .height(1.dp)
                .background(fade),
        )
        if (trailing != null) {
            Text(
                trailing,
                fontSize = 11.sp,
                color = colors.sub,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}
