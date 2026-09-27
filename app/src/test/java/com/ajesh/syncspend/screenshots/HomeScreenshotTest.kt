package com.ajesh.syncspend.screenshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import com.ajesh.syncspend.ui.components.BottomFadeAndNav
import com.ajesh.syncspend.ui.home.HomeScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-xxhdpi")
class HomeScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private fun render(name: String, dark: Boolean) {
        val container = sampleContainer()
        rule.snapshot(
            name, dark = dark, container = container,
            beforeCapture = { waitUntil(15_000) { onAllNodes(hasText("Coffee and snacks with Rohan")).fetchSemanticsNodes().isNotEmpty() } },
        ) {
            Box(androidx.compose.ui.Modifier.fillMaxSize()) {
                HomeScreen(onViewAllTransactions = {}, onOpenSubscriptions = {}, onOpenReminders = {})
                BottomFadeAndNav(currentRoute = "home", onNavigate = {}, onAddClick = {})
            }
        }
    }

    @Test fun light() = render("home_light", dark = false)
    @Test fun dark() = render("home_dark", dark = true)
}
