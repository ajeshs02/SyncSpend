package com.ajesh.syncspend.screenshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import com.ajesh.syncspend.domain.model.TransactionsTab
import com.ajesh.syncspend.ui.transactions.TransactionsScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A very tall viewport so the whole (normally scrolling) Stats page fits in one image. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h2300dp-mdpi")
class StatsFullPageScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @Test fun light() {
        val container = sampleContainer()
        container.selectionState.pendingTransactionsTab.value = TransactionsTab.ANALYTICS
        rule.snapshot(
            "stats_full_light", heightDp = 2300, container = container,
            beforeCapture = { waitUntil(20_000) { onAllNodes(hasText("Findings")).fetchSemanticsNodes().isNotEmpty() } },
        ) { Box(Modifier.fillMaxSize()) { TransactionsScreen() } }
    }
}
