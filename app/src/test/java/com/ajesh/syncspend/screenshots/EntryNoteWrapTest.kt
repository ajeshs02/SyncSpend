package com.ajesh.syncspend.screenshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.ajesh.syncspend.domain.model.EntryNote
import com.ajesh.syncspend.ui.transactions.DayGroupUi
import com.ajesh.syncspend.ui.transactions.EntriesTab
import com.ajesh.syncspend.ui.transactions.TxRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The note is never truncated: it wraps under the category. The input cap (EntryNote.MAX_LENGTH) is chosen so a
 * realistic note of that length wraps to at most two lines on the tightest row (Entries card: 22dp screen padding,
 * 13dp card padding, and a wide amount plus "27 Sep - 18:40" on the right), including on a 320dp phone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-xxhdpi")
class EntryNoteWrapTest {
    @get:Rule val rule = createComposeRule()

    // 40 characters, mixed case with several capitals: wider than typical prose.
    private val longest = "Dinner With Family At Mumbai Darbar Cafe"

    private fun row(id: Long, note: String) = TxRow(
        id = id, categoryLabel = "Recharge, Bills & Subscriptions", note = note,
        amountFormatted = "₹150,000", isPositive = false, dayLabel = "27 Sep - 18:40", iconKey = "receipt",
    )

    private fun linesAt(widthDp: Int, name: String): Pair<Float, Float> {
        val groups = listOf(DayGroupUi("Today", "₹150,000", listOf(row(1, "Tea"), row(2, longest))))
        rule.setContent {
            com.ajesh.syncspend.ui.theme.SyncSpendTheme(themeMode = com.ajesh.syncspend.domain.model.ThemeMode.LIGHT) {
                Box(Modifier.width(widthDp.dp).padding(horizontal = 22.dp)) { EntriesTab(groups, onRowClick = {}) }
            }
        }
        rule.waitForIdle()
        val oneLine = rule.onNodeWithText("Tea").getBoundsInRoot().height.value
        val wrapped = rule.onNodeWithText(longest).getBoundsInRoot().height.value
        rule.captureFrame(name)
        return oneLine to wrapped
    }

    @Test fun theCapMatchesTheDocumentedLength() {
        assertEquals(40, EntryNote.MAX_LENGTH)
        assertEquals(EntryNote.MAX_LENGTH, longest.length)
    }

    @Test fun aFullLengthNoteWrapsToTwoLinesAt360dp() {
        val (one, wrapped) = linesAt(360, "entries_note_wrap_360")
        assertTrue("wrapped=$wrapped one=$one", wrapped <= one * 2 + 1)
    }

    @Test fun aFullLengthNoteWrapsToTwoLinesAt320dp() {
        val (one, wrapped) = linesAt(320, "entries_note_wrap_320")
        assertTrue("wrapped=$wrapped one=$one", wrapped <= one * 2 + 1)
    }
}
