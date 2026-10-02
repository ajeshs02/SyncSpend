package com.ajesh.syncspend.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ajesh.syncspend.csv.BundleExporter
import com.ajesh.syncspend.csv.BundleImporter
import com.ajesh.syncspend.csv.BundleSection
import com.ajesh.syncspend.csv.CsvExporter
import com.ajesh.syncspend.csv.CsvImporter
import com.ajesh.syncspend.di.LocalAppContainer
import com.ajesh.syncspend.domain.analytics.AnalyticsEngine
import com.ajesh.syncspend.domain.model.CurrencyCode
import com.ajesh.syncspend.domain.model.ScopePeriod
import com.ajesh.syncspend.domain.model.ThemeMode
import com.ajesh.syncspend.ui.components.AnimatedSegmentedControl
import com.ajesh.syncspend.ui.components.ConfirmDialog
import com.ajesh.syncspend.ui.components.DesignSwitch
import com.ajesh.syncspend.ui.components.InfoDialog
import com.ajesh.syncspend.ui.components.PeriodPickerSheet
import com.ajesh.syncspend.ui.components.PickerChip
import com.ajesh.syncspend.ui.components.rememberNotificationGate
import com.ajesh.syncspend.ui.components.SquareIconButton
import com.ajesh.syncspend.ui.components.TimePickerSheet
import com.ajesh.syncspend.ui.components.SyncSpendChrome
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendCorners
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import com.ajesh.syncspend.util.DateUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onOpenCategories: () -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = SyncSpendTheme.colors
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    container.preferencesRepository,
                    container.transactionRepository,
                    container.selectionState,
                    container.dailyReminderManager,
                )
            }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = state.prefs

    var exportPickerOpen by remember { mutableStateOf(false) }
    var exportSectionsOpen by remember { mutableStateOf(false) }
    var selectedSections by remember { mutableStateOf(setOf(BundleSection.TRANSACTIONS)) }
    var pendingExportScope by remember { mutableStateOf<ScopePeriod>(ScopePeriod.AllTime) }
    var infoDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var timePickerOpen by remember { mutableStateOf(false) }
    var confirmClearAll by remember { mutableStateOf(false) }
    val notificationGate = rememberNotificationGate()

    // The common "just transactions" case keeps writing the exact same plain CSV every prior round
    // has produced; any other combination (more than one type, or any non-transaction type) writes
    // the richer JSON bundle instead — see BundleExporter's own doc for why.
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val all = container.transactionRepository.getAll().first()
                val cats = container.categoryRepository.getAll().first()
                val rows = CsvExporter.export(context, uri, AnalyticsEngine.scopeFilter(all, pendingExportScope), cats)
                infoDialog = "Export complete" to "$rows entries saved as CSV."
            } catch (e: Exception) {
                infoDialog = "Export failed" to (e.message ?: "Something went wrong writing the file.")
            }
        }
    }
    val bundleExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val cats = container.categoryRepository.getAll().first()
                val tx = if (BundleSection.TRANSACTIONS in selectedSections) {
                    AnalyticsEngine.scopeFilter(container.transactionRepository.getAll().first(), pendingExportScope)
                } else emptyList()
                val reminders = if (BundleSection.REMINDERS in selectedSections) container.reminderRepository.getAll().first() else emptyList()
                val subs = if (BundleSection.SUBSCRIPTIONS in selectedSections) container.subscriptionRepository.getAll().first() else emptyList()
                val forecasts = if (BundleSection.FORECASTS in selectedSections) container.forecastRepository.getAll().first() else emptyList()
                BundleExporter.export(context, uri, selectedSections, tx, cats, reminders, subs, forecasts)
                infoDialog = "Export complete" to "Your data was saved."
            } catch (e: Exception) {
                infoDialog = "Export failed" to (e.message ?: "Something went wrong writing the file.")
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                // Sniff the file's first non-blank character: a bundle is a JSON object ("{"), the
                // established plain export is a CSV header row — route to whichever importer matches.
                val head = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                val looksLikeBundle = head != null && String(head, Charsets.UTF_8).trimStart().startsWith("{")
                if (looksLikeBundle) {
                    val r = BundleImporter.import(context, uri, container.database)
                    if (r.error != null) {
                        infoDialog = "Import failed" to r.error
                    } else {
                        val parts = buildString {
                            if (r.importedTransactions > 0) append("\n${r.importedTransactions} transactions.")
                            if (r.importedReminders > 0) append("\n${r.importedReminders} reminders.")
                            if (r.importedSubscriptions > 0) append("\n${r.importedSubscriptions} subscriptions.")
                            if (r.importedForecasts > 0) append("\n${r.importedForecasts} forecasts.")
                            if (r.createdCategories > 0) append("\n${r.createdCategories} new categories were created.")
                        }
                        val total = r.importedTransactions + r.importedReminders + r.importedSubscriptions + r.importedForecasts
                        infoDialog = (if (total > 0) "Import complete" else "Already up to date") to
                            (if (parts.isEmpty()) "Nothing new to import." else "Imported:$parts")
                    }
                } else {
                    val r = CsvImporter.import(context, uri, container.database)
                    val extra = buildString {
                        if (r.duplicates > 0) append("\n${r.duplicates} already in your data, skipped.")
                        if (r.skipped > 0) append("\n${r.skipped} rows couldn't be read.")
                        if (r.createdCategories > 0) append("\n${r.createdCategories} new categories were created.")
                        if (r.problems.isNotEmpty()) append("\n\n" + r.problems.joinToString("\n"))
                    }
                    val title = when {
                        r.imported > 0 -> "Import complete"
                        r.duplicates > 0 -> "Already up to date"
                        else -> "Nothing imported"
                    }
                    infoDialog = title to "${r.imported} entries imported.$extra"
                }
            } catch (e: Exception) {
                infoDialog = "Import failed" to (e.message ?: "That file couldn't be read.")
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(top = SyncSpendChrome.screenTopInset)) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 23.sp),
            color = colors.ink,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 3.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = SyncSpendChrome.screenBottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SettingsCard {
                Text("Appearance", style = MaterialTheme.typography.labelLarge, color = colors.ink)
                AnimatedSegmentedControl(
                    options = listOf("Light", "Dark", "System"),
                    selectedIndex = prefs.themeMode.ordinal,
                    onSelect = { viewModel.setTheme(ThemeMode.entries[it]) },
                    height = 42.dp,
                    outerShape = RoundedCornerShape(14.dp),
                    innerShape = RoundedCornerShape(10.dp),
                    textStyle = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp),
                    modifier = Modifier.padding(top = 11.dp),
                )
                Text(
                    if (prefs.themeMode == ThemeMode.SYSTEM) {
                        "Following your device setting, currently ${if (colors.isDark) "dark" else "light"}"
                    } else "Set manually",
                    fontSize = 10.sp,
                    color = colors.sub,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            SettingsCard {
                Text("Currency", style = MaterialTheme.typography.labelLarge, color = colors.ink)
                Row(modifier = Modifier.padding(top = 11.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    CurrencyCode.entries.forEach { code ->
                        PickerChip(
                            label = code.symbol,
                            selected = prefs.currencyCode == code,
                            modifier = Modifier.weight(1f),
                            vertical = 10.dp,
                            radius = 13.dp,
                        ) { viewModel.setCurrency(code) }
                    }
                }
                Row(
                    modifier = Modifier.padding(top = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Allow paise / cents on entries", style = MaterialTheme.typography.labelLarge, color = colors.ink)
                        Text(
                            "Adds a decimal key to the Add Entry keypad. An amount already saved with decimals always shows them.",
                            fontSize = 10.5.sp,
                            color = colors.sub,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    DesignSwitch(
                        checked = prefs.allowDecimalInput,
                        onCheckedChange = { viewModel.setAllowDecimalInput(it) },
                    )
                }
            }

            ActionCard(
                icon = SyncSpendIcons.Layers,
                title = "Categories",
                note = "Manage your spending categories",
                onClick = onOpenCategories,
            )

            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    IconTile(SyncSpendIcons.Clock)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Daily log reminder", style = MaterialTheme.typography.labelLarge, color = colors.ink)
                        Text(
                            if (prefs.dailyReminderEnabled) "Nudges you at ${DateUtils.fmt12(prefs.dailyReminderMinuteOfDay)} daily" else "Turned off",
                            fontSize = 10.5.sp,
                            color = colors.sub,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    DesignSwitch(
                        checked = prefs.dailyReminderEnabled,
                        onCheckedChange = { on ->
                            if (on) notificationGate { viewModel.setReminderEnabled(true) }
                            else viewModel.setReminderEnabled(false)
                        },
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .alpha(if (prefs.dailyReminderEnabled) 1f else 0.45f)
                            .background(colors.selectedBrush, RoundedCornerShape(12.dp))
                            .border(1.dp, colors.line, RoundedCornerShape(12.dp))
                            .padding(horizontal = 15.dp, vertical = 9.dp),
                    ) {
                        Text(DateUtils.fmt12(prefs.dailyReminderMinuteOfDay), fontSize = 12.sp, color = colors.onSelected, style = MaterialTheme.typography.labelLarge)
                    }
                    SquareIconButton(
                        SyncSpendIcons.Pencil,
                        onClick = { timePickerOpen = true },
                        size = 34.dp, radius = 12.dp, iconSize = 15.dp, background = colors.card,
                        modifier = Modifier.border(1.dp, colors.line, RoundedCornerShape(12.dp)),
                    )
                }
            }

            ActionCard(
                icon = SyncSpendIcons.Upload,
                title = "Import data",
                note = "Works with SyncSpend's CSV and backup exports, and your old tracker's CSV",
                onClick = { importLauncher.launch(arrayOf("*/*")) },
            )
            ActionCard(
                icon = SyncSpendIcons.Download,
                title = "Export data",
                note = state.exportNote,
                onClick = { exportSectionsOpen = true },
            )

            ActionCard(
                icon = SyncSpendIcons.Trash,
                title = "Clear all data",
                note = "Permanently delete transactions, categories, subscriptions, reminders and forecasts",
                danger = true,
                onClick = { confirmClearAll = true },
            )

            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Version", style = MaterialTheme.typography.labelLarge, color = colors.ink)
                    Text("SyncSpend 1.0", style = MaterialTheme.typography.bodySmall, color = colors.sub)
                }
            }
        }
    }

    if (confirmClearAll) {
        ConfirmDialog(
            title = "Clear all data?",
            body = "This permanently deletes every transaction, category, subscription, reminder and forecast. " +
                "Export your data first if you want to keep a copy — this can't be undone.",
            cta = "Clear all data",
            onConfirm = {
                confirmClearAll = false
                scope.launch {
                    container.clearAllData()
                    infoDialog = "All data cleared" to "Your transactions, categories, subscriptions, reminders and forecasts were removed. Default categories were restored."
                }
            },
            onDismiss = { confirmClearAll = false },
        )
    }

    if (exportSectionsOpen) {
        ExportSectionsDialog(
            selected = selectedSections,
            onToggle = { section, on -> selectedSections = if (on) selectedSections + section else selectedSections - section },
            onContinue = {
                exportSectionsOpen = false
                if (BundleSection.TRANSACTIONS in selectedSections) {
                    exportPickerOpen = true
                } else {
                    val stamp = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                    bundleExportLauncher.launch("syncspend_backup_$stamp.json")
                }
            },
            onDismiss = { exportSectionsOpen = false },
        )
    }
    if (exportPickerOpen) {
        val current by container.selectionState.scope.collectAsStateWithLifecycle()
        val earliest by container.transactionRepository.getEarliestDate().collectAsStateWithLifecycle(null)
        PeriodPickerSheet(
            currentScope = current,
            earliestTransactionDate = earliest,
            showAllTime = true,
            onApply = { chosen ->
                pendingExportScope = chosen
                exportPickerOpen = false
                val stamp = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                // Transactions alone -> the plain CSV every prior round already produces; anything
                // else selected alongside it -> the richer JSON bundle.
                if (selectedSections == setOf(BundleSection.TRANSACTIONS)) {
                    exportLauncher.launch("syncspend_export_$stamp.csv")
                } else {
                    bundleExportLauncher.launch("syncspend_backup_$stamp.json")
                }
            },
            onDismiss = { exportPickerOpen = false },
        )
    }
    if (timePickerOpen) {
        TimePickerSheet(
            initialMinuteOfDay = prefs.dailyReminderMinuteOfDay,
            title = "Daily reminder time",
            onApply = { minute -> notificationGate { viewModel.setReminderTime(minute) } },
            onDismiss = { timePickerOpen = false },
        )
    }
    infoDialog?.let { (title, body) -> InfoDialog(title, body, onDismiss = { infoDialog = null }) }
}

