package com.ajesh.syncspend.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.ui.components.AppLogo
import com.ajesh.syncspend.ui.components.BottomFadeAndNav
import com.ajesh.syncspend.ui.components.FlowToggle
import com.ajesh.syncspend.ui.components.PrimaryButton
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-xxhdpi")
class ComponentsScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @Composable
    private fun Sample() {
        val colors = SyncSpendTheme.colors
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                AppLogo(30.dp)
                AppLogo(56.dp)
                AppLogo(96.dp)
            }
            FlowToggle(type = FlowType.EXPENSE, onSelect = {})
            PrimaryButton(
                text = "Save Entry",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                leading = { Box(Modifier.size(7.dp).background(colors.expenseOnSelected, RoundedCornerShape(2.dp))); Box(Modifier.size(8.dp)) },
            )
            PrimaryButton(text = "Save Changes", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
            Box(Modifier.fillMaxWidth().size(60.dp).background(colors.card, RoundedCornerShape(16.dp)))
        }
        Box(Modifier.fillMaxWidth()) { BottomFadeAndNav(currentRoute = "categories", onNavigate = {}, onAddClick = {}) }
    }

    @Test fun light() = rule.snapshot("components_light", heightDp = 520) { Sample() }
    @Test fun dark() = rule.snapshot("components_dark", dark = true, heightDp = 520) { Sample() }
}
