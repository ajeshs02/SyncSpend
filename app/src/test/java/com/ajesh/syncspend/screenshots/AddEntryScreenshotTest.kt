package com.ajesh.syncspend.screenshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import com.ajesh.syncspend.ui.addentry.AddEntryScreen
import com.ajesh.syncspend.ui.components.BottomFadeAndNav
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-xxhdpi")
class AddEntryScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @Test fun light() {
        rule.snapshot("add_entry_light", container = sampleContainer(), beforeCapture = { waitUntil(10_000) { onAllNodes(hasText("New Entry")).fetchSemanticsNodes().isNotEmpty() } }) {
            Box(Modifier.fillMaxSize()) {
                AddEntryScreen(onBack = {}, onSaved = {})
                BottomFadeAndNav(currentRoute = "add_entry", onNavigate = {}, onAddClick = {})
            }
        }
    }
}