/** Checkbox list for export scope: any combination of data types, not just "one type" or "everything." */
@Composable
private fun ExportSectionsDialog(
    selected: Set<BundleSection>,
    onToggle: (BundleSection, Boolean) -> Unit,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = SyncSpendTheme.colors
    val options = listOf(
        BundleSection.TRANSACTIONS to "Transactions",
        BundleSection.REMINDERS to "Reminders",
        BundleSection.SUBSCRIPTIONS to "Subscriptions & EMIs",
        BundleSection.FORECASTS to "Forecast",
    )
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.sheet, SyncSpendCorners.card)
                .border(1.dp, colors.line, SyncSpendCorners.card)
                .padding(20.dp),
        ) {
            Text("What should we export?", style = MaterialTheme.typography.titleSmall, color = colors.ink)
            Text(
                "Pick any combination. Picking only Transactions keeps the plain CSV format; anything else saves a backup file.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.sub,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            options.forEach { (section, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onToggle(section, section !in selected) },
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.ink, modifier = Modifier.weight(1f))
                    DesignSwitch(checked = section in selected, onCheckedChange = { onToggle(section, it) })
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(colors.card, RoundedCornerShape(15.dp))
                        .border(1.dp, colors.line, RoundedCornerShape(15.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Cancel", style = MaterialTheme.typography.labelLarge, color = colors.ink) }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(colors.button.copy(alpha = if (selected.isEmpty()) 0.4f else 1f), RoundedCornerShape(15.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = selected.isNotEmpty(),
                            onClick = onContinue,
                        )
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Continue", style = MaterialTheme.typography.labelLarge, color = colors.onButton) }
            }
        }
    }
}

