package com.ajesh.syncspend.ui.editentry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ajesh.syncspend.data.db.entity.TransactionEntity
import com.ajesh.syncspend.di.LocalAppContainer
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.ui.components.ConfirmDialog
import com.ajesh.syncspend.ui.components.DatePickerSheet
import com.ajesh.syncspend.ui.components.DesignSheet
import com.ajesh.syncspend.ui.components.DesignTextField
import com.ajesh.syncspend.ui.components.FlowToggle
import com.ajesh.syncspend.ui.components.NameIconDialog
import com.ajesh.syncspend.ui.components.PrimaryGradientButton
import com.ajesh.syncspend.ui.components.SheetDeleteButton
import com.ajesh.syncspend.ui.components.SheetHeader
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import com.ajesh.syncspend.util.DateUtils
import java.time.LocalDate
import kotlin.math.abs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Renders the Edit Entry sheet for whichever transaction is set on
 * [com.ajesh.syncspend.domain.state.SharedSelectionState.editingTransactionId].
 * Hosted once at the app root so it paints above the floating bottom nav.
 */
@Composable
fun EditEntryHost() {
    val container = LocalAppContainer.current
    val id by container.selectionState.editingTransactionId.collectAsStateWithLifecycle()
    id?.let { EditEntrySheet(it, onDismiss = { container.selectionState.editingTransactionId.value = null }) }
}

