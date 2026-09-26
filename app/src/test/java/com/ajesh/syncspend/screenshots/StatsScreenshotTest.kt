package com.ajesh.syncspend.screenshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import com.ajesh.syncspend.domain.model.TransactionsTab
import com.ajesh.syncspend.ui.components.BottomFadeAndNav
import com.ajesh.syncspend.ui.transactions.TransactionsScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-xxhdpi")
class StatsScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private fun render(name: String, dark: Boolean, height: Int) {
        val container = sampleContainer()
        // What tapping Home's hero card does: ask Transactions to open on the Stats tab.
        container.selectionState.pendingTransactionsTab.value = TransactionsTab.ANALYTICS
        rule.snapshot(
            name, dark = dark, heightDp = height, container = container,
            beforeCapture = { waitUntil(20_000) { onAllNodes(hasText("Month pace")).fetchSemanticsNodes().isNotEmpty() } },
        ) {
            Box(Modifier.fillMaxSize()) {
                TransactionsScreen()
                BottomFadeAndNav(currentRoute = "transactions", onNavigate = {}, onAddClick = {})
            }
        }
    }

    @Test fun statsLightTall() = render("stats_light_tall", dark = false, height = 2300)
    @Test fun statsDarkPhone() = render("stats_dark_phone", dark = true, height = 780)
}
