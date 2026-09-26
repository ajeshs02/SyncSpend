package com.ajesh.syncspend.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme

/**
 * The design's centered modal card (scrim rgba(8,14,10,.55), 22dp radius,
 * `sssheet` enter: 40px rise + fade). Content is passed in; both delete
 * confirmations and the name+icon "New category / subscription / reminder"
 * dialogs are built on this.
 */
@Composable
fun DesignDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val view = LocalView.current
        SideEffect { (view.parent as? DialogWindowProvider)?.window?.setDimAmount(0f) }
        val visible = remember { MutableTransitionState(false).apply { targetState = true } }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x8C080E0A))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                .padding(26.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visibleState = visible,
                enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 12 },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(24.dp, RoundedCornerShape(22.dp))
                        .background(SyncSpendTheme.colors.sheet, RoundedCornerShape(22.dp))
                        .border(1.dp, SyncSpendTheme.colors.line, RoundedCornerShape(22.dp))
                        // Swallow taps so they don't fall through to the scrim.
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                        .padding(20.dp),
                ) { content() }
            }
        }
    }
}

@Composable
fun DialogHeader(icon: ImageVector, iconTint: Color, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
        Box(
            modifier = Modifier.size(34.dp).background(SyncSpendTheme.colors.tile, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = iconTint, modifier = Modifier.size(17.dp)) }
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = SyncSpendTheme.colors.ink)
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = SyncSpendTheme.colors.sub,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun DialogButtons(
    cta: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    destructive: Boolean = false,
    ctaEnabled: Boolean = true,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .background(SyncSpendTheme.colors.card, RoundedCornerShape(15.dp))
                .border(1.dp, SyncSpendTheme.colors.line, RoundedCornerShape(15.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onCancel),
            contentAlignment = Alignment.Center,
        ) { Text("Cancel", style = MaterialTheme.typography.labelLarge, color = SyncSpendTheme.colors.ink) }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .background(
                    (if (destructive) SyncSpendTheme.colors.neg else SyncSpendTheme.colors.button).copy(alpha = if (ctaEnabled) 1f else 0.4f),
                    RoundedCornerShape(15.dp),
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = ctaEnabled,
                    onClick = onConfirm,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                cta,
                style = MaterialTheme.typography.labelLarge,
                color = if (destructive) Color.White else SyncSpendTheme.colors.onButton,
            )
        }
    }
}

/** "Delete this entry?" / "Delete category?" style confirmation. */
@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    cta: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true,
) {
    DesignDialog(onDismiss) {
        DialogHeader(
            icon = if (destructive) SyncSpendIcons.Trash else SyncSpendIcons.Plus,
            iconTint = if (destructive) SyncSpendTheme.colors.neg else SyncSpendTheme.colors.ink,
            title = title,
            body = body,
        )
        DialogButtons(cta = cta, onCancel = onDismiss, onConfirm = onConfirm, destructive = destructive)
    }
}

/** A single-button information dialog (e.g. CSV import summary). */
@Composable
fun InfoDialog(title: String, body: String, onDismiss: () -> Unit, ok: String = "OK") {
    DesignDialog(onDismiss) {
        DialogHeader(SyncSpendIcons.Spark, SyncSpendTheme.colors.ink, title, body)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
                .height(44.dp)
                .background(SyncSpendTheme.colors.button, RoundedCornerShape(15.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) { Text(ok, style = MaterialTheme.typography.labelLarge, color = SyncSpendTheme.colors.onButton) }
    }
}
