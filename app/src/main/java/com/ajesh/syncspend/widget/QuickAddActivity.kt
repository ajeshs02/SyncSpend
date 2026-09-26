package com.ajesh.syncspend.widget

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ajesh.syncspend.SyncSpendApp
import com.ajesh.syncspend.data.datastore.UserPreferences
import com.ajesh.syncspend.domain.model.FlowType
import com.ajesh.syncspend.domain.model.ThemeMode
import com.ajesh.syncspend.ui.addentry.AddEntryViewModel
import com.ajesh.syncspend.ui.components.AmountEntryPad
import com.ajesh.syncspend.ui.components.NameIconDialog
import com.ajesh.syncspend.ui.components.SquareIconButton
import com.ajesh.syncspend.ui.icons.SyncSpendIcons
import com.ajesh.syncspend.ui.theme.SyncSpendCorners
import com.ajesh.syncspend.ui.theme.SyncSpendTheme
import com.ajesh.syncspend.util.DateUtils
import com.ajesh.syncspend.util.NativePickers

/**
 * The widget's quick-add panel: a translucent overlay over the home screen with
 * the Add Entry pattern (amount, category, keypad with date key, save) locked
 * to Expense — the same lightweight "add without opening the app" feel as
 * Google Tasks' widget. Slides up, and slides back down before finishing.
 */
class QuickAddActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as SyncSpendApp).container
        setContent {
            val prefs by container.preferencesRepository.preferences.collectAsStateWithLifecycle(UserPreferences())
            val dark = when (prefs.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
                )
                onDispose {}
            }
            SyncSpendTheme(themeMode = prefs.themeMode) {
                QuickAddPanel(container = container, onFinished = {
                    finish()
                    @Suppress("DEPRECATION") overridePendingTransition(0, 0)
                })
            }
        }
    }
}

@Composable
private fun QuickAddPanel(container: com.ajesh.syncspend.di.AppContainer, onFinished: () -> Unit) {
    val viewModel: AddEntryViewModel = viewModel(
        factory = viewModelFactory {
            initializer { AddEntryViewModel(container.transactionRepository, container.categoryRepository, container.preferencesRepository) }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colors = SyncSpendTheme.colors
    val negative = colors.neg

    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    fun dismiss() { visible.targetState = false }
    LaunchedEffect(visible.currentState, visible.targetState) {
        if (!visible.targetState && !visible.currentState) onFinished()
    }
    BackHandler { dismiss() }

    var hint by remember { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66080E0A).copy(alpha = if (visible.targetState) 0.4f else 0f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = ::dismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visibleState = visible,
            enter = slideInVertically(tween(280)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(180)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.sheet, SyncSpendCorners.sheetTop)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .navigationBarsPadding()
                    .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(6.dp).background(negative, RoundedCornerShape(2.dp)))
                        Text("New expense", style = MaterialTheme.typography.titleMedium, color = colors.ink)
                    }
                    SquareIconButton(SyncSpendIcons.Close, { dismiss() }, size = 30.dp)
                }

                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(state.currencySymbol, fontSize = 22.sp, color = negative)
                        Text(state.amountText, fontSize = 42.sp, fontWeight = FontWeight.Medium, letterSpacing = (-1.2).sp, color = colors.ink)
                    }
                    Box(Modifier.padding(top = 10.dp).width(80.dp).height(2.dp).background(negative.copy(alpha = 0.5f), RoundedCornerShape(2.dp)))
                    Text(DateUtils.longDate(state.date), style = MaterialTheme.typography.bodySmall, color = colors.sub, modifier = Modifier.padding(top = 9.dp))
                }

                Row(
                    modifier = Modifier.padding(top = 16.dp).fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    state.categoriesForType.forEach { cat ->
                        val selected = state.selectedCategory?.id == cat.id
                        Row(
                            modifier = Modifier
                                .background(if (selected) colors.selectedBrush else SolidColor(colors.pill), RoundedCornerShape(15.dp))
                                .border(1.dp, colors.line, RoundedCornerShape(15.dp))
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    hint = null
                                    if (selected) viewModel.clearCategory() else viewModel.selectCategory(cat)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(SyncSpendIcons.iconFor(cat.iconKey), null, tint = if (selected) colors.onSelected else colors.ink, modifier = Modifier.size(14.dp))
                            Text(cat.name, style = MaterialTheme.typography.labelLarge, color = if (selected) colors.onSelected else colors.sub, maxLines = 1)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .background(colors.tile, RoundedCornerShape(15.dp))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showCreate = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(SyncSpendIcons.Plus, null, tint = colors.ink, modifier = Modifier.size(13.dp))
                        Text(if (state.categoriesForType.isEmpty()) "Add a category" else "New", style = MaterialTheme.typography.labelLarge, color = colors.ink)
                    }
                }

                hint?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = negative, modifier = Modifier.padding(top = 8.dp)) }
                Spacer(Modifier.height(if (hint == null) 16.dp else 8.dp))

                AmountEntryPad(
                    dateLabel = DateUtils.shortDate(state.date),
                    onDigit = { hint = null; viewModel.pressDigit(it) },
                    onBackspace = viewModel::pressBackspace,
                    onDateClick = { NativePickers.showDate(context, state.date, colors.isDark, viewModel::setDate) },
                )
                Row(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(colors.dark, RoundedCornerShape(18.dp))
                        .border(1.dp, colors.line, RoundedCornerShape(18.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            when {
                                state.selectedCategory == null -> hint = "Pick a category first."
                                (state.amountText.toDoubleOrNull() ?: 0.0) == 0.0 -> hint = "Enter an amount."
                                else -> viewModel.save {
                                    Toast.makeText(context, "Expense saved", Toast.LENGTH_SHORT).show()
                                    dismiss()
                                }
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Box(Modifier.size(7.dp).background(negative, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(8.dp))
                    Text("Save Entry", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }

    if (showCreate) {
        NameIconDialog(
            title = "New category",
            body = "Name it and pick an icon — it will be added to the expense list.",
            cta = "Create",
            initialName = "",
            initialIconKey = "receipt",
            namePlaceholder = "New Expense ${state.categoriesForType.size + 1}",
            onConfirm = { name, icon ->
                showCreate = false
                viewModel.createCategory(name, icon) { hint = null }
            },
            onDismiss = { showCreate = false },
        )
    }
}
