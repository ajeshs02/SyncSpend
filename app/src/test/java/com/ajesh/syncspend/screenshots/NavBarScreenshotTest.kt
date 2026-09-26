package com.ajesh.syncspend.screenshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
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
}