@Composable
private fun SettingsCard(gradient: Boolean = false, content: @Composable () -> Unit) {
    val colors = SyncSpendTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (gradient) colors.cardGradient else androidx.compose.ui.graphics.SolidColor(colors.card), SyncSpendCorners.card)
            .border(1.dp, colors.line, SyncSpendCorners.card)
            .padding(18.dp),
    ) { content() }
}

@Composable
private fun IconTile(icon: ImageVector, tint: androidx.compose.ui.graphics.Color = SyncSpendTheme.colors.ink) {
    Box(
        modifier = Modifier.size(32.dp).background(SyncSpendTheme.colors.tile, RoundedCornerShape(11.dp)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp)) }
}

@Composable
private fun ActionCard(icon: ImageVector, title: String, note: String, danger: Boolean = false, onClick: () -> Unit) {
    val colors = SyncSpendTheme.colors
    val accent = if (danger) colors.neg else colors.ink
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.card, SyncSpendCorners.card)
            .border(1.dp, colors.line, SyncSpendCorners.card)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        IconTile(icon, tint = accent)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = accent)
            Text(note, fontSize = 10.5.sp, color = colors.sub, modifier = Modifier.padding(top = 2.dp))
        }
        Icon(SyncSpendIcons.Next, null, tint = colors.sub, modifier = Modifier.size(17.dp))
    }
}
