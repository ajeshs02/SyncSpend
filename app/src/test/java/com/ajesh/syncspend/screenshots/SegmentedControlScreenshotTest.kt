package com.ajesh.syncspend.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.ThemeMode
import com.ajesh.syncspend.ui.components.AnimatedSegmentedControl
import com.ajesh.syncspend.ui.components.FlowToggle
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-xxhdpi")
class SegmentedControlScreenshotTest {
    @get:Rule val rule = createComposeRule()

    /** Frame 0 of a screen, no waiting: the pill must already sit under the selected segment. */
    @Test fun pillIsThereOnTheVeryFirstFrame() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            SyncSpendTheme(themeMode = ThemeMode.LIGHT) {
                Box(Modifier.fillMaxWidth().height(200.dp).background(SyncSpendTheme.colors.screenGradient).padding(22.dp)) {
                    Column {
                        AnimatedSegmentedControl(options = listOf("Entries", "Categories", "Stats"), selectedIndex = 2, onSelect = {})
                        Box(Modifier.height(12.dp))
                        FlowToggle(type = FlowType.INCOME, onSelect = {})
                    }
                }
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.captureFrame("segmented_first_frame")
    }

    @Test fun pillSlidesMidway() {
        var index by mutableStateOf(0)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            SyncSpendTheme(themeMode = ThemeMode.DARK) {
                Box(Modifier.fillMaxWidth().height(90.dp).background(SyncSpendTheme.colors.screenGradient).padding(22.dp)) {
                    AnimatedSegmentedControl(options = listOf("Entries", "Categories", "Stats"), selectedIndex = index, onSelect = {})
                }
            }
        }
        rule.mainClock.advanceTimeBy(300)
        index = 2
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        rule.mainClock.advanceTimeBy(130)
        rule.captureFrame("segmented_mid_slide")
    }
}
