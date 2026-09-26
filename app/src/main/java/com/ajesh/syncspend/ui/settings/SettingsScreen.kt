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
import com.ajesh.syncspend.csv.CsvExporter
import com.ajesh.syncspend.csv.CsvImporter
import com.ajesh.syncspend.di.LocalAppContainer
import com.ajesh.syncspend.domain.analytics.AnalyticsEngine
import com.ajesh.syncspend.domain.model.CurrencyCode
import com.ajesh.syncspend.domain.model.ScopePeriod
import com.ajesh.syncspend.domain.model.ThemeMode
import com.ajesh.syncspend.ui.components.AnimatedSegmentedControl
import com.ajesh.syncspend.ui.components.DesignSwitch
import com.ajesh.syncspend.ui.components.InfoDialog
import com.ajesh.syncspend.ui.components.PeriodPickerSheet
import com.ajesh.syncspend.ui.components.PickerChip
import com.ajesh.syncspend.ui.components.rememberNotificationGate
import com.ajesh.syncspend.ui.components.SquareIconButton
import com.ajesh.syncspend.ui.components.SyncSpendChrome
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import com.ajesh.syncspend.util.DateUtils
import com.ajesh.syncspend.util.NativePickers
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
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
                    onDailyReminderChanged = { enabled, minute ->
                        if (enabled) container.alarmScheduler.scheduleDailyReminder(minute)
                        else container.alarmScheduler.cancelDailyReminder()
                    },
                )
            }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = state.prefs

    var exportPickerOpen by remember { mutableStateOf(false) }
    var pendingExportScope by remember { mutableStateOf<ScopePeriod>(ScopePeriod.AllTime) }
    var infoDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    val notificationGate = rememberNotificationGate()

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
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
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
            } catch (e: Exception) {
                infoDialog = "Import failed" to (e.message ?: "That file couldn't be read.")
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(top = SyncSpendChrome.screenTopInset)) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineSmall,
            color = colors.ink,
            modifier = Modifier.padding(horizontal = 22.dp),
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
                    height = 38.dp,
                    outerShape = RoundedCornerShape(14.dp),
                    innerShape = RoundedCornerShape(10.dp),
                    textStyle = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp),
                    modifier = Modifier.padding(top = 11.dp),
                )
                Text(
                    if (prefs.themeMode == ThemeMode.SYSTEM) {
                        "Following your device setting — currently ${if (colors.isDark) "dark" else "light"}"
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
            }

            SettingsCard(gradient = true) {
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
                        onClick = {
                            NativePickers.showTime(context, prefs.dailyReminderMinuteOfDay, colors.isDark) { minute ->
                                notificationGate { viewModel.setReminderTime(minute) }
                            }
                        },
                        size = 34.dp, radius = 12.dp, iconSize = 15.dp, background = colors.card,
                        modifier = Modifier.border(1.dp, colors.line, RoundedCornerShape(12.dp)),
                    )
                }
            }

            ActionCard(
                icon = SyncSpendIcons.Download,
                title = "Export CSV",
                note = state.exportNote,
                onClick = { exportPickerOpen = true },
            )
            ActionCard(
                icon = SyncSpendIcons.Upload,
                title = "Import CSV",
                note = "Works with SyncSpend exports and your old tracker's CSV",
                onClick = { importLauncher.launch(arrayOf("*/*")) },
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

    if (exportPickerOpen) {
        val current by container.selectionState.scope.collectAsStateWithLifecycle()
        val earliest by container.transactionRepository.getEarliestDate().collectAsStateWithLifecycle(null)
        PeriodPickerSheet(
            currentScope = current,
            earliestTransactionDate = earliest,
            onApply = { chosen ->
                pendingExportScope = chosen
                exportPickerOpen = false
                val stamp = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                exportLauncher.launch("syncspend_export_$stamp.csv")
            },
            onDismiss = { exportPickerOpen = false },
        )
    }
    infoDialog?.let { (title, body) -> InfoDialog(title, body, onDismiss = { infoDialog = null }) }
}

@Composable
private fun SettingsCard(gradient: Boolean = false, content: @Composable () -> Unit) {
    val colors = SyncSpendTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (gradient) colors.cardGradient else androidx.compose.ui.graphics.SolidColor(colors.card), RoundedCornerShape(18.dp))
            .border(1.dp, colors.line, RoundedCornerShape(18.dp))
            .padding(15.dp),
    ) { content() }
}

@Composable
private fun IconTile(icon: ImageVector) {
    Box(
        modifier = Modifier.size(32.dp).background(SyncSpendTheme.colors.tile, RoundedCornerShape(11.dp)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = SyncSpendTheme.colors.ink, modifier = Modifier.size(16.dp)) }
}

@Composable
private fun ActionCard(icon: ImageVector, title: String, note: String, onClick: () -> Unit) {
    val colors = SyncSpendTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.card, RoundedCornerShape(18.dp))
            .border(1.dp, colors.line, RoundedCornerShape(18.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        IconTile(icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = colors.ink)
            Text(note, fontSize = 10.5.sp, color = colors.sub, modifier = Modifier.padding(top = 2.dp))
        }
        Icon(SyncSpendIcons.Next, null, tint = colors.sub, modifier = Modifier.size(17.dp))
    }
}