@Composable
private fun EditEntrySheet(transactionId: Long, onDismiss: () -> Unit) {
    val container = LocalAppContainer.current
    val colors = SyncSpendTheme.colors
    val scope = rememberCoroutineScope()

    val tx by produceState<TransactionEntity?>(null, transactionId) {
        value = container.transactionRepository.getById(transactionId).first()
    }
    val categories by container.categoryRepository.getAll().collectAsStateWithLifecycle(emptyList())
    val earliest by container.transactionRepository.getEarliestDate().collectAsStateWithLifecycle(null)
    val prefs by container.preferencesRepository.preferences.collectAsStateWithLifecycle(
        com.ajesh.syncspend.data.datastore.UserPreferences(),
    )

    val loaded = tx
    if (loaded == null) return

    var description by remember(loaded.id) { mutableStateOf(loaded.description) }
    var amountText by remember(loaded.id) { mutableStateOf(trimAmount(abs(loaded.amount))) }
    var date by remember(loaded.id) { mutableStateOf(loaded.date) }
    val originalType = if (loaded.amount > 0) FlowType.INCOME else FlowType.EXPENSE
    var type by remember(loaded.id) { mutableStateOf(originalType) }
    var categoryId by remember(loaded.id) { mutableStateOf<Long?>(loaded.categoryId) }
    var showDate by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showCreate by remember { mutableStateOf(false) }

    val isIncome = type == FlowType.INCOME
    // Expense and income categories never overlap. An archived category still shows while it is
    // this entry's own, so the entry's current label stays visible and selected.
    val flowCategories = categories.filter { it.type == type && (!it.archived || it.id == loaded.categoryId) }
    val parsedAmount = amountText.toDoubleOrNull()
    val canSave = parsedAmount != null && parsedAmount > 0 && categoryId != null

    DesignSheet(onDismiss = onDismiss) { close ->
        SheetHeader("Edit Entry", onClose = close)

        FlowToggle(
            type = type,
            onSelect = { newType ->
                if (newType != type) {
                    type = newType
                    // Categories don't carry across flows: start clean (returning to the original flow restores its category).
                    categoryId = if (newType == originalType) loaded.categoryId else null
                }
            },
            modifier = Modifier.padding(top = 14.dp),
        )

        FieldLabel("Description", top = 12.dp)
        DesignTextField(value = description, onValueChange = { description = it }, placeholder = "Description")

        Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                FieldLabel("Amount", top = 0.dp)
                DesignTextField(
                    value = amountText,
                    onValueChange = { v -> if (v.isEmpty() || v.matches(Regex("""\d*\.?\d{0,2}"""))) amountText = v },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textColor = if (isIncome) colors.pos else colors.neg,
                    placeholder = prefs.currencyCode.symbol + "0",
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                FieldLabel("Date", top = 0.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.tile, RoundedCornerShape(14.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showDate = true }
                        .padding(horizontal = 13.dp, vertical = 12.dp),
                ) {
                    Text(
                        "${DateUtils.shortDate(date)} ${date.year}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.ink,
                    )
                }
            }
        }

        FieldLabel("Category", top = 12.dp, bottom = 6.dp)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            flowCategories.forEach { cat ->
                val selected = cat.id == categoryId
                Box(
                    modifier = Modifier
                        .background(
                            if (selected) colors.selectedBrush else SolidColor(colors.pill),
                            RoundedCornerShape(15.dp),
                        )
                        .border(1.dp, colors.line, RoundedCornerShape(15.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { categoryId = cat.id }
                        .padding(horizontal = 13.dp, vertical = 7.dp),
                ) {
                    Text(
                        cat.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) colors.onSelected else colors.sub,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .background(colors.tile, RoundedCornerShape(15.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showCreate = true }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(SyncSpendIcons.Plus, null, tint = colors.ink, modifier = Modifier.size(12.dp))
                Text(
                    if (flowCategories.isEmpty()) "Add ${if (isIncome) "income" else "expense"} category" else "New",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.ink,
                )
            }
        }
        if (categoryId == null) {
            Text(
                "Pick a category to save this entry.",
                style = MaterialTheme.typography.labelSmall,
                color = colors.sub,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Row(modifier = Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            SheetDeleteButton(onClick = { showDelete = true })
            PrimaryGradientButton(
                text = "Save Changes",
                enabled = canSave,
                height = 48.dp,
                radius = 16.dp,
                modifier = Modifier.weight(1f),
                onClick = {
                    val v = parsedAmount ?: return@PrimaryGradientButton
                    val chosen = categoryId ?: return@PrimaryGradientButton
                    val category = categories.find { it.id == chosen }
                    scope.launch {
                        container.transactionRepository.update(
                            loaded.copy(
                                description = description.trim().ifEmpty { category?.name ?: loaded.description },
                                amount = if (isIncome) v else -v,
                                date = date,
                                categoryId = chosen,
                            ),
                        )
                        close()
                    }
                },
            )
        }

        if (showDate) {
            DatePickerSheet(
                initial = date,
                earliestTransactionDate = earliest,
                onApply = { date = it },
                onDismiss = { showDate = false },
            )
        }
        if (showCreate) {
            NameIconDialog(
                title = "New category",
                body = "It will be added to the ${if (isIncome) "income" else "expense"} list and used for this entry.",
                cta = "Create",
                initialName = "",
                initialIconKey = null,
                namePlaceholder = if (isIncome) "New Income category" else "New Expense category",
                onConfirm = { name, icon ->
                    showCreate = false
                    scope.launch { categoryId = container.categoryRepository.createNext(name, icon, type).id }
                },
                onDismiss = { showCreate = false },
            )
        }
        if (showDelete) {
            ConfirmDialog(
                title = "Delete this entry?",
                body = "Removing “${loaded.description}” cannot be undone.",
                cta = "Delete",
                onDismiss = { showDelete = false },
                onConfirm = {
                    showDelete = false
                    scope.launch {
                        container.transactionRepository.delete(loaded)
                        close()
                    }
                },
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String, top: androidx.compose.ui.unit.Dp, bottom: androidx.compose.ui.unit.Dp = 5.dp) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = SyncSpendTheme.colors.sub,
        modifier = Modifier.padding(top = top, bottom = bottom),
    )
}

private fun trimAmount(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else String.format(java.util.Locale.US, "%.2f", v)
