package com.ajesh.syncspend.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** Runs an action once POST_NOTIFICATIONS is available (Android 13+), asking first if needed. */
class NotificationGate internal constructor(private val request: (onGranted: () -> Unit, onDenied: () -> Unit) -> Unit) {
    operator fun invoke(onDenied: () -> Unit = {}, onGranted: () -> Unit) = request(onGranted, onDenied)
}

@Composable
fun rememberNotificationGate(): NotificationGate {
    val context = LocalContext.current
    var pendingGranted by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingDenied by remember { mutableStateOf<(() -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val ok = pendingGranted
        val no = pendingDenied
        pendingGranted = null
        pendingDenied = null
        if (granted) ok?.invoke() else {
            Toast.makeText(context, "Notifications are blocked — allow them in system settings to get reminders.", Toast.LENGTH_LONG).show()
            no?.invoke()
        }
    }
    return remember(launcher, context) {
        NotificationGate { onGranted, onDenied ->
            val needs = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            if (needs) {
                pendingGranted = onGranted
                pendingDenied = onDenied
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else onGranted()
        }
    }
}
