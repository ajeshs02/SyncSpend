package com.ajesh.syncspend.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.ajesh.syncspend.ui.components.BottomFadeAndNav
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-xxhdpi")
class NavBarScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private val routes = listOf("home", "transactions", "add_entry", "categories", "settings", "subs_reminders/{listMode}")

    @Test fun everySlotLight() {
        rule.snapshot("nav_all_slots_light", heightDp = 90 * routes.size) {
            Column {
                routes.forEach { route ->
                    Box(Modifier.fillMaxWidth().height(90.dp)) {
                        BottomFadeAndNav(currentRoute = route, onNavigate = {}, onAddClick = {})
                    }
                }
            }
        }
    }

    /** Tapping a tab moves the highlight at once (before any route change) and slides it across over ~280ms. */
    @Test fun pillLeavesOnTheTapAndSlidesAcross() {
        var route by mutableStateOf("home")
        rule.mainClock.autoAdvance = false
        rule.setContent {
            com.ajesh.syncspend.ui.theme.SyncSpendTheme(themeMode = com.ajesh.syncspend.domain.model.ThemeMode.DARK) {
                Box(Modifier.fillMaxWidth().height(90.dp).background(com.ajesh.syncspend.ui.theme.SyncSpendTheme.colors.screenGradient)) {
                    // The route deliberately never changes: only the tap can move the pill.
                    BottomFadeAndNav(currentRoute = route, onNavigate = {}, onAddClick = {})
                }
            }
        }
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithContentDescription("Settings").performClick()
        rule.mainClock.advanceTimeBy(120)
        rule.captureFrame("nav_pill_mid_slide")
        rule.mainClock.advanceTimeBy(300)
        rule.captureFrame("nav_pill_after_tap")
        // The route never followed the tap, so after the grace period the pill returns to Home.
        rule.mainClock.advanceTimeBy(1000)
        rule.captureFrame("nav_pill_reverted")
    }
}
