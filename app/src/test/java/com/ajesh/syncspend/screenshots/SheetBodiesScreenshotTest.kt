package com.ajesh.syncspend.screenshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.ajesh.syncspend.data.db.entity.CategoryEntity
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.ScopePeriod
import com.ajesh.syncspend.domain.model.ThemeMode
import com.ajesh.syncspend.ui.components.CustomRangeBody
import com.ajesh.syncspend.ui.components.PeriodPickerBody
import com.ajesh.syncspend.ui.components.rangePresets
import com.ajesh.syncspend.ui.editentry.EditEntryBody
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Sheets are separate windows and never reach the screenshot rig, so their bodies are rendered
 * directly: to look at the layout, and to assert the Edit Entry sheet never changes height.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-xxhdpi")
class SheetBodiesScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private val food = CategoryEntity(id = 1, name = "Food", iconKey = "utensils", type = FlowType.EXPENSE, sortOrder = 0)

    @androidx.compose.runtime.Composable
    private fun EditBody(type: FlowType, category: CategoryEntity?, modifier: Modifier = Modifier) = EditEntryBody(
        type = type, onTypeChange = {},
        note = "Coffee & Snacks", onNoteChange = {},
        amountText = "250", onAmountChange = {}, currencySymbol = "₹",
        dateLabel = "27 Sep 2026", onDateClick = {},
        category = category, onCategoryClick = {},
        canSave = category != null, onSave = {}, onDelete = {}, onClose = {},
        modifier = modifier,
    )

    @Test fun editEntryKeepsItsHeightWhateverIsSelected() {
        var type by mutableStateOf(FlowType.EXPENSE)
        var category by mutableStateOf<CategoryEntity?>(food)
        rule.setContent {
            SyncSpendTheme(themeMode = ThemeMode.LIGHT) {
                Box(Modifier.fillMaxWidth().padding(22.dp).testTag("body")) { EditBody(type, category) }
            }
        }
        fun height() = rule.onNodeWithTag("body").fetchSemanticsNode().size.height
        val withCategory = height()
        category = null // what switching Expense <-> Income does
        type = FlowType.INCOME
        rule.waitForIdle()
        assertEquals("sheet height changed when the category was cleared", withCategory, height())
    }

    @Test fun editEntryLight() = rule.snapshot("sheet_edit_entry_light", heightDp = 520) {
        Box(Modifier.padding(22.dp)) { EditBody(FlowType.EXPENSE, food) }
    }

    @Test fun editEntryDarkNoCategory() = rule.snapshot("sheet_edit_entry_dark", dark = true, heightDp = 520) {
        Box(Modifier.padding(22.dp)) { EditBody(FlowType.INCOME, null) }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w320dp-h700dp-xxhdpi")
class PeriodPickerBodyScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @Test fun aFewMonthsOfHistoryGreysOutWhatIsNotReachedYet() = rule.snapshot("sheet_period_short_history", widthDp = 360, heightDp = 560) {
        val now = YearMonth.now()
        Box(Modifier.padding(22.dp)) {
            PeriodPickerBody(
                draft = ScopePeriod.Month(now), onDraftChange = {},
                pickerYear = now.year, onYearChange = {},
                lowerBound = now.minusMonths(3), upperBound = now, // first entry three months back: Last 3 ok, Last 6 not
                showAllTime = false, onCancel = {}, onApply = {},
            )
        }
    }

    @Test fun narrowPhoneStillFitsThreeChips() = rule.snapshot("sheet_period_320", widthDp = 320, heightDp = 560) {
        val now = YearMonth.now()
        Box(Modifier.padding(22.dp)) {
            PeriodPickerBody(
                draft = ScopePeriod.LastMonths(3, now), onDraftChange = {},
                pickerYear = now.year, onYearChange = {},
                lowerBound = now.minusMonths(2), upperBound = now,
                showAllTime = false, onCancel = {}, onApply = {},
            )
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w320dp-h700dp-xxhdpi")
class CustomRangeBodyScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private val today = java.time.LocalDate.of(2026, 9, 27)

    @Test fun presetsHaveRoomAroundTheirLabelsAndGreyOutUntilReached() = rule.snapshot("sheet_custom_range", widthDp = 320, heightDp = 420) {
        Box(Modifier.padding(22.dp)) {
            CustomRangeBody(
                from = java.time.LocalDate.of(2026, 8, 1), to = today,
                presets = rangePresets(today, YearMonth.of(2026, 7)), // first entry in July: Last 2 and 3 months ok, 6 not
                onPickFrom = {}, onPickTo = {}, onPreset = {}, onCancel = {}, onApply = {},
            )
        }
    }
}
