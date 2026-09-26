package com.ajesh.syncspend.screenshots

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.dp
import com.ajesh.syncspend.domain.model.ThemeMode
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import java.io.File

/**
 * Renders [content] inside the app theme on the app's screen gradient at phone size
 * and writes a PNG to app/build/screenshots/<name>.png. These are eyeballing aids for
 * layout work done without a device — they assert nothing.
 */
fun ComposeContentTestRule.snapshot(
    name: String,
    dark: Boolean = false,
    widthDp: Int = 360,
    heightDp: Int = 780,
    content: @Composable () -> Unit,
) {
    setContent {
        SyncSpendTheme(themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
            Box(Modifier.size(widthDp.dp, heightDp.dp).background(SyncSpendTheme.colors.screenGradient)) {
                Box(Modifier.fillMaxSize()) { content() }
            }
        }
    }
    val bitmap = onRoot().captureToImage().asAndroidBitmap()
    val dir = File("build/screenshots").apply { mkdirs() }
    File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
}
