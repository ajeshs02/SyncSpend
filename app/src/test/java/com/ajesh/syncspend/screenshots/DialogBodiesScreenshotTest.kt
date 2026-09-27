package com.ajesh.syncspend.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.ajesh.syncspend.ui.components.IconPickerBody
import com.ajesh.syncspend.ui.components.NameIconDialogBody
import com.ajesh.syncspend.ui.icons.IconKind
import com.ajesh.syncspend.ui.icons.IconSuggestions
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The centred dialogs are separate windows, so their bodies are rendered directly (on the dialog card colour). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-xxhdpi")
class DialogBodiesScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @androidx.compose.runtime.Composable
    private fun Card(content: @androidx.compose.runtime.Composable () -> Unit) {
        Box(Modifier.padding(26.dp).background(SyncSpendTheme.colors.sheet, RoundedCornerShape(22.dp)).padding(20.dp)) { content() }
    }

    @androidx.compose.runtime.Composable
    private fun NewSubscription(showErrors: Boolean) = NameIconDialogBody(
        title = "New subscription", body = "Add the service, amount and billing cycle. You'll get a notification on the due date.",
        cta = "Add", name = "", onNameChange = {}, namePlaceholder = "Netflix, Spotify, Gym…",
        iconKey = null, onIconClick = {}, showErrors = showErrors, shake = 0, extraValid = false,
        onDelete = null, deleteLabel = "Delete", onCancel = {}, onConfirm = {},
        extra = { errors ->
            Text("Amount", modifier = Modifier.padding(top = 14.dp))
            com.ajesh.syncspend.ui.components.DesignTextField(value = "", onValueChange = {}, placeholder = "₹0", error = errors)
            if (errors) Text("Enter an amount")
        },
    )

    @Test fun addIsEnabledAndNothingIsHighlightedBeforeTheFirstTap() =
        rule.snapshot("dialog_new_subscription_fresh", heightDp = 620) { Card { NewSubscription(showErrors = false) } }

    @Test fun tappingAddWithNothingFilledHighlightsEachMissingField() =
        rule.snapshot("dialog_new_subscription_errors", heightDp = 620) { Card { NewSubscription(showErrors = true) } }

    @Test fun iconPickerLeadsWithSuggestionsForTheTypedName() =
        rule.snapshot("dialog_icon_picker_suggested", heightDp = 700) {
            Card { IconPickerBody(selectedKey = null, onPick = {}, suggestions = IconSuggestions.suggest(IconKind.SUBSCRIPTION, "Netflix")) }
        }

    @Test fun iconPickerDarkReminder() =
        rule.snapshot("dialog_icon_picker_reminder_dark", dark = true, heightDp = 700) {
            Card { IconPickerBody(selectedKey = "bell", onPick = {}, suggestions = IconSuggestions.suggest(IconKind.REMINDER, "")) }
        }
}
