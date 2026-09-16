package com.ajesh.syncspend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ajesh.syncspend.ui.theme.SyncSpendTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SyncSpendTheme {
                SyncSpendPlaceholder()
            }
        }
    }
}

@Composable
private fun SyncSpendPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SyncSpendTheme.colors.screenGradient),
        contentAlignment = Alignment.Center,
    ) {
        Text("SyncSpend", style = androidx.compose.material3.MaterialTheme.typography.headlineLarge)
    }
}
