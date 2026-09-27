package com.ajesh.syncspend.screenshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import com.ajesh.syncspend.ui.categories.CategoriesScreen
import com.ajesh.syncspend.ui.components.BottomFadeAndNav
import com.ajesh.syncspend.ui.transactions.TransactionsScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-xxhdpi")
class ListsScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @Test fun categories() {
        rule.snapshot(
            "categories_light", container = sampleContainer(),
            beforeCapture = { waitUntil(15_000) { onAllNodes(hasText("Groceries")).fetchSemanticsNodes().isNotEmpty() } },
        ) {
            Box(Modifier.fillMaxSize()) {
                CategoriesScreen()
                BottomFadeAndNav(currentRoute = "categories", onNavigate = {}, onAddClick = {})
            }
        }
    }

    @Test fun entries() {
        rule.snapshot(
            "entries_light", container = sampleContainer(),
            beforeCapture = { waitUntil(15_000) { onAllNodes(hasText("Coffee and snacks with Rohan")).fetchSemanticsNodes().isNotEmpty() } },
        ) {
            Box(Modifier.fillMaxSize()) {
                TransactionsScreen()
                BottomFadeAndNav(currentRoute = "transactions", onNavigate = {}, onAddClick = {})
            }
        }
    }
}
